import { maskIdentifier } from "./hashing";

type LogLevel = "debug" | "info" | "warn" | "error";

type SafeLogValue =
  | null
  | boolean
  | number
  | string
  | SafeLogValue[]
  | {
      [key: string]: SafeLogValue;
    };

export type SafeLogContext = Record<string, SafeLogValue>;

const SENSITIVE_KEYS = new Set([
  "authorization",
  "hashSecret",
  "idToken",
  "paymentUrl",
  "paymentPageUrl",
  "qrPayload",
  "paymentToken",
  "paymentTokenHash",
  "rawQuery",
  "secureHash",
  "token"
]);

export interface SafeLogger {
  debug(message: string, context?: SafeLogContext): void;
  info(message: string, context?: SafeLogContext): void;
  warn(message: string, context?: SafeLogContext): void;
  error(message: string, context?: SafeLogContext): void;
}

export class ConsoleSafeLogger implements SafeLogger {
  debug(message: string, context?: SafeLogContext): void {
    this.write("debug", message, context);
  }

  info(message: string, context?: SafeLogContext): void {
    this.write("info", message, context);
  }

  warn(message: string, context?: SafeLogContext): void {
    this.write("warn", message, context);
  }

  error(message: string, context?: SafeLogContext): void {
    this.write("error", message, context);
  }

  private write(level: LogLevel, message: string, context?: SafeLogContext): void {
    const payload = context === undefined ? undefined : sanitizeContext(context);
    const serialized = payload === undefined ? "" : ` ${JSON.stringify(payload)}`;
    const line = `[${level.toUpperCase()}] ${message}${serialized}`;
    if (level === "error") {
      console.error(line);
      return;
    }
    if (level === "warn") {
      console.warn(line);
      return;
    }
    console.info(line);
  }
}

function sanitizeContext(context: SafeLogContext): SafeLogContext {
  return Object.fromEntries(
    Object.entries(context).map(([key, value]) => [key, sanitizeValue(key, value)])
  );
}

function sanitizeValue(key: string, value: SafeLogValue): SafeLogValue {
  if (SENSITIVE_KEYS.has(key)) {
    return "[redacted]";
  }
  if (key === "uid" && typeof value === "string") {
    return maskIdentifier(value);
  }
  if (Array.isArray(value)) {
    return value.map((entry) => sanitizeValue(key, entry));
  }
  if (value !== null && typeof value === "object") {
    return Object.fromEntries(
      Object.entries(value).map(([childKey, childValue]) => [
        childKey,
        sanitizeValue(childKey, childValue)
      ])
    );
  }
  return value;
}
