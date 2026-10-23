import { cn } from '../../lib/cn'

const gradients = {
  rose: 'from-rose-300 via-rose-500 to-plum-700',
  lavender: 'from-lavender-200 via-lavender-400 to-violet-600',
  dusk: 'from-blush-200 via-lilac-500 to-violet-700',
} as const

/** Illustrated initials avatar — no photos of real people are used. */
export function Avatar({
  initials,
  tone = 'rose',
  className,
}: {
  initials: string
  tone?: keyof typeof gradients
  className?: string
}) {
  return (
    <span
      aria-hidden="true"
      className={cn(
        'inline-grid size-10 shrink-0 place-items-center rounded-full bg-linear-to-br font-display font-semibold text-night-950 ring-2 ring-night-900',
        gradients[tone],
        className,
      )}
    >
      {initials}
    </span>
  )
}
