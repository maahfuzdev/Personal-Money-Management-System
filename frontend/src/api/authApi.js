const API_BASE_URL = (import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081/api/v1').replace(/\/$/, '')

async function sendAuthRequest(path, body) {
  let response

  try {
    response = await fetch(`${API_BASE_URL}${path}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body),
    })
  } catch {
    throw new Error('Could not reach the server. Check that the backend is running and try again.')
  }

  const payload = await response.json().catch(() => null)
  if (!response.ok) {
    const error = new Error(payload?.message || 'Something went wrong. Please try again.')
    error.status = response.status
    error.fieldErrors = payload?.fieldErrors || {}
    throw error
  }

  return payload
}

export function registerAccount({ name, email, password }) {
  return sendAuthRequest('/auth/register', { name, email, password })
}

export function signIn({ email, password }) {
  return sendAuthRequest('/auth/login', { email, password })
}

export function signInWithGoogle(idToken) {
  return sendAuthRequest('/auth/google', { idToken })
}

export function refreshSession(refreshToken) {
  return sendAuthRequest('/auth/refresh', { refreshToken })
}

export function logoutSession(refreshToken) {
  return sendAuthRequest('/auth/logout', { refreshToken }).catch(() => null)
}
