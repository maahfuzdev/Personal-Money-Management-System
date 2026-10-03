import { useCallback, useEffect, useRef, useState } from 'react'
import { logoutSession, refreshSession } from './api/authApi.js'
import { setRefreshToken } from './api/transactionApi.js'
import AuthScreen from './features/auth/AuthScreen.jsx'
import Dashboard from './features/dashboard/Dashboard.jsx'

let sessionRestoreInFlight = null

function restoreSession(refreshToken) {
  if (!sessionRestoreInFlight) {
    sessionRestoreInFlight = refreshSession(refreshToken).finally(() => {
      sessionRestoreInFlight = null
    })
  }
  return sessionRestoreInFlight
}

function App() {
  const [session, setSession] = useState(null)
  const [isRestoringSession, setIsRestoringSession] = useState(true)
  const sessionRef = useRef(null)
  const [path, setPath] = useState(window.location.pathname)

  const navigate = useCallback((nextPath) => {
    if (window.location.pathname !== nextPath) window.history.pushState({}, '', nextPath)
    setPath(nextPath)
    window.scrollTo({ top: 0, behavior: 'auto' })
  }, [])

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    const onSessionRefreshed = (event) => {
      if (!sessionRef.current) return
      const nextSession = {
        ...sessionRef.current,
        token: event.detail.accessToken,
        refreshToken: event.detail.refreshToken,
      }
      sessionRef.current = nextSession
      setRefreshToken(nextSession.refreshToken)
      setSession(nextSession)
    }
    const onSessionExpired = () => {
      window.localStorage.removeItem('moneywise.refreshToken')
      sessionRef.current = null
      setRefreshToken(null)
      setSession(null)
      navigate('/login')
    }
    window.addEventListener('popstate', onPopState)
    window.addEventListener('auth:session-refreshed', onSessionRefreshed)
    window.addEventListener('auth:session-expired', onSessionExpired)
    return () => {
      window.removeEventListener('popstate', onPopState)
      window.removeEventListener('auth:session-refreshed', onSessionRefreshed)
      window.removeEventListener('auth:session-expired', onSessionExpired)
    }
  }, [navigate])

  useEffect(() => {
    let cancelled = false
    const savedRefreshToken = window.localStorage.getItem('moneywise.refreshToken')

    if (!savedRefreshToken) {
      setIsRestoringSession(false)
      return () => { cancelled = true }
    }

    setRefreshToken(savedRefreshToken)
    restoreSession(savedRefreshToken)
      .then((tokens) => {
        if (cancelled) return
        window.localStorage.setItem('moneywise.refreshToken', tokens.refreshToken)
        const restoredSession = {
          token: tokens.accessToken,
          refreshToken: tokens.refreshToken,
          user: tokens.user,
        }
        sessionRef.current = restoredSession
        setRefreshToken(tokens.refreshToken)
        setSession(restoredSession)
      })
      .catch(() => {
        if (!cancelled) {
          window.localStorage.removeItem('moneywise.refreshToken')
          setRefreshToken(null)
        }
      })
      .finally(() => { if (!cancelled) setIsRestoringSession(false) })

    return () => { cancelled = true }
  }, [])

  const handleAuthenticated = useCallback((nextSession) => {
    window.localStorage.setItem('moneywise.refreshToken', nextSession.refreshToken)
    sessionRef.current = nextSession
    setRefreshToken(nextSession.refreshToken)
    setSession(nextSession)
    navigate('/')
  }, [navigate])

  const handleSignOut = useCallback(() => {
    const currentSession = sessionRef.current
    if (currentSession?.refreshToken) void logoutSession(currentSession.refreshToken)
    window.localStorage.removeItem('moneywise.refreshToken')
    sessionRef.current = null
    setRefreshToken(null)
    setSession(null)
    navigate('/login')
  }, [navigate])

  if (isRestoringSession) return <main className="app-shell" aria-live="polite">Restoring your session…</main>

  if (session) return <Dashboard session={session} onSignOut={handleSignOut} navigate={navigate} path={path} />

  return (
    <main className="app-shell">
      <section className="auth-layout" aria-label="Account access">
        <div className="story-panel">
          <Brand />
          <div className="story-content">
            <p className="eyebrow">PERSONAL FINANCE, MADE CLEAR</p>
            <h1>Make room for the things that matter.</h1>
            <p className="story-copy">Bring your everyday money into focus and build habits that feel good to keep.</p>
            <div className="hero-preview" aria-label="Example monthly financial overview">
              <div className="preview-topline"><span>MONTHLY OVERVIEW</span><span className="preview-live"><i /> On track</span></div>
              <div className="preview-balance"><span>Available balance</span><strong>$4,820.50</strong><small>↑ 8.2% this month</small></div>
              <div className="preview-chart" aria-hidden="true"><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /><i /></div>
              <div className="preview-legend"><span><i /> Income</span><span><i /> Spending</span></div>
            </div>
            <div className="privacy-note"><span className="privacy-icon" aria-hidden="true">✳</span><span>Your money details stay yours.</span></div>
          </div>
          <p className="story-caption">A clearer view, one step at a time.</p>
        </div>
        <AuthScreen onAuthenticated={handleAuthenticated} />
      </section>
      <Footer />
    </main>
  )
}

function Brand() {
  return <a className="brand" href="/" aria-label="Moneywise home"><span className="brand-mark" aria-hidden="true">M</span><span className="brand-name">moneywise</span></a>
}

function Footer() {
  return <footer className="app-footer">Personal Money Management <span>·</span> Your financial space</footer>
}

export default App
