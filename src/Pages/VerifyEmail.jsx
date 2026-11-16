import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import apiClient from '../api/client'

const VerifyEmail = () => {
  const [params] = useSearchParams()
  const token = params.get('token')
  const [message, setMessage] = useState(token ? '' : 'This verification link is incomplete.')
  const [email, setEmail] = useState('')
  const [resendMessage, setResendMessage] = useState('')

  const verify = async () => {
    try {
      await apiClient.post('/auth/verify-email', { token })
      setMessage('Your email is verified. You can now log in.')
    } catch {
      setMessage('This verification link is invalid or expired. You can request another one below.')
    }
  }

  const resend = async (event) => {
    event.preventDefault()
    try {
      await apiClient.post('/auth/resend-verification', { email })
      setResendMessage('If the account needs verification, a new email has been sent.')
    } catch {
      setResendMessage('We could not send the email right now. Please try again later.')
    }
  }

  return (
    <section className="container py-5" style={{ maxWidth: 520 }}>
      <h1 className="h3">Email verification</h1>
      <p role="status">{message}</p>
      {token && !message && <button className="btn btn-primary mb-3" type="button" onClick={verify}>Verify email</button>}
      <form onSubmit={resend} className="mb-3">
        <label htmlFor="verify-email" className="form-label">Resend verification email</label>
        <input id="verify-email" type="email" className="form-control mb-2" required value={email} onChange={(event) => setEmail(event.target.value)} />
        <button className="btn btn-outline-primary" type="submit">Resend email</button>
        {resendMessage && <p className="mt-2" role="status">{resendMessage}</p>}
      </form>
      <Link to="/login">Go to login</Link>
    </section>
  )
}

export default VerifyEmail
