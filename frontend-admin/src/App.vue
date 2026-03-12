<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

type SummaryPayload = {
  policies: number;
  auditEvents: number;
};

type RulePayload = {
  id: string;
  platformModelAlias: string;
  scope: {
    type: string;
    value: string;
  };
  effect: string;
  createdBy: string;
};

type WalletPayload = {
  tenantId: string;
  balance: string;
  ledgerCount: number;
};

type QuotaPayload = {
  projectImageRemaining: string;
  userTokenRemaining: string;
};

type ClientPayload = {
  id: string;
  name: string;
  active: boolean;
};

type AssetPayload = {
  id: string;
  name: string;
  kind: string;
  storageUri: string;
  tags: string[];
};

type DbInfoPayload = {
  url: string;
  username: string;
  passwordConfigured: boolean;
};

const loading = ref(true);
const error = ref("");
const success = ref("");

const healthStatus = ref("加载中");
const dbInfo = ref<DbInfoPayload | null>(null);
const summary = ref<SummaryPayload>({ policies: 0, auditEvents: 0 });
const rules = ref<RulePayload[]>([]);
const wallet = ref<WalletPayload>({ tenantId: "tenant-demo", balance: "0", ledgerCount: 0 });
const quotas = ref<QuotaPayload>({ projectImageRemaining: "0", userTokenRemaining: "0" });
const clients = ref<ClientPayload[]>([]);
const assets = ref<AssetPayload[]>([]);

const ruleForm = reactive({
  ruleId: "rule-ui-1",
  actorId: "super-admin",
  platformModelAlias: "image-standard",
  scopeType: "tenant",
  scopeValue: "tenant-demo",
  effect: "allow",
  reason: "Created from Vue admin console",
});

const rechargeForm = reactive({
  entryId: "recharge-ui-1",
  amount: "100.00",
  description: "manual recharge",
  referenceId: "dashboard",
});

const quotaForm = reactive({
  allocationId: "quota-ui-1",
  scopeType: "project",
  scopeId: "project-demo",
  dimension: "image_count",
  limit: "50",
  used: "0",
});

const dbSummary = computed(() => {
  if (!dbInfo.value) {
    return "待检测";
  }
  return dbInfo.value.passwordConfigured ? "已配置" : "缺少密码";
});

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, init);
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

async function loadDashboard() {
  loading.value = true;
  error.value = "";
  const [
    health,
    dbPayload,
    summaryPayload,
    rulePayload,
    walletPayload,
    quotaPayload,
    clientPayload,
    assetPayload,
  ] = await Promise.all([
    fetchJson<{ status: string }>("/api/health"),
    fetchJson<DbInfoPayload>("/api/db-info"),
    fetchJson<SummaryPayload>("/api/admin/summary"),
    fetchJson<RulePayload[]>("/api/admin/model-access-rules"),
    fetchJson<WalletPayload>("/api/tenant/wallet"),
    fetchJson<QuotaPayload>("/api/tenant/quotas"),
    fetchJson<ClientPayload[]>("/api/tenant/clients"),
    fetchJson<AssetPayload[]>("/api/tenant/assets"),
  ]);

  healthStatus.value = health.status;
  dbInfo.value = dbPayload;
  summary.value = summaryPayload;
  rules.value = rulePayload;
  wallet.value = walletPayload;
  quotas.value = quotaPayload;
  clients.value = clientPayload;
  assets.value = assetPayload;
  loading.value = false;
}

async function createRule() {
  success.value = "";
  error.value = "";
  await fetchJson<RulePayload>("/api/admin/model-access-rules", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(ruleForm),
  });
  success.value = `规则 ${ruleForm.ruleId} 已创建`;
  await loadDashboard();
}

async function rechargeWallet() {
  success.value = "";
  error.value = "";
  await fetchJson<WalletPayload>("/api/tenant/wallet/recharge", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(rechargeForm),
  });
  success.value = `钱包已充值 ${rechargeForm.amount}`;
  await loadDashboard();
}

async function saveQuota() {
  success.value = "";
  error.value = "";
  await fetchJson<QuotaPayload>("/api/tenant/quotas", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(quotaForm),
  });
  success.value = `额度 ${quotaForm.allocationId} 已保存`;
  await loadDashboard();
}

async function runAction(action: () => Promise<void>) {
  try {
    await action();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "未知错误";
    loading.value = false;
  }
}

onMounted(() => {
  runAction(loadDashboard);
});
</script>

