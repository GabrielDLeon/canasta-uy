import { Link } from 'react-router-dom'

export function Footer() {
  return (
    <footer className="border-t bg-card/60 py-6">
      <div className="mx-auto flex w-full max-w-7xl flex-col items-center justify-between gap-4 px-4 text-sm text-muted-foreground md:flex-row">
        <p>
          CanastaUY &copy; {new Date().getFullYear()}
        </p>
        <div className="flex gap-4">
          <Link to="/" className="hover:text-foreground hover:underline">
            Inicio
          </Link>
          <Link to="/app" className="hover:text-foreground hover:underline">
            App
          </Link>
          <Link to="/account/profile" className="hover:text-foreground hover:underline">
            Cuenta
          </Link>
        </div>
      </div>
    </footer>
  )
}
