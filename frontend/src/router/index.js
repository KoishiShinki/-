import { createRouter, createWebHistory } from "vue-router";
import { cancelReads, getToken, setSession, http } from "@/api/http";
const routes = [
  {
    path: "/",
    component: () => import("@/views/HomeView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/explore",
    component: () => import("@/views/ExploreView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/explore/worldline/:id",
    component: () => import("@/views/PublicWorldlineView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/read/chapter/:id",
    component: () => import("@/views/ChapterReadView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/gallery/:worldlineId",
    component: () => import("@/views/PublicWorldlineView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/authors/:id",
    component: () => import("@/views/CreatorProfileView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/login",
    component: () => import("@/views/LoginView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/register",
    component: () => import("@/views/RegisterView.vue"),
    meta: { auth: false, admin: false },
  },
  {
    path: "/creator",
    component: () => import("@/views/CreatorDashboardView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldlines",
    component: () => import("@/views/CreatorWorldlinesView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/create",
    component: () => import("@/views/WorldlineFormView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id",
    component: () => import("@/views/CreatorWorldlineView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/upload",
    component: () => import("@/views/ScreenshotUploadView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/timeline",
    component: () => import("@/views/TimelineEditView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/stages",
    component: () => import("@/views/StageManageView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/chapters",
    component: () => import("@/views/ChapterManageView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/illustrations",
    component: () => import("@/views/IllustrationManageView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/chat",
    component: () => import("@/views/ChatView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/profile",
    component: () => import("@/views/ProfileView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/tasks",
    component: () => import("@/views/TaskView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/nations",
    component: () => import("@/views/ArchiveEditorView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/characters",
    component: () => import("@/views/ArchiveEditorView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/relations",
    component: () => import("@/views/RelationView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/creator/worldline/:id/settings",
    component: () => import("@/views/WorldlineSettingsView.vue"),
    meta: { auth: true, admin: false },
  },
  {
    path: "/admin/:section?",
    component: () => import("@/views/AdminView.vue"),
    meta: { auth: true, admin: true },
  },
  {
    path: "/:pathMatch(.*)*",
    component: () => import("@/views/NotFoundView.vue"),
  },
];
const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});
router.beforeEach(async (to) => {
  cancelReads();
  if (to.meta.auth && !getToken())
    return { path: "/login", query: { redirect: to.fullPath } };
  if (to.meta.admin) {
    try {
      const response = await http.get("/app/auth/profile");
      const user = response.user || response.data?.user;
      setSession(getToken(), user);
      if (!user?.roles?.includes("ADMIN")) return "/creator";
    } catch {
      return "/login";
    }
  }
});
export default router;
