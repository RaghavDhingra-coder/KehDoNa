import { cn } from './cn'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'dark'
export type ButtonSize = 'md' | 'lg'

const base =
  'group inline-flex items-center justify-center gap-2 rounded-pill font-semibold whitespace-nowrap ' +
  'transition-[transform,background-color,box-shadow,border-color,color] duration-300 ease-soft ' +
  'focus-visible:outline-2 focus-visible:outline-offset-4 focus-visible:outline-lavender-300 ' +
  'active:scale-[0.97] disabled:pointer-events-none disabled:opacity-45 select-none'

const variants: Record<ButtonVariant, string> = {
  primary:
    'bg-linear-to-r from-rose-400 to-rose-300 text-night-950 shadow-glow ' +
    'hover:-translate-y-0.5 hover:shadow-[0_14px_44px_-10px_rgb(240_123_156/0.7)]',
  secondary:
    'border border-line-strong bg-white/[0.04] text-ink ' +
    'hover:-translate-y-0.5 hover:border-lavender-300/50 hover:bg-white/[0.08]',
  ghost: 'text-ink-muted hover:bg-white/[0.06] hover:text-ink',
  dark:
    'bg-night-900 text-cream-50 hover:-translate-y-0.5 hover:bg-plum-800 ' +
    'focus-visible:outline-plum-700',
}

const sizes: Record<ButtonSize, string> = {
  md: 'h-11 px-5 text-sm',
  lg: 'h-13 px-7 text-[0.95rem]',
}

export function buttonStyles({
  variant = 'primary',
  size = 'md',
  className,
}: { variant?: ButtonVariant; size?: ButtonSize; className?: string } = {}) {
  return cn(base, variants[variant], sizes[size], className)
}
