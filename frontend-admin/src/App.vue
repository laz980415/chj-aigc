<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";

type SummaryPayload = {
  policies: number;
  auditEvents: number;
  users: number;
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

type BrandPayload = {
  id: string;
  clientId: string;
  name: string;
  summary: string;
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

type SessionPayload = {
  token: string;
  username: string;
  displayName: string;
  roleKey: string;
  tenantId: string | null;
  expiresAt: string;
};

type UserPayload = {
  id: string;
  username: string;
  displayName: string;
  roleKey: string;
  tenantId: string | null;
  active: boolean;
};

const loading = ref(true);
const error = ref("");
const success = ref("");
const isAuthenticated = ref(false);

const healthStatus = ref("加载中");
const dbInfo = ref<DbInfoPayload | null>(null);
const summary = ref<SummaryPayload>({ policies: 0, auditEvents: 0, users: 0 });
const rules = ref<RulePayload[]>([]);
const wallet = ref<WalletPayload>({ tenantId: "tenant-demo", balance: "0", ledgerCount: 0 });
const quotas = ref<QuotaPayload>({ projectImageRemaining: "0", userTokenRemaining: "0" });
const clients = ref<ClientPayload[]>([]);
const brands = ref<BrandPayload[]>([]);
const assets = ref<AssetPayload[]>([]);
const session = ref<SessionPayload | null>(null);
const users = ref<UserPayload[]>([]);
const roles = ref<string[]>([]);

const loginForm = reactive({
  username: "admin",
  password: "Admin@123",
});

const ruleForm = reactive({
  ruleId: "rule-ui-1",
  actorId: "super-admin",
  platformModelAlias: "image-standard",
  scopeType: "tenant",
  scopeValue: "tenant-demo",
  effect: "allow",
  reason: "Created from Vue admin console",
});

const userForm = reactive({
  userId: "user-ui-1",
  username: "new_user",
  password: "User@123",
  displayName: "新账号",
  roleKey: "tenant_member",
  tenantId: "tenant-demo",
});

const rechargeForm = reactive({
  entryId: "recharge-ui-1",
  amount: "100.00",
  description: "manual recharge",
  referenceId: "dashboard",
});

const clientForm = reactive({
  clientId: "client-ui-1",
  name: "新广告主",
});

const brandForm = reactive({
  brandId: "brand-ui-1",
  clientId: "client-demo",
  name: "新品牌",
  summary: "品牌简介",
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

const currentRole = computed(() => session.value?.roleKey ?? "");
const isPlatformAdmin = computed(() => currentRole.value === "platform_super_admin");
const canViewTenantWorkspace = computed(() => ["tenant_owner", "tenant_member"].includes(currentRole.value));
const canManageTenantWorkspace = computed(() => currentRole.value === "tenant_owner");
const canRechargeTenant = computed(() => ["platform_super_admin", "tenant_owner"].includes(currentRole.value));
const workspaceTitle = computed(() => isPlatformAdmin.value ? "平台后台" : "租户后台");
const workspaceCopy = computed(() => isPlatformAdmin.value
  ? "平台侧只管理租户开通、租户充值、模型权限和租户级别运营配置。"
  : "租户侧负责项目、成员、客户、品牌和素材等日常运营动作。");

async function fetchJson<T>(url: string, init?: RequestInit): Promise<T> {
  const token = localStorage.getItem("chj_aigc_token");
  const headers = new Headers(init?.headers ?? {});
  if (token) {
    headers.set("X-Auth-Token", token);
  }
  const response = await fetch(url, {
    ...init,
    headers,
  });
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json() as Promise<T>;
}

async function loadDashboard() {
  loading.value = true;
  error.value = "";
  summary.value = { policies: 0, auditEvents: 0, users: 0 };
  roles.value = [];
  users.value = [];
  rules.value = [];
  wallet.value = { tenantId: "tenant-demo", balance: "0", ledgerCount: 0 };
  quotas.value = { projectImageRemaining: "0", userTokenRemaining: "0" };
  clients.value = [];
  brands.value = [];
  assets.value = [];

  const [health, dbPayload] = await Promise.all([
    fetchJson<{ status: string }>("/api/health"),
    fetchJson<DbInfoPayload>("/api/db-info"),
  ]);

  healthStatus.value = health.status;
  dbInfo.value = dbPayload;

  if (isPlatformAdmin.value) {
    const [summaryPayload, rolePayload, userPayload, rulePayload, walletPayload] = await Promise.all([
      fetchJson<SummaryPayload>("/api/admin/summary"),
      fetchJson<string[]>("/api/admin/roles"),
      fetchJson<UserPayload[]>("/api/admin/users"),
      fetchJson<RulePayload[]>("/api/admin/model-access-rules"),
      fetchJson<WalletPayload>("/api/tenant/wallet"),
    ]);
    summary.value = summaryPayload;
    roles.value = rolePayload;
    users.value = userPayload;
    rules.value = rulePayload;
    wallet.value = walletPayload;
  }

  if (canViewTenantWorkspace.value) {
    const [walletPayload, quotaPayload, clientPayload, brandPayload, assetPayload] = await Promise.all([
      fetchJson<WalletPayload>("/api/tenant/wallet"),
      fetchJson<QuotaPayload>("/api/tenant/quotas"),
      fetchJson<ClientPayload[]>("/api/tenant/clients"),
      fetchJson<BrandPayload[]>("/api/tenant/brands"),
      fetchJson<AssetPayload[]>("/api/tenant/assets"),
    ]);
    wallet.value = walletPayload;
    quotas.value = quotaPayload;
    clients.value = clientPayload;
    brands.value = brandPayload;
    assets.value = assetPayload;
  }

  loading.value = false;
}

async function createUser() {
  success.value = "";
  error.value = "";
  await fetchJson<UserPayload>("/api/admin/users", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(userForm),
  });
  success.value = `账号 ${userForm.username} 已创建`;
  await loadDashboard();
}

async function login() {
  loading.value = true;
  error.value = "";
  success.value = "";

  const response = await fetch("/api/auth/login", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(loginForm),
  });
  if (!response.ok) {
    throw new Error("登录失败，请检查账号密码");
  }

  const payload = await response.json() as SessionPayload;
  localStorage.setItem("chj_aigc_token", payload.token);
  session.value = payload;
  isAuthenticated.value = true;
  success.value = `欢迎，${payload.displayName}`;
  await loadDashboard();
}

async function bootstrap() {
  const token = localStorage.getItem("chj_aigc_token");
  if (!token) {
    loading.value = false;
    isAuthenticated.value = false;
    return;
  }

  try {
    const me = await fetchJson<Omit<SessionPayload, "token">>("/api/auth/me");
    session.value = {
      ...me,
      token,
    };
    isAuthenticated.value = true;
    await loadDashboard();
  } catch {
    localStorage.removeItem("chj_aigc_token");
    isAuthenticated.value = false;
    loading.value = false;
  }
}

function logout() {
  localStorage.removeItem("chj_aigc_token");
  session.value = null;
  isAuthenticated.value = false;
  success.value = "";
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

async function createClient() {
  success.value = "";
  error.value = "";
  await fetchJson<ClientPayload>("/api/tenant/clients", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(clientForm),
  });
  success.value = `客户 ${clientForm.name} 已创建`;
  brandForm.clientId = clientForm.clientId;
  await loadDashboard();
}

async function createBrand() {
  success.value = "";
  error.value = "";
  await fetchJson<BrandPayload>("/api/tenant/brands", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(brandForm),
  });
  success.value = `品牌 ${brandForm.name} 已创建`;
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
  runAction(bootstrap);
});
</script>

