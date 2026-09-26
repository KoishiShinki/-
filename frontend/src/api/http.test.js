import {
  afterEach,
  beforeAll,
  beforeEach,
  describe,
  expect,
  it,
  vi,
} from "vitest";
import axios from "axios";
let api;
beforeAll(async () => {
  const storage = new Map();
  vi.stubGlobal("sessionStorage", {
    getItem: (k) => storage.get(k) || null,
    setItem: (k, v) => storage.set(k, String(v)),
    removeItem: (k) => storage.delete(k),
  });
  api = await import("./http.js");
});
beforeEach(() => {
  api.clearSession();
  api.requestError.value = "";
  api.cancelReads();
});
afterEach(() => {
  api.cancelReads();
  vi.restoreAllMocks();
});
function result(config, data = { code: 200, data: [] }) {
  return { data, status: 200, statusText: "OK", headers: {}, config };
}
describe("session-safe API transport", () => {
  it("deduplicates simultaneous identical GET reads and includes the bearer session", async () => {
    api.setSession("opaque-test-token", {
      username: "author",
      roles: ["USER"],
    });
    let calls = 0;
    api.http.defaults.adapter = async (config) => {
      calls++;
      expect(config.headers.Authorization).toBe("Bearer opaque-test-token");
      await new Promise((r) => setTimeout(r, 5));
      return result(config);
    };
    const first = api.http.get("/app/worldline/myList");
    const second = api.http.get("/app/worldline/myList");
    expect(first).toBe(second);
    await Promise.all([first, second]);
    expect(calls).toBe(1);
    expect(api.pendingRequests.value).toBe(0);
  });
  it("does not deduplicate reads across different sessions", async () => {
    api.http.defaults.adapter = async (config) => {
      await new Promise((r) => setTimeout(r, 5));
      return result(config);
    };
    api.setSession("first", { roles: ["USER"] });
    const first = api.http.get("/app/worldline/myList");
    api.setSession("second", { roles: ["USER"] });
    const second = api.http.get("/app/worldline/myList");
    expect(first).not.toBe(second);
    await Promise.all([first, second]);
  });
  it("cancelled old reads cannot evict an in-flight replacement", async () => {
    let finish;
    api.http.defaults.adapter = (config) =>
      new Promise((resolve) => {
        finish = () => resolve(result(config));
      });
    const old = api.http.get("/public/worldline/list").catch((e) => e);
    await Promise.resolve();
    api.cancelReads();
    const replacement = api.http.get("/public/worldline/list");
    await old;
    await Promise.resolve();
    const duplicate = api.http.get("/public/worldline/list");
    expect(duplicate).toBe(replacement);
    finish();
    await replacement;
  });
  it("preserves the blob export contract", async () => {
    const blob = new Blob(["word document"]);
    api.http.defaults.adapter = async (config) => result(config, blob);
    const response = await api.http.post(
      "/app/chapter/exportWord/1",
      undefined,
      { responseType: "blob" },
    );
    expect(response.data).toBe(blob);
  });
  it("clears authorization after an expired session and presents a useful message", async () => {
    api.setSession("expired", { roles: ["ADMIN"] });
    api.http.defaults.adapter = async (config) => {
      throw new axios.AxiosError(
        "Unauthorized",
        "ERR_BAD_REQUEST",
        config,
        null,
        {
          status: 401,
          data: { msg: "会话已过期" },
          config,
          headers: {},
          statusText: "Unauthorized",
        },
      );
    };
    await expect(api.http.get("/app/auth/profile")).rejects.toThrow(
      "会话已过期",
    );
    expect(api.getToken()).toBe("");
    expect(api.getUser()).toEqual({});
    expect(api.pendingRequests.value).toBe(0);
  });
  it("does not deduplicate mutations", async () => {
    let calls = 0;
    api.http.defaults.adapter = async (config) => {
      calls++;
      return result(config);
    };
    await Promise.all([
      api.http.post("/app/event", { eventTitle: "A" }),
      api.http.post("/app/event", { eventTitle: "B" }),
    ]);
    expect(calls).toBe(2);
  });
});
