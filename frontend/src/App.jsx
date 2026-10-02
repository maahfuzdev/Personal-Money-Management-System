import { useCallback, useEffect, useState } from 'react'
import AuthScreen from './features/auth/AuthScreen.jsx'
import Dashboard from './features/dashboard/Dashboard.jsx'

function App() {
  const [session, setSession] = useState(null)
  const [path, setPath] = useState(window.location.pathname)

  useEffect(() => {
    const onPopState = () => setPath(window.location.pathname)
    window.addEventListener('popstate', onPopState)
    return () => window.removeEventListener('popstate', onPopState)
  }, [])

  const navigate = useCallback((nextPath) => {
    if (window.location.pathname !== nextPath) window.history.pushState({}, '', nextPath)
    setPath(nextPath)
    window.scrollTo({ top: 0, behavior: 'auto' })
  }, [])

  const handleAuthenticated = useCallback((nextSession) => {
    setSession(nextSession)
    navigate('/')
  }, [navigate])

  const handleSignOut = useCallback(() => {
    setSession(null)
    navigate('/login')
  }, [navigate])

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
