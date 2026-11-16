import { useState } from 'react'
import { Link } from 'react-router-dom'
import apiClient from '../api/client'

const ForgotPassword = () => {
  const [email, setEmail] = useState('')
  const [message, setMessage] = useState('')

  const submit = async (event) => {
    event.preventDefault()
    try {
      await apiClient.post('/auth/forgot-password', { email })
      setMessage('If an active account uses that email, password reset instructions will be sent shortly.')
    } catch {
      setMessage('We could not process the request right now. Please try again later.')
    }
  }

  return (
    <section className="container py-5" style={{ maxWidth: 520 }}>
      <h1 className="h3">Reset your password</h1>
      <p>Enter your account email and we’ll send a reset link if it exists.</p>
      <form onSubmit={submit}>
        <label htmlFor="reset-email" className="form-label">Email</label>
        <input id="reset-email" type="email" className="form-control mb-3" required value={email} onChange={(event) => setEmail(event.target.value)} />
        <button className="btn btn-primary" type="submit">Send reset link</button>
      </form>
      {message && <p className="mt-3" role="status">{message}</p>}
      <Link to="/login">Back to login</Link>
    </section>
  )
}

export default ForgotPassword
