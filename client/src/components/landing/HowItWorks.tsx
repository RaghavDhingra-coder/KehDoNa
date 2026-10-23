import { HeartHandshake, Sparkles, UserRoundPlus } from 'lucide-react'
import { Container } from '../ui/Container'
import { IconBadge } from '../ui/IconBadge'
import { Reveal } from '../ui/Reveal'
import { SectionHeading } from '../ui/SectionHeading'

const steps = [
  {
    icon: UserRoundPlus,
    title: 'Create your profile.',
    description:
      'Share your vibe — the songs on repeat, the corners of campus you love, the things that make you, you.',
  },
  {
    icon: Sparkles,
    title: 'Discover people who match your vibe.',
    description: 'Meet students who see the world a little like you do, at your own pace.',
  },
  {
    icon: HeartHandshake,
    title: 'See where the connection goes.',
    description: 'Say hello, keep a little mystery, or let it turn into something more.',
  },
]

export function HowItWorks() {
  return (
    <section
      id="about"
      aria-labelledby="about-title"
      className="relative overflow-hidden border-y border-line bg-night-900 py-20 sm:py-28"
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-40 left-1/2 h-80 w-[48rem] -translate-x-1/2 rounded-full bg-violet-600/20 blur-[120px]"
      />
      <Container className="relative">
        <Reveal>
          <SectionHeading
            id="about-title"
            align="center"
            eyebrow="How it works"
            title="Three steps to someone who gets you."
            description="No endless swiping. Just a calmer, kinder way to meet people on campus."
          />
        </Reveal>

        <ol className="relative mt-16 grid gap-10 md:grid-cols-3 md:gap-8">
          {/* Connector line between the steps */}
          <span
            aria-hidden="true"
            className="absolute top-7 bottom-7 left-7 w-px bg-linear-to-b from-rose-300/50 via-lavender-300/40 to-transparent md:top-7 md:right-[16%] md:bottom-auto md:left-[16%] md:h-px md:w-auto md:bg-linear-to-r"
          />
          {steps.map((step, index) => (
            <Reveal
              as="li"
              key={step.title}
              delay={index * 120}
              className="relative flex gap-5 md:flex-col md:items-center md:text-center"
            >
              <span className="relative grid size-14 shrink-0 place-items-center rounded-full border border-line-strong bg-night-850 font-display text-xl text-rose-300 italic shadow-card">
                {String(index + 1).padStart(2, '0')}
              </span>
              <div className="md:mt-2">
                <IconBadge icon={step.icon} tone="lavender" className="hidden md:inline-grid" />
                <h3 className="font-display text-h3 font-medium text-ink md:mt-5">{step.title}</h3>
                <p className="mt-2 max-w-xs text-pretty text-ink-muted md:mx-auto">
                  {step.description}
                </p>
              </div>
            </Reveal>
          ))}
        </ol>
      </Container>
    </section>
  )
}
