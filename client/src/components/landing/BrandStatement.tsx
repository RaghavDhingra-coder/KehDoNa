import { ArrowRight } from 'lucide-react'
import { cn } from '../../lib/cn'
import { ButtonLink } from '../ui/Button'
import { Container } from '../ui/Container'
import { Reveal } from '../ui/Reveal'
import { SectionHeading } from '../ui/SectionHeading'

const unsaid = [
  { text: 'I never told you, but…', className: 'md:-rotate-3 md:translate-y-4' },
  { text: 'You probably don’t remember this, but…', className: 'md:rotate-2' },
  { text: 'I’ve written this three times already.', className: 'md:-rotate-1 md:translate-y-6' },
]

export function BrandStatement() {
  return (
    <section
      id="confessions"
      aria-labelledby="confessions-title"
      className="relative overflow-hidden bg-cream-50 py-20 text-ink-dark sm:py-28"
    >
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -right-32 -bottom-40 size-[34rem] rounded-full bg-blush-200/60 blur-[100px]"
      />
      <div
        aria-hidden="true"
        className="pointer-events-none absolute -top-40 -left-32 size-[28rem] rounded-full bg-lavender-200/70 blur-[100px]"
      />

      <Container className="relative">
        <Reveal>
          <SectionHeading
            id="confessions-title"
            tone="light"
            align="center"
            eyebrow="For everything left unsaid"
            title="Some words wait a whole semester to be said."
            description="The hello you rehearsed. The note you never passed. The feeling you almost shared. KehDoNa gives them somewhere gentle to go."
          />
        </Reveal>

        <ul
          className="mx-auto mt-14 grid max-w-4xl gap-4 md:grid-cols-3 md:gap-6"
          aria-label="Things people often leave unsaid"
        >
          {unsaid.map((line, index) => (
            <Reveal
              as="li"
              key={line.text}
              delay={index * 120}
              className={cn(
                'rounded-panel border border-cream-200 bg-white/80 px-6 py-5 shadow-[0_18px_40px_-28px_rgb(61_23_73/0.5)]',
                line.className,
              )}
            >
              <p className="font-display text-xl leading-snug text-plum-800 italic">{line.text}</p>
            </Reveal>
          ))}
        </ul>

        <Reveal className="mt-20 text-center sm:mt-24">
          <p className="font-display text-[clamp(2.4rem,1.4rem+4.6vw,5rem)] leading-[1.05] font-medium tracking-tight text-balance text-ink-dark">
            Dil mein hai,{' '}
            <span className="bg-linear-to-r from-plum-700 via-rose-500 to-violet-600 bg-clip-text pr-1 text-transparent italic">
              toh KehDoNa.
            </span>
          </p>
          <ButtonLink to="/join" size="lg" variant="dark" className="mt-10">
            Find Your Connection
            <ArrowRight
              className="size-4 transition-transform duration-300 group-hover:translate-x-1"
              aria-hidden="true"
            />
          </ButtonLink>
        </Reveal>
      </Container>
    </section>
  )
}
