import type { NextFunction, Request, Response } from "express";

import { HttpError } from "../util/httpError";

type Counter = {
  count: number;
  resetAt: number;
};

export function createSimpleRateLimit(options: {
  limit: number;
  windowMs: number;
  keyFactory: (request: Request) => string;
}) {
  const counters = new Map<string, Counter>();

  return function rateLimit(request: Request, _response: Response, next: NextFunction): void {
    const key = options.keyFactory(request);
    const now = Date.now();
    const current = counters.get(key);

    if (current === undefined || current.resetAt <= now) {
      counters.set(key, {
        count: 1,
        resetAt: now + options.windowMs
      });
      next();
      return;
    }

    current.count += 1;
    if (current.count > options.limit) {
      next(new HttpError(429, "RATE_LIMITED", "Too many requests."));
      return;
    }
    next();
  };
}
