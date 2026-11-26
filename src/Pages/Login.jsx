import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { toast } from 'react-toastify'
import apiClient from '../api/client'
import { useAuth } from '../auth/useAuth'
import '../Styles/login.css'

const Login = () => {
    const [email, setEmail] = useState('')
    const [password, setPassword] = useState('')
    const [showPassword, setShowPassword] = useState(false)
    const [isSubmitting, setIsSubmitting] = useState(false)

    const navigate = useNavigate()
    const location = useLocation()
    const { signIn } = useAuth()

    const submitHandler = async (e) => {
        e.preventDefault()

        if (isSubmitting) return

        setIsSubmitting(true)

        try {
            const response = await apiClient.post('/auth/login', {
                email: email.trim(),
                password
            })

            signIn(response.data)

            toast.success('Login successful!')

            const destination =
                location.state?.from?.pathname ||
                ((response.data.user?.roles || []).includes('CUSTOMER')
                    ? '/account'
                    : '/')

            navigate(destination, { replace: true })
        } catch (err) {
            toast.error(
                err.response?.data?.detail || 'Invalid email or password'
            )
        } finally {
            setIsSubmitting(false)
        }
    }

    return (
        <main className="mh-auth-page">
            {/* Left side - MarketHub introduction */}
            <section className="mh-auth-intro">
                <Link to="/" className="mh-brand">
                    <span className="mh-brand-icon">
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

                <div className="mh-intro-content">
                    <p className="mh-eyebrow">YOUR ONE-STOP SHOPPING DESTINATION</p>

                    <h1>
                        Welcome to
                        <br />
                        <span>MarketHub.</span>
                    </h1>

                    <p className="mh-intro-description">
                        Shop smarter. Discover more. Find everything you need
                        in one place.
                    </p>

                    <div className="mh-benefits">
                        <div className="mh-benefit">
                            <span className="mh-benefit-icon">%</span>
                            <div>
                                <h3>Great Deals</h3>
                                <p>Find products at great prices.</p>
                            </div>
                        </div>

                        <div className="mh-benefit">
                            <span className="mh-benefit-icon">✓</span>
                            <div>
                                <h3>Secure Shopping</h3>
                                <p>A simple and secure shopping experience.</p>
                            </div>
                        </div>

                        <div className="mh-benefit">
                            <span className="mh-benefit-icon">↗</span>
                            <div>
                                <h3>Wide Selection</h3>
                                <p>Explore products from different sellers.</p>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="mh-shopping-illustration" aria-hidden="true">
                    <div className="mh-illustration-circle"></div>

                    <div className="mh-shopping-bag mh-bag-one">
                        <span>M</span>
                    </div>

                    <div className="mh-shopping-bag mh-bag-two"></div>

                    <div className="mh-cart">
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

                <p className="mh-intro-footer">
                    Everything you need, all in one place.
                </p>
            </section>

            {/* Right side - Login form */}
            <section className="mh-auth-form-section">
                <div className="mh-login-card">
                    <div className="mh-login-heading">
                        <h2>Welcome Back!</h2>
                        <p>Sign in to continue to your MarketHub account.</p>
                    </div>

                    <form onSubmit={submitHandler}>
                        <div className="mh-form-group">
                            <label htmlFor="login-email">Email Address</label>

                            <div className="mh-input-wrapper">
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
                                    id="login-email"
                                    type="email"
                                    placeholder="Enter your email address"
                                    value={email}
                                    onChange={(e) => setEmail(e.target.value)}
                                    autoComplete="email"
                                    required
                                />
                            </div>
                        </div>

                        <div className="mh-form-group">
                            <label htmlFor="login-password">Password</label>

                            <div className="mh-input-wrapper">
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
                                    id="login-password"
                                    type={showPassword ? 'text' : 'password'}
                                    placeholder="Enter your password"
                                    value={password}
                                    onChange={(e) => setPassword(e.target.value)}
                                    autoComplete="current-password"
                                    required
                                />

                                <button
                                    type="button"
                                    className="mh-password-toggle"
                                    onClick={() => setShowPassword(!showPassword)}
                                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                                >
                                    {showPassword ? 'Hide' : 'Show'}
                                </button>
                            </div>
                        </div>

                        <div className="mh-login-options">
                            <span></span>
                            <Link to="/forgot-password">Forgot password?</Link>
                        </div>

                        <button
                            type="submit"
                            className="mh-login-button"
                            disabled={isSubmitting}
                        >
                            {isSubmitting ? 'Signing in...' : 'Login'}
                        </button>
                    </form>

                    <div className="mh-register-prompt">
                        Don't have an account?
                        <Link to="/register">Create an account</Link>
                    </div>
                </div>

                <p className="mh-form-footer">
                    © {new Date().getFullYear()} MarketHub. All rights reserved.
                </p>
            </section>
        </main>
    )
}

export default Login