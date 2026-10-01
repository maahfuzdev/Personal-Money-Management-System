import { useState } from 'react'
import AuthScreen from './features/auth/AuthScreen.jsx'

function App() {
  const [session, setSession] = useState(null)

  if (session) {
    return (
      <main className="app-shell">
        <section className="signed-in-card" aria-labelledby="signed-in-title">
          <Brand />
          <div className="signed-in-icon" aria-hidden="true">✓</div>
          <p className="eyebrow">ACCOUNT CONNECTED</p>
          <h1 id="signed-in-title">You’re in, {session.user.name.split(' ')[0]}.</h1>
          <p className="welcome-copy">
            Your account is ready. The finance dashboard is the next part we’ll build.
          </p>
          <button className="text-button sign-out-button" onClick={() => setSession(null)}>
            Sign out
          </button>
        </section>
        <Footer />
      </main>
    )
  }

  return (
    <main className="app-shell">
      <section className="auth-layout" aria-label="Account access">
        <div className="story-panel">
          <Brand />
          <div className="story-content">
            <p className="eyebrow">PERSONAL FINANCE, MADE CLEAR</p>
            <h1>Make room for the things that matter.</h1>
            <p className="story-copy">
              Bring your everyday money into focus and build habits that feel good to keep.
            </p>
            <div className="privacy-note">
              <span className="privacy-icon" aria-hidden="true">✳</span>
              <span>Your money details stay yours.</span>
            </div>
          </div>
          <p className="story-caption">A clearer view, one step at a time.</p>
        </div>
        <AuthScreen onAuthenticated={setSession} />
      </section>
      <Footer />
    </main>
  )
}

function Brand() {
  return (
    <a className="brand" href="/" aria-label="Moneywise home">
      <span className="brand-mark" aria-hidden="true">M</span>
      <span className="brand-name">moneywise</span>
    </a>
  )
}

function Footer() {
  return <footer className="app-footer">Personal Money Management <span>·</span> Your financial space</footer>
}

export default App
