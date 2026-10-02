import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createTransaction,
  deleteTransaction,
  getTransactionSummary,
  getTransactions,
  updateTransaction,
} from '../../api/transactionApi.js'

const emptyForm = () => ({
  type: 'EXPENSE', amount: '', category: '', note: '', transactionDate: new Date().toISOString().slice(0, 10),
})

const money = new Intl.NumberFormat('en-BD', { style: 'currency', currency: 'BDT', maximumFractionDigits: 2 })
const dateLabel = new Intl.DateTimeFormat('en', { day: 'numeric', month: 'short', year: 'numeric' })

function Dashboard({ session, onSignOut }) {
  const [transactions, setTransactions] = useState([])
  const [summary, setSummary] = useState({ totalIncome: 0, totalExpense: 0, balance: 0 })
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [formError, setFormError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [filter, setFilter] = useState('ALL')

  const loadDashboard = useCallback(async () => {
    setLoadError('')
    try {
      const [items, totals] = await Promise.all([
        getTransactions(session.token),
        getTransactionSummary(session.token),
      ])
      setTransactions(items)
      setSummary(totals)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    } finally {
      setIsLoading(false)
    }
  }, [onSignOut, session.token])

  useEffect(() => { loadDashboard() }, [loadDashboard])

  const visibleTransactions = useMemo(() => filter === 'ALL'
    ? transactions
    : transactions.filter((transaction) => transaction.type === filter), [filter, transactions])

  function startEdit(transaction) {
    setEditingId(transaction.id)
    setForm({
      type: transaction.type,
      amount: String(transaction.amount),
      category: transaction.category,
      note: transaction.note || '',
      transactionDate: transaction.transactionDate,
    })
    setFormError('')
    document.querySelector('#transaction-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function cancelEdit() {
    setEditingId(null)
    setForm(emptyForm())
    setFormError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setFormError('')
    setIsSaving(true)
    const payload = { ...form, amount: Number(form.amount), note: form.note.trim() || null }
    try {
      if (editingId) await updateTransaction(session.token, editingId, payload)
      else await createTransaction(session.token, payload)
      cancelEdit()
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setFormError(error.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(transaction) {
    if (!window.confirm(`Delete this ${transaction.type.toLowerCase()} of ${money.format(transaction.amount)}?`)) return
    try {
      await deleteTransaction(session.token, transaction.id)
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  return (
    <main className="dashboard-shell">
      <header className="dashboard-header">
        <a className="brand dashboard-brand" href="/" aria-label="Moneywise home">
          <span className="brand-mark" aria-hidden="true">M</span><span className="brand-name">moneywise</span>
        </a>
        <div className="profile-area">
          <div className="profile-avatar" aria-hidden="true">{session.user.name.slice(0, 1).toUpperCase()}</div>
          <div className="profile-copy"><strong>{session.user.name}</strong><span>{session.user.email}</span></div>
          <button className="text-button" type="button" onClick={onSignOut}>Sign out</button>
        </div>
      </header>

      <div className="dashboard-content">
        <section className="dashboard-welcome">
          <div><p className="eyebrow">YOUR MONEY, AT A GLANCE</p><h1>Good to see you, {session.user.name.split(' ')[0]}.</h1>
            <p>Here is how your personal finances are looking.</p></div>
          <span className="today-pill">{dateLabel.format(new Date())}</span>
        </section>

        <section className="summary-grid" aria-label="Account totals">
          <SummaryCard label="Total balance" value={summary.balance} kind="balance" icon="↗" />
          <SummaryCard label="Income" value={summary.totalIncome} kind="income" icon="↓" />
          <SummaryCard label="Expenses" value={summary.totalExpense} kind="expense" icon="↑" />
        </section>

        {loadError && <div className="dashboard-alert" role="alert">{loadError}<button type="button" onClick={loadDashboard}>Try again</button></div>}

        <div className="dashboard-columns">
          <section className="panel transaction-panel" aria-labelledby="activity-heading">
            <div className="panel-heading"><div><p className="eyebrow">YOUR ACTIVITY</p><h2 id="activity-heading">Transactions</h2></div>
              <label className="filter-label"><span className="sr-only">Filter transactions</span>
                <select value={filter} onChange={(event) => setFilter(event.target.value)}>
                  <option value="ALL">All activity</option><option value="INCOME">Income</option><option value="EXPENSE">Expenses</option>
                </select>
              </label>
            </div>
            {isLoading ? <div className="empty-state"><span className="loading-dot" />Loading your activity…</div>
              : visibleTransactions.length === 0 ? <div className="empty-state"><span className="empty-icon">⌁</span>
                <strong>{filter === 'ALL' ? 'Your story starts here' : `No ${filter.toLowerCase()} yet`}</strong>
                <span>{filter === 'ALL' ? 'Add a transaction to see where your money goes.' : 'Try another filter or add a transaction.'}</span>
              </div> : <div className="transaction-list">
                {visibleTransactions.map((transaction) => <TransactionRow key={transaction.id} transaction={transaction}
                  onEdit={() => startEdit(transaction)} onDelete={() => handleDelete(transaction)} />)}
              </div>}
          </section>

          <section className="panel form-panel" aria-labelledby="form-heading">
            <div className="panel-heading"><div><p className="eyebrow">KEEP TRACK</p><h2 id="form-heading">{editingId ? 'Edit transaction' : 'Add a transaction'}</h2></div></div>
            <form id="transaction-form" className="transaction-form" onSubmit={handleSubmit}>
              <div className="type-switch" role="group" aria-label="Transaction type">
                <button type="button" className={form.type === 'EXPENSE' ? 'type-option selected expense' : 'type-option'} onClick={() => setForm({ ...form, type: 'EXPENSE' })}>Expense</button>
                <button type="button" className={form.type === 'INCOME' ? 'type-option selected income' : 'type-option'} onClick={() => setForm({ ...form, type: 'INCOME' })}>Income</button>
              </div>
              {formError && <div className="form-alert" role="alert">{formError}</div>}
              <label className="form-field"><span>Amount <small>(BDT)</small></span>
                <input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />
              </label>
              <label className="form-field"><span>Category</span>
                <input required maxLength="60" placeholder={form.type === 'INCOME' ? 'e.g. Salary' : 'e.g. Groceries'} value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} />
              </label>
              <label className="form-field"><span>Date</span>
                <input required type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} />
              </label>
              <label className="form-field"><span>Note <small>(optional)</small></span>
                <input maxLength="500" placeholder="Add a little detail" value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} />
              </label>
              <button className="submit-button" type="submit" disabled={isSaving}>{isSaving ? 'Saving…' : editingId ? 'Save changes' : 'Add transaction'}<span aria-hidden="true">→</span></button>
              {editingId && <button className="cancel-edit" type="button" onClick={cancelEdit}>Cancel editing</button>}
            </form>
          </section>
        </div>
        <footer className="dashboard-footer">Your financial space, thoughtfully organized.</footer>
      </div>
    </main>
  )
}

function SummaryCard({ label, value, kind, icon }) {
  return <article className={`summary-card ${kind}`}><div className="summary-card-top"><span>{label}</span><span className="summary-icon" aria-hidden="true">{icon}</span></div>
    <strong>{money.format(value || 0)}</strong><span className="summary-caption">Across all your transactions</span>
  </article>
}

function TransactionRow({ transaction, onEdit, onDelete }) {
  const isIncome = transaction.type === 'INCOME'
  return <article className="transaction-row"><span className={`transaction-icon ${isIncome ? 'income' : 'expense'}`} aria-hidden="true">{isIncome ? '↙' : '↗'}</span>
    <div className="transaction-info"><strong>{transaction.category}</strong><span>{transaction.note || (isIncome ? 'Income' : 'Expense')} <i>·</i> {dateLabel.format(new Date(`${transaction.transactionDate}T00:00:00`))}</span></div>
    <strong className={`transaction-amount ${isIncome ? 'income' : 'expense'}`}>{isIncome ? '+' : '−'}{money.format(transaction.amount)}</strong>
    <div className="row-actions"><button type="button" onClick={onEdit} aria-label={`Edit ${transaction.category}`}>Edit</button><button type="button" onClick={onDelete} aria-label={`Delete ${transaction.category}`}>Delete</button></div>
  </article>
}

export default Dashboard
