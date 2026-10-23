import { useEffect, useState } from 'react'
import { getHealth, type HealthResponse } from '../lib/api'

type ApiHealth =
  { state: 'loading' } | { state: 'online'; data: HealthResponse } | { state: 'offline' }

/** Pings GET /api/health once on mount. */
export function useApiHealth() {
  const [health, setHealth] = useState<ApiHealth>({ state: 'loading' })

  useEffect(() => {
    const controller = new AbortController()
    getHealth(controller.signal)
      .then((data) => setHealth({ state: 'online', data }))
      .catch((error: unknown) => {
        if (!controller.signal.aborted) {
          console.warn('KehDoNa API unreachable:', error)
          setHealth({ state: 'offline' })
        }
      })
    return () => controller.abort()
  }, [])

  return health
}
