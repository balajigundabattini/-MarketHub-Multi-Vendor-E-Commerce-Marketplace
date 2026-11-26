import { Link } from 'react-router-dom'

const Home = () => (
  <div className="home-page">
    <section className="hero container">
      <p className="eyebrow">A marketplace built for many sellers</p>
      <h1>Good finds, from stores worth discovering.</h1>
      <p className="hero-copy">
        MarketHub brings independent sellers and customers together in one place.
        We are building the marketplace step by step.
      </p>
      <div className="hero-actions">
        <Link className="button button-primary text-decoration-none" to="/products">Browse products</Link>
        <Link className="button button-primary text-decoration-none" to="/register">Create an account</Link>
        <Link className="button button-secondary text-decoration-none" to="/login">Sign in</Link>
      </div>
    </section>

    <section className="intro container" aria-labelledby="intro-title">
      <div>
        <p className="eyebrow">The project</p>
        <h2 id="intro-title">One home for customers, sellers, and the people who keep it running.</h2>
      </div>
      <p>
        The first release is being designed around trusted seller onboarding,
        clear product information, and a checkout that can safely handle orders
        from more than one store.
      </p>
    </section>
  </div>
)

export default Home
