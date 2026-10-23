/**
 * Minimal client for the KehDoNa API. The base URL comes from VITE_API_URL so
 * the same build can point at a local or hosted server.
 */
const API_URL = (import.meta.env.VITE_API_URL ?? 'http://localhost:4000').replace(/\/$/, '')

export type HealthResponse = {
  status: 'ok'
  service: string
  version: string
  uptimeSeconds: number
  timestamp: string
  database: { status: 'up' | 'down' | 'not_configured'; latencyMs?: number }
}

export async function apiGet<T>(path: string, signal?: AbortSignal): Promise<T> {
  const response = await fetch(`${API_URL}${path}`, {
    signal,
    headers: { Accept: 'application/json' },
  })
  if (!response.ok) {
    throw new Error(`Request to ${path} failed with status ${response.status}`)
  }
  return (await response.json()) as T
}

export function getHealth(signal?: AbortSignal) {
  return apiGet<HealthResponse>('/api/health', signal)
}
