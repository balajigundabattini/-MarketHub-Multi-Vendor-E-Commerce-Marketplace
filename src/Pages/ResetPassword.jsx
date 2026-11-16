import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import apiClient from '../api/client'

const ResetPassword = () => {
  const [params] = useSearchParams()
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [message, setMessage] = useState('')
  const [done, setDone] = useState(false)
  const token = params.get('token')

  const submit = async (event) => {
    event.preventDefault()
    if (password !== confirmation) {
      setMessage('The passwords do not match.')
      return
    }
    try {
      await apiClient.post('/auth/reset-password', { token, password })
      setDone(true)
      setMessage('Your password has been changed. Log in with your new password.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'This reset link is invalid or expired.')
    }
  }

  return (
    <section className="container py-5" style={{ maxWidth: 520 }}>
      <h1 className="h3">Choose a new password</h1>
      {!token && <p role="alert">The reset link is incomplete.</p>}
      {token && !done && <form onSubmit={submit}>
        <label htmlFor="new-password" className="form-label">New password (at least 12 characters)</label>
        <input id="new-password" type="password" className="form-control mb-3" minLength={12} maxLength={72} autoComplete="new-password" required value={password} onChange={(event) => setPassword(event.target.value)} />
        <label htmlFor="confirm-password" className="form-label">Confirm password</label>
        <input id="confirm-password" type="password" className="form-control mb-3" minLength={12} maxLength={72} autoComplete="new-password" required value={confirmation} onChange={(event) => setConfirmation(event.target.value)} />
        <button className="btn btn-primary" type="submit">Change password</button>
      </form>}
      {message && <p className="mt-3" role="status">{message}</p>}
      {done && <Link to="/login">Go to login</Link>}
    </section>
  )
}

export default ResetPassword
