import { Menu, X } from 'lucide-react'
import { useEffect, useId, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { useScrolled } from '../../hooks/useScrolled'
import { cn } from '../../lib/cn'
import { sectionLinks } from '../../lib/navigation'
import { ButtonLink } from '../ui/Button'
import { Container } from '../ui/Container'
import { Logo } from '../ui/Logo'

export function Navbar() {
  const scrolled = useScrolled()
  const [open, setOpen] = useState(false)
  const menuId = useId()
  const location = useLocation()

  // Close the mobile menu whenever the route or hash changes.
  const [lastLocation, setLastLocation] = useState(location)
  if (location !== lastLocation) {
    setLastLocation(location)
    setOpen(false)
  }

  useEffect(() => {
    if (!open) return
    const onKey = (event: KeyboardEvent) => event.key === 'Escape' && setOpen(false)
    const onResize = () => window.innerWidth >= 768 && setOpen(false)
    window.addEventListener('keydown', onKey)
    window.addEventListener('resize', onResize)
    return () => {
      window.removeEventListener('keydown', onKey)
      window.removeEventListener('resize', onResize)
    }
  }, [open])

  const solid = scrolled || open

  return (
    <header
      className={cn(
        'fixed inset-x-0 top-0 z-50 border-b transition-[background-color,border-color,backdrop-filter] duration-500 ease-soft',
        solid
          ? 'border-line bg-night-950/85 backdrop-blur-xl'
          : 'border-transparent bg-transparent',
      )}
    >
      <Container className="flex h-18 items-center justify-between gap-6">
        <Logo />

        <nav aria-label="Primary" className="hidden md:block">
          <ul className="flex items-center gap-1">
            {sectionLinks.map((link) => (
              <li key={link.to}>
                <Link
                  to={link.to}
                  className="rounded-pill px-4 py-2 text-sm font-medium text-ink-muted transition-colors duration-300 hover:bg-white/[0.05] hover:text-ink"
                >
                  {link.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>

        <div className="hidden items-center gap-2 md:flex">
          <ButtonLink to="/login" variant="ghost">
            Log in
          </ButtonLink>
          <ButtonLink to="/join">Get Started</ButtonLink>
        </div>

        <button
          type="button"
          className="-mr-2 inline-grid size-11 place-items-center rounded-full text-ink transition-colors hover:bg-white/[0.06] md:hidden"
          aria-expanded={open}
          aria-controls={menuId}
          aria-label={open ? 'Close menu' : 'Open menu'}
          onClick={() => setOpen((value) => !value)}
        >
          {open ? <X className="size-5" /> : <Menu className="size-5" />}
        </button>
      </Container>

      <div
        id={menuId}
        hidden={!open}
        className="border-t border-line bg-night-950/95 backdrop-blur-xl md:hidden"
      >
        <Container className="flex flex-col gap-1 py-4">
          <nav aria-label="Mobile">
            <ul className="flex flex-col">
              {sectionLinks.map((link) => (
                <li key={link.to}>
                  <Link
                    to={link.to}
                    onClick={() => setOpen(false)}
                    className="flex h-12 items-center rounded-xl px-3 font-display text-xl text-ink transition-colors hover:bg-white/[0.05]"
                  >
                    {link.label}
                  </Link>
                </li>
              ))}
            </ul>
          </nav>
          <div className="mt-3 grid grid-cols-2 gap-3 border-t border-line pt-4">
            <ButtonLink to="/login" variant="secondary" onClick={() => setOpen(false)}>
              Log in
            </ButtonLink>
            <ButtonLink to="/join" onClick={() => setOpen(false)}>
              Get Started
            </ButtonLink>
          </div>
        </Container>
      </div>
    </header>
  )
}
