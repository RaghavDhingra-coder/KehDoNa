import request from 'supertest'
import { describe, expect, it } from 'vitest'
import { createApp } from '../src/app.js'
import type { DatabaseStatus } from '../src/lib/database.js'

function buildApp(database: DatabaseStatus = { status: 'up', latencyMs: 3 }) {
  return createApp({
    corsOrigins: ['http://localhost:5173'],
    checkDatabase: async () => database,
    logFormat: false,
  })
}

describe('GET /api/health', () => {
  it('reports the API and database as healthy', async () => {
    const response = await request(buildApp()).get('/api/health')

    expect(response.status).toBe(200)
    expect(response.headers['cache-control']).toBe('no-store')
    expect(response.body).toMatchObject({
      status: 'ok',
      service: 'kehdona-api',
      database: { status: 'up', latencyMs: 3 },
    })
    expect(typeof response.body.uptimeSeconds).toBe('number')
    expect(Number.isNaN(Date.parse(response.body.timestamp))).toBe(false)
  })

  it.each([{ status: 'down' }, { status: 'not_configured' }] as const)(
    'stays available when the database is $status',
    async (database) => {
      const response = await request(buildApp(database)).get('/api/health')

      expect(response.status).toBe(200)
      expect(response.body.status).toBe('ok')
      expect(response.body.database).toEqual(database)
    },
  )
})

describe('CORS', () => {
  it('allows configured origins', async () => {
    const response = await request(buildApp())
      .get('/api/health')
      .set('Origin', 'http://localhost:5173')

    expect(response.headers['access-control-allow-origin']).toBe('http://localhost:5173')
  })

  it('rejects unknown origins with a JSON error', async () => {
    const response = await request(buildApp())
      .get('/api/health')
      .set('Origin', 'https://example.invalid')

    expect(response.status).toBe(403)
    expect(response.body.error.code).toBe('cors_forbidden')
  })
})

describe('error handling', () => {
  it('returns JSON 404s for unknown routes', async () => {
    const response = await request(buildApp()).get('/api/does-not-exist')

    expect(response.status).toBe(404)
    expect(response.body).toEqual({
      error: { code: 'not_found', message: 'Route GET /api/does-not-exist does not exist' },
    })
  })

  it('rejects malformed JSON bodies', async () => {
    const response = await request(buildApp())
      .post('/api/health')
      .set('Content-Type', 'application/json')
      .send('{"oops":')

    expect(response.status).toBe(400)
    expect(response.body.error.code).toBe('invalid_json')
  })
})
