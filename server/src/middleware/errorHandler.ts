import type { ErrorRequestHandler, RequestHandler } from 'express'
import { HttpError } from '../lib/httpError.js'

export const notFoundHandler: RequestHandler = (req, _res, next) => {
  next(new HttpError(404, 'not_found', `Route ${req.method} ${req.path} does not exist`))
}

/** Converts any thrown error into a consistent JSON error response. */
export const errorHandler: ErrorRequestHandler = (err, _req, res, next) => {
  if (res.headersSent) return next(err)

  const error = toHttpError(err)
  if (error.status >= 500) console.error(err)

  res.status(error.status).json({
    error: { code: error.code, message: error.message },
  })
}

function toHttpError(err: unknown): HttpError {
  if (err instanceof HttpError) return err

  // Errors raised by express.json() carry a status and a `type`.
  if (isBodyParserError(err)) {
    if (err.type === 'entity.parse.failed') {
      return new HttpError(400, 'invalid_json', 'Request body is not valid JSON')
    }
    if (err.type === 'entity.too.large') {
      return new HttpError(413, 'payload_too_large', 'Request body is too large')
    }
    if (err.status >= 400 && err.status < 500) {
      return new HttpError(err.status, 'bad_request', 'Bad request')
    }
  }

  return new HttpError(500, 'internal_error', 'Something went wrong on our side')
}

function isBodyParserError(err: unknown): err is { status: number; type?: string } {
  return (
    typeof err === 'object' &&
    err !== null &&
    typeof (err as { status?: unknown }).status === 'number'
  )
}
