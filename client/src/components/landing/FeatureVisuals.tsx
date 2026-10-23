import { EyeOff, Heart, LockKeyhole } from 'lucide-react'
import { cn } from '../../lib/cn'
import { Avatar } from '../ui/Avatar'

/* Decorative illustrations for the feature cards. All content is fictional. */

const vibes = [
  { label: 'Late-night chai', shared: true },
  { label: 'Indie playlists', shared: true },
  { label: 'Debate club', shared: false },
  { label: 'Sunset walks', shared: true },
  { label: 'Film cameras', shared: false },
  { label: 'Poetry nights', shared: true },
  { label: 'Football', shared: false },
]

export function VibeVisual() {
  return (
    <div
      aria-hidden="true"
      className="grid w-full max-w-lg grid-cols-2 items-center gap-x-3 gap-y-5 sm:grid-cols-[auto_1fr_auto] sm:gap-5"
    >
      <Avatar
        initials="R"
        tone="lavender"
        className="size-12 justify-self-end text-lg sm:size-14 sm:justify-self-auto"
      />
      <ul className="col-span-2 row-start-2 flex flex-wrap justify-center gap-2 sm:col-span-1 sm:col-start-2 sm:row-start-1">
        {vibes.map((vibe) => (
          <li
            key={vibe.label}
            className={cn(
              'rounded-pill border px-3 py-1.5 text-xs font-medium transition-colors duration-500 sm:text-sm',
              vibe.shared
                ? 'border-lavender-300/40 bg-lavender-400/15 text-lavender-200 group-hover:border-lavender-300/70'
                : 'border-line text-ink-subtle',
            )}
          >
            {vibe.label}
          </li>
        ))}
      </ul>
      <Avatar
        initials="S"
        tone="rose"
        className="size-12 text-lg sm:col-start-3 sm:row-start-1 sm:size-14"
      />
    </div>
  )
}

export function ConfessionVisual() {
  return (
    <figure
      aria-hidden="true"
      className="w-full max-w-sm -rotate-2 rounded-panel border border-cream-200 bg-white p-5 shadow-[0_20px_40px_-24px_rgb(61_23_73/0.45)] transition-transform duration-500 ease-soft group-hover:rotate-0"
    >
      <p className="text-[0.68rem] font-bold tracking-widest text-plum-700 uppercase">
        Library · 2nd floor
      </p>
      <blockquote className="mt-2 font-display text-lg leading-snug text-ink-dark italic">
        “To the person who always saves the window seat — I’ve been taking the long way past it all
        semester.”
      </blockquote>
      <figcaption className="mt-4 inline-flex items-center gap-1.5 text-xs font-medium text-ink-dark-muted">
        <EyeOff className="size-3.5" />
        Posted anonymously
      </figcaption>
    </figure>
  )
}

export function SecretCrushVisual() {
  return (
    <div
      aria-hidden="true"
      className="relative flex h-40 w-full max-w-64 items-center justify-center"
    >
      <div className="absolute left-4 size-32 rounded-full border border-rose-300/30 bg-rose-400/10 transition-transform duration-700 ease-soft group-hover:translate-x-3" />
      <div className="absolute right-4 size-32 rounded-full border border-lavender-300/30 bg-lavender-400/10 transition-transform duration-700 ease-soft group-hover:-translate-x-3" />
      <span className="absolute left-12 font-display text-3xl text-rose-300/80 italic">?</span>
      <span className="absolute right-12 font-display text-3xl text-lavender-300/80 italic">?</span>
      <span className="relative grid size-14 place-items-center rounded-full bg-linear-to-br from-rose-400 to-lavender-400 shadow-glow">
        <LockKeyhole className="size-5 text-night-950 transition-opacity duration-500 group-hover:opacity-0" />
        <Heart className="absolute size-5 fill-night-950 text-night-950 opacity-0 transition-opacity duration-500 group-hover:opacity-100" />
      </span>
    </div>
  )
}

export function ConnectionVisual() {
  return (
    <div aria-hidden="true" className="flex w-full max-w-sm flex-col gap-3">
      <div className="flex items-end gap-2">
        <Avatar initials="A" tone="dusk" className="size-8 text-sm" />
        <p className="max-w-[80%] rounded-2xl rounded-bl-md bg-white/[0.08] px-4 py-2.5 text-sm text-ink">
          So… was that you at the open mic last night?
        </p>
      </div>
      <div className="flex items-end justify-end gap-2">
        <p className="max-w-[80%] rounded-2xl rounded-br-md bg-linear-to-r from-rose-400 to-rose-300 px-4 py-2.5 text-sm font-medium text-night-950">
          Maybe. Were you the one clapping the loudest?
        </p>
      </div>
      <div className="flex items-end gap-2">
        <Avatar initials="A" tone="dusk" className="size-8 text-sm" />
        <p className="inline-flex gap-1 rounded-2xl rounded-bl-md bg-white/[0.08] px-4 py-3.5">
          <span className="size-1.5 rounded-full bg-ink-muted" />
          <span className="size-1.5 rounded-full bg-ink-muted/70" />
          <span className="size-1.5 rounded-full bg-ink-muted/40" />
        </p>
      </div>
    </div>
  )
}
