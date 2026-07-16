import type { NextFunction, Request, Response } from "express";

import type { SafeLogger } from "../util/safeLogger";
import { HttpError } from "../util/httpError";

export function createErrorHandler(logger: SafeLogger, nodeEnv: string) {
  return function errorHandler(
    error: unknown,
    _request: Request,
    response: Response,
    _next: NextFunction
  ): void {
    void _next;
    if (error instanceof HttpError) {
      response.status(error.statusCode).json({
        error: {
          code: error.code,
          message: error.message
        }
      });
      return;
    }

    logger.error("Unhandled backend error", {
      error: error instanceof Error ? error.message : "unknown"
    });
    response.status(500).json({
      error: {
        code: "INTERNAL_ERROR",
        message: nodeEnv === "production" ? "Internal server error." : "Unhandled server error."
      }
    });
  };
}
