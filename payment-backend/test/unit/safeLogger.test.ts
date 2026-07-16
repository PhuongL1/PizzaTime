import { describe, expect, it } from "vitest";

import { ConsoleSafeLogger } from "../../src/util/safeLogger";

describe("ConsoleSafeLogger", () => {
  it("redacts secrets and masks uids", () => {
    const logger = new ConsoleSafeLogger();
    const lines: string[] = [];
    const original = console.info;
    console.info = (line?: unknown) => {
      lines.push(String(line));
    };

    try {
      logger.info("test", {
        uid: "customer-abcdef",
        token: "secret-token",
        paymentPageUrl: "https://secret.example",
        qrPayload: "https://secret.example",
        nested: {
          secureHash: "hash"
        }
      });
    } finally {
      console.info = original;
    }

    expect(lines[0]).toContain("cus***def");
    expect(lines[0]).toContain("\"token\":\"[redacted]\"");
    expect(lines[0]).toContain("\"paymentPageUrl\":\"[redacted]\"");
    expect(lines[0]).toContain("\"qrPayload\":\"[redacted]\"");
    expect(lines[0]).toContain("\"secureHash\":\"[redacted]\"");
  });
});
