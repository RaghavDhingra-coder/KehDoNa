import type { LucideIcon } from 'lucide-react'
import { ArrowLeft } from 'lucide-react'
import type { ReactNode } from 'react'
import { ButtonLink } from '../components/ui/Button'
import { Badge } from '../components/ui/Badge'
import { Container } from '../components/ui/Container'
import { IconBadge } from '../components/ui/IconBadge'
import { useDocumentTitle } from '../hooks/useDocumentTitle'

/**
 * Honest placeholder for routes that exist in the navigation but are not
 * built yet (sign-in, sign-up, policy pages, 404).
 */
export function PlaceholderPage({
  icon,
  badge,
  title,
  description,
  children,
}: {
  icon: LucideIcon
  badge: string
  title: string
  description: ReactNode
  children?: ReactNode
}) {
  useDocumentTitle(title)
  return (
    <section className="grain relative flex min-h-[78svh] items-center overflow-hidden pt-32 pb-20">
      <div
        aria-hidden="true"
        className="pointer-events-none absolute inset-0 -z-10 bg-[radial-gradient(70%_60%_at_50%_0%,var(--color-plum-800)_0%,transparent_70%)]"
      />
      <Container className="flex max-w-2xl flex-col items-center text-center">
        <IconBadge icon={icon} tone="rose" className="size-14 rounded-3xl" />
        <Badge tone="lavender" className="mt-6">
          {badge}
        </Badge>
        <h1 className="mt-5 font-display text-h2 font-medium text-balance text-ink">{title}</h1>
        <p className="mt-5 text-lead text-pretty text-ink-muted">{description}</p>
        {children}
        <ButtonLink to="/" variant="secondary" className="mt-10">
          <ArrowLeft className="size-4" aria-hidden="true" />
          Back to home
        </ButtonLink>
      </Container>
    </section>
  )
}
