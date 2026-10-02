import { refreshSession } from './authApi.js'

const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1').replace(/\/$/, '')
let refreshInFlight = null
let currentRefreshToken = null
let sessionGeneration = 0

export function setRefreshToken(token) {
  currentRefreshToken = token
  sessionGeneration += 1
  refreshInFlight = null
}

async function authorizedFetch(path, token, options = {}, allowRefresh = true) {
  let response
  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      ...options,
      headers: {
        Authorization: `Bearer ${token}`,
        ...(options.body ? { 'Content-Type': 'application/json' } : {}),
        ...options.headers,
      },
    })
  } catch {
    throw new Error('Could not reach the server. Check that the backend is running and try again.')
  }

  if (response.status === 401 && allowRefresh) {
    if (currentRefreshToken) {
      if (!refreshInFlight) {
        const generation = sessionGeneration
        const pendingRefresh = refreshSession(currentRefreshToken)
          .then((tokens) => {
            if (generation !== sessionGeneration) throw new Error('The account session changed. Please retry your request.')
            currentRefreshToken = tokens.refreshToken
            window.dispatchEvent(new CustomEvent('auth:session-refreshed', { detail: tokens }))
            return tokens
          })
          .catch((error) => {
            if (error.status === 401 && generation === sessionGeneration) {
              currentRefreshToken = null
              window.dispatchEvent(new CustomEvent('auth:session-expired'))
            }
            throw error
          })
          .finally(() => { if (refreshInFlight === pendingRefresh) refreshInFlight = null })
        refreshInFlight = pendingRefresh
      }
      const tokens = await refreshInFlight
      return authorizedFetch(path, tokens.accessToken, options, false)
    }
  }
  return response
}

async function request(path, token, options = {}) {
  const response = await authorizedFetch(path, token, options)

  if (response.status === 204) return null
  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(payload?.message || 'Something went wrong. Please try again.')
    error.status = response.status
    throw error
  }
  return payload
}

export function getTransactions(token, filters = {}) {
  const params = new URLSearchParams()
  params.set('page', String(filters.page ?? 0))
  params.set('size', String(filters.size ?? 10))
  if (filters.type) params.set('type', filters.type)
  if (filters.search) params.set('search', filters.search)
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)
  return request(`/transactions?${params.toString()}`, token)
}

export function getCategorySuggestions(token, type) {
  const query = type ? `?type=${encodeURIComponent(type)}` : ''
  return request(`/transactions/categories${query}`, token)
}

export async function exportTransactions(token, filters = {}) {
  const params = new URLSearchParams()
  if (filters.type) params.set('type', filters.type)
  if (filters.search) params.set('search', filters.search)
  if (filters.startDate) params.set('startDate', filters.startDate)
  if (filters.endDate) params.set('endDate', filters.endDate)

  const response = await authorizedFetch(`/transactions/export.csv?${params.toString()}`, token)

  if (!response.ok) {
    const payload = await response.json().catch(() => null)
    const error = new Error(payload?.message || 'Could not export transactions. Please try again.')
    error.status = response.status
    throw error
  }
  return response.blob()
}

export function getTransactionSummary(token) {
  return request('/transactions/summary', token)
}

export function createTransaction(token, transaction) {
  return request('/transactions', token, { method: 'POST', body: JSON.stringify(transaction) })
}

export function updateTransaction(token, id, transaction) {
  return request(`/transactions/${id}`, token, { method: 'PUT', body: JSON.stringify(transaction) })
}

export function deleteTransaction(token, id) {
  return request(`/transactions/${id}`, token, { method: 'DELETE' })
}

export function getBudgets(token, month) {
  return request(`/budgets?month=${encodeURIComponent(month)}`, token)
}

export function createBudget(token, budget) {
  return request('/budgets', token, { method: 'POST', body: JSON.stringify(budget) })
}

export function updateBudget(token, id, budget) {
  return request(`/budgets/${id}`, token, { method: 'PUT', body: JSON.stringify(budget) })
}

export function deleteBudget(token, id) {
  return request(`/budgets/${id}`, token, { method: 'DELETE' })
}

export function getGoals(token) {
  return request('/goals', token)
}

export function getRecurringTransactions(token) {
  return request('/recurring-transactions', token)
}

export function createRecurringTransaction(token, recurring) {
  return request('/recurring-transactions', token, { method: 'POST', body: JSON.stringify(recurring) })
}

export function setRecurringTransactionActive(token, id, active) {
  return request(`/recurring-transactions/${id}/active`, token, { method: 'PATCH', body: JSON.stringify({ active }) })
}

export function deleteRecurringTransaction(token, id) {
  return request(`/recurring-transactions/${id}`, token, { method: 'DELETE' })
}

export function createGoal(token, goal) {
  return request('/goals', token, { method: 'POST', body: JSON.stringify(goal) })
}

export function updateGoal(token, id, goal) {
  return request(`/goals/${id}`, token, { method: 'PUT', body: JSON.stringify(goal) })
}

export function deleteGoal(token, id) {
  return request(`/goals/${id}`, token, { method: 'DELETE' })
}

export function getDashboardAnalytics(token, month) {
  return request(`/dashboard/analytics?month=${encodeURIComponent(month)}`, token)
}
