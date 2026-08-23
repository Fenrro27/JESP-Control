const TOKEN_KEY = 'jesp_token'
const USER_KEY = 'jesp_user'

export function getToken() {
  return localStorage.getItem(TOKEN_KEY)
}

export function getStoredUser() {
  try {
    const raw = localStorage.getItem(USER_KEY)
    return raw ? JSON.parse(raw) : null
  } catch {
    return null
  }
}

export function storeSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token)
  localStorage.setItem(USER_KEY, JSON.stringify(user))
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
}

function handleUnauthorized() {
  clearSession()
  if (!window.location.hash.includes('/login')) {
    window.location.hash = '#/login'
  }
}

export class ApiError extends Error {
  constructor(status, message) {
    super(message || `HTTP ${status}`)
    this.status = status
  }
}

/**
 * Cliente de la API. Añade el token JWT y redirige al login en caso de 401.
 */
export async function api(path, { method = 'GET', body = undefined } = {}) {
  const headers = {}
  const token = getToken()
  if (token) headers['Authorization'] = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  let response
  try {
    response = await fetch(path, {
      method,
      headers,
      body: body === undefined ? null : JSON.stringify(body),
    })
  } catch (err) {
    throw new ApiError(0, 'No se pudo conectar con el servidor')
  }

  if (response.status === 401 && token) {
    handleUnauthorized()
    throw new ApiError(401, 'Sesión expirada')
  }

  if (!response.ok) {
    let message = `HTTP ${response.status}`
    const text = await response.text()
    try {
      const json = JSON.parse(text)
      message = json.error || json.message || text || message
    } catch {
      if (text) message = text
    }
    throw new ApiError(response.status, message)
  }

  if (response.status === 204) return null
  const text = await response.text()
  return text ? JSON.parse(text) : text
}
