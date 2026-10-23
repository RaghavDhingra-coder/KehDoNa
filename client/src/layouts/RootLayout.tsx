import { useEffect } from 'react'
import { Outlet, useLocation } from 'react-router-dom'
import { Footer } from '../components/layout/Footer'
import { Navbar } from '../components/layout/Navbar'

export function RootLayout() {
  useScrollToHash()
  return (
    <div className="flex min-h-svh flex-col">
      <a
        href="#main"
        className="sr-only z-[60] rounded-pill bg-cream-50 px-4 py-2 font-semibold text-ink-dark focus:not-sr-only focus:fixed focus:top-3 focus:left-3"
      >
        Skip to content
      </a>
      <Navbar />
      <main id="main" className="flex-1">
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}

/** Scrolls to `#section` links after navigation, or to the top for new pages. */
function useScrollToHash() {
  const { pathname, hash, key } = useLocation()

  useEffect(() => {
    if (!hash) {
      window.scrollTo({ top: 0 })
      return
    }
    const target = document.getElementById(decodeURIComponent(hash.slice(1)))
    target?.scrollIntoView({ block: 'start' })
  }, [pathname, hash, key])
}
