import { useCallback, useEffect, useRef, useState } from 'react'
import {
  createTransaction,
  createBudget,
  createGoal,
  addGoalContribution,
  createRecurringTransaction,
  deleteGoal,
  deleteRecurringTransaction,
  deleteBudget,
  deleteTransaction,
  exportTransactions,
  exportAccountBackup,
  restoreAccountBackup,
  previewTransactionImport,
  importTransactions,
  getBudgets,
  getCategorySuggestions,
  getDashboardAnalytics,
  getDataHealth,
  getGoals,
  getGoalContributions,
  getRecurringTransactions,
  getTransactionSummary,
  getTransactions,
  getMoneyAccounts,
  createMoneyAccount,
  getMoneyTransfers,
  createMoneyTransfer,
  getAccountAdjustments,
  createAccountAdjustment,
  updateTransaction,
  updateBudget,
  updateGoal,
  setRecurringTransactionActive,
} from '../../api/transactionApi.js'

const currentMonth = () => {
  const now = new Date()
  return `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}`
}
const todayInDhaka = () => new Date(Date.now() + 6 * 60 * 60 * 1000).toISOString().slice(0, 10)
const dayAfter = (date, days) => {
  const [year, month, day] = date.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
}
const emptyForm = () => ({
  accountId: '', type: 'EXPENSE', amount: '', category: '', note: '', transactionDate: new Date().toISOString().slice(0, 10),
})
const emptyBudget = () => ({ category: '', monthlyLimit: '', month: currentMonth() })
const emptyGoal = () => ({ name: '', targetAmount: '', currentAmount: '0', targetDate: '', note: '' })
const emptyRecurring = () => ({ type: 'EXPENSE', amount: '', category: '', note: '', frequency: 'MONTHLY', startDate: todayInDhaka(), endDate: '' })

const money = new Intl.NumberFormat('en-BD', { style: 'currency', currency: 'BDT', maximumFractionDigits: 2 })
const dateLabel = new Intl.DateTimeFormat('en', { day: 'numeric', month: 'short', year: 'numeric' })

const NAV_ITEMS = [
  { path: '/', label: 'Overview', icon: 'overview' },
  { path: '/accounts', label: 'Accounts', icon: 'accounts' },
  { path: '/transactions', label: 'Transactions', icon: 'transactions' },
  { path: '/budgets', label: 'Budgets', icon: 'budgets' },
  { path: '/goals', label: 'Savings goals', icon: 'goals' },
  { path: '/recurring', label: 'Recurring', icon: 'recurring' },
  { path: '/reports', label: 'Reports', icon: 'reports' },
]

const PAGE_COPY = {
  '/': ['YOUR MONEY, AT A GLANCE', 'Overview', 'A clear picture of your money, all in one place.'],
  '/transactions': ['YOUR ACTIVITY', 'Transactions', 'Review, search, and manage the money moving in and out.'],
  '/accounts': ['YOUR MONEY, ORGANIZED', 'Accounts & wallets', 'Track Cash, bank accounts and mobile wallets separately.'],
  '/budgets': ['PLAN WITH CONFIDENCE', 'Budgets', 'Set monthly limits and keep your spending on track.'],
  '/goals': ['MAKE IT HAPPEN', 'Savings goals', 'Give your savings a purpose and celebrate each milestone.'],
  '/recurring': ['STAY AHEAD', 'Recurring transactions', 'Schedule regular income and expenses so your records stay up to date.'],
  '/reports': ['UNDERSTAND YOUR HABITS', 'Reports', 'Explore your cash flow and see where your money goes.'],
}

function calculateAmountExpression(expression) {
  const compact = expression.replace(/\s+/g, '')
  const tokens = compact.match(/(?:\d+(?:\.\d*)?|\.\d+|[()+\-*/])/g) || []
  if (!compact || tokens.join('') !== compact) throw new Error('Use numbers and +, −, ×, ÷ only.')

  let position = 0
  function parseFactor() {
    const token = tokens[position++]
    if (token === '+') return parseFactor()
    if (token === '-') return -parseFactor()
    if (token === '(') {
      const value = parseExpression()
      if (tokens[position++] !== ')') throw new Error('Close the bracket to finish the calculation.')
      return value
    }
    if (token === undefined || !/^(?:\d+(?:\.\d*)?|\.\d+)$/.test(token)) {
      throw new Error('Complete the calculation first.')
    }
    return Number(token)
  }

  function parseTerm() {
    let value = parseFactor()
    while (tokens[position] === '*' || tokens[position] === '/') {
      const operator = tokens[position++]
      const next = parseFactor()
      if (operator === '/' && next === 0) throw new Error('Cannot divide by zero.')
      value = operator === '*' ? value * next : value / next
    }
    return value
  }

  function parseExpression() {
    let value = parseTerm()
    while (tokens[position] === '+' || tokens[position] === '-') {
      const operator = tokens[position++]
      const next = parseTerm()
      value = operator === '+' ? value + next : value - next
    }
    return value
  }

  const result = parseExpression()
  if (position !== tokens.length || !Number.isFinite(result)) throw new Error('Check the calculation and try again.')
  return result
}

