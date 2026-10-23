import type { PrismaClient } from '../generated/prisma/client.js'

export type DatabaseStatus =
  { status: 'up'; latencyMs: number } | { status: 'down' } | { status: 'not_configured' }

export type DatabaseCheck = () => Promise<DatabaseStatus>

/** Pings the database with `SELECT 1`, giving up after `timeoutMs`. */
export function createDatabaseCheck(prisma: PrismaClient | null, timeoutMs = 2000): DatabaseCheck {
  return async () => {
    if (!prisma) return { status: 'not_configured' }

    const started = performance.now()
    let timer: NodeJS.Timeout | undefined
    try {
      await Promise.race([
        prisma.$queryRaw`SELECT 1`,
        new Promise((_, reject) => {
          timer = setTimeout(() => reject(new Error('Database ping timed out')), timeoutMs)
        }),
      ])
      return { status: 'up', latencyMs: Math.round(performance.now() - started) }
    } catch (error) {
      console.warn(`Database health check failed: ${summarize(error)}`)
      return { status: 'down' }
    } finally {
      clearTimeout(timer)
    }
  }
}

/** Prisma errors span several lines; keep the last meaningful one for logs. */
function summarize(error: unknown): string {
  const lines = String(error instanceof Error ? error.message : error)
    .split('\n')
    .map((line) => line.trim())
    .filter(Boolean)
  return lines.at(-1) ?? 'unknown error'
}
