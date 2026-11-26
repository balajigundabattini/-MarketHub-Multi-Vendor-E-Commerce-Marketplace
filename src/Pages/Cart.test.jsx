import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import Cart from './Cart'
import { fetchCart, updateCartItem } from '../api/shopping'

vi.mock('../api/shopping', () => ({ fetchCart: vi.fn(), updateCartItem: vi.fn(), removeCartItem: vi.fn() }))

const cart = (quantity) => ({
  currency: 'INR', subtotal: 120, items: [{ id: 7, productId: 12, name: 'Desk lamp', quantity, availableQuantity: 4, unitPrice: 120, lineTotal: 120 * quantity }],
})

describe('Cart', () => {
  beforeEach(() => {
    fetchCart.mockResolvedValue(cart(1))
    updateCartItem.mockResolvedValue(cart(2))
  })

  it('loads cart contents and applies server returned quantity changes', async () => {
    const user = userEvent.setup()
    render(<MemoryRouter><Cart /></MemoryRouter>)
    expect(await screen.findByRole('link', { name: 'Desk lamp' })).toBeTruthy()
    await user.click(screen.getByRole('button', { name: 'Increase Desk lamp quantity' }))
    expect(updateCartItem).toHaveBeenCalledWith(7, 2)
    expect(await screen.findByText('2', { selector: '[aria-live="polite"]' })).toBeTruthy()
  })
})
