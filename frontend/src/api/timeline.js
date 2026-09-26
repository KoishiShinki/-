import { http } from "./http";

export const publicApi = {
  worldlines: (params) => http.get("/public/worldline/list", { params }),
  worldline: (id) => http.get(`/public/worldline/${id}`),
  timeline: (id) => http.get(`/public/worldline/timeline/${id}`),
  chapters: (id) => http.get(`/public/chapter/list/${id}`),
  chapter: (id) => http.get(`/public/chapter/${id}`),
  illustrations: (id) => http.get(`/public/illustration/list/${id}`),
  creator: (id) => http.get(`/public/creator/${id}`),
};

export const authApi = {
  login: (data) => http.post("/app/auth/login", data),
  register: (data) => http.post("/app/auth/register", data),
  profile: () => http.get("/app/auth/profile"),
  updateProfile: (data) => http.put("/app/auth/profile", data),
};

export const appApi = {
  myWorldlines: () => http.get("/app/worldline/myList"),
  worldline: (id) => http.get(`/app/worldline/${id}`),
  createWorldline: (data) => http.post("/app/worldline", data),
  updateWorldline: (data) => http.put("/app/worldline", data),
  deleteWorldline: (id) => http.delete(`/app/worldline/${id}`),
  setVisibility: (data) => http.post("/app/worldline/setVisibility", data),
  stat: (id) => http.get(`/app/worldline/stat/${id}`),
  uploadCover: (formData) =>
    http.post("/app/worldline/uploadCover", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  screenshots: (id) => http.get(`/app/screenshot/list/${id}`),
  uploadScreenshot: (formData) =>
    http.post("/app/screenshot/upload", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  recognizeScreenshot: (id) => http.post(`/app/screenshot/recognize/${id}`),
  confirmDraft: (screenshotId) =>
    http.post("/app/screenshot/confirmDraft", { screenshotId }),
  rejectDraft: (screenshotId) =>
    http.post("/app/screenshot/rejectDraft", { screenshotId }),
  events: (id) => http.get(`/app/event/list/${id}`),
  createEvent: (data) => http.post("/app/event", data),
  updateEvent: (data) => http.put("/app/event", data),
  deleteEvent: (id) => http.delete(`/app/event/${id}`),
  generateFragment: (id) => http.post(`/app/event/generateFragment/${id}`),
  markDivergence: (id) => http.post(`/app/event/markDivergence/${id}`),
  stages: (id) => http.get(`/app/stage/list/${id}`),
  createStage: (data) => http.post("/app/stage", data),
  updateStage: (data) => http.put("/app/stage", data),
  deleteStage: (id) => http.delete(`/app/stage/${id}`),
  autoSplitStages: (id) => http.post(`/app/stage/autoSplit/${id}`),
  bindEvents: (data) => http.post("/app/stage/bindEvents", data),
  generateStageNovel: (id, data = {}) =>
    http.post(`/app/stage/generateNovel/${id}`, data),
  chapters: (id) => http.get(`/app/chapter/list/${id}`),
  chapter: (id) => http.get(`/app/chapter/${id}`),
  createChapter: (data) => http.post("/app/chapter", data),
  updateChapter: (data) => http.put("/app/chapter", data),
  deleteChapter: (id) => http.delete(`/app/chapter/${id}`),
  setChapterPublic: (data) => http.post("/app/chapter/setPublic", data),
  exportMarkdown: (id) => http.post(`/app/chapter/exportMarkdown/${id}`),
  exportWord: (id) =>
    http.post(`/app/chapter/exportWord/${id}`, null, { responseType: "blob" }),
  illustrations: (id) => http.get(`/app/illustration/list/${id}`),
  generateIllustrationPrompt: (data) =>
    http.post("/app/illustration/generatePrompt", data),
  generateIllustrationImage: (data) =>
    http.post("/app/illustration/generateImage", data),
  adoptIllustration: (id) => http.post(`/app/illustration/adopt/${id}`),
  discardIllustration: (id) => http.post(`/app/illustration/discard/${id}`),
  setChapterCover: (data) =>
    http.post("/app/illustration/setChapterCover", data),
  characters: (id) => http.get(`/app/character/list/${id}`),
  uploadCharacterPortrait: (formData) =>
    http.post("/app/character/uploadPortrait", formData, {
      headers: { "Content-Type": "multipart/form-data" },
    }),
  ask: (data) => http.post("/app/chat/ask", data),
  chatHistory: (id) => http.get(`/app/chat/history/${id}`),
  aiTasks: () => http.get("/app/aiTask/recent"),
};