function Dashboard({ session, onSignOut, navigate, path }) {
  const route = PAGE_COPY[path] ? path : '/'
  const [eyebrow, pageTitle, pageDescription] = PAGE_COPY[route]
  const [transactions, setTransactions] = useState([])
  const [accounts, setAccounts] = useState([])
  const [transfers, setTransfers] = useState([])
  const [adjustments, setAdjustments] = useState([])
  const [accountDraft, setAccountDraft] = useState({ name: '', type: 'MOBILE_WALLET', openingBalance: '' })
  const [transferDraft, setTransferDraft] = useState({ fromAccountId: '', toAccountId: '', amount: '', transferDate: todayInDhaka(), note: '' })
  const [accountError, setAccountError] = useState('')
  const [transferError, setTransferError] = useState('')
  const [isSavingAccount, setIsSavingAccount] = useState(false)
  const [isSavingTransfer, setIsSavingTransfer] = useState(false)
  const [adjustmentDraft, setAdjustmentDraft] = useState({ accountId: '', actualBalance: '', adjustmentDate: todayInDhaka(), note: '' })
  const [adjustmentError, setAdjustmentError] = useState('')
  const [isSavingAdjustment, setIsSavingAdjustment] = useState(false)
  const [summary, setSummary] = useState({ totalIncome: 0, totalExpense: 0, balance: 0 })
  const [isLoading, setIsLoading] = useState(true)
  const [loadError, setLoadError] = useState('')
  const [successMessage, setSuccessMessage] = useState('')
  const [formError, setFormError] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [isTransactionFormOpen, setIsTransactionFormOpen] = useState(true)
  const [editingId, setEditingId] = useState(null)
  const [form, setForm] = useState(emptyForm)
  const [isAmountCalculatorOpen, setIsAmountCalculatorOpen] = useState(false)
  const [calculatorExpression, setCalculatorExpression] = useState('')
  const [calculatorResult, setCalculatorResult] = useState(null)
  const [calculatorError, setCalculatorError] = useState('')
  const [deleteTarget, setDeleteTarget] = useState(null)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const isConfirmingDeleteRef = useRef(false)
  const [filter, setFilter] = useState('ALL')
  const [accountFilter, setAccountFilter] = useState('')
  const [searchDraft, setSearchDraft] = useState('')
  const [search, setSearch] = useState('')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [transactionPage, setTransactionPage] = useState(0)
  const [transactionPageInfo, setTransactionPageInfo] = useState({ totalItems: 0, totalPages: 0, size: 10 })
  const [isExporting, setIsExporting] = useState(false)
  const [isExportingBackup, setIsExportingBackup] = useState(false)
  const [restorePreview, setRestorePreview] = useState(null)
  const [restoreError, setRestoreError] = useState('')
  const [isRestoringBackup, setIsRestoringBackup] = useState(false)
  const [importPreview, setImportPreview] = useState(null)
  const [importError, setImportError] = useState('')
  const [isImporting, setIsImporting] = useState(false)
  const [budgets, setBudgets] = useState([])
  const [currentBudgets, setCurrentBudgets] = useState([])
  const [budgetForm, setBudgetForm] = useState(emptyBudget)
  const [isBudgetFormVisible, setIsBudgetFormVisible] = useState(true)
  const [budgetMonth, setBudgetMonth] = useState(currentMonth())
  const [editingBudgetId, setEditingBudgetId] = useState(null)
  const [budgetError, setBudgetError] = useState('')
  const [isSavingBudget, setIsSavingBudget] = useState(false)
  const [goals, setGoals] = useState([])
  const [recurringTransactions, setRecurringTransactions] = useState([])
  const [recurringForm, setRecurringForm] = useState(emptyRecurring)
  const [recurringError, setRecurringError] = useState('')
  const [isSavingRecurring, setIsSavingRecurring] = useState(false)
  const [goalForm, setGoalForm] = useState(emptyGoal)
  const [editingGoalId, setEditingGoalId] = useState(null)
  const [goalError, setGoalError] = useState('')
  const [isSavingGoal, setIsSavingGoal] = useState(false)
  const [analytics, setAnalytics] = useState(null)
  const [statementTransactions, setStatementTransactions] = useState([])
  const [isLoadingStatement, setIsLoadingStatement] = useState(false)
  const [statementError, setStatementError] = useState('')
  const [dataHealth, setDataHealth] = useState(null)
  const [isLoadingDataHealth, setIsLoadingDataHealth] = useState(false)
  const [dataHealthError, setDataHealthError] = useState('')
  const [categorySuggestions, setCategorySuggestions] = useState({ INCOME: [], EXPENSE: [] })
  const reminderCutoff = dayAfter(todayInDhaka(), 7)
  const reminderItems = recurringTransactions.filter((item) => item.active && item.nextRunDate <= reminderCutoff
    && (!item.endDate || item.nextRunDate <= item.endDate)).sort((first, second) => first.nextRunDate.localeCompare(second.nextRunDate))
  const activeFilterCount = [filter !== 'ALL', Boolean(accountFilter), Boolean(searchDraft.trim()), Boolean(startDate), Boolean(endDate)].filter(Boolean).length
  const hasInvalidDateRange = Boolean(startDate && endDate && startDate > endDate)
  const selectedBudgetWarnings = budgetMonth === currentMonth()
    ? budgets.map((budget) => ({ ...budget, usedPercent: Number(budget.monthlyLimit) > 0 ? Number(budget.spent) / Number(budget.monthlyLimit) * 100 : 0 }))
      .filter((budget) => budget.usedPercent >= 80).sort((a, b) => b.usedPercent - a.usedPercent)
    : []
  const budgetWarnings = currentBudgets.map((budget) => ({ ...budget,
    usedPercent: Number(budget.monthlyLimit) > 0 ? Number(budget.spent) / Number(budget.monthlyLimit) * 100 : 0,
  })).filter((budget) => budget.usedPercent >= 80).sort((a, b) => b.usedPercent - a.usedPercent)

  const loadDashboard = useCallback(async (selectedMonth = budgetMonth) => {
    setLoadError('')
    try {
      const activeBudgetsRequest = getBudgets(session.token, currentMonth())
      const selectedBudgetsRequest = selectedMonth === currentMonth()
        ? activeBudgetsRequest : getBudgets(session.token, selectedMonth)
      const [transactionResult, totals, monthlyBudgets, activeMonthBudgets, savingsGoals, monthlyAnalytics, recurring, accountResult, transferResult, adjustmentResult] = await Promise.all([
        getTransactions(session.token, {
          page: transactionPage,
          size: 10,
          accountId: accountFilter,
          type: filter === 'ALL' ? undefined : filter,
          search,
          startDate,
          endDate,
        }),
        getTransactionSummary(session.token),
        selectedBudgetsRequest,
        activeBudgetsRequest,
        getGoals(session.token),
        getDashboardAnalytics(session.token, selectedMonth),
        getRecurringTransactions(session.token),
        getMoneyAccounts(session.token),
        getMoneyTransfers(session.token),
        getAccountAdjustments(session.token),
      ])
      setTransactions(transactionResult.items)
      setTransactionPageInfo(transactionResult)
      const lastAvailablePage = Math.max(0, transactionResult.totalPages - 1)
      if (transactionPage !== lastAvailablePage) setTransactionPage(lastAvailablePage)
      setSummary(totals)
      setBudgets(monthlyBudgets)
      setCurrentBudgets(activeMonthBudgets)
      setGoals(savingsGoals)
      setAnalytics(monthlyAnalytics)
      setRecurringTransactions(recurring)
      setAccounts(accountResult)
      setTransfers(transferResult)
      setAdjustments(adjustmentResult)
      setForm((current) => current.accountId || !accountResult.length
        ? current : { ...current, accountId: String(accountResult[0].id) })
      setAdjustmentDraft((current) => current.accountId || !accountResult.length
        ? current : { ...current, accountId: String(accountResult[0].id) })
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    } finally {
      setIsLoading(false)
    }
  }, [accountFilter, budgetMonth, endDate, filter, onSignOut, search, session.token, startDate, transactionPage])

  useEffect(() => { loadDashboard() }, [loadDashboard])

  useEffect(() => {
    if (route !== '/reports') return undefined
    let isCurrent = true
    setIsLoadingDataHealth(true)
    setDataHealthError('')
    getDataHealth(session.token)
      .then((result) => { if (isCurrent) setDataHealth(result) })
      .catch((error) => { if (isCurrent) setDataHealthError(error.message || 'Could not check your data.') })
      .finally(() => { if (isCurrent) setIsLoadingDataHealth(false) })
    return () => { isCurrent = false }
  }, [route, session.token])

  useEffect(() => {
    if (route !== '/reports') return undefined
    let isCurrent = true
    async function loadStatementTransactions() {
      setIsLoadingStatement(true)
      setStatementError('')
      try {
        const [year, month] = budgetMonth.split('-').map(Number)
        const startDate = `${budgetMonth}-01`
        const endDate = new Date(Date.UTC(year, month, 0)).toISOString().slice(0, 10)
        const pageSize = 50
        const firstPage = await getTransactions(session.token, { page: 0, size: pageSize, startDate, endDate })
        const maximumRows = 10000
        const pageCount = Math.min(firstPage.totalPages, Math.ceil(maximumRows / pageSize))
        const pages = [...firstPage.items]
        for (let batchStart = 1; batchStart < pageCount; batchStart += 8) {
          const batchPages = Array.from({ length: Math.min(8, pageCount - batchStart) }, (_, index) => batchStart + index)
          const batchResults = await Promise.all(batchPages.map((page) => getTransactions(session.token, {
            page, size: pageSize, startDate, endDate,
          })))
          batchResults.forEach((result) => pages.push(...result.items))
        }
        if (isCurrent) setStatementTransactions(pages.slice(0, maximumRows))
      } catch (error) {
        if (isCurrent) setStatementError(error.message || 'Could not load this month’s transactions.')
      } finally {
        if (isCurrent) setIsLoadingStatement(false)
      }
    }
    void loadStatementTransactions()
    return () => { isCurrent = false }
  }, [budgetMonth, route, session.token])

  useEffect(() => {
    let isCurrent = true
    Promise.all([
      getCategorySuggestions(session.token, 'INCOME'),
      getCategorySuggestions(session.token, 'EXPENSE'),
    ]).then(([income, expense]) => {
      if (isCurrent) setCategorySuggestions({ INCOME: income, EXPENSE: expense })
    }).catch((error) => {
      if (isCurrent && error.status === 401) onSignOut()
    })
    return () => { isCurrent = false }
  }, [onSignOut, session.token])

  useEffect(() => {
    const timer = window.setTimeout(() => {
      setTransactionPage(0)
      setSearch(searchDraft.trim())
    }, 300)
    return () => window.clearTimeout(timer)
  }, [searchDraft])

  useEffect(() => {
    if (!successMessage) return undefined
    const timer = window.setTimeout(() => setSuccessMessage(''), 4500)
    return () => window.clearTimeout(timer)
  }, [successMessage])

  useEffect(() => {
    if (!deleteTarget) return undefined
    const previouslyFocused = document.activeElement
    const dialog = document.querySelector('[data-confirm-dialog]')
    const actions = dialog?.querySelectorAll('button:not(:disabled)')
    actions?.[0]?.focus()
    function handleDialogKeydown(event) {
      if (event.key === 'Escape' && !isConfirmingDeleteRef.current) setDeleteTarget(null)
      if (event.key !== 'Tab') return
      const currentActions = dialog?.querySelectorAll('button:not(:disabled)')
      if (!currentActions?.length) return
      const first = currentActions[0]
      const last = currentActions[currentActions.length - 1]
      if (event.shiftKey && document.activeElement === first) {
        event.preventDefault()
        last.focus()
      } else if (!event.shiftKey && document.activeElement === last) {
        event.preventDefault()
        first.focus()
      }
    }
    window.addEventListener('keydown', handleDialogKeydown)
    return () => {
      window.removeEventListener('keydown', handleDialogKeydown)
      previouslyFocused?.focus?.()
    }
  }, [deleteTarget])

  function changeTransactionFilter(value) {
    setFilter(value)
    setTransactionPage(0)
  }

  function clearTransactionFilters() {
    setFilter('ALL')
    setAccountFilter('')
    setSearchDraft('')
    setSearch('')
    setStartDate('')
    setEndDate('')
    setTransactionPage(0)
  }

  function requestDelete(kind, item) {
    setDeleteTarget({ kind, item })
  }

  async function confirmDelete() {
    if (!deleteTarget || isConfirmingDelete) return
    isConfirmingDeleteRef.current = true
    setIsConfirmingDelete(true)
    try {
      if (deleteTarget.kind === 'transaction') await handleDelete(deleteTarget.item)
      if (deleteTarget.kind === 'budget') await handleBudgetDelete(deleteTarget.item)
      if (deleteTarget.kind === 'goal') await handleGoalDelete(deleteTarget.item)
      if (deleteTarget.kind === 'recurring') await handleRecurringDelete(deleteTarget.item)
      setDeleteTarget(null)
    } finally {
      isConfirmingDeleteRef.current = false
      setIsConfirmingDelete(false)
    }
  }

  async function handleExport() {
    setIsExporting(true)
    setLoadError('')
    try {
      const file = await exportTransactions(session.token, {
        accountId: accountFilter,
        type: filter === 'ALL' ? undefined : filter,
        search: searchDraft.trim(),
        startDate,
        endDate,
      })
      const downloadUrl = URL.createObjectURL(file)
      const link = document.createElement('a')
      link.href = downloadUrl
      link.download = 'transactions.csv'
      document.body.append(link)
      link.click()
      link.remove()
      window.setTimeout(() => URL.revokeObjectURL(downloadUrl), 1000)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    } finally {
      setIsExporting(false)
    }
  }

  async function handleBackupExport() {
    setIsExportingBackup(true)
    try {
      const blob = await exportAccountBackup(session.token)
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `money-manager-backup-${todayInDhaka()}.json`
      document.body.append(link); link.click(); link.remove(); window.setTimeout(() => URL.revokeObjectURL(url), 1000)
      setSuccessMessage('Your account backup has been downloaded.')
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    } finally { setIsExportingBackup(false) }
  }

  async function handleBackupRestoreFile(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setRestoreError(''); setRestorePreview(null)
    try {
      if (file.size > 20 * 1024 * 1024) throw new Error('Backup files must be 20 MB or smaller.')
      const backup = JSON.parse(await file.text())
      if (![1, 2].includes(backup?.formatVersion) || !Array.isArray(backup.transactions))
        throw new Error('This does not look like a supported Money Manager backup file.')
      const records = ['accounts', 'transfers', 'adjustments', 'transactions', 'budgets', 'goals', 'contributions', 'recurringTransactions']
        .reduce((count, key) => count + (Array.isArray(backup[key]) ? backup[key].length : 0), 0)
      setRestorePreview({ fileName: file.name, backup, records })
    } catch (error) { setRestoreError(error instanceof SyntaxError ? 'The selected file is not valid JSON.' : error.message) }
  }

  async function handleBackupRestore() {
    if (!restorePreview || isRestoringBackup) return
    setIsRestoringBackup(true); setRestoreError('')
    try {
      const result = await restoreAccountBackup(session.token, restorePreview.backup)
      setSuccessMessage(`${result.importedCount} records restored; ${result.skippedCount} existing records skipped.`)
      setRestorePreview(null)
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setRestoreError(error.message || 'Could not restore this backup.')
    } finally { setIsRestoringBackup(false) }
  }

  async function handleImportFile(event) {
    const file = event.target.files?.[0]
    event.target.value = ''
    if (!file) return
    setImportError(''); setImportPreview(null); setIsImporting(true)
    try { setImportPreview(await previewTransactionImport(session.token, file)) }
    catch (error) { setImportError(error.message) }
    finally { setIsImporting(false) }
  }

  async function handleImportConfirm() {
    const validRows = importPreview?.rows.filter((row) => row.transaction && !row.duplicate && row.errors.length === 0) || []
    if (!validRows.length) return
    setIsImporting(true); setImportError('')
    try {
      const result = await importTransactions(session.token, validRows.map((row) => row.transaction))
      setSuccessMessage(`${result.importedCount} transaction${result.importedCount === 1 ? '' : 's'} imported${result.duplicateCount ? `; ${result.duplicateCount} duplicate${result.duplicateCount === 1 ? '' : 's'} skipped` : ''}.`)
      setImportPreview(null)
      await loadDashboard()
    } catch (error) { setImportError(error.message) }
    finally { setIsImporting(false) }
  }

  function startEdit(transaction) {
    setIsTransactionFormOpen(true)
    setEditingId(transaction.id)
    setForm({
      accountId: String(transaction.accountId),
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

  function updateCalculatorExpression(nextExpression) {
    setCalculatorExpression(nextExpression)
    setCalculatorError('')
    try {
      setCalculatorResult(calculateAmountExpression(nextExpression))
    } catch {
      setCalculatorResult(null)
    }
  }

  function handleCalculatorKey(key) {
    if (key === 'C') return updateCalculatorExpression('')
    if (key === '⌫') return updateCalculatorExpression(calculatorExpression.slice(0, -1))
    const operators = { '×': '*', '÷': '/', '−': '-' }
    updateCalculatorExpression(`${calculatorExpression}${operators[key] || key}`)
  }

  function useCalculatedAmount() {
    if (calculatorResult === null) {
      setCalculatorError('Complete the calculation before using the amount.')
      return
    }
    if (calculatorResult <= 0) {
      setCalculatorError('Amount must be greater than zero.')
      return
    }
    setForm((current) => ({ ...current, amount: calculatorResult.toFixed(2) }))
    setIsAmountCalculatorOpen(false)
    setCalculatorError('')
  }

  async function handleSubmit(event) {
    event.preventDefault()
    const wasEditing = Boolean(editingId)
    setFormError('')
    setSuccessMessage('')
    setIsSaving(true)
    const payload = { ...form, accountId: Number(form.accountId) || null, amount: Number(form.amount), note: form.note.trim() || null }
    try {
      if (editingId) await updateTransaction(session.token, editingId, payload)
      else await createTransaction(session.token, payload)
      setCategorySuggestions((current) => ({
        ...current,
        [payload.type]: [...new Set([...current[payload.type], payload.category])].sort((a, b) => a.localeCompare(b)),
      }))
      setSuccessMessage(wasEditing ? 'Transaction updated.' : 'Transaction added.')
      cancelEdit()
      setIsTransactionFormOpen(false)
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setFormError(error.message)
    } finally {
      setIsSaving(false)
    }
  }

  async function handleDelete(transaction) {
    setSuccessMessage('')
    try {
      await deleteTransaction(session.token, transaction.id)
      setSuccessMessage('Transaction deleted.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  async function handleCreateAccount(event) {
    event.preventDefault()
    setAccountError('')
    setIsSavingAccount(true)
    try {
      await createMoneyAccount(session.token, {
        ...accountDraft,
        name: accountDraft.name.trim(),
        openingBalance: Number(accountDraft.openingBalance || 0),
      })
      setAccountDraft({ name: '', type: 'MOBILE_WALLET', openingBalance: '' })
      setSuccessMessage('Account added.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setAccountError(error.message)
    } finally {
      setIsSavingAccount(false)
    }
  }

  async function handleCreateTransfer(event) {
    event.preventDefault()
    setTransferError('')
    setIsSavingTransfer(true)
    try {
      await createMoneyTransfer(session.token, {
        ...transferDraft,
        fromAccountId: Number(transferDraft.fromAccountId),
        toAccountId: Number(transferDraft.toAccountId),
        amount: Number(transferDraft.amount),
      })
      setTransferDraft({ fromAccountId: '', toAccountId: '', amount: '', transferDate: todayInDhaka(), note: '' })
      setSuccessMessage('Transfer recorded. Account balances are updated.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setTransferError(error.message)
    } finally {
      setIsSavingTransfer(false)
    }
  }

  async function handleCreateAdjustment(event) {
    event.preventDefault()
    setAdjustmentError('')
    setIsSavingAdjustment(true)
    try {
      const created = await createAccountAdjustment(session.token, Number(adjustmentDraft.accountId), {
        actualBalance: Number(adjustmentDraft.actualBalance),
        adjustmentDate: adjustmentDraft.adjustmentDate,
        note: adjustmentDraft.note.trim() || null,
      })
      setAdjustmentDraft((current) => ({ ...current, actualBalance: '', note: '', adjustmentDate: todayInDhaka() }))
      setSuccessMessage(`${created.accountName} balance adjusted and recorded.`)
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setAdjustmentError(error.message)
    } finally {
      setIsSavingAdjustment(false)
    }
  }

  function printStatement() {
    document.body.classList.add('printing-statement')
    const cleanup = () => document.body.classList.remove('printing-statement')
    window.addEventListener('afterprint', cleanup, { once: true })
    window.setTimeout(() => window.print(), 0)
  }

  function startBudgetEdit(budget) {
    setEditingBudgetId(budget.id)
    setIsBudgetFormVisible(true)
    setBudgetForm({ category: budget.category, monthlyLimit: String(budget.monthlyLimit), month: budget.month })
    setBudgetError('')
    document.querySelector('#budget-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function cancelBudgetEdit() {
    setEditingBudgetId(null)
    setBudgetForm({ ...emptyBudget(), month: budgetMonth })
    setBudgetError('')
  }

  function changeBudgetMonth(month) {
    if (!month || editingBudgetId) return
    setBudgetMonth(month)
    setBudgetForm({ ...emptyBudget(), month })
  }

  async function handleBudgetSubmit(event) {
    event.preventDefault()
    const wasEditing = Boolean(editingBudgetId)
    setBudgetError('')
    setSuccessMessage('')
    setIsSavingBudget(true)
    const payload = { ...budgetForm, category: budgetForm.category.trim(), monthlyLimit: Number(budgetForm.monthlyLimit) }
    try {
      if (editingBudgetId) await updateBudget(session.token, editingBudgetId, payload)
      else await createBudget(session.token, payload)
      setCategorySuggestions((current) => ({
        ...current,
        EXPENSE: [...new Set([...current.EXPENSE, payload.category])].sort((a, b) => a.localeCompare(b)),
      }))
      setSuccessMessage(wasEditing ? 'Budget updated.' : 'Budget created.')
      setBudgetMonth(payload.month)
      setEditingBudgetId(null)
      setBudgetForm({ ...emptyBudget(), month: payload.month })
      setIsBudgetFormVisible(false)
      await loadDashboard(payload.month)
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setBudgetError(error.message)
    } finally {
      setIsSavingBudget(false)
    }
  }

  async function handleBudgetDelete(budget) {
    setSuccessMessage('')
    try {
      await deleteBudget(session.token, budget.id)
      setSuccessMessage('Budget deleted.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  function startGoalEdit(goal) {
    setEditingGoalId(goal.id)
    setGoalForm({
      name: goal.name,
      targetAmount: String(goal.targetAmount),
      currentAmount: String(goal.currentAmount),
      targetDate: goal.targetDate || '',
      note: goal.note || '',
    })
    setGoalError('')
    document.querySelector('#goal-form')?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  function cancelGoalEdit() {
    setEditingGoalId(null)
    setGoalForm(emptyGoal())
    setGoalError('')
  }

  async function handleGoalSubmit(event) {
    event.preventDefault()
    const wasEditing = Boolean(editingGoalId)
    setGoalError('')
    setSuccessMessage('')
    setIsSavingGoal(true)
    const payload = {
      ...goalForm,
      name: goalForm.name.trim(),
      targetAmount: Number(goalForm.targetAmount),
      currentAmount: Number(goalForm.currentAmount),
      targetDate: goalForm.targetDate || null,
      note: goalForm.note.trim() || null,
    }
    try {
      if (editingGoalId) await updateGoal(session.token, editingGoalId, payload)
      else await createGoal(session.token, payload)
      setSuccessMessage(wasEditing ? 'Savings goal updated.' : 'Savings goal created.')
      cancelGoalEdit()
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setGoalError(error.message)
    } finally {
      setIsSavingGoal(false)
    }
  }

  async function handleGoalDelete(goal) {
    setSuccessMessage('')
    try {
      await deleteGoal(session.token, goal.id)
      setSuccessMessage('Savings goal deleted.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  async function handleRecurringSubmit(event) {
    event.preventDefault()
    setRecurringError('')
    setIsSavingRecurring(true)
    try {
      await createRecurringTransaction(session.token, {
        ...recurringForm,
        amount: Number(recurringForm.amount),
        category: recurringForm.category.trim(),
        note: recurringForm.note.trim() || null,
        endDate: recurringForm.endDate || null,
      })
      setRecurringForm(emptyRecurring())
      setSuccessMessage('Recurring transaction scheduled.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setRecurringError(error.message)
    } finally {
      setIsSavingRecurring(false)
    }
  }

  async function handleRecurringToggle(recurring) {
    try {
      const updated = await setRecurringTransactionActive(session.token, recurring.id, !recurring.active)
      setRecurringTransactions((items) => items.map((item) => item.id === updated.id ? updated : item))
      setSuccessMessage(updated.active ? 'Recurring transaction resumed.' : 'Recurring transaction paused.')
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  async function handleRecurringDelete(recurring) {
    try {
      await deleteRecurringTransaction(session.token, recurring.id)
      setSuccessMessage('Recurring transaction deleted.')
      await loadDashboard()
    } catch (error) {
      if (error.status === 401) onSignOut()
      else setLoadError(error.message)
    }
  }

  return (
    <div className="app-frame">
      <aside className="app-sidebar">
        <a className="brand sidebar-brand" href="/" onClick={(event) => { event.preventDefault(); navigate('/') }} aria-label="Moneywise overview">
          <span className="brand-mark" aria-hidden="true">M</span><span className="brand-name">moneywise</span>
        </a>
        <div className="workspace-label">PERSONAL SPACE</div>
        <nav className="sidebar-nav" aria-label="Main navigation">
          {NAV_ITEMS.map((item) => <NavItem key={item.path} item={item} active={route === item.path} navigate={navigate} />)}
        </nav>
        <div className="sidebar-note"><span className="sidebar-note-mark">✦</span><div><strong>Your money, your pace.</strong><span>Small steps add up.</span></div></div>
        <div className="sidebar-account"><div className="profile-avatar" aria-hidden="true">{session.user.name.slice(0, 1).toUpperCase()}</div>
          <div className="profile-copy"><strong>{session.user.name}</strong><span>{session.user.email}</span></div>
          <button className="sidebar-signout" type="button" onClick={onSignOut} aria-label="Sign out">↗</button>
        </div>
      </aside>

      <div className="app-main">
        <header className="app-topbar">
          <div className="topbar-title"><span>PERSONAL FINANCE</span><strong>{pageTitle}</strong></div>
          <div className="topbar-profile"><span className="online-indicator" /><span>Welcome back, {session.user.name.split(' ')[0]}</span>
            <div className="profile-avatar" aria-hidden="true">{session.user.name.slice(0, 1).toUpperCase()}</div>
            <button type="button" onClick={onSignOut}>Sign out</button>
          </div>
        </header>

      <main className="page-content">
        <section className="dashboard-welcome">
          <div><p className="eyebrow">{eyebrow}</p><h1>{route === '/' ? `Good to see you, ${session.user.name.split(' ')[0]}.` : pageTitle}</h1>
            <p>{route === '/' ? pageDescription : pageDescription}</p></div>
          <div className="welcome-actions">
            <span className="today-pill">{dateLabel.format(new Date())}</span>
            {route === '/' && <button className="primary-action" type="button" onClick={() => navigate('/transactions')}><span aria-hidden="true">+</span> Add transaction</button>}
          </div>
        </section>

        {route === '/' && <section className="summary-grid" aria-label="Account totals">
          <SummaryCard label="Total balance" value={summary.balance} kind="balance" icon="↗" />
          <SummaryCard label="Income" value={summary.totalIncome} kind="income" icon="↓" />
          <SummaryCard label="Expenses" value={summary.totalExpense} kind="expense" icon="↑" />
        </section>}

        {route === '/' && !isLoading && budgetWarnings.length > 0 && <section className="budget-nudge-panel" aria-labelledby="budget-nudge-heading" role="status" aria-live="polite">
          <div className="budget-nudge-heading"><span className="budget-nudge-icon" aria-hidden="true">!</span><div><p className="eyebrow">MONTHLY BUDGET CHECK</p><h2 id="budget-nudge-heading">{budgetWarnings.some((budget) => budget.usedPercent >= 100) ? 'Some budgets are over limit' : 'You’re getting close to a budget limit'}</h2></div>
            <button type="button" className="subtle-link" onClick={() => navigate('/budgets')}>View budgets <span aria-hidden="true">→</span></button>
          </div>
          <div className="budget-nudge-list">{budgetWarnings.slice(0, 4).map((budget) => {
            const limit = Number(budget.monthlyLimit)
            const spent = Number(budget.spent)
            const remaining = limit - spent
            const isOver = remaining < 0
            return <button className={`budget-nudge${isOver ? ' over-limit' : ''}`} type="button" key={budget.id} onClick={() => navigate('/budgets')}>
              <span className="budget-nudge-copy"><strong>{budget.category}</strong><span>{Math.round(budget.usedPercent)}% used · {isOver ? `${money.format(Math.abs(remaining))} over` : `${money.format(remaining)} left`}</span></span>
              <span className="budget-nudge-track"><i style={{ width: `${Math.min(100, budget.usedPercent)}%` }} /></span>
            </button>
          })}</div>
        </section>}

        {(route === '/' || route === '/reports') && <section className="insights-grid" aria-label="Monthly spending insights">
          <article className="panel insight-panel trend-panel">
            <div className="panel-heading insight-heading"><div><p className="eyebrow">THE BIG PICTURE</p><h2>Cash flow</h2></div>
              <label className="budget-month-label"><span className="sr-only">Choose analytics month</span>
                <input type="month" value={budgetMonth} onChange={(event) => {
                  if (!event.target.value) return
                  setBudgetMonth(event.target.value)
                  setBudgetForm({ ...emptyBudget(), month: event.target.value })
                  setEditingBudgetId(null)
                }} />
              </label>
            </div>
            <div className="insight-totals">
              <div><span>Income this month</span><strong className="income-text">{money.format(analytics?.monthIncome || 0)}</strong></div>
              <div><span>Expenses this month</span><strong className="expense-text">{money.format(analytics?.monthExpense || 0)}</strong></div>
              <div><span>Net cash flow</span><strong>{money.format(analytics?.monthBalance || 0)}</strong></div>
            </div>
            {analytics && <div className={`month-comparison ${Number(analytics.expenseChangePercent) > 0 ? 'increased' : 'decreased'}`}>
              {analytics.expenseChangePercent === null
                ? 'Previous month had no expenses to compare.'
                : <>{`Expenses ${Number(analytics.expenseChangePercent) > 0 ? 'increased' : Number(analytics.expenseChangePercent) < 0 ? 'decreased' : 'stayed level'} by `}
                  <strong>{money.format(Math.abs(Number(analytics.monthExpense) - Number(analytics.previousMonthExpense)))}</strong>
                  {` (${Math.abs(Number(analytics.expenseChangePercent))}%) vs the previous month.`}</>}
            </div>}
            {isLoading ? <div className="empty-state chart-loading"><span className="loading-dot" /></div>
              : <CashFlowChart trend={analytics?.monthlyTrend || []} />}
          </article>
          <article className="panel insight-panel category-panel">
            <div className="panel-heading"><div><p className="eyebrow">WHERE IT GOES</p><h2>Spending by category</h2></div></div>
            {isLoading ? <div className="empty-state chart-loading"><span className="loading-dot" /></div>
              : <CategoryChart categories={analytics?.expenseByCategory || []} />}
          </article>
        </section>}

        {(route === '/' || route === '/reports') && Boolean(analytics?.spendingAlerts?.length) && <section className="spending-alerts" aria-labelledby="spending-alerts-title">
          <div className="spending-alerts-heading"><span className="spending-alert-icon" aria-hidden="true">!</span><div><p className="eyebrow">SPENDING WATCH</p><h2 id="spending-alerts-title">A category is spending more than usual</h2></div></div>
          <div className="spending-alert-list">{analytics.spendingAlerts.map((alert) => <article className="spending-alert-row" key={alert.category}>
            <div><strong>{alert.category}</strong><span>{Math.round(Number(alert.increasePercent))}% above its average over the previous 3 months</span></div>
            <div><strong>{money.format(alert.currentAmount)}</strong><span>3-month average {money.format(alert.threeMonthAverage)}</span></div>
          </article>)}</div>
        </section>}

        {successMessage && <div className="success-banner" role="status" aria-live="polite">
          <span aria-hidden="true">✓</span><span>{successMessage}</span>
          <button type="button" onClick={() => setSuccessMessage('')} aria-label="Dismiss success message">Dismiss</button>
        </div>}
        {loadError && <div className="dashboard-alert" role="alert">{loadError}<button type="button" onClick={loadDashboard}>Try again</button></div>}

        {route === '/' && <section className="panel recent-panel" aria-labelledby="recent-heading">
          <div className="panel-heading"><div><p className="eyebrow">LATEST MOVEMENT</p><h2 id="recent-heading">Recent transactions</h2></div>
            <button className="subtle-link" type="button" onClick={() => navigate('/transactions')}>View all <span aria-hidden="true">→</span></button>
          </div>
          {isLoading ? <div className="empty-state"><span className="loading-dot" />Loading recent activity…</div>
            : transactions.length === 0 ? <div className="empty-state"><strong>No transactions yet</strong><span>Add your first transaction from the Transactions page.</span></div>
              : <div className="transaction-list">{transactions.slice(0, 5).map((transaction) =>
                <TransactionRow key={transaction.id} transaction={transaction} />)}</div>}
        </section>}

        {route === '/' && <section className="panel upcoming-panel" aria-labelledby="upcoming-heading">
          <div className="panel-heading"><div><p className="eyebrow">COMING UP</p><h2 id="upcoming-heading">Scheduled transactions</h2></div>
            <button className="subtle-link" type="button" onClick={() => navigate('/recurring')}>Manage schedules <span aria-hidden="true">→</span></button>
          </div>
          {isLoading ? <div className="empty-state compact"><span className="loading-dot" />Loading reminders…</div>
            : reminderItems.length === 0
              ? <div className="empty-state compact"><strong>Nothing due in the next 7 days</strong><span>Scheduled bills and income will appear here before their date.</span><button className="subtle-link" type="button" onClick={() => navigate('/recurring')}>Manage schedules →</button></div>
              : <div className="upcoming-list">{reminderItems.slice(0, 5).map((item) => {
                const days = Math.round((Date.parse(`${item.nextRunDate}T00:00:00Z`) - Date.parse(`${todayInDhaka()}T00:00:00Z`)) / 86400000)
                const dateText = days <= 0 ? 'Due today' : days === 1 ? 'Tomorrow' : `In ${days} days`
                return <article className="upcoming-item" key={item.id}>
                  <span className={`upcoming-type ${item.type === 'INCOME' ? 'income' : 'expense'}`} aria-hidden="true">{item.type === 'INCOME' ? '↙' : '↗'}</span>
                  <div className="upcoming-copy"><strong>{item.category}</strong><span>{item.frequency.toLowerCase()} · {dateLabel.format(new Date(`${item.nextRunDate}T12:00:00`))}</span></div>
                  <span className={`reminder-pill${days <= 1 ? ' due' : ''}`}>{dateText}</span>
                  <strong className={item.type === 'INCOME' ? 'income-text' : 'expense-text'}>{money.format(item.amount)}</strong>
                </article>
              })}</div>}
        </section>}

        {route === '/transactions' && <div className="dashboard-columns">
          <section className="panel transaction-panel" aria-labelledby="activity-heading">
            <div className="panel-heading transaction-heading"><div><p className="eyebrow">YOUR ACTIVITY</p><h2 id="activity-heading">Transactions</h2></div>
              <div className="transaction-toolbar">
                <label className="transaction-search"><span className="sr-only">Search transactions</span>
                  <input type="search" maxLength="100" placeholder="Search category or note" value={searchDraft} onChange={(event) => setSearchDraft(event.target.value)} />
                </label>
                <label className="filter-label"><span className="sr-only">Filter transaction type</span>
                <select value={filter} onChange={(event) => changeTransactionFilter(event.target.value)}>
                  <option value="ALL">All activity</option><option value="INCOME">Income</option><option value="EXPENSE">Expenses</option>
                </select>
                </label>
                <label className="filter-label account-filter-label"><span className="sr-only">Filter by account</span>
                  <select value={accountFilter} onChange={(event) => { setAccountFilter(event.target.value); setTransactionPage(0) }}>
                    <option value="">All accounts</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                  </select>
                </label>
                <label className="date-filter"><span>From</span><input type="date" max={endDate || undefined} value={startDate} onChange={(event) => {
                  setStartDate(event.target.value)
                  setTransactionPage(0)
                }} /></label>
                <label className="date-filter"><span>To</span><input type="date" min={startDate || undefined} value={endDate} onChange={(event) => {
                  setEndDate(event.target.value)
                  setTransactionPage(0)
                }} /></label>
                <button className="export-button" type="button" onClick={handleExport}
                  disabled={isExporting || hasInvalidDateRange || transactionPageInfo.totalItems === 0}>
                  {isExporting ? 'Preparing…' : 'Export CSV'}
                </button>
                <button className="backup-button" type="button" onClick={handleBackupExport} disabled={isExportingBackup}>
                  {isExportingBackup ? 'Preparing…' : 'Download backup'}
                </button>
                <label className="import-button">{isImporting && !importPreview ? 'Reading…' : 'Import CSV'}
                  <input type="file" accept=".csv,text/csv" onChange={handleImportFile} disabled={isImporting} />
                </label>
                <label className="restore-button">Restore backup
                  <input type="file" accept=".json,application/json" onChange={handleBackupRestoreFile} disabled={isRestoringBackup} />
                </label>
                <button className="clear-filters" type="button" onClick={clearTransactionFilters} disabled={activeFilterCount === 0}>
                  Clear filters{activeFilterCount > 0 ? ` (${activeFilterCount})` : ''}
                </button>
              </div>
            </div>
            {(importPreview || importError) && <section className="import-preview" aria-live="polite">
              <div className="import-preview-heading"><div><strong>CSV import preview</strong>{importPreview && <span>{importPreview.validCount} ready · {importPreview.duplicateCount} duplicate · {importPreview.invalidCount} invalid</span>}</div>
                <button type="button" onClick={() => { setImportPreview(null); setImportError('') }}>Close</button></div>
              {importError && <p className="form-alert" role="alert">{importError}</p>}
              {importPreview && <><div className="import-preview-rows">{importPreview.rows.slice(0, 100).map((row) => <div className="import-preview-row" key={row.rowNumber}>
                <span>Row {row.rowNumber}</span><span>{row.date} · {row.type} · {row.category} · ৳{row.amount}</span>
                <strong className={row.errors.length ? 'invalid' : row.duplicate ? 'duplicate' : 'ready'}>{row.errors.join(' ') || (row.duplicate ? 'Duplicate' : 'Ready')}</strong>
              </div>)}</div>{importPreview.rows.length > 100 && <p>Showing first 100 of {importPreview.rows.length} rows.</p>}
                <button className="submit-button import-confirm" type="button" disabled={isImporting || importPreview.validCount === 0} onClick={handleImportConfirm}>
                  {isImporting ? 'Importing…' : `Import ${importPreview.validCount} transaction${importPreview.validCount === 1 ? '' : 's'}`}</button></>}
            </section>}
            {(restorePreview || restoreError) && <section className="import-preview restore-preview" aria-live="polite">
              <div className="import-preview-heading"><div><strong>Backup restore preview</strong>{restorePreview && <span>{restorePreview.fileName} · {restorePreview.records} records</span>}</div>
                <button type="button" onClick={() => { setRestorePreview(null); setRestoreError('') }}>Close</button></div>
              {restoreError && <p className="form-alert" role="alert">{restoreError}</p>}
              {restorePreview && <><p>Restore adds records to this account and skips matches. Existing data will not be deleted.</p>
                <button className="submit-button import-confirm" type="button" disabled={isRestoringBackup} onClick={handleBackupRestore}>
                  {isRestoringBackup ? 'Restoring…' : `Restore ${restorePreview.records} records`}</button></>}
            </section>}
            {hasInvalidDateRange && <div className="filter-warning" role="status">Choose a start date that is on or before the end date. CSV export is unavailable until the range is corrected.</div>}
            {isLoading ? <div className="empty-state"><span className="loading-dot" />Loading your activity…</div>
              : transactions.length === 0 ? <div className="empty-state"><span className="empty-icon">⌁</span>
                <strong>{filter === 'ALL' ? 'Your story starts here' : `No ${filter.toLowerCase()} yet`}</strong>
                <span>{search ? 'No transactions match that search. Try another word or clear the search.' : filter === 'ALL' ? 'Add a transaction to see where your money goes.' : 'Try another filter or add a transaction.'}</span>
              </div> : <div className="transaction-list">
                {transactions.map((transaction) => <TransactionRow key={transaction.id} transaction={transaction}
                  onEdit={() => startEdit(transaction)} onDelete={() => requestDelete('transaction', transaction)} />)}
              </div>}
            {!isLoading && transactionPageInfo.totalItems > 0 && <div className="transaction-pagination">
              <span>Showing {transactionPage * transactionPageInfo.size + 1}–{Math.min((transactionPage + 1) * transactionPageInfo.size, transactionPageInfo.totalItems)} of {transactionPageInfo.totalItems}</span>
              <div><button type="button" disabled={transactionPage <= 0} onClick={() => setTransactionPage((page) => Math.max(0, page - 1))}>Previous</button>
                <span>Page {transactionPage + 1} of {transactionPageInfo.totalPages}</span>
                <button type="button" disabled={transactionPage + 1 >= transactionPageInfo.totalPages} onClick={() => setTransactionPage((page) => page + 1)}>Next</button>
              </div>
            </div>}
          </section>

          <section className="panel form-panel" aria-labelledby="form-heading">
            <div className="panel-heading">
              <div><p className="eyebrow">KEEP TRACK</p><h2 id="form-heading">{editingId ? 'Edit transaction' : 'Add a transaction'}</h2></div>
              <button className="transaction-form-toggle" type="button" aria-expanded={isTransactionFormOpen} aria-controls={isTransactionFormOpen ? 'transaction-form' : undefined} onClick={() => {
                if (isTransactionFormOpen && editingId) cancelEdit()
                setIsTransactionFormOpen((open) => !open)
              }}>{isTransactionFormOpen ? 'Close' : 'Add transaction'}</button>
            </div>
            {isTransactionFormOpen && <form id="transaction-form" className="transaction-form" onSubmit={handleSubmit}>
              <div className="type-switch" role="group" aria-label="Transaction type">
                <button type="button" className={form.type === 'EXPENSE' ? 'type-option selected expense' : 'type-option'} onClick={() => setForm({ ...form, type: 'EXPENSE' })}>Expense</button>
                <button type="button" className={form.type === 'INCOME' ? 'type-option selected income' : 'type-option'} onClick={() => setForm({ ...form, type: 'INCOME' })}>Income</button>
              </div>
              {formError && <div className="form-alert" role="alert">{formError}</div>}
              <label className="form-field"><span>Account / wallet</span>
                <select required value={form.accountId || accounts[0]?.id || ''} onChange={(event) => setForm({ ...form, accountId: event.target.value })}>
                  {accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}
                </select>
              </label>
              <div className="form-field amount-field">
                <div className="amount-field-heading"><label htmlFor="transaction-amount">Amount <small>(BDT)</small></label>
                  <button className="calculator-toggle" type="button" aria-expanded={isAmountCalculatorOpen} aria-controls={isAmountCalculatorOpen ? 'amount-calculator' : undefined} onClick={() => {
                    const nextOpen = !isAmountCalculatorOpen
                    setIsAmountCalculatorOpen(nextOpen)
                    setCalculatorError('')
                    if (nextOpen) updateCalculatorExpression(form.amount)
                  }}>{isAmountCalculatorOpen ? 'Close calculator' : 'Use calculator'}</button>
                </div>
                <input id="transaction-amount" required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={form.amount} onChange={(event) => setForm({ ...form, amount: event.target.value })} />
                {isAmountCalculatorOpen && <div id="amount-calculator" className="amount-calculator">
                  <label className="calculator-expression-label" htmlFor="calculator-expression">Calculation</label>
                  <input id="calculator-expression" className="calculator-expression" inputMode="decimal" placeholder="e.g. 1200 + 350 × 2" value={calculatorExpression} onChange={(event) => updateCalculatorExpression(event.target.value)} />
                  <div className="calculator-keys" aria-label="Calculator keys">
                    {['7', '8', '9', '÷', '4', '5', '6', '×', '1', '2', '3', '−', 'C', '0', '.', '+', '(', ')', '⌫'].map((key) => <button key={key} className={['÷', '×', '−', '+'].includes(key) ? 'operator' : ''} type="button" onClick={() => handleCalculatorKey(key)} aria-label={key === '⌫' ? 'Delete last character' : key === 'C' ? 'Clear calculation' : key}>{key}</button>)}
                  </div>
                  {calculatorError && <span className="calculator-error" role="alert">{calculatorError}</span>}
                  <div className="calculator-result"><span>Result</span><strong>{calculatorResult === null ? '—' : calculatorResult.toLocaleString('en-BD', { maximumFractionDigits: 2 })} ৳</strong></div>
                  <button className="calculator-use-button" type="button" disabled={calculatorResult === null || calculatorResult <= 0} onClick={useCalculatedAmount}>Use amount</button>
                </div>}
              </div>
              <label className="form-field"><span>Category</span>
                <input required list={`category-suggestions-${form.type}`} maxLength="60" placeholder={form.type === 'INCOME' ? 'e.g. Salary' : 'e.g. Groceries'} value={form.category} onChange={(event) => setForm({ ...form, category: event.target.value })} />
              </label>
              <label className="form-field"><span>Date</span>
                <input required type="date" value={form.transactionDate} onChange={(event) => setForm({ ...form, transactionDate: event.target.value })} />
              </label>
              <label className="form-field"><span>Note <small>(optional)</small></span>
                <input maxLength="500" placeholder="Add a little detail" value={form.note} onChange={(event) => setForm({ ...form, note: event.target.value })} />
              </label>
              <button className="submit-button" type="submit" disabled={isSaving}>{isSaving ? 'Saving…' : editingId ? 'Save changes' : 'Add transaction'}<span aria-hidden="true">→</span></button>
              {editingId && <button className="cancel-edit" type="button" onClick={cancelEdit}>Cancel editing</button>}
            </form>}
          </section>
        </div>}

        {route === '/accounts' && <section className="accounts-page page-panel" aria-labelledby="accounts-heading">
          <div className="accounts-balance-banner"><div><p className="eyebrow">ALL ACCOUNTS</p><span>Combined balance</span></div><strong>{money.format(accounts.reduce((total, account) => total + Number(account.balance || 0), 0))}</strong></div>
          <div className="account-grid">{accounts.map((account) => <article className="account-card" key={account.id}>
            <div className="account-card-top"><span className="account-card-icon"><NavIcon name="accounts" /></span><span>{account.type.replace('_', ' ')}</span></div>
            <h2>{account.name}</h2><strong>{money.format(account.balance || 0)}</strong><small>Opening balance {money.format(account.openingBalance || 0)}</small>
          </article>)}</div>
          <div className="accounts-layout">
            <form className="panel account-form" onSubmit={handleCreateAccount}>
              <div className="panel-heading"><div><p className="eyebrow">ADD A PLACE TO TRACK</p><h2>New account or wallet</h2></div></div>
              <div className="account-form-fields">{accountError && <div className="form-alert" role="alert">{accountError}</div>}
                <label className="form-field"><span>Name</span><input required maxLength="80" placeholder="e.g. bKash, Salary account" value={accountDraft.name} onChange={(event) => setAccountDraft({ ...accountDraft, name: event.target.value })} /></label>
                <label className="form-field"><span>Type</span><select value={accountDraft.type} onChange={(event) => setAccountDraft({ ...accountDraft, type: event.target.value })}><option value="CASH">Cash</option><option value="BANK">Bank</option><option value="MOBILE_WALLET">Mobile wallet</option><option value="CREDIT_CARD">Credit card</option><option value="OTHER">Other</option></select></label>
                <label className="form-field"><span>Starting balance (BDT)</span><input type="number" min="0" step="0.01" value={accountDraft.openingBalance} onChange={(event) => setAccountDraft({ ...accountDraft, openingBalance: event.target.value })} placeholder="0.00" /></label>
                <button className="submit-button" type="submit" disabled={isSavingAccount}>{isSavingAccount ? 'Saving…' : 'Add account'}<span aria-hidden="true">→</span></button>
              </div>
            </form>
            <form className="panel transfer-form" onSubmit={handleCreateTransfer}>
              <div className="panel-heading"><div><p className="eyebrow">MOVE MONEY</p><h2>Transfer between accounts</h2></div></div>
              <div className="account-form-fields">{transferError && <div className="form-alert" role="alert">{transferError}</div>}
                <label className="form-field"><span>From</span><select required value={transferDraft.fromAccountId} onChange={(event) => setTransferDraft({ ...transferDraft, fromAccountId: event.target.value })}><option value="">Choose account</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label>
                <label className="form-field"><span>To</span><select required value={transferDraft.toAccountId} onChange={(event) => setTransferDraft({ ...transferDraft, toAccountId: event.target.value })}><option value="">Choose account</option>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name}</option>)}</select></label>
                <label className="form-field"><span>Amount (BDT)</span><input required type="number" min="0.01" step="0.01" value={transferDraft.amount} onChange={(event) => setTransferDraft({ ...transferDraft, amount: event.target.value })} placeholder="0.00" /></label>
                <label className="form-field"><span>Date</span><input required type="date" value={transferDraft.transferDate} onChange={(event) => setTransferDraft({ ...transferDraft, transferDate: event.target.value })} /></label>
                <label className="form-field"><span>Note <small>(optional)</small></span><input maxLength="300" value={transferDraft.note} onChange={(event) => setTransferDraft({ ...transferDraft, note: event.target.value })} placeholder="e.g. Cash out" /></label>
                <button className="submit-button" type="submit" disabled={isSavingTransfer || accounts.length < 2}>{isSavingTransfer ? 'Saving…' : 'Record transfer'}<span aria-hidden="true">→</span></button>
              </div>
            </form>
          </div>
          <div className="accounts-adjustment-layout">
            <form className="panel adjustment-form" onSubmit={handleCreateAdjustment}>
              <div className="panel-heading"><div><p className="eyebrow">BALANCE CHECK</p><h2>Correct an account balance</h2></div></div>
              <div className="account-form-fields">
                {adjustmentError && <div className="form-alert" role="alert">{adjustmentError}</div>}
                <p className="adjustment-help">Count the cash or check your wallet/bank app, then enter its actual balance. We’ll record the difference without changing your transaction history.</p>
                <label className="form-field"><span>Account</span><select required value={adjustmentDraft.accountId} onChange={(event) => setAdjustmentDraft({ ...adjustmentDraft, accountId: event.target.value })}>{accounts.map((account) => <option key={account.id} value={account.id}>{account.name} · {money.format(account.balance || 0)}</option>)}</select></label>
                <label className="form-field"><span>Actual balance (BDT)</span><input required type="number" min="0" step="0.01" inputMode="decimal" value={adjustmentDraft.actualBalance} onChange={(event) => setAdjustmentDraft({ ...adjustmentDraft, actualBalance: event.target.value })} placeholder="What you counted" /></label>
                <label className="form-field"><span>Date</span><input required type="date" value={adjustmentDraft.adjustmentDate} onChange={(event) => setAdjustmentDraft({ ...adjustmentDraft, adjustmentDate: event.target.value })} /></label>
                <label className="form-field"><span>Reason <small>(optional)</small></span><input maxLength="300" value={adjustmentDraft.note} onChange={(event) => setAdjustmentDraft({ ...adjustmentDraft, note: event.target.value })} placeholder="e.g. Cash count correction" /></label>
                <button className="submit-button" type="submit" disabled={isSavingAdjustment || !accounts.length}>{isSavingAdjustment ? 'Saving…' : 'Save balance adjustment'}<span aria-hidden="true">→</span></button>
              </div>
            </form>
            <section className="panel adjustment-history"><div className="panel-heading"><div><p className="eyebrow">AUDIT TRAIL</p><h2>Balance adjustments</h2></div></div>
              {adjustments.length === 0 ? <div className="empty-state compact"><strong>No balance corrections yet</strong><span>Any correction you make will be listed here with its reason.</span></div> : <div className="adjustment-list">{adjustments.slice(0, 20).map((adjustment) => <article className="adjustment-row" key={adjustment.id}>
                <span className={`adjustment-delta${Number(adjustment.adjustmentAmount) >= 0 ? ' positive' : ' negative'}`}>{Number(adjustment.adjustmentAmount) >= 0 ? '+' : '−'}</span>
                <div><strong>{adjustment.accountName} · {money.format(adjustment.previousBalance)} → {money.format(adjustment.actualBalance)}</strong><small>{adjustment.adjustmentDate}{adjustment.note ? ` · ${adjustment.note}` : ''}</small></div>
                <b>{Number(adjustment.adjustmentAmount) >= 0 ? '+' : '−'}{money.format(Math.abs(Number(adjustment.adjustmentAmount)))}</b>
              </article>)}</div>}
            </section>
          </div>
          <section className="panel transfer-history"><div className="panel-heading"><div><p className="eyebrow">ACCOUNT ACTIVITY</p><h2>Recent transfers</h2></div></div>
            {transfers.length === 0 ? <div className="empty-state compact"><strong>No transfers yet</strong><span>Transfers between your own accounts won’t count as income or expense.</span></div> : <div className="transfer-list">{transfers.map((transfer) => <article className="transfer-row" key={transfer.id}><span className="transfer-arrow">↗</span><div><strong>{transfer.fromAccountName} <span>to</span> {transfer.toAccountName}</strong><small>{transfer.transferDate}{transfer.note ? ` · ${transfer.note}` : ''}</small></div><b>{money.format(transfer.amount)}</b></article>)}</div>}
          </section>
        </section>}

        {route === '/budgets' && <section className="panel budgets-panel page-panel" aria-labelledby="budgets-heading">
          <div className="panel-heading budget-heading"><div><p className="eyebrow">PLAN AHEAD</p><h2 id="budgets-heading">Monthly budgets</h2></div>
            <div className="budget-heading-actions">
              <label className="budget-month-label"><span className="sr-only">Choose budget month</span>
                <input type="month" value={budgetMonth} disabled={Boolean(editingBudgetId)} onChange={(event) => changeBudgetMonth(event.target.value)} />
              </label>
              {!isBudgetFormVisible && <button className="export-button" type="button" onClick={() => setIsBudgetFormVisible(true)}>Set a budget</button>}
            </div>
          </div>
          {!isLoading && selectedBudgetWarnings.length > 0 && <div className={`budget-alerts ${selectedBudgetWarnings.some((budget) => budget.usedPercent >= 100) ? 'over-limit' : ''}`} role="status" aria-live="polite">
            <strong>{selectedBudgetWarnings.some((budget) => budget.usedPercent >= 100) ? 'Budget limit reached' : 'Budget heads-up'}</strong>
            <span>{selectedBudgetWarnings.map((budget) => `${budget.category}: ${Math.round(budget.usedPercent)}% used`).join(' · ')}</span>
          </div>}
          <div className="budget-layout">
            <div className="budget-list">
              {isLoading ? <div className="empty-state compact"><span className="loading-dot" />Loading budgets…</div>
                : budgets.length === 0 ? <div className="empty-state compact"><strong>No budgets for this month yet</strong><span>Set a limit and keep an eye on your spending.</span></div>
                  : budgets.map((budget) => <BudgetRow key={budget.id} budget={budget} isCurrentMonth={budgetMonth === currentMonth()} onEdit={() => startBudgetEdit(budget)} onDelete={() => requestDelete('budget', budget)} />)}
            </div>
            {isBudgetFormVisible && <form id="budget-form" className="budget-form" onSubmit={handleBudgetSubmit}>
              <h3>{editingBudgetId ? 'Edit budget' : 'Set a category limit'}</h3>
              {budgetError && <div className="form-alert" role="alert">{budgetError}</div>}
              <label className="form-field"><span>Category</span><input required list="category-suggestions-EXPENSE" maxLength="60" placeholder="e.g. Food" value={budgetForm.category} onChange={(event) => setBudgetForm({ ...budgetForm, category: event.target.value })} /></label>
              <label className="form-field"><span>Monthly limit <small>(BDT)</small></span><input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={budgetForm.monthlyLimit} onChange={(event) => setBudgetForm({ ...budgetForm, monthlyLimit: event.target.value })} /></label>
              <label className="form-field"><span>Month</span><input required type="month" value={budgetForm.month} onChange={(event) => setBudgetForm({ ...budgetForm, month: event.target.value })} /></label>
              <button className="submit-button" type="submit" disabled={isSavingBudget}>{isSavingBudget ? 'Saving…' : editingBudgetId ? 'Save budget' : 'Create budget'}<span aria-hidden="true">→</span></button>
              {editingBudgetId && <button className="cancel-edit" type="button" onClick={cancelBudgetEdit}>Cancel editing</button>}
            </form>}
          </div>
        </section>}

        {route === '/goals' && <section className="panel goals-panel page-panel" aria-labelledby="goals-heading">
          <div className="panel-heading"><div><p className="eyebrow">MAKE IT HAPPEN</p><h2 id="goals-heading">Savings goals</h2></div>
            <span className="goals-count">{goals.filter((goal) => goal.completed).length} of {goals.length} complete</span>
          </div>
          <div className="goals-layout">
            <div className="goal-grid">
              {isLoading ? <div className="empty-state compact"><span className="loading-dot" />Loading goals…</div>
                : goals.length === 0 ? <div className="empty-state compact"><span className="empty-icon">☆</span><strong>Give your savings a purpose</strong><span>Create a goal and celebrate each step forward.</span></div>
                : goals.map((goal) => <GoalCard key={goal.id} goal={goal} token={session.token}
                  onContributed={async (amount) => { await loadDashboard(); setSuccessMessage(`${money.format(amount)} added to ${goal.name}.`) }}
                  onEdit={() => startGoalEdit(goal)} onDelete={() => requestDelete('goal', goal)} />)}
            </div>
            <form id="goal-form" className="goal-form" onSubmit={handleGoalSubmit}>
              <h3>{editingGoalId ? 'Edit savings goal' : 'Create a goal'}</h3>
              {goalError && <div className="form-alert" role="alert">{goalError}</div>}
              <label className="form-field"><span>Goal name</span><input required maxLength="100" placeholder="e.g. New laptop" value={goalForm.name} onChange={(event) => setGoalForm({ ...goalForm, name: event.target.value })} /></label>
              <div className="goal-amount-fields">
                <label className="form-field"><span>Target <small>(BDT)</small></span><input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={goalForm.targetAmount} onChange={(event) => setGoalForm({ ...goalForm, targetAmount: event.target.value })} /></label>
                <label className="form-field"><span>Saved so far <small>(BDT)</small></span><input required min="0" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={goalForm.currentAmount} onChange={(event) => setGoalForm({ ...goalForm, currentAmount: event.target.value })} /></label>
              </div>
              <label className="form-field"><span>Target date <small>(optional)</small></span><input type="date" value={goalForm.targetDate} onChange={(event) => setGoalForm({ ...goalForm, targetDate: event.target.value })} /></label>
              <label className="form-field"><span>Note <small>(optional)</small></span><input maxLength="300" placeholder="Why is this important to you?" value={goalForm.note} onChange={(event) => setGoalForm({ ...goalForm, note: event.target.value })} /></label>
              <button className="submit-button" type="submit" disabled={isSavingGoal}>{isSavingGoal ? 'Saving…' : editingGoalId ? 'Save changes' : 'Create savings goal'}<span aria-hidden="true">→</span></button>
              {editingGoalId && <button className="cancel-edit" type="button" onClick={cancelGoalEdit}>Cancel editing</button>}
            </form>
          </div>
        </section>}
        {route === '/recurring' && <section className="panel recurring-panel page-panel" aria-labelledby="recurring-heading">
          <div className="panel-heading"><div><p className="eyebrow">AUTOMATE YOUR ROUTINE</p><h2 id="recurring-heading">Recurring transactions</h2></div></div>
          <div className="recurring-layout">
            <div className="recurring-list">
              {isLoading ? <div className="empty-state compact"><span className="loading-dot" />Loading schedules…</div>
                : recurringTransactions.length === 0 ? <div className="empty-state compact"><strong>No recurring transactions yet</strong><span>Schedule rent, bills or regular income to keep your records current.</span></div>
                  : recurringTransactions.map((item) => <RecurringCard key={item.id} item={item}
                    onToggle={() => handleRecurringToggle(item)} onDelete={() => requestDelete('recurring', item)} />)}
            </div>
            <form className="recurring-form" onSubmit={handleRecurringSubmit}>
              <h3>Schedule a transaction</h3>
              {recurringError && <div className="form-alert" role="alert">{recurringError}</div>}
              <div className="type-switch" role="group" aria-label="Recurring transaction type">
                <button type="button" className={recurringForm.type === 'EXPENSE' ? 'type-option selected expense' : 'type-option'} onClick={() => setRecurringForm({ ...recurringForm, type: 'EXPENSE' })}>Expense</button>
                <button type="button" className={recurringForm.type === 'INCOME' ? 'type-option selected income' : 'type-option'} onClick={() => setRecurringForm({ ...recurringForm, type: 'INCOME' })}>Income</button>
              </div>
              <label className="form-field"><span>Amount <small>(BDT)</small></span><input required min="0.01" step="0.01" type="number" inputMode="decimal" placeholder="0.00" value={recurringForm.amount} onChange={(event) => setRecurringForm({ ...recurringForm, amount: event.target.value })} /></label>
              <label className="form-field"><span>Category</span><input required maxLength="60" placeholder="e.g. Rent" value={recurringForm.category} onChange={(event) => setRecurringForm({ ...recurringForm, category: event.target.value })} /></label>
              <label className="form-field"><span>Frequency</span><select required value={recurringForm.frequency} onChange={(event) => setRecurringForm({ ...recurringForm, frequency: event.target.value })}><option value="WEEKLY">Weekly</option><option value="MONTHLY">Monthly</option><option value="YEARLY">Yearly</option></select></label>
              <label className="form-field"><span>First transaction date</span><input required type="date" min={todayInDhaka()} value={recurringForm.startDate} onChange={(event) => setRecurringForm({ ...recurringForm, startDate: event.target.value })} /></label>
              <label className="form-field"><span>Last transaction date <small>(optional)</small></span><input type="date" min={recurringForm.startDate} value={recurringForm.endDate} onChange={(event) => setRecurringForm({ ...recurringForm, endDate: event.target.value })} /></label>
              <label className="form-field"><span>Note <small>(optional)</small></span><input maxLength="500" placeholder="Add a reminder" value={recurringForm.note} onChange={(event) => setRecurringForm({ ...recurringForm, note: event.target.value })} /></label>
              <button className="submit-button" type="submit" disabled={isSavingRecurring}>{isSavingRecurring ? 'Saving…' : 'Create schedule'}<span aria-hidden="true">→</span></button>
            </form>
          </div>
        </section>}

        {route === '/reports' && <section className="data-health-panel" aria-labelledby="data-health-heading">
          <div className="data-health-heading"><div><p className="eyebrow">REVIEW YOUR RECORDS</p><h2 id="data-health-heading">Data health check</h2></div>
            {dataHealth && <span>Checked {dateLabel.format(new Date(dataHealth.checkedAt))}</span>}</div>
          {isLoadingDataHealth ? <div className="empty-state compact"><span className="loading-dot" />Checking your records…</div>
            : dataHealthError ? <div className="form-alert" role="alert">{dataHealthError}</div>
              : dataHealth && <>
                <p className="data-health-help">Possible exact duplicates, expenses at least 5× your average, and negative cash or bank balances are listed for you to review. Nothing is changed automatically.</p>
                {dataHealth.possibleDuplicates.length === 0 && dataHealth.largeExpenses.length === 0 && dataHealth.negativeAccounts.length === 0
                  ? <div className="data-health-clear"><span aria-hidden="true">✓</span><strong>No potential issues found.</strong><small>We checked your transactions and account balances.</small></div>
                  : <div className="data-health-findings">
                    {dataHealth.possibleDuplicates.length > 0 && <section><h3>Possible duplicate transactions <span>{dataHealth.possibleDuplicates.length}</span></h3>
                      {dataHealth.possibleDuplicates.map((item) => <article className="data-health-row" key={`${item.accountName}-${item.category}-${item.date}-${item.type}-${item.amount}`}>
                        <div><strong>{item.category} · {item.accountName}</strong><small>{item.date} · {item.type} · {item.copies} matching entries{item.note ? ` · ${item.note}` : ''}</small></div><b>{money.format(item.amount)}</b>
                      </article>)}</section>}
                    {dataHealth.largeExpenses.length > 0 && <section><h3>Unusually large expenses <span>{dataHealth.largeExpenses.length}</span></h3>
                      {dataHealth.largeExpenses.map((item) => <article className="data-health-row" key={item.transactionId}>
                        <div><strong>{item.category} · {item.accountName}</strong><small>{item.date}{item.note ? ` · ${item.note}` : ''}</small></div><b>{money.format(item.amount)}</b>
                      </article>)}</section>}
                    {dataHealth.negativeAccounts.length > 0 && <section><h3>Accounts below zero <span>{dataHealth.negativeAccounts.length}</span></h3>
                      {dataHealth.negativeAccounts.map((item) => <article className="data-health-row" key={item.accountId}>
                        <div><strong>{item.name}</strong><small>{item.type} balance</small></div><b className="expense-text">{money.format(item.balance)}</b>
                      </article>)}</section>}
                  </div>}
              </>}
        </section>}

        {route === '/reports' && <section className="monthly-statement" aria-labelledby="statement-heading">
          <div className="statement-header"><div><p className="eyebrow">PERSONAL FINANCE REPORT</p><h2 id="statement-heading">{formatMonth(budgetMonth)} statement</h2><p>{session.user.name} · Generated {dateLabel.format(new Date())}</p></div>
            <div className="statement-actions"><span>Select <strong>Print / Save PDF</strong>, then choose “Save as PDF”.</span><button className="primary-action" type="button" onClick={printStatement}>Print / Save PDF</button></div>
          </div>
          {isLoadingStatement ? <div className="empty-state compact"><span className="loading-dot" />Loading the month’s transactions…</div>
            : statementError ? <div className="form-alert" role="alert">{statementError}</div>
              : <>
                <div className="statement-totals">
                  <div><span>Income</span><strong className="income-text">{money.format(analytics?.monthIncome || 0)}</strong></div>
                  <div><span>Expenses</span><strong className="expense-text">{money.format(analytics?.monthExpense || 0)}</strong></div>
                  <div><span>Net cash flow</span><strong>{money.format(analytics?.monthBalance || 0)}</strong></div>
                </div>
                <div className="statement-columns">
                  <section className="statement-categories"><h3>Expenses by category</h3>
                    {!analytics?.expenseByCategory?.length ? <p className="statement-empty">No expenses recorded for this month.</p>
                      : <div className="statement-category-list">{analytics.expenseByCategory.map((category) => <div key={category.category}>
                        <span><strong>{category.category}</strong><small>{category.sharePercent}%</small></span><b>{money.format(category.amount)}</b>
                      </div>)}</div>}
                  </section>
                  <section className="statement-transactions"><div className="statement-table-heading"><h3>Transactions</h3><span>{statementTransactions.length}{statementTransactions.length >= 10000 ? '+' : ''} entries</span></div>
                    {!statementTransactions.length ? <p className="statement-empty">No transactions recorded for this month.</p>
                      : <div className="statement-table-wrap"><table><thead><tr><th>Date</th><th>Details</th><th>Type</th><th className="number-cell">Amount</th></tr></thead>
                        <tbody>{statementTransactions.map((transaction) => <tr key={transaction.id}>
                          <td>{dateLabel.format(new Date(`${transaction.transactionDate}T00:00:00`))}</td>
                          <td><strong>{transaction.category}</strong><small>{transaction.note || transaction.accountName || '—'}</small></td>
                          <td>{transaction.type === 'INCOME' ? 'Income' : 'Expense'}</td>
                          <td className={`number-cell ${transaction.type === 'INCOME' ? 'income-text' : 'expense-text'}`}>{transaction.type === 'INCOME' ? '+' : '−'}{money.format(transaction.amount)}</td>
                        </tr>)}</tbody></table></div>}
                  </section>
                </div>
              </>}
        </section>}
        {route === '/reports' && <div className="report-note"><span aria-hidden="true">i</span><p>This is a personal finance summary based on the transactions you recorded. Use the month selector above to change the report period.</p></div>}
        {deleteTarget && <div className="confirm-overlay" role="presentation" onMouseDown={(event) => {
          if (event.target === event.currentTarget && !isConfirmingDelete) setDeleteTarget(null)
        }}>
          <section className="confirm-dialog" data-confirm-dialog role="alertdialog" aria-modal="true" aria-labelledby="confirm-title" aria-describedby="confirm-description">
            <span className="confirm-icon" aria-hidden="true">!</span>
            <p className="eyebrow">PLEASE CONFIRM</p>
            <h2 id="confirm-title">Delete {deleteTarget.kind === 'goal' ? 'savings goal' : deleteTarget.kind === 'recurring' ? 'recurring schedule' : deleteTarget.kind}?</h2>
            <p id="confirm-description">{deleteTarget.kind === 'transaction'
              ? `This will permanently delete the ${deleteTarget.item.type.toLowerCase()} of ${money.format(deleteTarget.item.amount)} in ${deleteTarget.item.category}.`
              : deleteTarget.kind === 'budget'
                ? `This will permanently delete the ${deleteTarget.item.category} budget for ${formatMonth(deleteTarget.item.month)}.`
                : deleteTarget.kind === 'recurring'
                  ? `This will stop future ${deleteTarget.item.frequency.toLowerCase()} ${deleteTarget.item.type.toLowerCase()} entries for ${deleteTarget.item.category}. Existing transactions stay in your account.`
                  : `This will permanently delete “${deleteTarget.item.name}” and its savings progress.`}</p>
            <div className="confirm-actions">
              <button className="confirm-cancel" data-confirm-cancel type="button" disabled={isConfirmingDelete} onClick={() => setDeleteTarget(null)}>Cancel</button>
              <button className="confirm-delete" type="button" aria-busy={isConfirmingDelete} onClick={confirmDelete}>{isConfirmingDelete ? 'Deleting…' : 'Delete'}</button>
            </div>
          </section>
        </div>}
        <datalist id="category-suggestions-INCOME">{categorySuggestions.INCOME.map((category) => <option key={category} value={category} />)}</datalist>
        <datalist id="category-suggestions-EXPENSE">{categorySuggestions.EXPENSE.map((category) => <option key={category} value={category} />)}</datalist>
        </main>
        <footer className="dashboard-footer">Your financial space, thoughtfully organized.</footer>
      </div>
      <nav className="mobile-nav" aria-label="Main navigation">
        {NAV_ITEMS.map((item) => <NavItem key={item.path} item={item} active={route === item.path} navigate={navigate} compact />)}
      </nav>
    </div>
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
    <div className="transaction-info"><strong>{transaction.category}</strong><span>{transaction.accountName ? `${transaction.accountName} · ` : ''}{transaction.note || (isIncome ? 'Income' : 'Expense')} <i>·</i> {dateLabel.format(new Date(`${transaction.transactionDate}T00:00:00`))}</span></div>
    <strong className={`transaction-amount ${isIncome ? 'income' : 'expense'}`}>{isIncome ? '+' : '−'}{money.format(transaction.amount)}</strong>
    {(onEdit || onDelete) && <div className="row-actions">{onEdit && <button type="button" onClick={onEdit} aria-label={`Edit ${transaction.category}`}>Edit</button>}{onDelete && <button type="button" onClick={onDelete} aria-label={`Delete ${transaction.category}`}>Delete</button>}</div>}
  </article>
}

function NavItem({ item, active, navigate, compact = false }) {
  return <a className={`nav-item${active ? ' active' : ''}${compact ? ' compact' : ''}`} href={item.path}
    aria-current={active ? 'page' : undefined} onClick={(event) => {
      if (event.button !== 0 || event.metaKey || event.ctrlKey || event.shiftKey || event.altKey) return
      event.preventDefault()
      navigate(item.path)
    }}>
    <NavIcon name={item.icon} /><span>{item.label}</span>
  </a>
}

function NavIcon({ name }) {
  const shapes = {
    overview: <><rect x="3" y="3" width="8" height="8" rx="2" /><rect x="13" y="3" width="8" height="5" rx="2" /><rect x="13" y="10" width="8" height="11" rx="2" /><rect x="3" y="13" width="8" height="8" rx="2" /></>,
    accounts: <><rect x="3" y="5" width="18" height="14" rx="2" /><path d="M3 10h18M7 15h4" /></>,
    transactions: <><path d="M7 7h13M17 4l3 3-3 3" /><path d="M17 17H4m3-3-3 3 3 3" /></>,
    budgets: <><path d="M4 19V5m0 14h17" /><path d="m7 15 4-4 3 2 5-6" /></>,
    goals: <><circle cx="12" cy="12" r="9" /><circle cx="12" cy="12" r="5" /><circle cx="12" cy="12" r="1" /></>,
    recurring: <><path d="M20 7h-5l2-2" /><path d="M4 17h5l-2 2" /><path d="M6.1 9A7 7 0 0 1 18 7m-12 10a7 7 0 0 0 11.9-2" /></>,
    reports: <><path d="M5 20V10m7 10V4m7 16v-7" /><path d="M3 20h18" /></>,
  }
  return <svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round">{shapes[name]}</svg>
}

function RecurringCard({ item, onToggle, onDelete }) {
  const details = `${item.frequency.charAt(0)}${item.frequency.slice(1).toLowerCase()} · ${item.type.toLowerCase()}`
  const finished = Boolean(item.endDate && item.nextRunDate > item.endDate)
  return <article className={`recurring-card${item.active ? '' : ' paused'}`}>
    <div className="recurring-card-main">
      <span className={`transaction-icon ${item.type === 'INCOME' ? 'income' : 'expense'}`} aria-hidden="true">{item.type === 'INCOME' ? '↓' : '↑'}</span>
      <div className="recurring-card-copy"><strong>{item.category}</strong><span>{details}{item.note ? ` · ${item.note}` : ''}</span>
        <small>{finished ? 'Schedule ended' : item.active ? `Next on ${dateLabel.format(new Date(`${item.nextRunDate}T00:00:00`))}` : 'Paused'}{item.endDate ? ` · Ends ${dateLabel.format(new Date(`${item.endDate}T00:00:00`))}` : ''}</small>
      </div>
      <strong className={`recurring-amount ${item.type === 'INCOME' ? 'income-text' : 'expense-text'}`}>{money.format(item.amount)}</strong>
    </div>
    <div className="recurring-card-actions"><button type="button" disabled={finished} onClick={onToggle}>{finished ? 'Completed' : item.active ? 'Pause' : 'Resume'}</button><button type="button" onClick={onDelete}>Delete</button></div>
  </article>
}

function BudgetRow({ budget, isCurrentMonth, onEdit, onDelete }) {
  const spent = Number(budget.spent)
  const limit = Number(budget.monthlyLimit)
  const percent = limit > 0 ? (spent / limit) * 100 : 0
  const overBudget = percent > 100
  const progress = Math.min(percent, 100)
  const currentDay = Number(todayInDhaka().slice(8, 10))
  const daysInMonth = new Date(Number(budget.month.slice(0, 4)), Number(budget.month.slice(5, 7)), 0).getDate()
  const projected = isCurrentMonth && currentDay >= 3 && spent > 0
    ? Math.round((spent / currentDay) * daysInMonth * 100) / 100 : null
  const projectedOver = projected !== null && projected > limit
  return <article className="budget-row">
    <div className="budget-row-top"><div><strong>{budget.category}</strong><span>{money.format(spent)} spent of {money.format(limit)}</span></div>
      <div className="budget-row-actions"><button type="button" onClick={onEdit} aria-label={`Edit ${budget.category} budget`}>Edit</button><button type="button" onClick={onDelete} aria-label={`Delete ${budget.category} budget`}>Delete</button></div>
    </div>
    <div className="budget-progress" role="progressbar" aria-label={`${budget.category} budget used`} aria-valuemin="0" aria-valuemax="100" aria-valuenow={Math.round(progress)}>
      <span className={overBudget ? 'over-budget' : ''} style={{ width: `${progress}%` }} />
    </div>
    <span className={overBudget ? 'budget-remaining over-budget' : 'budget-remaining'}>{overBudget ? `${money.format(Math.abs(Number(budget.remaining)))} over limit` : `${money.format(budget.remaining)} remaining`}</span>
    {projected !== null && <span className={`budget-projection ${projectedOver ? 'projected-over' : ''}`}>
      {projectedOver
        ? `At this pace, month-end spending may reach ${money.format(projected)} (${money.format(projected - limit)} over).`
        : `At this pace, month-end spending may reach ${money.format(projected)}.`}
    </span>}
  </article>
}

function GoalCard({ goal, token, onContributed, onEdit, onDelete }) {
  const [showContribution, setShowContribution] = useState(false)
  const [showHistory, setShowHistory] = useState(false)
  const [contributions, setContributions] = useState([])
  const [contributionAmount, setContributionAmount] = useState('')
  const [contributionNote, setContributionNote] = useState('')
  const [contributionError, setContributionError] = useState('')
  const [isSavingContribution, setIsSavingContribution] = useState(false)
  const progress = Number(goal.completionPercent)
  async function toggleHistory() {
    if (!showHistory) {
      try { setContributions(await getGoalContributions(token, goal.id)) }
      catch (error) { setContributionError(error.message); return }
    }
    setContributionError(''); setShowHistory(!showHistory)
  }
  async function submitContribution(event) {
    event.preventDefault(); setContributionError(''); setIsSavingContribution(true)
    try {
      const amount = Number(contributionAmount)
      await addGoalContribution(token, goal.id, { amount, note: contributionNote.trim() || null })
      setContributionAmount(''); setContributionNote(''); setShowContribution(false)
      await onContributed(amount)
      if (showHistory) setContributions(await getGoalContributions(token, goal.id))
    } catch (error) { setContributionError(error.message) }
    finally { setIsSavingContribution(false) }
  }
  return <article className={`goal-card ${goal.completed ? 'completed' : ''}`}>
    <div className="goal-card-heading"><span className="goal-badge" aria-hidden="true">{goal.completed ? '✓' : '☆'}</span>
      <div className="goal-card-title"><strong>{goal.name}</strong><span>{goal.targetDate ? `Target ${dateLabel.format(new Date(`${goal.targetDate}T00:00:00`))}` : 'No target date'}</span></div>
      <div className="goal-row-actions"><button type="button" onClick={onEdit} aria-label={`Edit ${goal.name} goal`}>Edit</button><button type="button" onClick={onDelete} aria-label={`Delete ${goal.name} goal`}>Delete</button></div>
    </div>
    <div className="goal-values"><strong>{money.format(goal.currentAmount)}</strong><span>of {money.format(goal.targetAmount)}</span><b>{progress}%</b></div>
    <div className="goal-progress" role="progressbar" aria-label={`${goal.name} savings progress`} aria-valuemin="0" aria-valuemax="100" aria-valuenow={Math.round(progress)}><span style={{ width: `${progress}%` }} /></div>
    <div className="goal-card-bottom"><span>{goal.note || (goal.completed ? 'You reached your goal!' : 'Every little bit adds up.')}</span><strong>{goal.completed ? 'Completed' : `${money.format(goal.remainingAmount)} to go`}</strong></div>
    <div className="goal-contribution-actions"><button type="button" disabled={goal.completed} onClick={() => { setShowContribution(!showContribution); setContributionError('') }}>{goal.completed ? 'Goal completed' : 'Add contribution'}</button>
      <button type="button" onClick={toggleHistory}>{showHistory ? 'Hide history' : 'History'}</button></div>
    {contributionError && <div className="form-alert" role="alert">{contributionError}</div>}
    {showContribution && <form className="goal-contribution-form" onSubmit={submitContribution}>
      <label className="form-field"><span>Amount <small>(BDT)</small></span><input autoFocus required min="0.01" max={goal.remainingAmount} step="0.01" type="number" inputMode="decimal" value={contributionAmount} onChange={(event) => setContributionAmount(event.target.value)} /></label>
      <label className="form-field"><span>Note <small>(optional)</small></span><input maxLength="300" value={contributionNote} onChange={(event) => setContributionNote(event.target.value)} placeholder="e.g. Monthly savings" /></label>
      <button className="submit-button" type="submit" disabled={isSavingContribution}>{isSavingContribution ? 'Adding…' : 'Add to goal'}</button>
    </form>}
    {showHistory && <div className="goal-contribution-history">{contributions.length === 0 ? <span>No contributions recorded yet.</span> : contributions.map((item) => <div key={item.id}>
      <span>{item.note || 'Contribution'} · {dateLabel.format(new Date(item.createdAt))}</span><strong>+{money.format(item.amount)}</strong>
    </div>)}</div>}
  </article>
}

const monthShortLabel = new Intl.DateTimeFormat('en', { month: 'short' })
const monthFullLabel = new Intl.DateTimeFormat('en', { month: 'long', year: 'numeric' })
const formatMonth = (month) => monthFullLabel.format(new Date(`${month}-01T00:00:00`))

function CashFlowChart({ trend }) {
  if (!trend.length || trend.every((month) => Number(month.income) === 0 && Number(month.expense) === 0)) {
    return <div className="empty-state chart-empty">Add transactions to see your six-month cash flow.</div>
  }
  const maximum = Math.max(1, ...trend.flatMap((month) => [Number(month.income), Number(month.expense), Math.abs(Number(month.income) - Number(month.expense))]))
  return <div className="cashflow-chart" role="img" aria-label="Income, expense, and net cash flow comparison for the selected month and previous five months">
    <div className="chart-legend"><span><i className="legend-income" />Income</span><span><i className="legend-expense" />Expenses</span><span><i className="legend-net" />Net cash flow</span></div>
    <div className="chart-columns">{trend.map((month) => {
      const incomeHeight = Number(month.income) / maximum * 100
      const expenseHeight = Number(month.expense) / maximum * 100
      const netCashFlow = Number(month.income) - Number(month.expense)
      const netHeight = Math.abs(netCashFlow) / maximum * 100
      return <div className="chart-month" key={month.month}>
        <div className="chart-bars"><span className="chart-bar-income" style={{ height: `${incomeHeight}%` }} title={`Income ${money.format(month.income)}`} />
          <span className="chart-bar-expense" style={{ height: `${expenseHeight}%` }} title={`Expenses ${money.format(month.expense)}`} />
          <span className={`chart-bar-net${netCashFlow < 0 ? ' negative' : ''}`} style={{ height: `${netHeight}%` }} title={`Net cash flow ${money.format(netCashFlow)}`} /></div>
        <span>{monthShortLabel.format(new Date(`${month.month}-01T00:00:00`))}</span>
      </div>
    })}</div>
  </div>
}

function CategoryChart({ categories }) {
  if (!categories.length) return <div className="empty-state chart-empty">No expenses recorded for this month.</div>
  const palette = ['#3975d5', '#45aa8a', '#edaa52', '#d96f78', '#8b75d7', '#95a2b4']
  const sorted = [...categories].sort((first, second) => Number(second.amount) - Number(first.amount))
  const topCategories = sorted.slice(0, 5).map((category, index) => ({ ...category, color: palette[index] }))
  if (sorted.length > 5) {
    topCategories.push({
      category: 'Other',
      amount: sorted.slice(5).reduce((sum, category) => sum + Number(category.amount), 0),
      color: palette[5],
    })
  }
  const total = topCategories.reduce((sum, category) => sum + Number(category.amount), 0)
  let accumulatedShare = 0
  const gradientSegments = topCategories.map((category) => {
    const start = accumulatedShare
    accumulatedShare += Number(category.amount) / total * 100
    return `${category.color} ${start}% ${accumulatedShare}%`
  })
  return <div className="category-chart">
    <div className="category-donut" role="img" aria-label={`Total spending ${money.format(total)} split across ${categories.length} categories`} style={{ background: `conic-gradient(${gradientSegments.join(', ')})` }}>
      <div className="category-donut-center"><span>Total spending</span><strong>{money.format(total)}</strong></div>
    </div>
    <ul className="category-legend">
      {topCategories.map((category) => {
        const share = total > 0 ? Number(category.amount) / total * 100 : 0
        return <li key={category.category}>
          <span className="category-legend-name"><i style={{ backgroundColor: category.color }} />{category.category}</span>
          <span className="category-legend-value"><strong>{money.format(category.amount)}</strong><small>{Math.round(share)}%</small></span>
        </li>
      })}
    </ul>
  </div>
}

export default Dashboard
