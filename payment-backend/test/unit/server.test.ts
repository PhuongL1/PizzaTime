import { mkdtempSync, rmSync, writeFileSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

describe("server bootstrap", () => {
  const originalEnv = { ...process.env };

  beforeEach(() => {
    vi.resetModules();
    vi.restoreAllMocks();
    process.env = { ...originalEnv };
  });

  afterEach(() => {
    process.env = { ...originalEnv };
  });

  it("loads dotenv before server startup", async () => {
    const tempDir = mkdtempSync(join(tmpdir(), "payment-backend-dotenv-"));
    const envPath = join(tempDir, ".env");
    const listen = vi.fn((_port: number, callback?: () => void) => {
      callback?.();
      return {} as never;
    });
    const close = vi.fn((callback?: () => void) => {
      callback?.();
      return {} as never;
    });
    const createServer = vi.fn(() => ({
      headersTimeout: 0,
      requestTimeout: 0,
      listen,
      close
    }));
    const createApp = vi.fn(() => {
      expect(process.env.FIREBASE_PROJECT_ID).toBe("demo-project");
      expect(process.env.PUBLIC_BASE_URL).toBe("https://payments.example.test");
      expect(process.env.DEMO_PAYMENT_TOKEN_SECRET).toBe(
        "demo-secret-demo-secret-demo-secret-1234"
      );

      return {
        app: {} as never,
        env: { port: 8080 }
      };
    });

    writeFileSync(
      envPath,
      [
        "FIREBASE_PROJECT_ID=demo-project",
        "PUBLIC_BASE_URL=https://payments.example.test",
        "DEMO_PAYMENT_TOKEN_SECRET=demo-secret-demo-secret-demo-secret-1234"
      ].join("\n")
    );

    delete process.env.FIREBASE_PROJECT_ID;
    delete process.env.PUBLIC_BASE_URL;
    delete process.env.DEMO_PAYMENT_TOKEN_SECRET;
    process.env.DOTENV_CONFIG_PATH = envPath;

    vi.doMock("../../src/app", () => ({
      createApp
    }));
    vi.doMock("node:http", () => ({
      createServer
    }));
    vi.spyOn(console, "info").mockImplementation(() => undefined);
    vi.spyOn(process, "on").mockImplementation(() => process);

    try {
      await import("../../src/server");
    } finally {
      rmSync(tempDir, { force: true, recursive: true });
    }

    expect(createApp).toHaveBeenCalledTimes(1);
    expect(createServer).toHaveBeenCalledTimes(1);
    expect(listen).toHaveBeenCalledWith(8080, expect.any(Function));
  });
});
