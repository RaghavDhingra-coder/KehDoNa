import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

export function SectionHeading({
  eyebrow,
  title,
  description,
  align = 'left',
  tone = 'dark',
  id,
  className,
}: {
  eyebrow: string
  title: ReactNode
  description?: ReactNode
  align?: 'left' | 'center'
  tone?: 'dark' | 'light'
  id?: string
  className?: string
}) {
  const onLight = tone === 'light'
  return (
    <div className={cn('max-w-2xl', align === 'center' && 'mx-auto text-center', className)}>
      <p
        className={cn(
          'text-label font-semibold uppercase',
          onLight ? 'text-plum-700' : 'text-rose-300',
        )}
      >
        {eyebrow}
      </p>
      <h2
        id={id}
        className={cn(
          'mt-4 font-display text-h2 font-medium text-balance',
          onLight ? 'text-ink-dark' : 'text-ink',
        )}
      >
        {title}
      </h2>
      {description && (
        <p
          className={cn(
            'mt-5 text-lead text-pretty',
            onLight ? 'text-ink-dark-muted' : 'text-ink-muted',
          )}
        >
          {description}
        </p>
      )}
    </div>
  )
}
