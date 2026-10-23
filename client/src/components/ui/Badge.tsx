import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'

type BadgeTone = 'rose' | 'lavender' | 'neutral' | 'cream'

const tones: Record<BadgeTone, string> = {
  rose: 'border-rose-300/25 bg-rose-400/10 text-blush-200',
  lavender: 'border-lavender-300/25 bg-lavender-400/10 text-lavender-200',
  neutral: 'border-line-strong bg-white/[0.04] text-ink-muted',
  cream: 'border-plum-700/15 bg-plum-700/[0.07] text-plum-700',
}

export function Badge({
  children,
  tone = 'neutral',
  icon,
  className,
}: {
  children: ReactNode
  tone?: BadgeTone
  icon?: ReactNode
  className?: string
}) {
  return (
    <span
      className={cn(
        'inline-flex items-center gap-1.5 rounded-pill border px-3 py-1.5 text-label font-semibold uppercase',
        tones[tone],
        className,
      )}
    >
      {icon}
      {children}
    </span>
  )
}
