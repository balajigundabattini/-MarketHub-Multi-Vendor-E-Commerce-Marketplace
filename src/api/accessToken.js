let accessToken = null
let sessionUser = null

export const getAccessToken = () => accessToken

export const setAccessToken = (token) => {
  accessToken = token
}

export const setSession = (token, user) => {
  accessToken = token
  sessionUser = user || null
}

export const getSessionUser = () => sessionUser

export const clearAccessToken = () => {
  accessToken = null
  sessionUser = null
  if (typeof window !== 'undefined') window.dispatchEvent(new Event('markethub:session-cleared'))
}
