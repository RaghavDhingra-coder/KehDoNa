import { Link } from 'react-router-dom'
import { useApiHealth } from '../../hooks/useApiHealth'
import { cn } from '../../lib/cn'
import { sectionLinks } from '../../lib/navigation'
import { Container } from '../ui/Container'
import { Logo } from '../ui/Logo'

const policyLinks = [
  { label: 'Privacy', to: '/privacy' },
  { label: 'Community Guidelines', to: '/guidelines' },
  { label: 'Contact', to: '/contact' },
] as const

export function Footer() {
  return (
    <footer className="border-t border-line bg-night-950">
      <Container className="grid gap-12 py-16 md:grid-cols-[1.4fr_1fr_1fr] md:py-20">
        <div className="max-w-sm">
          <Logo />
          <p className="mt-5 font-display text-xl text-ink-muted italic">
            Dil mein hai, toh KehDoNa.
          </p>
        </div>

        <FooterColumn title="Explore" links={sectionLinks} />
        <FooterColumn title="KehDoNa" links={policyLinks} />
      </Container>

      <Container className="flex flex-col gap-3 border-t border-line py-6 text-sm text-ink-subtle sm:flex-row sm:items-center sm:justify-between">
        <p>© {new Date().getFullYear()} KehDoNa. Made for campus, with care.</p>
        {import.meta.env.DEV && <ApiStatus />}
      </Container>
    </footer>
  )
}

function FooterColumn({
  title,
  links,
}: {
  title: string
  links: ReadonlyArray<{ label: string; to: string }>
}) {
  return (
    <div>
      <h2 className="text-label font-semibold text-ink-subtle uppercase">{title}</h2>
      <ul className="mt-5 space-y-1">
        {links.map((link) => (
          <li key={link.to}>
            <Link
              to={link.to}
              className="inline-flex min-h-10 items-center text-ink-muted transition-colors hover:text-rose-300"
            >
              {link.label}
            </Link>
          </li>
        ))}
      </ul>
    </div>
  )
}

/** Development-only indicator showing whether the API is reachable. */
function ApiStatus() {
  const health = useApiHealth()
  const label =
    health.state === 'loading'
      ? 'Checking API…'
      : health.state === 'online'
        ? `API online · database ${health.data.database.status.replace('_', ' ')}`
        : 'API offline'
  return (
    <p className="inline-flex items-center gap-2" role="status">
      <span
        aria-hidden="true"
        className={cn(
          'size-2 rounded-full',
          health.state === 'online' && 'bg-lavender-300',
          health.state === 'offline' && 'bg-rose-500',
          health.state === 'loading' && 'bg-ink-subtle',
        )}
      />
      <span>Dev · {label}</span>
    </p>
  )
}
