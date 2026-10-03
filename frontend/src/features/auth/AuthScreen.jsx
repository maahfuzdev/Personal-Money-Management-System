import { cloneElement, useEffect, useRef, useState } from 'react'
import { registerAccount, signIn, signInWithGoogle } from '../../api/authApi.js'

function AuthScreen({ onAuthenticated }) {
  const [mode, setMode] = useState('login')
  const [name, setName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [fieldErrors, setFieldErrors] = useState({})
  const [isSubmitting, setIsSubmitting] = useState(false)
  const googleButton = useRef(null)
  const googleClientId = import.meta.env.VITE_GOOGLE_CLIENT_ID

  useEffect(() => {
    if (!googleClientId || !googleButton.current) return undefined
    let attempts = 0
    const renderGoogleButton = () => {
      if (window.google?.accounts?.id && googleButton.current) {
        window.google.accounts.id.initialize({ client_id: googleClientId, callback: async ({ credential }) => {
          setError('')
          setIsSubmitting(true)
          try {
            const response = await signInWithGoogle(credential)
            onAuthenticated({ token: response.accessToken, refreshToken: response.refreshToken, user: response.user })
          } catch (requestError) {
            setError(requestError.message)
          } finally {
            setIsSubmitting(false)
          }
        } })
        window.google.accounts.id.renderButton(googleButton.current, { theme: 'outline', size: 'large', shape: 'rectangular', text: 'continue_with', width: Math.min(360, googleButton.current.clientWidth) })
      } else if (attempts++ < 30) window.setTimeout(renderGoogleButton, 100)
    }
    renderGoogleButton()
    return () => { if (googleButton.current) googleButton.current.replaceChildren() }
  }, [googleClientId, onAuthenticated])

  const isRegistering = mode === 'register'

  async function handleSubmit(event) {
    event.preventDefault()
    setError('')
    setFieldErrors({})
    setIsSubmitting(true)

    try {
      const response = isRegistering
        ? await registerAccount({ name: name.trim(), email: email.trim(), password })
        : await signIn({ email: email.trim(), password })
      onAuthenticated({ token: response.accessToken, refreshToken: response.refreshToken, user: response.user })
    } catch (requestError) {
      setError(requestError.message)
      setFieldErrors(requestError.fieldErrors || {})
    } finally {
      setIsSubmitting(false)
    }
  }

  function changeMode(nextMode) {
    setMode(nextMode)
    setError('')
    setFieldErrors({})
  }

  return (
    <div className="auth-panel">
      <div className="auth-heading">
        <p className="eyebrow">WELCOME {isRegistering ? 'ABOARD' : 'BACK'}</p>
        <h2>{isRegistering ? 'Create your account' : 'Sign in to your space'}</h2>
        <p>{isRegistering ? 'A few details and you’re ready to begin.' : 'Pick up where you left off.'}</p>
      </div>

      <div className="auth-tabs" role="tablist" aria-label="Account action">
        <button
          className={mode === 'login' ? 'auth-tab active' : 'auth-tab'}
          id="login-tab"
          type="button"
          role="tab"
          aria-selected={mode === 'login'}
          aria-controls="auth-form-panel"
          onClick={() => changeMode('login')}
        >
          Sign in
        </button>
        <button
          className={mode === 'register' ? 'auth-tab active' : 'auth-tab'}
          id="register-tab"
          type="button"
          role="tab"
          aria-selected={mode === 'register'}
          aria-controls="auth-form-panel"
          onClick={() => changeMode('register')}
        >
          Create account
        </button>
      </div>

      <form
        id="auth-form-panel"
        className="auth-form"
        role="tabpanel"
        aria-labelledby={isRegistering ? 'register-tab' : 'login-tab'}
        onSubmit={handleSubmit}
        noValidate
      >
        {error && <div className="form-alert" role="alert">{error}</div>}

        {isRegistering && (
          <FormField label="Your name" error={fieldErrors.name}>
            <input
              autoComplete="name"
              maxLength={100}
              name="name"
              onChange={(event) => setName(event.target.value)}
              placeholder="e.g. Amina Rahman"
              required
              value={name}
            />
          </FormField>
        )}

        <FormField label="Email address" error={fieldErrors.email}>
          <input
            autoComplete="email"
            maxLength={254}
            name="email"
            onChange={(event) => setEmail(event.target.value)}
            placeholder="you@example.com"
            required
            type="email"
            value={email}
          />
        </FormField>

        <FormField
          label="Password"
          error={fieldErrors.password}
          hint={isRegistering ? 'Use at least 12 characters.' : undefined}
        >
          <input
            autoComplete={isRegistering ? 'new-password' : 'current-password'}
            maxLength={128}
            minLength={isRegistering ? 12 : undefined}
            name="password"
            onChange={(event) => setPassword(event.target.value)}
            placeholder={isRegistering ? 'At least 12 characters' : 'Enter your password'}
            required
            type="password"
            value={password}
          />
        </FormField>

        <button className="submit-button" disabled={isSubmitting} type="submit">
          {isSubmitting ? 'Please wait…' : isRegistering ? 'Create my account' : 'Sign in'}
          {!isSubmitting && <span aria-hidden="true">→</span>}
        </button>
      </form>

      <div className="google-auth-area">
        <div className="auth-divider"><span>or {isRegistering ? 'create an account' : 'continue'} with</span></div>
        {googleClientId
          ? <div className="google-button" ref={googleButton} />
          : <button className="google-button google-unconfigured" type="button" disabled>Continue with Google <span>Google sign-in is not configured yet</span></button>}
      </div>

      <p className="auth-privacy">
        Stay signed in on this device for up to 30 days. Sign out to end your session here.
      </p>
    </div>
  )
}

function FormField({ children, error, hint, label }) {
  const fieldId = label.toLowerCase().replaceAll(' ', '-')
  const errorId = `${fieldId}-error`
  const input = cloneElement(children, {
    id: fieldId,
    'aria-invalid': Boolean(error),
    'aria-describedby': error ? errorId : undefined,
  })

  return (
    <div className="form-field">
      <label htmlFor={fieldId}>{label}</label>
      {input}
      {hint && <span className="field-hint">{hint}</span>}
      {error && <span className="field-error" id={errorId}>{error}</span>}
    </div>
  )
}

export default AuthScreen
