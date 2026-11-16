import Routings from './Routes/Routings'
import Header from './Components/Header'
import Footer from './Components/Footer'
import { ToastContainer } from 'react-toastify'
import './App.css'

const App = () => {
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
