import { describe, expect, it } from "vitest";

import { resolveClientIpAddress } from "../../src/util/ipAddress";

describe("resolveClientIpAddress", () => {
  it("prefers cf-connecting-ip when valid", () => {
    const value = resolveClientIpAddress({
      headers: {
        "cf-connecting-ip": "203.0.113.8"
      },
      socket: {
        remoteAddress: "10.0.0.1"
      }
    } as never);

    expect(value).toBe("203.0.113.8");
  });

  it("normalizes ipv4-mapped ipv6", () => {
    const value = resolveClientIpAddress({
      headers: {},
      socket: {
        remoteAddress: "::ffff:127.0.0.1"
      }
    } as never);

    expect(value).toBe("127.0.0.1");
  });
});
