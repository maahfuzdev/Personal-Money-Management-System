function App() {
  return (
    <main className="welcome-shell">
      <section className="welcome-card" aria-labelledby="welcome-title">
        <span className="brand-mark" aria-hidden="true">M</span>
        <p className="eyebrow">PERSONAL FINANCE, MADE CLEAR</p>
        <h1 id="welcome-title">Your money, in better view.</h1>
        <p className="welcome-copy">
          A calmer way to understand your spending, plan ahead, and make progress
          toward your goals.
        </p>
        <div className="setup-note">
          <span className="status-dot" aria-hidden="true" />
          <span>Workspace is ready for the first feature.</span>
        </div>
      </section>
      <footer className="welcome-footer">Personal Money Management</footer>
    </main>
  )
}

export default App
