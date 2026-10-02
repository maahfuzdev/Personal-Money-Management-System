package com.maahfuzdev.moneymanager.auth;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@Service
public class AuthService {

    private final AppUserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final Clock clock;
    private final JwtDecoder googleIdTokenDecoder;
    private final String issuer;
    private final Duration tokenLifetime;
    private final Duration refreshTokenLifetime;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(
            AppUserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtEncoder jwtEncoder,
            @Qualifier("googleIdTokenDecoder") JwtDecoder googleIdTokenDecoder,
            Clock clock,
            @Value("${JWT_ISSUER:personal-money-manager}") String issuer,
            @Value("${JWT_ACCESS_TOKEN_MINUTES:15}") long accessTokenMinutes,
            @Value("${JWT_REFRESH_TOKEN_DAYS:30}") long refreshTokenDays) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.googleIdTokenDecoder = googleIdTokenDecoder;
        this.clock = clock;
        this.issuer = issuer;
        this.tokenLifetime = Duration.ofMinutes(accessTokenMinutes);
        this.refreshTokenLifetime = Duration.ofDays(refreshTokenDays);
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        AppUser user = userRepository.save(new AppUser(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password())
        ));
        return issueTokens(user, UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, request.password()));
        } catch (AuthenticationException exception) {
            throw new InvalidCredentialsException();
        }

        AppUser user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);
        return issueTokens(user, UUID.randomUUID().toString());
    }

    @Transactional
    public AuthResponse googleSignIn(String idToken) {
        org.springframework.security.oauth2.jwt.Jwt googleToken;
        try {
            googleToken = googleIdTokenDecoder.decode(idToken);
        } catch (org.springframework.security.oauth2.jwt.JwtException exception) {
            throw new InvalidCredentialsException();
        }
        if (!Boolean.TRUE.equals(googleToken.getClaim("email_verified"))) {
            throw new InvalidCredentialsException();
        }
        String verifiedEmail = googleToken.getClaimAsString("email");
        if (verifiedEmail == null || verifiedEmail.isBlank()) throw new InvalidCredentialsException();
        String email = normalizeEmail(verifiedEmail);
        AppUser user = userRepository.findByEmail(email).orElseGet(() -> {
            String name = googleToken.getClaimAsString("name");
            if (name == null || name.isBlank()) name = email.substring(0, email.indexOf('@'));
            return userRepository.save(new AppUser(name.substring(0, Math.min(name.length(), 100)), email,
                    passwordEncoder.encode(UUID.randomUUID().toString())));
        });
        return issueTokens(user, UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
    public AuthResponse refresh(RefreshRequest request) {
        Instant now = clock.instant();
        RefreshToken storedToken = refreshTokenRepository.findForUpdateByTokenHash(hashToken(request.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (storedToken.getRevokedAt() != null) {
            refreshTokenRepository.revokeActiveFamily(storedToken.getFamilyId(), now);
            throw new InvalidRefreshTokenException();
        }
        if (storedToken.isExpiredAt(now)) {
            storedToken.revoke(now, null);
            throw new InvalidRefreshTokenException();
        }

        String nextRefreshToken = newRefreshToken();
        String nextRefreshHash = hashToken(nextRefreshToken);
        storedToken.revoke(now, nextRefreshHash);
        refreshTokenRepository.save(storedToken);
        return issueTokens(storedToken.getUser(), storedToken.getFamilyId(), nextRefreshToken, now);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        Instant now = clock.instant();
        refreshTokenRepository.findForUpdateByTokenHash(hashToken(request.refreshToken()))
                .ifPresent(token -> refreshTokenRepository.revokeActiveFamily(token.getFamilyId(), now));
    }

    @Transactional(readOnly = true)
    public UserSummary currentUser(String email) {
        AppUser user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(InvalidCredentialsException::new);
        return new UserSummary(user.getId(), user.getName(), user.getEmail());
    }

    private AuthResponse issueTokens(AppUser user, String familyId) {
        Instant issuedAt = clock.instant();
        return issueTokens(user, familyId, newRefreshToken(), issuedAt);
    }

    private AuthResponse issueTokens(AppUser user, String familyId, String rawRefreshToken, Instant issuedAt) {
        Instant expiresAt = issuedAt.plus(tokenLifetime);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .subject(user.getEmail())
                .claim("scope", "user")
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        refreshTokenRepository.deleteByExpiresAtBefore(issuedAt);
        refreshTokenRepository.save(new RefreshToken(
                user,
                hashToken(rawRefreshToken),
                familyId,
                issuedAt,
                issuedAt.plus(refreshTokenLifetime)
        ));
        return new AuthResponse(
                token,
                "Bearer",
                tokenLifetime.toSeconds(),
                rawRefreshToken,
                refreshTokenLifetime.toSeconds(),
                new UserSummary(user.getId(), user.getName(), user.getEmail())
        );
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
