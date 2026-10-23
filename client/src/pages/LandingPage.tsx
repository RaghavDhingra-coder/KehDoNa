import { BrandStatement } from '../components/landing/BrandStatement'
import { Features } from '../components/landing/Features'
import { Hero } from '../components/landing/Hero'
import { HowItWorks } from '../components/landing/HowItWorks'
import { useDocumentTitle } from '../hooks/useDocumentTitle'

export function LandingPage() {
  useDocumentTitle()
  return (
    <>
      <Hero />
      <Features />
      <HowItWorks />
      <BrandStatement />
    </>
  )
}
