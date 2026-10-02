const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')

async function request(path, token, options = {}) {
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

  if (response.status === 204) return null
  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(payload?.message || 'Something went wrong. Please try again.')
    error.status = response.status
    throw error
  }
  return payload
}

export function getTransactions(token) {
  return request('/transactions', token)
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
