import { ArrowRight, EyeOff, HeartHandshake, LockKeyhole, Sparkles } from 'lucide-react'
import { ButtonLink } from '../ui/Button'
import { Badge } from '../ui/Badge'
import { Container } from '../ui/Container'
import { ProfilePreviewCard, type PreviewProfile } from './ProfilePreviewCard'

const previewProfile: PreviewProfile = {
  name: 'Mehr',
  details: 'B.Des · 2nd year',
  location: 'Usually at the campus café',
  prompt: {
    question: 'The way to my heart',
    answer: 'Send me a song that sounds like a rainy evening.',
  },
  vibes: ['Indie playlists', 'Late-night chai', 'Poetry', 'Film cameras'],
}

const principles = [
  { icon: EyeOff, label: 'Your name, your choice' },
  { icon: HeartHandshake, label: 'Mutual before it’s revealed' },
]

export function Hero() {
  return (
    <section
      aria-labelledby="hero-title"
      className="grain relative overflow-hidden pt-28 pb-20 sm:pt-36 lg:pt-40 lg:pb-28"
    >
      <HeroBackdrop />

      <Container className="grid items-center gap-16 lg:grid-cols-[1.1fr_1fr] lg:gap-10">
        <div className="max-w-2xl">
          <div className="animate-[rise_0.9s_var(--ease-soft)_both]">
            <Badge tone="rose" icon={<Sparkles className="size-3.5" aria-hidden="true" />}>
              Coming soon to campus
            </Badge>
          </div>

          <h1
            id="hero-title"
            className="mt-7 animate-[rise_0.9s_var(--ease-soft)_0.08s_both] font-display text-display font-medium text-balance text-ink"
          >
            Some feelings are meant to be{' '}
            <span className="text-gradient-rose pr-1 italic">found.</span>
          </h1>

          <p className="mt-6 max-w-xl animate-[rise_0.9s_var(--ease-soft)_0.16s_both] text-lead text-pretty text-ink-muted">
            A little mystery. A real connection. A place to find the people who just get you.
          </p>

          <div className="mt-10 flex animate-[rise_0.9s_var(--ease-soft)_0.24s_both] flex-col gap-3 sm:flex-row sm:items-center">
            <ButtonLink to="/join" size="lg">
              Find Your Connection
              <ArrowRight
                className="size-4 transition-transform duration-300 group-hover:translate-x-1"
                aria-hidden="true"
              />
            </ButtonLink>
            <ButtonLink to="/#discover" size="lg" variant="secondary">
              Explore KehDoNa
            </ButtonLink>
          </div>

          <ul className="mt-10 flex animate-[rise_0.9s_var(--ease-soft)_0.32s_both] flex-wrap gap-x-6 gap-y-3 text-sm text-ink-subtle">
            {principles.map(({ icon: Icon, label }) => (
              <li key={label} className="inline-flex items-center gap-2">
                <Icon className="size-4 text-lavender-300" aria-hidden="true" />
                {label}
              </li>
            ))}
          </ul>
        </div>

        <HeroPreview />
      </Container>
    </section>
  )
}

function HeroBackdrop() {
  return (
    <div aria-hidden="true" className="pointer-events-none absolute inset-0 -z-10">
      <div className="absolute inset-0 bg-[radial-gradient(120%_80%_at_80%_0%,var(--color-plum-800)_0%,transparent_60%)]" />
      <div className="absolute inset-0 bg-[radial-gradient(70%_60%_at_0%_100%,rgb(76_55_160/0.35)_0%,transparent_70%)]" />
      <div className="absolute top-[18%] right-[8%] size-[28rem] rounded-full bg-rose-500/20 blur-[110px]" />
      <div className="absolute inset-x-0 bottom-0 h-40 bg-linear-to-b from-transparent to-night-950" />
    </div>
  )
}

/** Layered, illustrative product preview. All content is fictional. */
function HeroPreview() {
  return (
    <figure className="relative mx-auto w-full max-w-[26rem] animate-[rise_1.1s_var(--ease-soft)_0.2s_both] lg:max-w-[28rem]">
      <div className="relative px-6 pt-12 pb-32 sm:px-10 sm:pb-28">
        {/* Soft glow behind the stack */}
        <div
          aria-hidden="true"
          className="absolute inset-x-8 top-16 bottom-32 -z-10 rounded-full bg-violet-500/35 blur-3xl"
        />

        {/* Card peeking out behind the main profile */}
        <div
          aria-hidden="true"
          className="absolute inset-x-12 top-8 bottom-36 rotate-[7deg] rounded-card border border-line bg-linear-to-br from-violet-700 to-plum-800 opacity-80 shadow-card sm:inset-x-16"
        />

        <ProfilePreviewCard profile={previewProfile} className="relative" />

        {/* Vibe match chip */}
        <div className="absolute top-0 left-0 flex items-center gap-3 rounded-panel border border-line-strong bg-night-900/90 py-2.5 pr-4 pl-2.5 shadow-card backdrop-blur-md sm:-left-4">
          <MatchRing value={92} />
          <div className="leading-tight">
            <p className="text-sm font-semibold text-ink">Vibe match</p>
            <p className="text-xs text-ink-subtle">4 shared interests</p>
          </div>
        </div>

        {/* Secret crush chip */}
        <div className="absolute top-36 -right-1 inline-flex animate-float items-center gap-2 rounded-pill border border-rose-300/30 bg-night-900/90 py-2 pr-4 pl-2 text-sm font-medium text-blush-200 shadow-glow backdrop-blur-md sm:top-[46%] sm:-right-6">
          <span className="grid size-7 place-items-center rounded-full bg-rose-400/20">
            <LockKeyhole className="size-3.5 text-rose-300" aria-hidden="true" />
          </span>
          It’s mutual
        </div>

        {/* Confession snippet */}
        <blockquote className="absolute bottom-0 left-0 w-[78%] rounded-panel border border-plum-700/10 bg-cream-50 p-4 text-ink-dark shadow-lift sm:-left-6 sm:w-[72%]">
          <p className="text-[0.68rem] font-bold tracking-widest text-plum-700 uppercase">
            Confession #214
          </p>
          <p className="mt-1.5 font-display text-[0.98rem] leading-snug italic">
            “To whoever leaves sticky notes in the library books — you made my whole week.”
          </p>
        </blockquote>
      </div>

      <figcaption className="mt-5 text-center text-xs text-ink-subtle">
        Illustrative preview. People and posts shown are fictional.
      </figcaption>
    </figure>
  )
}

function MatchRing({ value }: { value: number }) {
  const radius = 17
  const circumference = 2 * Math.PI * radius
  return (
    <span className="relative grid size-11 place-items-center">
      <svg viewBox="0 0 40 40" className="absolute inset-0 -rotate-90" aria-hidden="true">
        <defs>
          <linearGradient id="match-ring" x1="0" x2="1">
            <stop offset="0%" stopColor="var(--color-rose-300)" />
            <stop offset="100%" stopColor="var(--color-lavender-300)" />
          </linearGradient>
        </defs>
        <circle
          cx="20"
          cy="20"
          r={radius}
          fill="none"
          stroke="var(--color-line-strong)"
          strokeWidth="3"
        />
        <circle
          cx="20"
          cy="20"
          r={radius}
          fill="none"
          stroke="url(#match-ring)"
          strokeWidth="3"
          strokeLinecap="round"
          strokeDasharray={circumference}
          strokeDashoffset={circumference * (1 - value / 100)}
        />
      </svg>
      <span className="text-[0.7rem] font-bold text-ink">{value}%</span>
    </span>
  )
}
