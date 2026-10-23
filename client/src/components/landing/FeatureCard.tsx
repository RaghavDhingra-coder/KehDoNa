import type { LucideIcon } from 'lucide-react'
import type { ReactNode } from 'react'
import { cn } from '../../lib/cn'
import { Badge } from '../ui/Badge'
import { IconBadge } from '../ui/IconBadge'

export type FeatureTone = 'lavender' | 'cream' | 'plum' | 'violet'

const tones: Record<
  FeatureTone,
  { card: string; title: string; copy: string; icon: 'rose' | 'lavender' | 'violet' | 'plum' }
> = {
  lavender: {
    card: 'border-line bg-[radial-gradient(120%_90%_at_100%_0%,rgb(185_166_239/0.18),transparent_60%)] bg-night-850',
    title: 'text-ink',
    copy: 'text-ink-muted',
    icon: 'lavender',
  },
  cream: {
    card: 'border-cream-200 bg-cream-50',
    title: 'text-ink-dark',
    copy: 'text-ink-dark-muted',
    icon: 'plum',
  },
  plum: {
    card: 'border-rose-300/15 bg-[radial-gradient(90%_80%_at_50%_0%,rgb(224_86_127/0.28),transparent_65%)] bg-plum-900',
    title: 'text-ink',
    copy: 'text-blush-200/80',
    icon: 'rose',
  },
  violet: {
    card: 'border-violet-400/20 bg-linear-to-br from-violet-700 via-night-850 to-night-850',
    title: 'text-ink',
    copy: 'text-ink-muted',
    icon: 'violet',
  },
}

/** A large showcase card for a feature that is coming in a later release. */
export function FeatureCard({
  title,
  description,
  icon,
  tone,
  visual,
  className,
}: {
  title: string
  description: string
  icon: LucideIcon
  tone: FeatureTone
  visual: ReactNode
  className?: string
}) {
  const styles = tones[tone]
  return (
    <article
      className={cn(
        'group relative flex h-full flex-col overflow-hidden rounded-card border shadow-card',
        'transition-[transform,box-shadow] duration-500 ease-soft hover:-translate-y-1 hover:shadow-lift',
        styles.card,
        className,
      )}
    >
      <div className="relative flex min-h-56 flex-1 items-center justify-center px-6 pt-8 sm:min-h-64 sm:px-8">
        {visual}
      </div>
      <div className="relative p-6 sm:p-8">
        <div className="flex items-center justify-between gap-4">
          <IconBadge icon={icon} tone={styles.icon} />
          <Badge tone={tone === 'cream' ? 'cream' : 'neutral'}>Coming soon</Badge>
        </div>
        <h3 className={cn('mt-5 font-display text-h3 font-medium', styles.title)}>{title}</h3>
        <p className={cn('mt-2 max-w-md text-pretty', styles.copy)}>{description}</p>
      </div>
    </article>
  )
}
