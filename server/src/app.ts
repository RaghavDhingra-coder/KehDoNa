import cors from 'cors'
import express from 'express'
import helmet from 'helmet'
import morgan from 'morgan'
import type { DatabaseCheck } from './lib/database.js'
import { HttpError } from './lib/httpError.js'
import { errorHandler, notFoundHandler } from './middleware/errorHandler.js'
import { healthRouter } from './routes/health.js'

export type AppOptions = {
  corsOrigins: string[]
  checkDatabase: DatabaseCheck
  logFormat?: 'dev' | 'combined' | false
}

export function createApp({ corsOrigins, checkDatabase, logFormat = 'dev' }: AppOptions) {
  const app = express()

  app.disable('x-powered-by')
  app.set('trust proxy', 'loopback')

  app.use(helmet())
  app.use(
    cors({
      origin(origin, callback) {
        // Requests without an Origin header (curl, server-to-server) are allowed.
        if (!origin || corsOrigins.includes(origin)) return callback(null, true)
        callback(new HttpError(403, 'cors_forbidden', `Origin ${origin} is not allowed`))
      },
    }),
  )
  if (logFormat) app.use(morgan(logFormat))
  app.use(express.json({ limit: '100kb' }))

  app.use('/api/health', healthRouter(checkDatabase))

  app.use(notFoundHandler)
  app.use(errorHandler)

  return app
}
