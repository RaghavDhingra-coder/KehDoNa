import { createApp } from './app.js'
import { createDatabaseCheck } from './lib/database.js'
import { env } from './lib/env.js'
import { createPrismaClient } from './lib/prisma.js'

const prisma = env.DATABASE_URL ? createPrismaClient(env.DATABASE_URL) : null
if (!prisma) {
  console.warn('DATABASE_URL is not set — the API will run without a database.')
}

const app = createApp({
  corsOrigins: env.CORS_ORIGINS,
  checkDatabase: createDatabaseCheck(prisma),
  logFormat: env.NODE_ENV === 'production' ? 'combined' : env.NODE_ENV === 'test' ? false : 'dev',
})

const server = app.listen(env.PORT, (error) => {
  if (error) {
    console.error(`Could not start the API on port ${env.PORT}:`, error.message)
    process.exit(1)
  }
  console.log(`KehDoNa API listening on http://localhost:${env.PORT}`)
})

let shuttingDown = false

async function shutdown(signal: NodeJS.Signals) {
  if (shuttingDown) return
  shuttingDown = true
  console.log(`${signal} received — shutting down gracefully…`)

  // Force exit if open connections keep the server alive too long.
  const forceExit = setTimeout(() => {
    console.error('Shutdown timed out; forcing exit.')
    process.exit(1)
  }, 10_000)
  forceExit.unref()

  server.close(async (error) => {
    await prisma?.$disconnect()
    process.exit(error ? 1 : 0)
  })
  server.closeIdleConnections()
}

process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
