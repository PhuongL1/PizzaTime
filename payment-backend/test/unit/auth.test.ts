import { describe, expect, it, vi } from "vitest";

import { createFirebaseAuthMiddleware } from "../../src/auth/firebaseAuthMiddleware";

describe("firebase auth middleware", () => {
  it("rejects missing authorization headers", async () => {
    const middleware = createFirebaseAuthMiddleware({
      verifyIdToken: vi.fn()
    });
    const request = {
      header: vi.fn().mockReturnValue(undefined)
    };
    const next = vi.fn();

    await middleware(request as never, {} as never, next);

    expect(next).toHaveBeenCalled();
    expect(next.mock.calls[0]?.[0]).toMatchObject({ statusCode: 401, code: "UNAUTHENTICATED" });
  });

  it("rejects the wrong authorization scheme", async () => {
    const middleware = createFirebaseAuthMiddleware({
      verifyIdToken: vi.fn()
    });
    const request = {
      header: vi.fn().mockReturnValue("Basic abc")
    };
    const next = vi.fn();

    await middleware(request as never, {} as never, next);

    expect(next.mock.calls[0]?.[0]).toMatchObject({ statusCode: 401, code: "UNAUTHENTICATED" });
  });

  it("attaches the verified uid", async () => {
    const middleware = createFirebaseAuthMiddleware({
      verifyIdToken: vi.fn().mockResolvedValue({ uid: "customer-a" })
    });
    const request = {
      header: vi.fn().mockReturnValue("Bearer valid-token")
    } as Record<string, unknown>;
    const next = vi.fn();

    await middleware(request as never, {} as never, next);

    expect(request.principal).toEqual({ uid: "customer-a" });
    expect(next).toHaveBeenCalledWith();
  });
});
