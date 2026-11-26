import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import apiClient from '../api/client'
import { clearAccessToken } from '../api/accessToken'
import '../Styles/Register.css'

const Register = () => {
    const [displayName, setDisplayName] = useState('')
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [confirmPassword, setConfirmPassword] = useState('')

    const [showPassword, setShowPassword] = useState(false)
    const [showConfirmPassword, setShowConfirmPassword] = useState(false)
    const [isSubmitting, setIsSubmitting] = useState(false)

    const navigate = useNavigate()

    const submitHandler = async (e) => {
        e.preventDefault()

        if (isSubmitting) return

        if (password !== confirmPassword) {
            toast.error('Passwords do not match')
            return
        }

        const passwordBytes = new TextEncoder().encode(password).length

        if (passwordBytes < 12 || passwordBytes > 72) {
            toast.error('Password must be between 12 and 72 UTF-8 bytes')
            return
        }

        setIsSubmitting(true)

        try {
            const userData = {
                displayName: displayName.trim(),
                email: email.trim(),
                password
            }

            await apiClient.post('/auth/register', userData)

            clearAccessToken()

            toast.success(
                'Check your email to verify your account before logging in.'
            )

            setDisplayName('')
            setEmail('')
            setPassword('')
            setConfirmPassword('')

            navigate('/login', { replace: true })

        } catch (err) {
            toast.error(
                err.response?.data?.detail || 'Registration failed'
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="mh-register-page">

            {/* Left introduction section */}
            <section className="mh-register-intro">

                <Link to="/" className="mh-register-brand">
                    <span className="mh-register-brand-icon">
                        <svg viewBox="0 0 24 24" fill="none">
                            <path
                                d="M5 8h14l-1 13H6L5 8Z"
                                stroke="currentColor"
                                strokeWidth="1.8"
                                strokeLinejoin="round"
                            />
                            <path
                                d="M9 9V6a3 3 0 0 1 6 0v3"
                                stroke="currentColor"
                                strokeWidth="1.8"
                                strokeLinecap="round"
                            />
                        </svg>
                    </span>
                    <span>Market<span>Hub</span></span>
                </Link>

                <div className="mh-register-intro-content">

                    <p className="mh-register-eyebrow">
                        JOIN THE MARKETHUB COMMUNITY
                    </p>

                    <h1>
                        Start your
                        <br />
                        <span>Shopping</span> Journey.
                    </h1>

                    <p className="mh-register-description">
                        Create your account and explore a better way to shop
                        with MarketHub.
                    </p>

                    <div className="mh-register-benefits">

                        <div className="mh-register-benefit">
                            <span className="mh-register-benefit-icon">%</span>
                            <div>
                                <h3>Best Prices</h3>
                                <p>Get more for less.</p>
                            </div>
                        </div>

                        <div className="mh-register-benefit">
                            <span className="mh-register-benefit-icon">✓</span>
                            <div>
                                <h3>Secure Shopping</h3>
                                <p>Shop with confidence.</p>
                            </div>
                        </div>

                        <div className="mh-register-benefit">
                            <span className="mh-register-benefit-icon">↗</span>
                            <div>
                                <h3>Multiple Sellers</h3>
                                <p>Explore products in one place.</p>
                            </div>
                        </div>

                    </div>
                </div>

                {/* Shopping illustration */}
                <div className="mh-register-illustration" aria-hidden="true">
                    <div className="mh-register-illustration-circle"></div>

                    <div className="mh-register-bag mh-register-bag-one">
                        <span>M</span>
                    </div>

                    <div className="mh-register-bag mh-register-bag-two"></div>

                    <div className="mh-register-cart">
                        <svg viewBox="0 0 180 130" fill="none">
                            <path
                                d="M12 14h18l17 76h94l20-58H39"
                                stroke="currentColor"
                                strokeWidth="7"
                                strokeLinecap="round"
                                strokeLinejoin="round"
                            />
                            <path
                                d="M48 90h94"
                                stroke="currentColor"
                                strokeWidth="6"
                                strokeLinecap="round"
                            />
                            <circle cx="61" cy="111" r="10" fill="currentColor" />
                            <circle cx="132" cy="111" r="10" fill="currentColor" />
                        </svg>
                    </div>
                </div>

                <p className="mh-register-tagline">
                    Shop Smart. Live Better.
                </p>

            </section>

            {/* Right registration section */}
            <section className="mh-register-form-section">

                <div className="mh-register-card">

                    <div className="mh-register-heading">
                        <span className="mh-register-heading-line"></span>
                        <h2>Create Your Account</h2>
                        <p>Join MarketHub and start shopping today!</p>
                    </div>

                    <form onSubmit={submitHandler}>

                        {/* Full name */}
                        <div className="mh-register-form-group">
                            <label htmlFor="register-name">
                                Full Name <span>*</span>
                            </label>

                            <div className="mh-register-input-wrapper">
                                <svg viewBox="0 0 24 24" fill="none">
                                    <circle
                                        cx="12"
                                        cy="8"
                                        r="4"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                    />
                                    <path
                                        d="M4 21v-2a8 8 0 0 1 16 0v2"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                        strokeLinecap="round"
                                    />
                                </svg>

                                <input
                                    id="register-name"
                                    type="text"
                                    placeholder="Enter your full name"
                                    value={displayName}
                                    onChange={(e) => setDisplayName(e.target.value)}
                                    autoComplete="name"
                                    required
                                />
                            </div>
                        </div>

                        {/* Email */}
                        <div className="mh-register-form-group">
                            <label htmlFor="register-email">
                                Email Address <span>*</span>
                            </label>

                            <div className="mh-register-input-wrapper">
                                <svg viewBox="0 0 24 24" fill="none">
                                    <rect
                                        x="3"
                                        y="5"
                                        width="18"
                                        height="14"
                                        rx="2"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                    />
                                    <path
                                        d="m4 7 8 6 8-6"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                        strokeLinecap="round"
                                        strokeLinejoin="round"
                                    />
                                </svg>

                                <input
                                    id="register-email"
                                    type="email"
                                    placeholder="Enter your email address"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoComplete="email"
                                    required
                                />
                            </div>
                        </div>

                        {/* Password */}
                        <div className="mh-register-form-group">
                            <label htmlFor="register-password">
                                Password <span>*</span>
                            </label>

                            <div className="mh-register-input-wrapper">
                                <svg viewBox="0 0 24 24" fill="none">
                                    <rect
                                        x="4"
                                        y="10"
                                        width="16"
                                        height="11"
                                        rx="2"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                    />
                                    <path
                                        d="M8 10V7a4 4 0 1 1 8 0v3"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                        strokeLinecap="round"
                                    />
                                </svg>

                                <input
                                    id="register-password"
                                    type={showPassword ? 'text' : 'password'}
                                    placeholder="Create a password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    autoComplete="new-password"
                                    required
                                />

                                <button
                                    type="button"
                                    className="mh-register-password-toggle"
                                    onClick={() => setShowPassword(!showPassword)}
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showPassword ? 'Hide' : 'Show'}
                                </button>
                            </div>
                        </div>

                        {/* Confirm password */}
                        <div className="mh-register-form-group">
                            <label htmlFor="register-confirm-password">
                                Confirm Password <span>*</span>
                            </label>

                            <div className="mh-register-input-wrapper">
                                <svg viewBox="0 0 24 24" fill="none">
                                    <rect
                                        x="4"
                                        y="10"
                                        width="16"
                                        height="11"
                                        rx="2"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                    />
                                    <path
                                        d="M8 10V7a4 4 0 1 1 8 0v3"
                                        stroke="currentColor"
                                        strokeWidth="1.7"
                                        strokeLinecap="round"
                                    />
                                </svg>

                                <input
                                    id="register-confirm-password"
                                    type={showConfirmPassword ? 'text' : 'password'}
                                    placeholder="Confirm your password"
                                    value={confirmPassword}
                                    onChange={(e) => setConfirmPassword(e.target.value)}
                                    autoComplete="new-password"
                                    required
                                />

                                <button
                                    type="button"
                                    className="mh-register-password-toggle"
                                    onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                                    aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showConfirmPassword ? 'Hide' : 'Show'}
                                </button>
                            </div>
                        </div>

                        <button
                            type="submit"
                            className="mh-register-button"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Creating Account...' : 'Register'}
                            {!isSubmitting && <span>→</span>}
                        </button>

                    </form>

                    <div className="mh-register-divider">
                        <span>OR</span>
                    </div>

                    <p className="mh-register-login-prompt">
                        Already have an account?
                        <Link to="/login">Login here</Link>
                    </p>

                    <div className="mh-register-security">
                        <span>✓</span>
                        Your information is safe with us.
                    </div>

                </div>

                <p className="mh-register-footer">
                    © {new Date().getFullYear()} MarketHub. All rights reserved.
                </p>

            </section>

        </main>
    )
}

export default Register