<template>
  <div class="page-shell">
    <section v-if="!isAuthenticated" class="login-panel">
      <div class="login-card">
        <p class="eyebrow">CHJ AIGC</p>
        <h1>后台登录</h1>
        <p class="hero-copy">先完成登录，再进入超管与租户后台。</p>
        <form class="form-stack" @submit.prevent="runAction(login)">
          <input v-model="loginForm.username" placeholder="用户名" required>
          <input v-model="loginForm.password" type="password" placeholder="密码" required>
          <button class="action-button full-button" type="submit">登录系统</button>
        </form>
        <div class="login-tip">
          演示账号：admin / Admin@123，tenant_owner / Tenant@123
        </div>
      </div>
    </section>

    <template v-else>
    <header class="hero">
      <div class="hero-copy-block">
        <p class="eyebrow">CHJ AIGC</p>
        <h1>{{ workspaceTitle }}</h1>
        <p class="hero-copy">
          {{ workspaceCopy }}
        </p>
      </div>
      <div class="hero-status">
        <div class="status-card">
          <span>当前账号</span>
          <strong>{{ session?.displayName }}</strong>
        </div>
        <div class="status-card">
          <span>角色</span>
          <strong>{{ session?.roleKey }}</strong>
        </div>
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
            <p class="panel-label">{{ isPlatformAdmin ? "平台总览" : "租户总览" }}</p>
            <h2>{{ isPlatformAdmin ? "租户运营概览" : "租户工作台概览" }}</h2>
          </div>
          <div class="panel-actions">
            <button class="action-button" type="button" @click="runAction(loadDashboard)">
              刷新
            </button>
            <button class="action-button secondary-button" type="button" @click="logout">
              退出登录
            </button>
          </div>
        </div>
        <div class="stats-grid">
          <article v-if="isPlatformAdmin" class="stat-card">
            <span>策略数</span>
            <strong>{{ summary.policies }}</strong>
          </article>
          <article v-if="isPlatformAdmin" class="stat-card">
            <span>账号数</span>
            <strong>{{ summary.users }}</strong>
          </article>
          <article v-if="isPlatformAdmin" class="stat-card">
            <span>审计事件</span>
            <strong>{{ summary.auditEvents }}</strong>
          </article>
          <article class="stat-card">
            <span>钱包余额</span>
            <strong>{{ wallet.balance }}</strong>
          </article>
          <article v-if="canViewTenantWorkspace" class="stat-card">
            <span>成员 Token 剩余</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </article>
          <article v-if="canViewTenantWorkspace" class="stat-card">
            <span>客户数</span>
            <strong>{{ clients.length }}</strong>
          </article>
          <article v-if="canViewTenantWorkspace" class="stat-card">
            <span>品牌数</span>
            <strong>{{ brands.length }}</strong>
          </article>
        </div>
      </section>

      <section v-if="isPlatformAdmin" class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">账号管理</p>
            <h2>用户与角色</h2>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="runAction(createUser)">
          <div class="form-grid">
            <input v-model="userForm.userId" placeholder="用户 ID" required>
            <input v-model="userForm.username" placeholder="登录名" required>
            <input v-model="userForm.displayName" placeholder="显示名" required>
            <input v-model="userForm.password" placeholder="初始密码" required>
            <select v-model="userForm.roleKey">
              <option v-for="role in roles" :key="role" :value="role">{{ role }}</option>
            </select>
            <input v-model="userForm.tenantId" placeholder="租户 ID，可为空">
          </div>
          <button class="action-button" type="submit">创建账号</button>
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>账号</th>
                <th>显示名</th>
                <th>角色</th>
                <th>租户</th>
                <th>状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName }}</td>
                <td>{{ user.roleKey }}</td>
                <td>{{ user.tenantId ?? "-" }}</td>
                <td>{{ user.active ? "启用" : "停用" }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="isPlatformAdmin" class="panel">
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
            <p class="panel-label">{{ isPlatformAdmin ? "平台侧租户运营" : "租户资金" }}</p>
            <h2>{{ isPlatformAdmin ? "租户充值" : "钱包与额度" }}</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form v-if="canRechargeTenant" class="form-stack" @submit.prevent="runAction(rechargeWallet)">
            <h3>钱包充值</h3>
            <input v-model="rechargeForm.entryId" placeholder="流水 ID" required>
            <input v-model="rechargeForm.amount" placeholder="充值金额" required>
            <input v-model="rechargeForm.description" placeholder="说明" required>
            <input v-model="rechargeForm.referenceId" placeholder="引用 ID" required>
            <button class="action-button" type="submit">充值</button>
          </form>

          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(saveQuota)">
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
            <span>{{ isPlatformAdmin ? "充值流水数" : "项目图片额度" }}</span>
            <strong>{{ isPlatformAdmin ? wallet.ledgerCount : quotas.projectImageRemaining }}</strong>
          </div>
          <div v-if="canViewTenantWorkspace" class="mini-stat">
            <span>成员 Token 额度</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </div>
        </div>
      </section>

      <section v-if="canViewTenantWorkspace" class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">租户品牌资产</p>
            <h2>客户与素材</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(createClient)">
            <h3>创建客户</h3>
            <input v-model="clientForm.clientId" placeholder="客户 ID" required>
            <input v-model="clientForm.name" placeholder="客户名称" required>
            <button class="action-button" type="submit">创建客户</button>
          </form>

          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(createBrand)">
            <h3>创建品牌</h3>
            <input v-model="brandForm.brandId" placeholder="品牌 ID" required>
            <select v-model="brandForm.clientId">
              <option v-for="client in clients" :key="client.id" :value="client.id">{{ client.name }}</option>
            </select>
            <input v-model="brandForm.name" placeholder="品牌名称" required>
            <input v-model="brandForm.summary" placeholder="品牌简介" required>
            <button class="action-button" type="submit">创建品牌</button>
          </form>
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
            <h3>品牌列表</h3>
            <ul class="card-list">
              <li v-for="brand in brands" :key="brand.id">
                <strong>{{ brand.name }}</strong>
                <div class="subtext">{{ brand.clientId }} · {{ brand.summary }}</div>
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
    </template>
  </div>
</template>
