import axios from "axios";
import { ref } from "vue";
const TOKEN_KEY = "chronicle.session.v1",
  USER_KEY = "chronicle.user.v1";
export const sessionToken = ref(sessionStorage.getItem(TOKEN_KEY) || "");
export const sessionUser = ref(readUser());
export const pendingRequests = ref(0);
export const requestError = ref("");
function readUser() {
  try {
    return JSON.parse(sessionStorage.getItem(USER_KEY) || "{}");
  } catch {
    return {};
  }
}
export function getToken() {
  return sessionToken.value;
}
export function getUser() {
  return sessionUser.value;
}
export function setSession(token, user) {
  sessionToken.value = token || "";
  sessionUser.value = user || {};
  sessionStorage.setItem(TOKEN_KEY, token || "");
  sessionStorage.setItem(USER_KEY, JSON.stringify(user || {}));
}
export function clearSession() {
  sessionStorage.removeItem(TOKEN_KEY);
  sessionStorage.removeItem(USER_KEY);
  sessionToken.value = "";
  sessionUser.value = {};
}
export const http = axios.create({
  baseURL: "/api",
  timeout: 660000,
  withCredentials: true,
});
const inflight = new Map(),
  controllers = new Set();
export function cancelReads() {
  controllers.forEach((c) => c.abort());
  controllers.clear();
  inflight.clear();
}
http.interceptors.request.use((config) => {
  pendingRequests.value++;
  if (getToken()) config.headers.Authorization = `Bearer ${getToken()}`;
  return config;
});
http.interceptors.response.use(
  (response) => {
    pendingRequests.value = Math.max(0, pendingRequests.value - 1);
    if (response.config.responseType === "blob") return { data: response.data };
    const data = response.data;
    if (
      data &&
      typeof data === "object" &&
      "code" in data &&
      data.code !== 200
    ) {
      const error = new Error(data.msg || "请求失败");
      if (data.code === 401) clearSession();
      requestError.value = error.message;
      return Promise.reject(error);
    }
    return data;
  },
  (error) => {
    pendingRequests.value = Math.max(0, pendingRequests.value - 1);
    if (axios.isCancel(error)) return Promise.reject(error);
    if (error.response?.status === 401) clearSession();
    const message =
      error.response?.data?.msg ||
      (error.code === "ECONNABORTED"
        ? "请求等待超时，请稍后重试。"
        : error.response
          ? "请求失败，请稍后重试。"
          : "无法连接服务器，请检查后端服务是否已启动。");
    requestError.value = message;
    return Promise.reject(new Error(message));
  },
);
const originalGet = http.get.bind(http);
http.get = (url, config = {}) => {
  const key =
    getToken() + "|" + url + "|" + JSON.stringify(config.params || {});
  if (inflight.has(key)) return inflight.get(key);
  const controller = new AbortController();
  controllers.add(controller);
  const promise = originalGet(url, {
    ...config,
    signal: config.signal || controller.signal,
  }).finally(() => {
    if (inflight.get(key) === promise) inflight.delete(key);
    controllers.delete(controller);
  });
  inflight.set(key, promise);
  return promise;
};
