import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import apiClient from '../api/client'

const blankAddress = {
  recipientName: '', phone: '', line1: '', line2: '', city: '', region: '',
  postalCode: '', countryCode: 'IN', label: '', isDefault: false,
}

const Account = () => {
  const [profile, setProfile] = useState({ displayName: '', email: '', phone: '', birthDate: '' })
  const [addresses, setAddresses] = useState([])
  const [address, setAddress] = useState(blankAddress)
  const [editingId, setEditingId] = useState(null)
  const [message, setMessage] = useState('Loading your account…')

  const loadAccount = useCallback(async () => {
    try {
      const [profileResult, addressResult] = await Promise.all([
        apiClient.get('/customer/profile'), apiClient.get('/customer/addresses'),
      ])
      setProfile({ ...profileResult.data, birthDate: profileResult.data.birthDate || '' })
      setAddresses(addressResult.data)
      setMessage('')
    } catch (error) {
      setMessage(error.response?.status === 401
        ? 'Sign in to manage your account.'
        : error.response?.data?.detail || 'Could not load your account.')
    }
  }, [])

  useEffect(() => {
    Promise.resolve().then(loadAccount)
  }, [loadAccount])

  const saveProfile = async (event) => {
    event.preventDefault()
    try {
      const { data } = await apiClient.put('/customer/profile', {
        ...profile,
        birthDate: profile.birthDate || null,
      })
      setProfile({ ...data, birthDate: data.birthDate || '' })
      setMessage('Profile saved.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'Could not save your profile.')
    }
  }

  const saveAddress = async (event) => {
    event.preventDefault()
    try {
      if (editingId) await apiClient.put(`/customer/addresses/${editingId}`, address)
      else await apiClient.post('/customer/addresses', address)
      setAddress(blankAddress)
      setEditingId(null)
      await loadAccount()
      setMessage('Address saved.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'Could not save the address.')
    }
  }

  const chooseAddress = (item) => {
    setEditingId(item.id)
    setAddress({ ...item })
  }

  const deleteAddress = async (id) => {
    try {
      await apiClient.delete(`/customer/addresses/${id}`)
      await loadAccount()
      setMessage('Address removed.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'Could not remove the address.')
    }
  }

  const makeDefault = async (id) => {
    try {
      await apiClient.post(`/customer/addresses/${id}/default`)
      await loadAccount()
      setMessage('Default address updated.')
    } catch (error) {
      setMessage(error.response?.data?.detail || 'Could not update the default address.')
    }
  }

  return (
    <main className="container py-4" style={{ maxWidth: 960 }}>
      <h1 className="h2 mb-4">Your account</h1>
      {message && <p role="status" className="alert alert-info">{message}</p>}
      {message === 'Sign in to manage your account.' && <Link to="/login">Sign in</Link>}
      <section className="card p-4 mb-4">
        <h2 className="h4">Profile</h2>
        <form onSubmit={saveProfile} className="row g-3">
          <div className="col-md-6"><label className="form-label" htmlFor="profile-name">Name</label><input id="profile-name" className="form-control" maxLength={120} required value={profile.displayName} onChange={(event) => setProfile({ ...profile, displayName: event.target.value })} /></div>
          <div className="col-md-6"><label className="form-label" htmlFor="profile-email">Email</label><input id="profile-email" className="form-control" type="email" value={profile.email} disabled /></div>
          <div className="col-md-6"><label className="form-label" htmlFor="profile-phone">Phone</label><input id="profile-phone" className="form-control" maxLength={32} value={profile.phone || ''} onChange={(event) => setProfile({ ...profile, phone: event.target.value })} /></div>
          <div className="col-md-6"><label className="form-label" htmlFor="profile-birth-date">Birth date (optional)</label><input id="profile-birth-date" className="form-control" type="date" value={profile.birthDate} onChange={(event) => setProfile({ ...profile, birthDate: event.target.value })} /></div>
          <div><button className="btn btn-primary" type="submit">Save profile</button></div>
        </form>
      </section>
      <section className="card p-4 mb-4">
        <h2 className="h4">Delivery addresses</h2>
        <ul className="list-group mb-4">
          {addresses.map((item) => <li className="list-group-item" key={item.id}>
            <div className="d-flex flex-wrap justify-content-between gap-2">
              <div><strong>{item.recipientName}</strong> {item.isDefault && <span className="badge text-bg-primary">Default</span>}
                <div>{item.line1}{item.line2 ? `, ${item.line2}` : ''}, {item.city}, {item.region} {item.postalCode}, {item.countryCode}</div>
                <small>{item.phone}{item.label ? ` · ${item.label}` : ''}</small>
              </div>
              <div className="d-flex gap-2 align-items-start">
                <button className="btn btn-sm btn-outline-secondary" type="button" onClick={() => chooseAddress(item)}>Edit</button>
                {!item.isDefault && <button className="btn btn-sm btn-outline-primary" type="button" onClick={() => makeDefault(item.id)}>Make default</button>}
                <button className="btn btn-sm btn-outline-danger" type="button" onClick={() => deleteAddress(item.id)}>Remove</button>
              </div>
            </div>
          </li>)}
          {addresses.length === 0 && <li className="list-group-item">No saved addresses yet.</li>}
        </ul>
        <h3 className="h5">{editingId ? 'Edit address' : 'Add an address'}</h3>
        <form onSubmit={saveAddress} className="row g-3">
          {[
            ['recipientName', 'Recipient name', 120], ['phone', 'Phone', 32], ['line1', 'Address line 1', 200],
            ['line2', 'Address line 2 (optional)', 200], ['city', 'City', 100], ['region', 'State / region', 100],
            ['postalCode', 'Postal code', 24], ['countryCode', 'Country code (2 letters)', 2], ['label', 'Label (optional)', 40],
          ].map(([key, label, maxLength]) => <div className="col-md-6" key={key}>
            <label htmlFor={`address-${key}`} className="form-label">{label}</label>
            <input id={`address-${key}`} className="form-control" maxLength={maxLength}
              required={!['line2', 'label'].includes(key)} value={address[key] || ''}
              onChange={(event) => setAddress({ ...address, [key]: event.target.value })} />
          </div>)}
          <div className="col-12 form-check ms-2"><input id="address-default" className="form-check-input" type="checkbox" checked={address.isDefault} onChange={(event) => setAddress({ ...address, isDefault: event.target.checked })} /><label htmlFor="address-default" className="form-check-label">Set as default address</label></div>
          <div className="d-flex gap-2"><button className="btn btn-primary" type="submit">{editingId ? 'Save address' : 'Add address'}</button>{editingId && <button className="btn btn-outline-secondary" type="button" onClick={() => { setEditingId(null); setAddress(blankAddress) }}>Cancel edit</button>}</div>
        </form>
      </section>
    </main>
  )
}

export default Account
