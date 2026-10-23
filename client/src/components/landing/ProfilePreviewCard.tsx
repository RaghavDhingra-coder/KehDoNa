import { MapPin } from 'lucide-react'
import { cn } from '../../lib/cn'

export type PreviewProfile = {
  name: string
  details: string
  location: string
  prompt: { question: string; answer: string }
  vibes: string[]
}

/**
 * Illustrative profile card used in marketing previews. The people shown are
 * fictional; nothing here is backed by real data.
 */
export function ProfilePreviewCard({
  profile,
  className,
}: {
  profile: PreviewProfile
  className?: string
}) {
  return (
    <article
      aria-label={`Illustrative profile preview for a fictional student named ${profile.name}`}
      className={cn(
        'overflow-hidden rounded-card border border-line-strong bg-night-850 shadow-lift',
        className,
      )}
    >
      <div className="relative h-44 overflow-hidden bg-linear-to-br from-plum-700 via-violet-600 to-rose-400 sm:h-52">
        <div className="absolute -top-10 -left-8 size-44 rounded-full bg-rose-300/60 blur-2xl" />
        <div className="absolute -right-10 -bottom-16 size-52 rounded-full bg-violet-700/80 blur-2xl" />
        <div className="absolute top-1/2 left-1/2 size-28 -translate-x-1/2 -translate-y-[40%] rounded-full bg-linear-to-b from-blush-200/90 to-rose-300/60 shadow-[0_0_60px_rgb(249_207_217/0.5)]" />
        <div className="absolute inset-x-0 bottom-0 h-24 bg-linear-to-t from-night-850 to-transparent" />
        <span className="absolute top-4 right-4 rounded-pill bg-night-950/55 px-2.5 py-1 text-[0.68rem] font-semibold tracking-wide text-ink-muted uppercase backdrop-blur">
          Preview
        </span>
      </div>

      <div className="relative -mt-8 px-5 pb-5 sm:px-6 sm:pb-6">
        <h3 className="font-display text-3xl font-medium text-ink">{profile.name}</h3>
        <p className="mt-1 text-sm text-ink-muted">{profile.details}</p>
        <p className="mt-1 inline-flex items-center gap-1 text-xs text-ink-subtle">
          <MapPin className="size-3.5" aria-hidden="true" />
          {profile.location}
        </p>

        <div className="mt-5 rounded-panel border border-line bg-white/[0.03] p-4">
          <p className="text-[0.7rem] font-semibold tracking-wider text-lavender-300 uppercase">
            {profile.prompt.question}
          </p>
          <p className="mt-1.5 font-display text-[1.05rem] leading-snug text-ink italic">
            {profile.prompt.answer}
          </p>
        </div>

        <ul className="mt-4 flex flex-wrap gap-2" aria-label="Vibes">
          {profile.vibes.map((vibe) => (
            <li
              key={vibe}
              className="rounded-pill border border-line-strong px-3 py-1 text-xs font-medium text-ink-muted"
            >
              {vibe}
            </li>
          ))}
        </ul>
      </div>
    </article>
  )
}
