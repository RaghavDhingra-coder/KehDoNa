import { useEffect, useRef, useState } from 'react'

/** Reports once an element has scrolled into view, then stops observing. */
export function useInView<T extends Element>(options: IntersectionObserverInit = {}) {
  const ref = useRef<T>(null)
  // Without IntersectionObserver support, show content immediately.
  const [inView, setInView] = useState(() => typeof IntersectionObserver === 'undefined')
  const { rootMargin = '0px 0px -10% 0px', threshold = 0.15 } = options

  useEffect(() => {
    const node = ref.current
    if (!node || inView) return
    const observer = new IntersectionObserver(
      ([entry]) => {
        if (entry?.isIntersecting) {
          setInView(true)
          observer.disconnect()
        }
      },
      { rootMargin, threshold },
    )
    observer.observe(node)
    return () => observer.disconnect()
  }, [inView, rootMargin, threshold])

  return { ref, inView }
}
