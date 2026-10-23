import type { LucideIcon } from 'lucide-react'
import { cn } from '../../lib/cn'

type IconTone = 'rose' | 'lavender' | 'violet' | 'plum'

const tones: Record<IconTone, string> = {
  rose: 'bg-rose-400/12 text-rose-300 ring-rose-300/20',
  lavender: 'bg-lavender-400/12 text-lavender-300 ring-lavender-300/20',
  violet: 'bg-violet-500/18 text-lavender-200 ring-violet-400/25',
  plum: 'bg-plum-700/12 text-plum-700 ring-plum-700/15',
}

/** A small rounded container that frames a single icon. */
export function IconBadge({
  icon: Icon,
  tone = 'rose',
  className,
}: {
  icon: LucideIcon
  tone?: IconTone
  className?: string
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'inline-grid size-11 shrink-0 place-items-center rounded-2xl ring-1 ring-inset',
        tones[tone],
        className,
      )}
    >
      <Icon className="size-5" strokeWidth={1.75} />
    </span>
  )
}
