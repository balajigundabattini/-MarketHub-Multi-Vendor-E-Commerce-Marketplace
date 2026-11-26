
import { useCallback, useEffect, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import AdminTable from '../Components/AdminTable'
import {
  fetchAdminCustomers,
  fetchAdminOrders,
  fetchAdminProducts,
  fetchAdminSellerApplications,
  moderateAdminProduct,
  refundAdminOrder,
  reviewSellerApplication,
  updateAdminCustomerStatus,
  fetchAdminReviews,
  moderateAdminReview,
  fetchAdminCancellationRequests,
  reviewAdminCancellationRequest
} from '../api/admin'

const configs = {
  customers: {
    title: 'Customers',
    load: fetchAdminCustomers,
    columns: (act) => [
      { key: 'name', label: 'Customer' },
      { key: 'email', label: 'Email' },
      { key: 'status', label: 'Status' },
      { key: 'createdAt', label: 'Joined' },
      {
        key: 'action',
        label: 'Action',
        render: (r) => (
          <button
            className="btn btn-sm btn-outline-secondary"
            type="button"
            onClick={() => act(r, r.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED')}
          >
            {r.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend'}
          </button>
        )
      }
    ],
    run: (r, s) => updateAdminCustomerStatus(r.id, s),
    actionText: 'Customer status updated.'
  },

  sellers: {
    title: 'Seller applications',
    load: fetchAdminSellerApplications,
    columns: (act) => [
      { key: 'businessName', label: 'Business' },
      { key: 'legalName', label: 'Applicant' },
      { key: 'email', label: 'Email' },
      { key: 'status', label: 'Status' },
      { key: 'createdAt', label: 'Applied' },
      {
        key: 'action',
        label: 'Review',
        render: (r) => (
          <div className="d-flex gap-2">
            <button
              className="btn btn-sm btn-success"
              type="button"
              disabled={r.status !== 'PENDING_REVIEW'}
              onClick={() => act(r, 'APPROVED')}
            >
              Approve
            </button>
            <button
              className="btn btn-sm btn-outline-danger"
              type="button"
              disabled={r.status !== 'PENDING_REVIEW'}
              onClick={() => act(r, 'REJECTED')}
            >
              Reject
            </button>
          </div>
        )
      }
    ],
    run: (r, s) => reviewSellerApplication(r.id, s),
    actionText: 'Seller application reviewed.'
  },

  products: {
    title: 'Product moderation',
    load: fetchAdminProducts,
    columns: (act) => [
      { key: 'name', label: 'Product' },
      { key: 'sellerName', label: 'Seller' },
      { key: 'sku', label: 'SKU' },
      { key: 'status', label: 'Status' },
      { key: 'createdAt', label: 'Submitted' },
      {
        key: 'action',
        label: 'Decision',
        render: (r) => (
          <div className="d-flex gap-2">
            <button
              className="btn btn-sm btn-success"
              type="button"
              disabled={r.status !== 'PENDING_APPROVAL'}
              onClick={() => act(r, 'APPROVED')}
            >
              Approve
            </button>
            <button
              className="btn btn-sm btn-outline-danger"
              type="button"
              disabled={r.status !== 'PENDING_APPROVAL'}
              onClick={() => act(r, 'REJECTED')}
            >
              Reject
            </button>
          </div>
        )
      }
    ],
    run: (r, s) => moderateAdminProduct(r.id, s),
    actionText: 'Moderation decision saved.'
  },

  orders: {
    title: 'Orders and refunds',
    load: fetchAdminOrders,
    columns: (act) => [
      { key: 'orderNumber', label: 'Order' },
      { key: 'customerName', label: 'Customer' },
      { key: 'createdAt', label: 'Date' },
      { key: 'total', label: 'Total' },
      { key: 'paymentStatus', label: 'Payment' },
      { key: 'status', label: 'Order status' },
      {
        key: 'action',
        label: 'Refund',
        render: (r) => (
          <button
            className="btn btn-sm btn-outline-danger"
            type="button"
            disabled={!r.canRefund}
            onClick={() => act(r, 'REFUND')}
          >
            Issue eligible refund
          </button>
        )
      }
    ],
    run: (r) => refundAdminOrder(r.id, { reason: 'Admin initiated refund' }),
    actionText: 'Refund request submitted.'
  },

  reviews: {
    title: 'Product reviews',
    load: fetchAdminReviews,
    columns: (act) => [
      { key: 'reviewer', label: 'Customer' },
      { key: 'productId', label: 'Product ID' },
      { key: 'rating', label: 'Rating' },
      { key: 'title', label: 'Title' },
      { key: 'body', label: 'Review' },
      { key: 'createdAt', label: 'Submitted' },
      {
        key: 'action',
        label: 'Decision',
        render: (r) => (
          <div className="d-flex gap-2">
            <button
              className="btn btn-sm btn-success"
              type="button"
              onClick={() => act(r, 'APPROVED')}
            >
              Approve
            </button>
            <button
              className="btn btn-sm btn-outline-danger"
              type="button"
              onClick={() => act(r, 'REJECTED')}
            >
              Reject
            </button>
          </div>
        )
      }
    ],
    run: (r, status) => moderateAdminReview(r.id, status),
    actionText: 'Review moderation saved.'
  }
}

const AdminQueue = ({ kind }) => {
  const config = configs[kind]
  const [params, setParams] = useSearchParams()

  const [rows, setRows] = useState([])
  const [page, setPage] = useState({ number: 0, totalPages: 0 })
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState('')
  const [error, setError] = useState('')
  const [message, setMessage] = useState('')

  // Cancellation request state
  const [cancellationRequests, setCancellationRequests] = useState([])
  const [cancellationLoading, setCancellationLoading] = useState(false)

  const load = useCallback(async () => {
    setLoading(true)

    try {
      const result = await config.load({
        page: Number(params.get('page') || 0),
        size: 20,
        status: params.get('status') || undefined
      })

      setRows(result.content || result.items || result || [])
      setPage(result)
      setError('')
    } catch (e) {
      setError(e.response?.data?.detail || `${config.title} could not be loaded.`)
    } finally {
      setLoading(false)
    }
  }, [config, params])

  const loadCancellationRequests = useCallback(async () => {
    if (kind !== 'orders') return

    setCancellationLoading(true)

    try {
      const result = await fetchAdminCancellationRequests()
      setCancellationRequests(result || [])
    } catch (e) {
      setError(
        e.response?.data?.detail ||
        'Cancellation requests could not be loaded.'
      )
    } finally {
      setCancellationLoading(false)
    }
  }, [kind])

  useEffect(() => {
    Promise.resolve().then(load)
  }, [load])

  useEffect(() => {
    if (kind === 'orders') {
      Promise.resolve().then(loadCancellationRequests)
    }
  }, [kind, loadCancellationRequests])

  const act = async (row, status) => {
    setBusy(row.id)
    setError('')
    setMessage('')

    try {
      await config.run(row, status)
      setMessage(config.actionText)
      await load()
    } catch (e) {
      setError(e.response?.data?.detail || 'The action could not be completed.')
    } finally {
      setBusy('')
    }
  }

  const reviewCancellation = async (request, status) => {
    if (busy) return

    const action = status === 'APPROVED' ? 'approve' : 'reject'

    if (!window.confirm(`Are you sure you want to ${action} this cancellation request?`)) {
      return
    }

    const note = window.prompt(
      'Enter an admin review note:',
      status === 'APPROVED'
        ? 'Cancellation approved by admin'
        : 'Cancellation rejected by admin'
    )

    if (note === null) return

    setBusy(`cancellation-${request.requestId}`)
    setError('')
    setMessage('')

    try {
      await reviewAdminCancellationRequest(request.requestId, {
        status,
        note: note.trim()
      })

      setMessage(`Cancellation request ${status.toLowerCase()} successfully.`)

      await Promise.all([
        loadCancellationRequests(),
        load()
      ])
    } catch (e) {
      setError(
        e.response?.data?.detail ||
        'Cancellation review could not be completed.'
      )
    } finally {
      setBusy('')
    }
  }

  const columns = config.columns((row, status) => {
    if (busy) return

    if (status === 'REJECTED' && !window.confirm('Reject this submission?')) {
      return
    }

    void act(row, status)
  })

  const statuses =
    kind === 'sellers'
      ? ['PENDING_REVIEW', 'APPROVED', 'REJECTED', 'SUSPENDED']
      : kind === 'customers'
        ? ['ACTIVE', 'SUSPENDED']
        : kind === 'products'
          ? ['PENDING_APPROVAL', 'ACTIVE', 'REJECTED', 'INACTIVE', 'DRAFT']
          : kind === 'orders'
            ? ['PENDING_PAYMENT', 'CONFIRMED', 'PROCESSING', 'SHIPPED', 'DELIVERED', 'CANCELLED', 'REFUNDED']
            : kind === 'reviews'
              ? ['PENDING']
              : ['PENDING_REVIEW', 'ACTIVE', 'APPROVED', 'REJECTED', 'SUSPENDED']

  return (
    <main>
      <h1 className="h2">{config.title}</h1>

      {/* Pending cancellation requests */}
      {kind === 'orders' && (
        <section className="mb-4">
          <div className="d-flex justify-content-between align-items-center mb-3">
            <h2 className="h4 mb-0">Pending Cancellation Requests</h2>

            <button
              type="button"
              className="btn btn-sm btn-outline-secondary"
              onClick={loadCancellationRequests}
              disabled={cancellationLoading}
            >
              Refresh
            </button>
          </div>

          {cancellationLoading ? (
            <p>Loading cancellation requests...</p>
          ) : cancellationRequests.length === 0 ? (
            <div className="alert alert-light border">
              No pending cancellation requests.
            </div>
          ) : (
            <div className="table-responsive">
              <table className="table table-bordered table-hover align-middle">
                <thead className="table-light">
                  <tr>
                    <th>Order</th>
                    <th>Customer</th>
                    <th>Product</th>
                    <th>Quantity</th>
                    <th>Reason</th>
                    <th>Requested At</th>
                    <th>Action</th>
                  </tr>
                </thead>

                <tbody>
                  {cancellationRequests.map((request) => (
                    <tr key={request.requestId}>
                      <td>{request.orderNumber}</td>
                      <td>{request.customerName}</td>
                      <td>{request.productName}</td>
                      <td>{request.quantity}</td>
                      <td>{request.reason}</td>
                      <td>
                        {request.requestedAt
                          ? new Date(request.requestedAt).toLocaleString()
                          : '-'}
                      </td>
                      <td>
                        <div className="d-flex gap-2">
                          <button
                            type="button"
                            className="btn btn-sm btn-success"
                            disabled={!!busy}
                            onClick={() =>
                              reviewCancellation(request, 'APPROVED')
                            }
                          >
                            Approve
                          </button>

                          <button
                            type="button"
                            className="btn btn-sm btn-outline-danger"
                            disabled={!!busy}
                            onClick={() =>
                              reviewCancellation(request, 'REJECTED')
                            }
                          >
                            Reject
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </section>
      )}

      <div className="d-flex flex-wrap gap-2 align-items-end my-3">
        <div>
          <label className="form-label" htmlFor="queue-status">
            Filter by status
          </label>

          <select
            id="queue-status"
            className="form-select"
            value={params.get('status') || ''}
            onChange={(e) => {
              const next = new URLSearchParams(params)
              e.target.value
                ? next.set('status', e.target.value)
                : next.delete('status')
              next.delete('page')
              setParams(next)
            }}
          >
            <option value="">All statuses</option>
            {statuses.map((s) => (
              <option key={s}>{s}</option>
            ))}
          </select>
        </div>
      </div>

      {message && (
        <p className="alert alert-success" role="status">
          {message}
        </p>
      )}

      {error && (
        <p className="alert alert-warning" role="alert">
          {error}
        </p>
      )}

      {loading ? (
        <p role="status">Loading records…</p>
      ) : (
        !error && (
          <AdminTable
            columns={columns}
            rows={rows}
            emptyMessage="No records match this filter."
          />
        )
      )}

      {!loading && page.totalPages > 1 && (
        <div className="d-flex justify-content-center align-items-center gap-3 mt-3">
          <button
            className="btn btn-outline-secondary"
            type="button"
            disabled={!page.number}
            onClick={() => {
              const next = new URLSearchParams(params)
              next.set('page', String(page.number - 1))
              setParams(next)
            }}
          >
            Previous
          </button>

          <span>
            Page {page.number + 1} of {page.totalPages}
          </span>

          <button
            className="btn btn-outline-secondary"
            type="button"
            disabled={page.number + 1 >= page.totalPages}
            onClick={() => {
              const next = new URLSearchParams(params)
              next.set('page', String(page.number + 1))
              setParams(next)
            }}
          >
            Next
          </button>
        </div>
      )}
    </main>
  )
}

export default AdminQueue
