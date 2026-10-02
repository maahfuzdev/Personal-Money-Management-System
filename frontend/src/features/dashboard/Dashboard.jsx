import { useCallback, useEffect, useMemo, useState } from 'react'
import {
  createTransaction,
  createBudget,
  deleteBudget,
  deleteTransaction,
  getBudgets,
  getTransactionSummary,
  getTransactions,
  updateTransaction,
  updateBudget,
} from '../../api/transactionApi.js'

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}
const emptyForm = () => ({
  type: 'EXPENSE', amount: '', category: '', note: '', transactionDate: new Date().toISOString().slice(0, 10),
})
const emptyBudget = () => ({ category: '', monthlyLimit: '', month: currentMonth() })

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
  const [budgets, setBudgets] = useState([])
  const [budgetForm, setBudgetForm] = useState(emptyBudget)
  const [budgetMonth, setBudgetMonth] = useState(currentMonth())
  const [editingBudgetId, setEditingBudgetId] = useState(null)
  const [budgetError, setBudgetError] = useState('')
  const [isSavingBudget, setIsSavingBudget] = useState(false)

  const loadDashboard = useCallback(async (selectedMonth = budgetMonth) => {
    setLoadError('')
    try {
      const [items, totals, monthlyBudgets] = await Promise.all([
        getTransactions(session.token),
        getTransactionSummary(session.token),
        getBudgets(session.token, selectedMonth),
      ])
      setTransactions(items)
      setSummary(totals)
      setBudgets(monthlyBudgets)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    } finally {
      setIsLoading(false)
    }
  }, [budgetMonth, onSignOut, session.token])

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

  function startBudgetEdit(budget) {
    setEditingBudgetId(budget.id)
    setBudgetForm({ category: budget.category, monthlyLimit: String(budget.monthlyLimit), month: budget.month })
    setBudgetError('')
    document.querySelector('#budget-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function cancelBudgetEdit() {
    setEditingBudgetId(null)
    setBudgetForm({ ...emptyBudget(), month: budgetMonth })
    setBudgetError('')
  }

  async function handleBudgetSubmit(event) {
    event.preventDefault()
    setBudgetError('')
    setIsSavingBudget(true)
    const payload = { ...budgetForm, category: budgetForm.category.trim(), monthlyLimit: Number(budgetForm.monthlyLimit) }
    try {
      if (editingBudgetId) await updateBudget(session.token, editingBudgetId, payload)
      else await createBudget(session.token, payload)
      setBudgetMonth(payload.month)
      setEditingBudgetId(null)
      setBudgetForm({ ...emptyBudget(), month: payload.month })
      await loadDashboard(payload.month)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setBudgetError(error.message)
    } finally {
      setIsSavingBudget(false)
    }
  }

  async function handleBudgetDelete(budget) {
    if (!window.confirm(`Delete the ${budget.category} budget for ${budget.month}?`)) return
    try {
      await deleteBudget(session.token, budget.id)
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

        <section className="panel budgets-panel" aria-labelledby="budgets-heading">
          <div className="panel-heading budget-heading"><div><p className="eyebrow">PLAN AHEAD</p><h2 id="budgets-heading">Monthly budgets</h2></div>
            <label className="budget-month-label"><span className="sr-only">Budget month</span>
              <input type="month" value={budgetMonth} onChange={(event) => {
                setBudgetMonth(event.target.value)
                setBudgetForm({ ...emptyBudget(), month: event.target.value })
                setEditingBudgetId(null)
              }} />
            </label>
          </div>
          <div className="budget-layout">
            <div className="budget-list">
              {isLoading ? <div className="empty-state compact"><span className="loading-dot" />Loading budgets…</div>
                : budgets.length === 0 ? <div className="empty-state compact"><strong>No budgets for this month yet</strong><span>Set a limit and keep an eye on your spending.</span></div>
                  : budgets.map((budget) => <BudgetRow key={budget.id} budget={budget} onEdit={() => startBudgetEdit(budget)} onDelete={() => handleBudgetDelete(budget)} />)}
            </div>
            <form id="budget-form" className="budget-form" onSubmit={handleBudgetSubmit}>
              <h3>{editingBudgetId ? 'Edit budget' : 'Set a category limit'}</h3>
              {budgetError && <div className="form-alert" role="alert">{budgetError}</div>}
              <label className="form-field"><span>Category</span><input required maxLength="60" placeholder="e.g. Food" value={budgetForm.category} onChange={(event) => setBudgetForm({ ...budgetForm, category: event.target.value })} /></label>
              <label className="form-field"><span>Monthly limit <small>(BDT)</small></span><input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={budgetForm.monthlyLimit} onChange={(event) => setBudgetForm({ ...budgetForm, monthlyLimit: event.target.value })} /></label>
              <label className="form-field"><span>Month</span><input required type="month" value={budgetForm.month} onChange={(event) => setBudgetForm({ ...budgetForm, month: event.target.value })} /></label>
              <button className="submit-button" type="submit" disabled={isSavingBudget}>{isSavingBudget ? 'Saving…' : editingBudgetId ? 'Save budget' : 'Create budget'}<span aria-hidden="true">→</span></button>
              {editingBudgetId && <button className="cancel-edit" type="button" onClick={cancelBudgetEdit}>Cancel editing</button>}
            </form>
          </div>
        </section>
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

function BudgetRow({ budget, onEdit, onDelete }) {
  const spent = Number(budget.spent)
  const limit = Number(budget.monthlyLimit)
  const percent = limit > 0 ? (spent / limit) * 100 : 0
  const overBudget = percent > 100
  const progress = Math.min(percent, 100)
  return <article className="budget-row">
    <div className="budget-row-top"><div><strong>{budget.category}</strong><span>{money.format(spent)} spent of {money.format(limit)}</span></div>
      <div className="budget-row-actions"><button type="button" onClick={onEdit} aria-label={`Edit ${budget.category} budget`}>Edit</button><button type="button" onClick={onDelete} aria-label={`Delete ${budget.category} budget`}>Delete</button></div>
    </div>
    <div className="budget-progress" role="progressbar" aria-label={`${budget.category} budget used`} aria-valuemin="0" aria-valuemax="100" aria-valuenow={Math.round(progress)}>
      <span className={overBudget ? 'over-budget' : ''} style={{ width: `${progress}%` }} />
    </div>
    <span className={overBudget ? 'budget-remaining over-budget' : 'budget-remaining'}>{overBudget ? `${money.format(Math.abs(Number(budget.remaining)))} over limit` : `${money.format(budget.remaining)} remaining`}</span>
  </article>
}

export default Dashboard
