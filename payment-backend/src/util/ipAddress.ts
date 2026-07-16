import type { Request } from "express";
import { isIP } from "node:net";

export function resolveClientIpAddress(request: Request): string {
  const candidates = [
    singleHeaderIp(request.headers["cf-connecting-ip"]),
    singleHeaderIp(request.headers["x-real-ip"]),
    forwardedForIp(request.headers["x-forwarded-for"]),
    normalizeIp(request.socket.remoteAddress)
  ];

  for (const candidate of candidates) {
    if (candidate !== undefined) {
      return candidate;
    }
  }

  return "127.0.0.1";
}

function singleHeaderIp(value: string | string[] | undefined): string | undefined {
  if (Array.isArray(value)) {
    return undefined;
  }
  return normalizeIp(value);
}

function forwardedForIp(value: string | string[] | undefined): string | undefined {
  if (Array.isArray(value)) {
    return undefined;
  }
  if (value === undefined) {
    return undefined;
  }
  const first = value.split(",")[0]?.trim();
  return normalizeIp(first);
}

function normalizeIp(value: string | undefined): string | undefined {
  if (value === undefined || value.length === 0) {
    return undefined;
  }
  const normalized = value.startsWith("::ffff:") ? value.slice(7) : value;
  return isIP(normalized) === 0 ? undefined : normalized;
}
