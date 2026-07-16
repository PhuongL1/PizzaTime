import { createHash, createHmac, timingSafeEqual } from "node:crypto";

export function sha256Hex(input: string): string {
  return createHash("sha256").update(input, "utf8").digest("hex");
}

export function hmacSha512Hex(secret: string, input: string): string {
  return createHmac("sha512", secret).update(input, "utf8").digest("hex");
}

export function hmacSha256Base64Url(secret: string, input: string): string {
  return createHmac("sha256", secret).update(input, "utf8").digest("base64url");
}

export function safeCompareHex(left: string, right: string): boolean {
  const leftBuffer = Buffer.from(left, "utf8");
  const rightBuffer = Buffer.from(right, "utf8");
  if (leftBuffer.length !== rightBuffer.length) {
    return false;
  }
  return timingSafeEqual(leftBuffer, rightBuffer);
}

export function maskIdentifier(value: string): string {
  if (value.length <= 6) {
    return "***";
  }
  return `${value.slice(0, 3)}***${value.slice(-3)}`;
}