<template>
  <div class="page-shell">
    <header class="hero">
      <div class="hero-copy-block">
        <p class="eyebrow">CHJ AIGC</p>
        <h1>管理后台</h1>
        <p class="hero-copy">
          当前采用前后端分离架构。Vue 3 负责运营后台界面，Spring Boot 负责
          API 和业务编排。
        </p>
      </div>
      <div class="hero-status">
        <div class="status-card">
          <span>后端服务</span>
          <strong>{{ healthStatus }}</strong>
        </div>
        <div class="status-card">
          <span>数据库</span>
          <strong>{{ dbSummary }}</strong>
        </div>
      </div>
    </header>

    <main class="dashboard-grid">
      <section class="panel panel-wide">
        <div class="panel-head">
          <div>
            <p class="panel-label">总览</p>
            <h2>平台概览</h2>
          </div>
          <button class="action-button" type="button" @click="runAction(loadDashboard)">
            刷新
          </button>
        </div>
        <div class="stats-grid">
          <article class="stat-card">
            <span>策略数</span>
            <strong>{{ summary.policies }}</strong>
          </article>
          <article class="stat-card">
            <span>审计事件</span>
            <strong>{{ summary.auditEvents }}</strong>
          </article>
          <article class="stat-card">
            <span>钱包余额</span>
            <strong>{{ wallet.balance }}</strong>
          </article>
          <article class="stat-card">
            <span>用户 Token 剩余</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </article>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">超管</p>
            <h2>模型访问策略</h2>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="runAction(createRule)">
          <div class="form-grid">
            <input v-model="ruleForm.ruleId" placeholder="规则 ID" required>
            <input v-model="ruleForm.actorId" placeholder="操作人 ID" required>
            <input v-model="ruleForm.platformModelAlias" placeholder="模型别名" required>
            <select v-model="ruleForm.scopeType">
              <option value="tenant">租户</option>
              <option value="project">项目</option>
              <option value="role">角色</option>
            </select>
            <input v-model="ruleForm.scopeValue" placeholder="范围值" required>
            <select v-model="ruleForm.effect">
              <option value="allow">允许</option>
              <option value="deny">拒绝</option>
            </select>
            <input v-model="ruleForm.reason" class="full-span" placeholder="原因说明" required>
          </div>
          <button class="action-button" type="submit">创建规则</button>
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>规则</th>
                <th>模型</th>
                <th>范围</th>
                <th>效果</th>
                <th>操作人</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="rule in rules" :key="rule.id">
                <td>{{ rule.id }}</td>
                <td>{{ rule.platformModelAlias }}</td>
                <td>{{ rule.scope.type }}:{{ rule.scope.value }}</td>
                <td>{{ rule.effect }}</td>
                <td>{{ rule.createdBy }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">租户资金</p>
            <h2>钱包与额度</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form class="form-stack" @submit.prevent="runAction(rechargeWallet)">
            <h3>钱包充值</h3>
            <input v-model="rechargeForm.entryId" placeholder="流水 ID" required>
            <input v-model="rechargeForm.amount" placeholder="充值金额" required>
            <input v-model="rechargeForm.description" placeholder="说明" required>
            <input v-model="rechargeForm.referenceId" placeholder="引用 ID" required>
            <button class="action-button" type="submit">充值</button>
          </form>

          <form class="form-stack" @submit.prevent="runAction(saveQuota)">
            <h3>额度配置</h3>
            <input v-model="quotaForm.allocationId" placeholder="额度 ID" required>
            <select v-model="quotaForm.scopeType">
              <option value="project">项目</option>
              <option value="user">用户</option>
            </select>
            <input v-model="quotaForm.scopeId" placeholder="范围 ID" required>
            <select v-model="quotaForm.dimension">
              <option value="image_count">图片数量</option>
              <option value="tokens">Token</option>
              <option value="video_seconds">视频秒数</option>
            </select>
            <input v-model="quotaForm.limit" placeholder="上限" required>
            <input v-model="quotaForm.used" placeholder="已使用" required>
            <button class="action-button" type="submit">保存额度</button>
          </form>
        </div>
        <div class="quota-strip">
          <div class="mini-stat">
            <span>项目图片额度</span>
            <strong>{{ quotas.projectImageRemaining }}</strong>
          </div>
          <div class="mini-stat">
            <span>用户 Token 额度</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">品牌约束</p>
            <h2>客户与素材</h2>
          </div>
        </div>
        <div class="assets-layout">
          <div>
            <h3>客户列表</h3>
            <ul class="card-list">
              <li v-for="client in clients" :key="client.id">
                <strong>{{ client.name }}</strong>
                <div class="subtext">{{ client.id }}</div>
              </li>
            </ul>
          </div>
          <div>
            <h3>素材列表</h3>
            <ul class="card-list">
              <li v-for="asset in assets" :key="asset.id">
                <strong>{{ asset.name }}</strong>
                <div class="subtext">{{ asset.kind }} · {{ asset.storageUri }}</div>
              </li>
            </ul>
          </div>
        </div>
      </section>
    </main>

    <footer class="footer-bar">
      <span v-if="loading">页面加载中...</span>
      <span v-else-if="error" class="error-text">{{ error }}</span>
      <span v-else-if="success" class="success-text">{{ success }}</span>
      <span v-else>系统已就绪</span>
    </footer>
  </div>
</template>
