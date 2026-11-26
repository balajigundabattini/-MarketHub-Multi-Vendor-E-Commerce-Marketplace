import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import ProtectedRoute from './ProtectedRoute'
import { useAuth } from './useAuth'

vi.mock('./useAuth', () => ({ useAuth: vi.fn() }))

const renderProtected = (auth) => {
  useAuth.mockReturnValue(auth)
  return render(<MemoryRouter initialEntries={['/private']}><Routes>
    <Route path="/private" element={<ProtectedRoute allowedRoles={['CUSTOMER']}><h1>Private account</h1></ProtectedRoute>} />
    <Route path="/login" element={<h1>Login page</h1>} />
    <Route path="/forbidden" element={<h1>Access denied</h1>} />
  </Routes></MemoryRouter>)
}

describe('ProtectedRoute', () => {
  beforeEach(() => useAuth.mockReset())

  it('shows a loading state while restoring a session', () => {
    renderProtected({ user: null, loading: true })
    expect(screen.getByRole('status')).toBeTruthy()
  })

  it('redirects anonymous users to login', () => {
    renderProtected({ user: null, loading: false })
    expect(screen.getByRole('heading', { name: 'Login page' })).toBeTruthy()
  })

  it('blocks users without an allowed role and renders allowed users', () => {
    const { unmount } = renderProtected({ user: { roles: ['SELLER'] }, loading: false })
    expect(screen.getByRole('heading', { name: 'Access denied' })).toBeTruthy()
    unmount()
    renderProtected({ user: { roles: ['CUSTOMER'] }, loading: false })
    expect(screen.getByRole('heading', { name: 'Private account' })).toBeTruthy()
  })
})
