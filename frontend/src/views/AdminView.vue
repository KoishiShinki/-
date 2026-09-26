<template>
  <section class="page section">
    <div class="toolbar">
      <div>
        <h1 class="serif">内容管理</h1>
        <p class="muted">管理世界线创作业务、内容公开状态与 AI 任务。</p>
      </div>
      <RouterLink class="btn" to="/creator">返回创作工作台</RouterLink>
    </div>
    <div class="admin-layout">
      <aside>
        <nav class="admin-nav" aria-label="管理功能">
          <RouterLink
            v-for="[key, label] in navigation"
            :key="key"
            :to="`/admin/${key}`"
            >{{ label }}</RouterLink
          >
        </nav>
      </aside>
      <div>
        <template v-if="section === 'analysis'"
          ><h2>世界线分析</h2>
          <form class="filter-row" @submit.prevent="loadAnalysis">
            <label class="field"
              ><span>世界线编号</span
              ><input
                class="input"
                type="number"
                min="1"
                v-model.number="worldlineId"
                required /></label
            ><button class="btn primary">查看分析</button>
          </form>
          <template v-if="analysis"
            ><div class="paper-panel upload-simple">
              <h3>历史偏离度：{{ analysis.divergenceScore || 0 }}</h3>
              <p>
                事件节点 {{ analysis.nodes?.length || 0 }} 个；事件关系
                {{ analysis.edges?.length || 0 }} 条。
              </p>
            </div>
            <article
              v-for="node in analysis.nodes || []"
              :key="node.id"
              class="record"
            >
              <span class="record-id"
                >{{ node.year }} 年 / 编号 {{ node.id }}</span
              >
              <h3>{{ node.name }}</h3>
              <span v-if="node.divergence" class="badge">历史分歧点</span>
            </article>
            <h3>事件关系</h3>
            <div
              v-for="(edge, index) in analysis.edges || []"
              :key="index"
              class="relation-item"
            >
              <strong>{{ nodeName(edge.source) }}</strong
              ><span>⟶</span><strong>{{ nodeName(edge.target) }}</strong>
              <p>{{ edge.description || edge.type }}</p>
            </div></template
          ></template
        ><template v-else-if="section === 'aiConfig'"
          ><h2>AI 服务配置</h2>
          <p class="muted">此处仅显示运行状态。服务凭据由部署环境配置。</p>
          <div class="table-scroll">
            <table class="data-table">
              <tbody>
                <tr v-for="(value, key) in config" :key="key">
                  <th>{{ key }}</th>
                  <td>{{ value }}</td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="record-actions">
            <ActionButton label="检查服务配置" :action="testConfig" /><button
              class="btn"
              @click="load"
            >
              刷新状态
            </button>
          </div>
          <p v-if="configResult" class="paper-panel upload-simple">
            {{ configResult }}
          </p></template
        ><template v-else
          ><div class="archive-toolbar">
            <h2>{{ section === "audit" ? "内容审核" : schema.title }}</h2>
            <button
              v-if="canCreate"
              class="btn primary"
              @click="editing = { worldlineId: worldlineId || null }"
            >
              ＋ 新增{{ schema.title }}
            </button>
          </div>
          <form
            class="filter-row"
            @submit.prevent="
              page = 1;
              load();
            "
          >
            <label v-if="section === 'audit'" class="field"
              ><span>审核内容</span
              ><select
                class="input"
                v-model="auditType"
                @change="
                  page = 1;
                  load();
                "
              >
                <option value="worldline">世界线</option>
                <option value="chapter">章节</option>
                <option value="illustration">插图</option>
              </select></label
            ><label v-if="resource !== 'worldline'" class="field"
              ><span>世界线编号</span
              ><input
                class="input"
                type="number"
                min="1"
                v-model.number="worldlineId"
                :required="requiresWorldline"
                :placeholder="
                  requiresWorldline ? '请输入世界线编号' : '全部世界线'
                " /></label
            ><label v-if="resource === 'worldline'" class="field"
              ><span>世界线名称</span
              ><input
                class="input"
                v-model="searchName"
                placeholder="按名称搜索" /></label
            ><button class="btn" :disabled="loading">筛选</button>
          </form>
          <SchemaForm
            v-if="editing"
            :fields="editFields"
            :initial="editing"
            :title="
              editing[schema.id] ? '编辑' + schema.title : '新增' + schema.title
            "
            :save="save"
            closable
            @close="editing = null"
            @saved="
              editing = null;
              load();
            "
          />
          <div v-if="detail" class="paper-panel upload-simple">
            <div class="toolbar">
              <h3>{{ detail[schema.name] || schema.title }}详情</h3>
              <button class="btn" @click="detail = null">关闭详情</button>
            </div>
            <img
              v-if="detail.imageUrl || detail.coverUrl || detail.portraitUrl"
              :src="
                mediaOrFallback(
                  detail.imageUrl || detail.coverUrl || detail.portraitUrl,
                )
              "
              class="record-image"
              :alt="detail[schema.name] || schema.title"
            />
            <dl class="detail-list">
              <template v-for="(value, key) in detail" :key="key"
                ><dt>{{ fieldLabel(key) }}</dt>
                <dd>{{ value }}</dd></template
              >
            </dl>
          </div>
          <div class="table-scroll">
            <table class="data-table">
              <thead>
                <tr>
                  <th>编号</th>
                  <th>{{ schema.title }}名称</th>
                  <th>世界线</th>
                  <th>状态 / 时间</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in records" :key="item[schema.id]">
                  <td>{{ item[schema.id] }}</td>
                  <td>
                    <div class="text-preview">
                      {{ item[schema.name] || "未命名" }}
                    </div>
                  </td>
                  <td>{{ item.worldlineId }}</td>
                  <td>
                    {{ statusText(item) }}<br /><small>{{
                      item.updateTime || item.createTime
                    }}</small>
                  </td>
                  <td>
                    <div class="record-actions">
                      <button class="btn" @click="showDetail(item)">详情</button
                      ><template v-if="section === 'audit'"
                        ><ActionButton
                          label="通过公开"
                          :action="() => audit(item, true)"
                          @done="load" /><ActionButton
                          label="拒绝 / 下架"
                          danger
                          :action="() => audit(item, false)"
                          @done="load" /></template
                      ><template v-else
                        ><button
                          v-if="schema.fields.length"
                          class="btn"
                          @click="
                            editing = { ...item };
                            detail = null;
                          "
                        >
                          编辑</button
                        ><template v-if="resource === 'worldline'"
                          ><ActionButton
                            :label="
                              item.visibility === '1' ? '设为私人' : '公开'
                            "
                            :action="
                              () =>
                                http.post(
                                  '/timeline/worldline/auditVisibility',
                                  {
                                    worldlineId: item.worldlineId,
                                    visibility:
                                      item.visibility === '1' ? '0' : '1',
                                  },
                                )
                            "
                            @done="load" /><ActionButton
                            label="统计"
                            :action="() => loadStat(item)" /></template
                        ><template v-if="resource === 'event'"
                          ><ActionButton
                            label="生成片段"
                            :action="
                              () =>
                                http.post(
                                  `/timeline/event/fragment/${item.eventId}`,
                                )
                            "
                            @done="load" /><ActionButton
                            label="标记分歧"
                            :action="
                              () =>
                                http.post(
                                  `/timeline/event/divergence/${item.eventId}`,
                                )
                            "
                            @done="load" /></template
                        ><ActionButton
                          v-if="resource === 'chapter'"
                          :label="item.status === '3' ? '取消公开' : '公开章节'"
                          :action="
                            () =>
                              http.post('/timeline/chapter/public', {
                                chapterId: item.chapterId,
                                publish: item.status !== '3',
                              })
                          "
                          @done="load" /><template
                          v-if="resource === 'illustration'"
                          ><ActionButton
                            label="采用"
                            :disabled="!item.imageUrl"
                            :action="
                              () =>
                                http.post(
                                  `/timeline/illustration/adopt/${item.illustrationId}`,
                                )
                            "
                            @done="load" /><ActionButton
                            label="弃用"
                            :action="
                              () =>
                                http.post(
                                  `/timeline/illustration/discard/${item.illustrationId}`,
                                )
                            "
                            @done="load" /></template
                        ><ActionButton
                          v-if="
                            resource === 'aiTask' &&
                            String(item.taskStatus) === '3'
                          "
                          label="重试"
                          :action="
                            () =>
                              http.post(`/timeline/aiTask/retry/${item.taskId}`)
                          "
                          @done="load" /><ActionButton
                          label="删除"
                          danger
                          :confirm="`确定删除“${item[schema.name] || item[schema.id]}”？`"
                          :action="
                            () =>
                              http.delete(
                                `/timeline/${resource}/${item[schema.id]}`,
                              )
                          "
                          @done="load"
                      /></template>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-if="!loading && !records.length" class="empty">
            {{
              requiresWorldline && !worldlineId
                ? "请先输入世界线编号，查看其中的档案。"
                : "没有符合条件的记录。"
            }}
          </div>
          <nav class="pagination" aria-label="列表分页">
            <button
              class="btn"
              :disabled="page <= 1 || loading"
              @click="
                page--;
                load();
              "
            >
              上一页</button
            ><span>第 {{ page }} 页 / 共 {{ total }} 条</span
            ><button
              class="btn"
              :disabled="page * 20 >= total || loading"
              @click="
                page++;
                load();
              "
            >
              下一页
            </button>
          </nav></template
        >
        <p v-if="loading" role="status">正在读取管理数据…</p>
        <p v-if="error" class="error" role="alert">{{ error }}</p>
      </div>
    </div>
  </section>
