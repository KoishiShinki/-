export function mediaOrFallback(value, fallback) {
  if (typeof value !== "string" || !value.trim()) return fallback;
  if (value.startsWith("/api/")) return value;
  if (value.startsWith("/") && !value.startsWith("//")) return "/api" + value;
  try {
    const url = new URL(value);
    return ["http:", "https:"].includes(url.protocol) ? url.href : fallback;
  } catch {
    return fallback;
  }
}
