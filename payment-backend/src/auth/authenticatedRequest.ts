import type { Request } from "express";

export type AuthenticatedPrincipal = {
  uid: string;
};

export type AuthenticatedRequest = Request & {
  principal: AuthenticatedPrincipal;
};

export type FirebaseAuthVerifier = {
  verifyIdToken(token: string): Promise<AuthenticatedPrincipal>;
};
