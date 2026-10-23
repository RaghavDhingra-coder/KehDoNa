import { Link } from 'react-router-dom'
import { cn } from '../../lib/cn'

/** KehDoNa wordmark with a small heart mark. */
export function Logo({ className }: { className?: string }) {
  return (
    <Link
      to="/"
      aria-label="KehDoNa home"
      className={cn('group inline-flex items-center gap-2 rounded-lg', className)}
    >
      <svg
        viewBox="0 0 32 32"
        aria-hidden="true"
        className="size-7 transition-transform duration-500 ease-soft group-hover:scale-110 group-hover:-rotate-6"
      >
        <defs>
          <linearGradient id="kdn-heart" x1="0" y1="0" x2="1" y2="1">
            <stop offset="0%" stopColor="var(--color-rose-300)" />
            <stop offset="100%" stopColor="var(--color-lavender-400)" />
          </linearGradient>
        </defs>
        <path
          fill="url(#kdn-heart)"
          d="M16 27.5c-.4 0-.8-.1-1.1-.4C9.4 22.4 4 17.9 4 12.2 4 8.6 6.8 5.5 10.4 5.5c2.2 0 4.2 1.1 5.6 2.9 1.4-1.8 3.4-2.9 5.6-2.9 3.6 0 6.4 3.1 6.4 6.7 0 5.7-5.4 10.2-10.9 14.9-.3.3-.7.4-1.1.4Z"
        />
        <path
          fill="none"
          stroke="var(--color-night-950)"
          strokeWidth="1.6"
          strokeLinecap="round"
          d="M11.5 13.2c1.1 1.5 2.6 2.3 4.5 2.3s3.4-.8 4.5-2.3"
        />
      </svg>
      <span className="font-display text-[1.45rem] font-semibold tracking-tight text-ink">
        Keh<span className="text-rose-300 italic">Do</span>Na
      </span>
    </Link>
  )
}
