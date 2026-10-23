import { Router } from 'express'
import type { DatabaseCheck } from '../lib/database.js'

const SERVICE_NAME = 'kehdona-api'
const VERSION = process.env.npm_package_version ?? '0.1.0'

/**
 * GET /api/health
 *
 * Always responds 200 while the API process is serving requests. Database
 * reachability is reported separately in `database.status` so an outage there
 * doesn't make the whole API look down.
 */
export function healthRouter(checkDatabase: DatabaseCheck) {
  const router = Router()

  router.get('/', async (_req, res) => {
    res.set('Cache-Control', 'no-store')
    res.json({
      status: 'ok',
      service: SERVICE_NAME,
      version: VERSION,
      uptimeSeconds: Math.round(process.uptime()),
      timestamp: new Date().toISOString(),
      database: await checkDatabase(),
    })
  })

  return router
}
