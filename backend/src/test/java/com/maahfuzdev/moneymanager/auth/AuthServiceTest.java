package com.maahfuzdev.moneymanager.auth;

import com.maahfuzdev.moneymanager.user.AppUser;
import com.maahfuzdev.moneymanager.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

    @Mock private AppUserRepository users;
    @Mock private RefreshTokenRepository refreshTokens;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtEncoder jwtEncoder;

    private AuthService service;

    @BeforeEach
    void setUp() {
        service = new AuthService(users, refreshTokens, passwordEncoder, authenticationManager, jwtEncoder,
                Clock.fixed(NOW, ZoneOffset.UTC), "personal-money-manager", 15, 30);
    }

    private void stubJwtEncoder() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(Jwt.withTokenValue("access-token").header("alg", "HS256")
                        .claim("sub", "amina@example.com").build());
    }

    @Test
    void registerNormalizesEmailAndStoresOnlyHashedRefreshToken() throws Exception {
        stubJwtEncoder();
        when(users.existsByEmail("amina@example.com")).thenReturn(false);
        when(passwordEncoder.encode("a-long-password")).thenReturn("password-hash");
        when(users.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = service.register(new RegisterRequest(" Amina ", "AMINA@example.com", "a-long-password"));

        assertEquals("access-token", response.accessToken());
        assertEquals(900, response.expiresIn());
        assertEquals(2_592_000, response.refreshExpiresIn());
        assertEquals("amina@example.com", response.user().email());
        assertEquals(43, response.refreshToken().length());

        ArgumentCaptor<RefreshToken> savedToken = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens).save(savedToken.capture());
        assertEquals(hash(response.refreshToken()), savedToken.getValue().getTokenHash());
        assertNotEquals(response.refreshToken(), savedToken.getValue().getTokenHash());
    }

    @Test
    void refreshRotatesTokenAndKeepsItsSessionFamily() throws Exception {
        stubJwtEncoder();
        AppUser user = new AppUser("Amina", "amina@example.com", "password-hash");
        String familyId = UUID.randomUUID().toString();
        RefreshToken current = new RefreshToken(user, hash("old-refresh-token"), familyId,
                NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(refreshTokens.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(current));

        AuthResponse response = service.refresh(new RefreshRequest("old-refresh-token"));

        assertEquals(NOW, current.getRevokedAt());
        assertNotEquals("old-refresh-token", response.refreshToken());
        ArgumentCaptor<RefreshToken> savedTokens = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokens, times(2)).save(savedTokens.capture());
        assertEquals(familyId, savedTokens.getAllValues().get(1).getFamilyId());
        assertEquals(hash(response.refreshToken()), savedTokens.getAllValues().get(1).getTokenHash());
    }

    @Test
    void reuseOfRevokedRefreshTokenRevokesItsWholeFamily() throws Exception {
        String familyId = UUID.randomUUID().toString();
        RefreshToken reused = new RefreshToken(new AppUser("Amina", "amina@example.com", "hash"),
                hash("replayed-token"), familyId, NOW.minusSeconds(60), NOW.plusSeconds(3600));
        reused.revoke(NOW.minusSeconds(10), "replacement-hash");
        when(refreshTokens.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(reused));

        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(new RefreshRequest("replayed-token")));

        verify(refreshTokens).revokeActiveFamily(familyId, NOW);
    }

    @Test
    void expiredRefreshTokenIsRejectedAndMarkedRevoked() throws Exception {
        RefreshToken expired = new RefreshToken(new AppUser("Amina", "amina@example.com", "hash"),
                hash("expired-token"), UUID.randomUUID().toString(), NOW.minusSeconds(60), NOW);
        when(refreshTokens.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(expired));

        assertThrows(InvalidRefreshTokenException.class, () -> service.refresh(new RefreshRequest("expired-token")));

        assertEquals(NOW, expired.getRevokedAt());
    }

    @Test
    void logoutRevokesActiveRefreshTokenFamily() throws Exception {
        String familyId = UUID.randomUUID().toString();
        RefreshToken active = new RefreshToken(new AppUser("Amina", "amina@example.com", "hash"),
                hash("logout-token"), familyId, NOW.minusSeconds(60), NOW.plusSeconds(3600));
        when(refreshTokens.findForUpdateByTokenHash(anyString())).thenReturn(Optional.of(active));

        service.logout(new RefreshRequest("logout-token"));

        verify(refreshTokens).revokeActiveFamily(familyId, NOW);
    }

    private static String hash(String token) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}
