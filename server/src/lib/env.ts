import { config as loadEnv } from 'dotenv'
import { z } from 'zod'

loadEnv({ quiet: true })

const envSchema = z.object({
  NODE_ENV: z.enum(['development', 'test', 'production']).default('development'),
  PORT: z.coerce.number().int().min(1).max(65535).default(4000),
  CORS_ORIGINS: z
    .string()
    .default('http://localhost:5173')
    .transform((value) =>
      value
        .split(',')
        .map((origin) => origin.trim())
        .filter(Boolean),
    ),
  DATABASE_URL: z
    .string()
    .trim()
    .optional()
    .transform((value) => value || undefined),
})

export type Env = z.infer<typeof envSchema>

function parseEnv(): Env {
  const result = envSchema.safeParse(process.env)
  if (!result.success) {
    console.error('Invalid environment configuration:\n' + z.prettifyError(result.error))
    process.exit(1)
  }
  return result.data
}

export const env = parseEnv()
