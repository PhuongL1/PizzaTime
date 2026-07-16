import type { NextFunction, Request, Response } from "express";

import { HttpError } from "../util/httpError";

import type { FirebaseAuthVerifier } from "./authenticatedRequest";

export function createFirebaseAuthMiddleware(verifier: FirebaseAuthVerifier) {
  return async function firebaseAuthMiddleware(
    request: Request,
    _response: Response,
    next: NextFunction
  ): Promise<void> {
    try {
      const authorization = request.header("authorization");
      if (authorization === undefined) {
        throw new HttpError(401, "UNAUTHENTICATED", "Authorization header is required.");
      }
      const [scheme, token] = authorization.split(" ");
      if (scheme !== "Bearer" || token === undefined || token.length === 0) {
        throw new HttpError(401, "UNAUTHENTICATED", "Bearer token is required.");
      }
      const principal = await verifier.verifyIdToken(token);
      Object.assign(request, { principal });
      next();
    } catch (error) {
      next(error);
    }
  };
}
