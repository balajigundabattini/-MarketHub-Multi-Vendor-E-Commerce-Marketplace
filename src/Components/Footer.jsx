const Footer = () => {
  return (
    <footer className='footer'>
        <div className='footer-content'>
            <div className='footer-left'>
                <h3>MarketHub</h3>
                <p>A marketplace for independent sellers and curious shoppers.</p>
            </div>
        </div>
        <div className='footer-bottom'>
            <p>(c) {new Date().getFullYear()} MarketHub</p>
        </div>
    </footer>
  )
}

export default Footer
