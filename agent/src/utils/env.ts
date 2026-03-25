export function envOrDefault(key: string, fallback: string): string {
  return process.env[key]?.trim() || fallback;
}

export function envRequired(key: string): string {
  const value = process.env[key]?.trim();
  if (!value) {
    throw new Error(`Missing required environment variable: ${key}`);
  }
  return value;
}

export function isTruthy(value: string | undefined): boolean {
  return value === "1" || value === "true" || value === "yes";
}
