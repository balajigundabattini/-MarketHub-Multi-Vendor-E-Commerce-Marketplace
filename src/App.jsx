import { useEffect } from 'react'
import Routings from './Routes/Routings'
import Header from './Components/Header'
import Footer from './Components/Footer'
import { ToastContainer } from 'react-toastify'
import { restoreSession } from './api/client'
import { clearAccessToken } from './api/accessToken'
import './App.css'

const App = () => {
  useEffect(() => {
    restoreSession().catch(() => clearAccessToken())
  }, [])

  return (
    <div className="app-shell">
      <Header />
      <main className="app-main">
        <Routings />
      </main>
      <Footer />
      <ToastContainer position="top-right" autoClose={3000} />
    </div>
  )
}

export default App