</template>
<script setup>
import { computed, onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import { http } from "@/api/http";
import { schemas } from "@/schemas";
import { mediaOrFallback } from "@/utils/media";
import SchemaForm from "@/components/SchemaForm.vue";
import ActionButton from "@/components/ActionButton.vue";
const route = useRoute(),
  section = computed(() => route.params.section || "worldline"),
  auditType = ref("worldline"),
  resource = computed(() =>
    section.value === "audit" ? auditType.value : section.value,
  ),
  schema = computed(() => schemas[resource.value] || schemas.worldline);
const navigation = [
  ["worldline", "世界线管理"],
  ["screenshot", "截图记录"],
  ["event", "事件管理"],
  ["stage", "阶段管理"],
  ["nation", "国家档案"],
  ["character", "人物档案"],
  ["chapter", "章节管理"],
  ["illustration", "插图管理"],
  ["audit", "公开内容审核"],
  ["analysis", "事件关系分析"],
  ["aiTask", "AI 任务"],
  ["aiConfig", "AI 服务状态"],
];
const records = ref([]),
  total = ref(0),
  page = ref(1),
  worldlineId = ref(null),
  searchName = ref(""),
  editing = ref(null),
  detail = ref(null),
  loading = ref(false),
  error = ref(""),
  analysis = ref(null),
  config = ref({}),
  configResult = ref("");
const requiresWorldline = computed(() =>
  ["stage", "nation", "character"].includes(resource.value),
);
const canCreate = computed(
  () => !["worldline", "screenshot", "aiTask", "audit"].includes(section.value),
);
const editFields = computed(() => [
  { key: "worldlineId", label: "世界线编号", type: "number", required: true },
  ...schema.value.fields.filter((f) => f.key !== "worldlineId"),
]);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    if (requiresWorldline.value && !worldlineId.value) {
      records.value = [];
      total.value = 0;
      return;
    }
    if (section.value === "analysis") return;
    if (section.value === "aiConfig") {
      const r = await http.get("/timeline/aiConfig/list");
      config.value = r.data || {};
      return;
    }
    const r = await http.get(
      section.value === "audit"
        ? `/timeline/audit/${auditType.value}/list`
        : `/timeline/${resource.value}/list`,
      {
        params: {
          pageNum: page.value,
          pageSize: 20,
          ...(worldlineId.value ? { worldlineId: worldlineId.value } : {}),
          ...(searchName.value ? { worldlineName: searchName.value } : {}),
        },
      },
    );
    records.value = r.rows || r.data || [];
    total.value = r.total ?? records.value.length;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
async function save(value) {
  await http[value[schema.value.id] ? "put" : "post"](
    "/timeline/" + resource.value,
    value,
  );
}
async function showDetail(item) {
  try {
    const r = await http.get(
      `/timeline/${resource.value}/${item[schema.value.id]}`,
    );
    detail.value = r.data || item;
    editing.value = null;
  } catch (e) {
    error.value = e.message;
  }
}
async function audit(item, pass) {
  await http.post("/timeline/audit/" + (pass ? "pass" : "reject"), {
    targetType: auditType.value,
    targetId: item[schema.value.id],
  });
}
async function loadAnalysis() {
  loading.value = true;
  error.value = "";
  try {
    const r = await http.get("/timeline/analysis/" + worldlineId.value);
    analysis.value = r.data;
  } catch (e) {
    error.value = e.message;
  } finally {
    loading.value = false;
  }
}
function nodeName(id) {
  return (
    analysis.value?.nodes?.find((n) => Number(n.id) === Number(id))?.name || id
  );
}
async function testConfig() {
  const r = await http.post("/timeline/aiConfig/test");
  configResult.value =
    typeof r.data === "string" ? r.data : JSON.stringify(r.data || r.msg);
}
async function loadStat(item) {
  const r = await http.get("/timeline/worldline/stat/" + item.worldlineId);
  detail.value = { worldlineName: item.worldlineName, ...r.data };
}
function statusText(item) {
  if (resource.value === "worldline")
    return item.visibility === "1" ? "公开" : "私人";
  if (resource.value === "chapter")
    return (
      { 0: "草稿", 1: "生成中", 2: "已生成", 3: "已公开" }[item.status] ||
      item.status
    );
  if (resource.value === "screenshot")
    return (
      {
        0: "待识别",
        1: "识别中",
        2: "待审核",
        3: "已确认",
        4: "已驳回",
        5: "失败",
      }[item.aiStatus] || item.aiStatus
    );
  if (resource.value === "illustration")
    return (
      {
        0: "待生成",
        1: "提示词就绪",
        2: "待采用",
        3: "生成失败",
        4: "已采用",
        5: "已弃用",
      }[item.generateStatus] || item.generateStatus
    );
  if (resource.value === "aiTask")
    return (
      { 0: "等待中", 1: "处理中", 2: "已完成", 3: "失败" }[item.taskStatus] ||
      item.taskStatus
    );
  return item.recordYear || item.eventYear || item.startYear || "—";
}
function fieldLabel(key) {
  return (
    schema.value.fields.find((f) => f.key === key)?.label ||
    {
      eventCount: "事件数量",
      chapterCount: "章节数量",
      stageCount: "阶段数量",
      illustrationCount: "插图数量",
      divergenceScore: "历史偏离度",
      worldlineId: "世界线编号",
      createTime: "创建时间",
      updateTime: "更新时间",
      createBy: "创建者",
    }[key] ||
    key
  );
}
onMounted(load);
</script>
<style scoped>
.detail-list {
  display: grid;
  grid-template-columns: 150px minmax(0, 1fr);
  gap: 10px;
  font-size: 14px;
}
.detail-list dt {
  color: var(--muted);
}
.detail-list dd {
  margin: 0;
  white-space: pre-wrap;
  overflow-wrap: anywhere;
}
.record-actions {
  margin: 0;
}
.record-actions .btn {
  min-height: 32px;
  font-size: 12px;
  padding: 0 9px;
}
@media (max-width: 600px) {
  .detail-list {
    grid-template-columns: 1fr;
  }
  .detail-list dt {
    font-weight: bold;
  }
}
</style>
