import { Compass, HeartHandshake, LockKeyhole, MessageCircleHeart } from 'lucide-react'
import { Container } from '../ui/Container'
import { Reveal } from '../ui/Reveal'
import { SectionHeading } from '../ui/SectionHeading'
import { FeatureCard } from './FeatureCard'
import { ConfessionVisual, ConnectionVisual, SecretCrushVisual, VibeVisual } from './FeatureVisuals'

export function Features() {
  return (
    <section id="discover" aria-labelledby="discover-title" className="py-20 sm:py-28">
      <Container>
        <Reveal className="flex flex-col gap-6 md:flex-row md:items-end md:justify-between">
          <SectionHeading
            id="discover-title"
            eyebrow="What’s coming"
            title={
              <>
                Made for the feelings <span className="text-rose-300 italic">in between.</span>
              </>
            }
            description="KehDoNa is being built one thoughtful feature at a time. Here’s a first look at what’s on the way."
          />
          <p className="max-w-56 text-sm text-ink-subtle md:text-right">
            Previews only — these features aren’t live yet.
          </p>
        </Reveal>

        <div className="mt-14 grid gap-5 md:grid-cols-2 lg:grid-cols-12 [&>*]:min-w-0">
          <Reveal className="md:col-span-2 lg:col-span-7">
            <FeatureCard
              title="Discover Your Vibe"
              description="Find people who see the world a little like you do."
              icon={Compass}
              tone="lavender"
              visual={<VibeVisual />}
            />
          </Reveal>
          <Reveal className="lg:col-span-5" delay={100}>
            <FeatureCard
              title="Anonymous Confessions"
              description="Some things are easier to say when your name stays out of it."
              icon={MessageCircleHeart}
              tone="cream"
              visual={<ConfessionVisual />}
            />
          </Reveal>
          <Reveal className="lg:col-span-5">
            <FeatureCard
              title="Secret Crush"
              description="Some feelings are better discovered together."
              icon={LockKeyhole}
              tone="plum"
              visual={<SecretCrushVisual />}
            />
          </Reveal>
          <Reveal className="md:col-span-2 lg:col-span-7" delay={100}>
            <FeatureCard
              title="Meaningful Connections"
              description="Turn a little curiosity into a real connection."
              icon={HeartHandshake}
              tone="violet"
              visual={<ConnectionVisual />}
            />
          </Reveal>
        </div>
      </Container>
    </section>
  )
}
