import { describe, it, expect } from "vitest";
import { mediaOrFallback } from "./media";
describe("media URLs", () => {
  it("proxies local private media on the same origin", () => {
    expect(mediaOrFallback("/profile/file.png")).toBe("/api/profile/file.png");
    expect(mediaOrFallback("/api/profile/file.png")).toBe(
      "/api/profile/file.png",
    );
  });
  it("rejects script, inline data and protocol-relative sources", () => {
    for (const input of [
      "javascript:alert(1)",
      "data:image/svg+xml,<svg/>",
      "//untrusted.example/image.png",
      "",
    ])
      expect(mediaOrFallback(input, "fallback")).toBe("fallback");
  });
  it("allows explicit remote HTTP(S) images", () =>
    expect(mediaOrFallback("https://example.org/image.png")).toBe(
      "https://example.org/image.png",
    ));
});
