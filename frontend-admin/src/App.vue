<script setup lang="ts">
/**
 * 管理后台单页入口。
 * 当前按登录角色动态渲染平台后台和租户后台，并通过左侧主菜单和顶部页签切分页面，避免整屏堆叠成演示页。
 */
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";

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

type QuotaAllocationPayload = {
  id: string;
  scopeType: string;
  scopeId: string;
  dimension: string;
  limit: string;
  used: string;
};

type ProjectPayload = {
  id: string;
  name: string;
  active: boolean;
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

type TenantOverviewPayload = {
  tenantId: string;
  displayName: string;
  memberCount: number;
  ownerCount: number;
  activeMemberCount: number;
  walletBalance: string;
  ledgerCount: number;
};

type LedgerEntryPayload = {
  id: string;
  tenantId: string;
  entryType: string;
  amount: string;
  description: string;
  referenceId: string;
  createdAt: string;
};

type TenantDetailPayload = {
  tenant: TenantOverviewPayload;
  members: UserPayload[];
  rules: RulePayload[];
  ledgerEntries: LedgerEntryPayload[];
  paymentOrders: PaymentOrderPayload[];
};

type PaymentOrderPayload = {
  id: string;
  tenantId: string;
  channel: string;
  status: string;
  amount: string;
  description: string;
  referenceId: string;
  qrCode: string;
  createdAt: string;
  paidAt: string | null;
};

type DbInfoPayload = {
  url: string;
  username: string;
  passwordConfigured: boolean;
};

type SessionPayload = {
  token: string;
  userId: string;
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

type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

type WorkspaceTab = {
  key: string;
  label: string;
};

type WorkspaceMenu = {
  key: string;
  label: string;
  icon: string;
  badge: string;
  tabs: WorkspaceTab[];
};

const loading = ref(true);
const error = ref("");
const success = ref("");
const isAuthenticated = ref(false);
const currentPath = ref(window.location.pathname || "/");
const activeMenuKey = ref("");
const activeTabKey = ref("");

const healthStatus = ref("加载中");
const dbInfo = ref<DbInfoPayload | null>(null);
const summary = ref<SummaryPayload>({ policies: 0, auditEvents: 0, users: 0 });
const rules = ref<RulePayload[]>([]);
const tenants = ref<TenantOverviewPayload[]>([]);
const selectedTenantId = ref("tenant-demo");
const tenantDetail = ref<TenantDetailPayload | null>(null);
const paymentOrders = ref<PaymentOrderPayload[]>([]);
const wallet = ref<WalletPayload>({ tenantId: "tenant-demo", balance: "0", ledgerCount: 0 });
const quotas = ref<QuotaPayload>({ projectImageRemaining: "0", userTokenRemaining: "0" });
const quotaAllocations = ref<QuotaAllocationPayload[]>([]);
const projects = ref<ProjectPayload[]>([]);
const clients = ref<ClientPayload[]>([]);
const brands = ref<BrandPayload[]>([]);
const assets = ref<AssetPayload[]>([]);
const session = ref<SessionPayload | null>(null);
const users = ref<UserPayload[]>([]);
const members = ref<UserPayload[]>([]);
const roles = ref<string[]>([]);
const memberRoleDrafts = reactive<Record<string, string>>({});

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
  reason: "由平台后台创建",
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
  orderId: "pay-ui-1",
  tenantId: "tenant-demo",
  amount: "100.00",
  description: "后台人工充值",
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

const projectForm = reactive({
  projectId: "project-ui-1",
  name: "新投放项目",
});

const memberForm = reactive({
  userId: "tenant-member-ui-1",
  username: "tenant_member_new",
  password: "Member@123",
  displayName: "新租户成员",
  roleKey: "tenant_member",
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
const showLoginView = computed(() => !isAuthenticated.value || currentPath.value === "/login");
const workspaceCopy = computed(() => isPlatformAdmin.value
  ? "平台侧只管理租户开通、租户充值、模型权限和租户级别运营配置。"
  : "租户侧负责项目、成员、客户、品牌和素材等日常运营动作。");
const quotaScopeOptions = computed(() => quotaForm.scopeType === "project"
  ? projects.value.map((project) => ({ value: project.id, label: project.name }))
  : members.value.map((member) => ({ value: member.id, label: `${member.displayName} (${member.username})` })));
const projectNameMap = computed(() => new Map(projects.value.map((project) => [project.id, project.name])));
const memberNameMap = computed(() => new Map(members.value.map((member) => [member.id, `${member.displayName} (${member.username})`])));
const workspaceMenus = computed<WorkspaceMenu[]>(() => isPlatformAdmin.value
  ? [
    {
      key: "platform-dashboard",
      label: "平台总览",
      icon: "📊",
      badge: String(summary.value.users),
      tabs: [
        { key: "overview", label: "运营概览" },
        { key: "tenants", label: "租户总览" },
      ],
    },
    {
      key: "platform-manage",
      label: "平台管理",
      icon: "⚙️",
      badge: String(summary.value.policies),
      tabs: [
        { key: "accounts", label: "账号角色" },
        { key: "policies", label: "模型策略" },
      ],
    },
    {
      key: "platform-finance",
      label: "租户运营",
      icon: "💰",
      badge: wallet.value.balance,
      tabs: [
        { key: "finance", label: "租户充值" },
      ],
    },
  ]
  : [
    {
      key: "tenant-dashboard",
      label: "租户总览",
      icon: "🏠",
      badge: String(projects.value.length),
      tabs: [
        { key: "overview", label: "工作台概览" },
        { key: "finance", label: "钱包额度" },
      ],
    },
    {
      key: "tenant-collaboration",
      label: "项目协作",
      icon: "👥",
      badge: String(members.value.length),
      tabs: [
        { key: "members", label: "项目成员" },
      ],
    },
    {
      key: "tenant-assets",
      label: "品牌资产",
      icon: "🎨",
      badge: String(brands.value.length),
      tabs: [
        { key: "assets", label: "客户素材" },
      ],
    },
  ]);
const activeMenu = computed(() => workspaceMenus.value.find((menu) => menu.key === activeMenuKey.value) ?? workspaceMenus.value[0] ?? null);
const activeTabs = computed(() => activeMenu.value?.tabs ?? []);
const activeTab = computed(() => activeTabs.value.find((tab) => tab.key === activeTabKey.value) ?? activeTabs.value[0] ?? null);
const tenantRuleCount = computed(() => tenantDetail.value?.rules.length ?? 0);

// 统一附带令牌访问 API，失败时直接抛错交给页面底部状态栏展示。
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
    try {
      const payload = await response.json() as ApiResponse<unknown>;
      throw new Error(payload.message || `请求失败：${response.status}`);
    } catch {
      throw new Error(`请求失败：${response.status}`);
    }
  }
  const payload = await response.json() as ApiResponse<T> | T;
  if (payload && typeof payload === "object" && "code" in payload && "data" in payload) {
    const envelope = payload as ApiResponse<T>;
    if (envelope.code !== 0) {
      throw new Error(envelope.message || "请求失败");
    }
    return envelope.data;
  }
  return payload as T;
}

// 按当前登录角色加载对应工作台数据，避免平台侧继续看到租户内部项目明细。
async function loadDashboard() {
  loading.value = true;
  error.value = "";
  summary.value = { policies: 0, auditEvents: 0, users: 0 };
  roles.value = [];
  users.value = [];
  members.value = [];
  rules.value = [];
  tenants.value = [];
  tenantDetail.value = null;
  paymentOrders.value = [];
  wallet.value = { tenantId: "tenant-demo", balance: "0", ledgerCount: 0 };
  quotas.value = { projectImageRemaining: "0", userTokenRemaining: "0" };
  quotaAllocations.value = [];
  projects.value = [];
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
    const [summaryPayload, rolePayload, userPayload, rulePayload, walletPayload, tenantPayload] = await Promise.all([
      fetchJson<SummaryPayload>("/api/admin/summary"),
      fetchJson<string[]>("/api/admin/roles"),
      fetchJson<UserPayload[]>("/api/admin/users"),
      fetchJson<RulePayload[]>("/api/admin/model-access-rules"),
      fetchJson<WalletPayload>("/api/tenant/wallet"),
      fetchJson<TenantOverviewPayload[]>("/api/admin/tenants"),
    ]);
    summary.value = summaryPayload;
    roles.value = rolePayload;
    users.value = userPayload;
    rules.value = rulePayload;
    wallet.value = walletPayload;
    tenants.value = tenantPayload;
    if (tenantPayload.length > 0) {
      selectedTenantId.value = tenantPayload.some((tenant) => tenant.tenantId === selectedTenantId.value)
        ? selectedTenantId.value
        : tenantPayload[0]!.tenantId;
      await loadTenantDetail(selectedTenantId.value);
    }
  }

  if (canViewTenantWorkspace.value) {
    const [walletPayload, quotaPayload, quotaAllocationPayload, paymentOrderPayload, projectPayload, memberPayload, clientPayload, brandPayload, assetPayload] = await Promise.all([
      fetchJson<WalletPayload>("/api/tenant/wallet"),
      fetchJson<QuotaPayload>("/api/tenant/quotas"),
      fetchJson<QuotaAllocationPayload[]>("/api/tenant/quota-allocations"),
      fetchJson<PaymentOrderPayload[]>("/api/tenant/wallet/payment-orders"),
      fetchJson<ProjectPayload[]>("/api/tenant/projects"),
      fetchJson<UserPayload[]>("/api/tenant/members"),
      fetchJson<ClientPayload[]>("/api/tenant/clients"),
      fetchJson<BrandPayload[]>("/api/tenant/brands"),
      fetchJson<AssetPayload[]>("/api/tenant/assets"),
    ]);
    wallet.value = walletPayload;
    quotas.value = quotaPayload;
    quotaAllocations.value = quotaAllocationPayload;
    paymentOrders.value = paymentOrderPayload;
    projects.value = projectPayload;
    members.value = memberPayload;
    clients.value = clientPayload;
    brands.value = brandPayload;
    assets.value = assetPayload;
  }

  syncMemberRoleDrafts();
  syncQuotaScope();
  syncWorkspaceNavigation();
  loading.value = false;
}

// 平台超管加载单个租户详情，查看成员、模型策略和钱包流水。
async function loadTenantDetail(tenantId: string) {
  selectedTenantId.value = tenantId;
  rechargeForm.tenantId = tenantId;
  ruleForm.scopeType = "tenant";
  ruleForm.scopeValue = tenantId;
  tenantDetail.value = await fetchJson<TenantDetailPayload>(`/api/admin/tenants/${tenantId}`);
}

// 平台超管创建任意账号。
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

// 登录成功后写入本地令牌，并触发当前角色的工作台加载。
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

  const envelope = await response.json() as ApiResponse<SessionPayload>;
  const payload = envelope.data;
  localStorage.setItem("chj_aigc_token", payload.token);
  session.value = payload;
  isAuthenticated.value = true;
  navigate("/workspace");
  success.value = `欢迎，${payload.displayName}`;
  await loadDashboard();
}

// 页面首次挂载时尝试复用已有令牌恢复登录态。
async function bootstrap() {
  const token = localStorage.getItem("chj_aigc_token");
  if (!token) {
    loading.value = false;
    isAuthenticated.value = false;
    navigate("/login", true);
    return;
  }

  try {
    const me = await fetchJson<Omit<SessionPayload, "token">>("/api/auth/me");
    session.value = {
      ...me,
      token,
    };
    isAuthenticated.value = true;
    navigate(currentPath.value === "/login" ? "/workspace" : currentPath.value, true);
    await loadDashboard();
  } catch {
    localStorage.removeItem("chj_aigc_token");
    isAuthenticated.value = false;
    loading.value = false;
    navigate("/login", true);
  }
}

function logout() {
  localStorage.removeItem("chj_aigc_token");
  session.value = null;
  isAuthenticated.value = false;
  success.value = "";
  navigate("/login");
}

function quotaScopeLabel(allocation: QuotaAllocationPayload) {
  if (allocation.scopeType === "PROJECT") {
    return projectNameMap.value.get(allocation.scopeId) ?? allocation.scopeId;
  }
  if (allocation.scopeType === "USER") {
    return memberNameMap.value.get(allocation.scopeId) ?? allocation.scopeId;
  }
  return allocation.scopeId;
}

function quotaScopeTypeLabel(scopeType: string) {
  if (scopeType === "PROJECT") {
    return "项目";
  }
  if (scopeType === "USER") {
    return "成员";
  }
  return scopeType;
}

// 平台超管创建模型访问规则。
async function createRule() {
  success.value = "";
  error.value = "";
  if (isPlatformAdmin.value && ruleForm.scopeType === "tenant") {
    ruleForm.scopeValue = selectedTenantId.value;
  }
  await fetchJson<RulePayload>("/api/admin/model-access-rules", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(ruleForm),
  });
  success.value = `规则 ${ruleForm.ruleId} 已创建`;
  await loadDashboard();
  if (isPlatformAdmin.value) {
    await loadTenantDetail(selectedTenantId.value);
  }
}

// 创建微信支付订单，等待模拟支付成功后再到账。
async function rechargeWallet() {
  success.value = "";
  error.value = "";
  if (isPlatformAdmin.value) {
    rechargeForm.tenantId = selectedTenantId.value;
  }
  await fetchJson<PaymentOrderPayload>("/api/tenant/wallet/payment-orders/wechat", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(rechargeForm),
  });
  success.value = `微信支付订单 ${rechargeForm.orderId} 已创建`;
  await loadDashboard();
  if (isPlatformAdmin.value) {
    await loadTenantDetail(selectedTenantId.value);
  }
}

// 模拟微信支付回调成功，把订单状态改为已支付并写入钱包流水。
async function mockPayOrder(orderId: string) {
  success.value = "";
  error.value = "";
  await fetchJson<PaymentOrderPayload>(`/api/tenant/wallet/payment-orders/${orderId}/mock-paid`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
  });
  success.value = `支付订单 ${orderId} 已模拟支付成功`;
  await loadDashboard();
  if (isPlatformAdmin.value) {
    await loadTenantDetail(selectedTenantId.value);
  }
}

// 保存项目或成员额度分配。
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

// 租户负责人创建客户。
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

// 租户负责人创建项目，并把额度范围默认切到新项目。
async function createProject() {
  success.value = "";
  error.value = "";
  await fetchJson<ProjectPayload>("/api/tenant/projects", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(projectForm),
  });
  success.value = `项目 ${projectForm.name} 已创建`;
  quotaForm.scopeId = projectForm.projectId;
  await loadDashboard();
}

// 租户负责人创建成员，并把额度范围默认切到新成员。
async function createMember() {
  success.value = "";
  error.value = "";
  await fetchJson<UserPayload>("/api/tenant/members", {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(memberForm),
  });
  success.value = `成员 ${memberForm.displayName} 已创建`;
  quotaForm.scopeType = "user";
  quotaForm.scopeId = memberForm.userId;
  await loadDashboard();
}

// 启用或停用账号：平台超管走认证服务接口，租户负责人走租户成员接口。
async function updateMemberStatus(member: UserPayload, active: boolean) {
  success.value = "";
  error.value = "";
  const url = isPlatformAdmin.value
    ? `/api/auth/users/${member.id}/status`
    : `/api/tenant/members/${member.id}/status`;
  await fetchJson<UserPayload>(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ active }),
  });
  success.value = `成员 ${member.displayName} 已${active ? "启用" : "停用"}`;
  await loadDashboard();
}

// 调整账号角色：平台超管走认证服务接口，租户负责人走租户成员接口。
async function updateMemberRole(member: UserPayload) {
  success.value = "";
  error.value = "";
  const url = isPlatformAdmin.value
    ? `/api/auth/users/${member.id}/role`
    : `/api/tenant/members/${member.id}/role`;
  await fetchJson<UserPayload>(url, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ roleKey: memberRoleDrafts[member.id] ?? member.roleKey }),
  });
  success.value = `成员 ${member.displayName} 角色已更新`;
  await loadDashboard();
}

// 租户负责人创建品牌。
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

// 所有按钮动作统一走这一层，避免页面逻辑分散处理异常。
async function runAction(action: () => Promise<void>) {
  try {
    await action();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "未知错误";
    loading.value = false;
  }
}

function syncQuotaScope() {
  const options = quotaScopeOptions.value;
  if (options.length === 0) {
    quotaForm.scopeId = "";
    return;
  }
  if (!options.some((option) => option.value === quotaForm.scopeId)) {
    quotaForm.scopeId = options[0]!.value;
  }
}

function syncMemberRoleDrafts() {
  Object.keys(memberRoleDrafts).forEach((key) => {
    delete memberRoleDrafts[key];
  });
  members.value.forEach((member) => {
    memberRoleDrafts[member.id] = member.roleKey;
  });
  users.value.forEach((user) => {
    memberRoleDrafts[user.id] = user.roleKey;
  });
}

function syncWorkspaceNavigation() {
  const menus = workspaceMenus.value;
  if (menus.length === 0) {
    activeMenuKey.value = "";
    activeTabKey.value = "";
    return;
  }
  const menu = menus.find((item) => item.key === activeMenuKey.value) ?? menus[0]!;
  activeMenuKey.value = menu.key;
  const tab = menu.tabs.find((item) => item.key === activeTabKey.value) ?? menu.tabs[0]!;
  activeTabKey.value = tab.key;
}

function selectMenu(menuKey: string) {
  const menu = workspaceMenus.value.find((item) => item.key === menuKey);
  if (!menu) {
    return;
  }
  activeMenuKey.value = menu.key;
  activeTabKey.value = menu.tabs[0]?.key ?? "";
}

function selectTab(tabKey: string) {
  activeTabKey.value = tabKey;
}

function tenantCardClass(tenantId: string) {
  return {
    active: selectedTenantId.value === tenantId,
  };
}

function navigate(path: string, replace = false) {
  if (replace) {
    window.history.replaceState({}, "", path);
  } else if (window.location.pathname !== path) {
    window.history.pushState({}, "", path);
  }
  currentPath.value = window.location.pathname;
}

function handlePopState() {
  currentPath.value = window.location.pathname;
}

onMounted(() => {
  window.addEventListener("popstate", handlePopState);
  runAction(bootstrap);
});

onBeforeUnmount(() => {
  window.removeEventListener("popstate", handlePopState);
});
</script>

<template>
  <div class="page-shell">
    <section v-if="showLoginView" class="login-shell">
      <div class="login-hero">
        <div class="login-copy">
          <p class="eyebrow">CHJ AIGC PLATFORM</p>
          <h1>多租户 AIGC 运营平台</h1>
          <p class="hero-copy">平台超管管理租户与模型，租户负责人管理项目、成员与品牌资产。</p>
        </div>
        <div class="login-card">
          <p class="eyebrow">后台登录</p>
          <h2>进入工作台</h2>
          <form class="form-stack" @submit.prevent="runAction(login)">
            <input v-model="loginForm.username" placeholder="用户名" required>
            <input v-model="loginForm.password" type="password" placeholder="密码" required>
            <button class="action-button full-button" type="submit">登录系统</button>
          </form>
          <div class="login-tip">
            演示账号：admin / Admin@123，tenant_owner / Tenant@123，tenant_member / Member@123
          </div>
        </div>
      </div>
    </section>

    <div v-else class="workspace-shell">
      <aside class="sidebar">
        <div class="sidebar-brand">
          <p class="eyebrow">CHJ AIGC</p>
          <h2>{{ workspaceTitle }}</h2>
          <p class="sidebar-copy">{{ workspaceCopy }}</p>
        </div>
        <div class="sidebar-user">
          <span>当前账号</span>
          <strong>{{ session?.displayName }}</strong>
          <small>{{ session?.roleKey }}</small>
        </div>
        <nav class="sidebar-nav">
          <button
            v-for="menu in workspaceMenus"
            :key="menu.key"
            type="button"
            class="sidebar-link"
            :class="{ active: activeMenuKey === menu.key }"
            @click="selectMenu(menu.key)"
          >
            <span class="sidebar-link-label"><em>{{ menu.icon }}</em>{{ menu.label }}</span>
            <strong>{{ menu.badge }}</strong>
          </button>
        </nav>
        <div class="sidebar-status">
          <div class="status-card compact">
            <span>后端服务</span>
            <strong>{{ healthStatus }}</strong>
          </div>
          <div class="status-card compact">
            <span>数据库</span>
            <strong>{{ dbSummary }}</strong>
          </div>
          <button class="action-button secondary-button full-button" type="button" @click="logout">
            退出登录
          </button>
        </div>
      </aside>

      <section class="workspace-main">
        <header class="hero top-hero">
          <div class="hero-copy-block">
            <p class="eyebrow">{{ activeMenu?.label ?? workspaceTitle }}</p>
            <h1>{{ activeTab?.label ?? workspaceTitle }}</h1>
          </div>
          <div class="hero-status">
            <div class="status-card">
              <span>钱包余额</span>
              <strong>¥ {{ wallet.balance }}</strong>
            </div>
            <div class="status-card">
              <span>后端 / 数据库</span>
              <strong>{{ healthStatus }} · {{ dbSummary }}</strong>
            </div>
          </div>
        </header>

        <nav class="workspace-nav tab-nav">
          <button
            v-for="tab in activeTabs"
            :key="tab.key"
            type="button"
            class="nav-pill"
            :class="{ active: activeTabKey === tab.key }"
            @click="selectTab(tab.key)"
          >
            {{ tab.label }}
          </button>
          <button class="action-button refresh-button" type="button" @click="runAction(loadDashboard)">
            刷新数据
          </button>
        </nav>

        <main class="dashboard-grid">
          <section v-show="activeTabKey === 'overview'" id="overview" class="panel panel-wide">
            <div class="panel-head">
              <div>
                <p class="panel-label">{{ isPlatformAdmin ? "平台总览" : "租户总览" }}</p>
                <h2>{{ isPlatformAdmin ? "租户运营概览" : "租户工作台概览" }}</h2>
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
                <span>项目数</span>
                <strong>{{ projects.length }}</strong>
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

          <section v-if="isPlatformAdmin" v-show="activeTabKey === 'tenants'" class="panel panel-wide">
            <div class="panel-head">
              <div>
                <p class="panel-label">平台租户</p>
                <h2>租户总览</h2>
              </div>
            </div>
            <div class="tenant-grid">
              <article
                v-for="tenant in tenants"
                :key="tenant.tenantId"
                class="stat-card tenant-card"
                :class="tenantCardClass(tenant.tenantId)"
                @click="runAction(() => loadTenantDetail(tenant.tenantId))"
              >
                <span>{{ tenant.displayName }}</span>
                <strong>{{ tenant.walletBalance }}</strong>
                <div class="subtext">成员 {{ tenant.memberCount }} · 负责人 {{ tenant.ownerCount }} · 启用 {{ tenant.activeMemberCount }}</div>
              </article>
            </div>
            <div v-if="tenantDetail" class="tenant-detail-grid">
              <section class="panel detail-panel">
                <div class="panel-head">
                  <div>
                    <p class="panel-label">租户详情</p>
                    <h3>{{ tenantDetail.tenant.displayName }}</h3>
                  </div>
                </div>
                <div class="quota-strip">
                  <div class="mini-stat">
                    <span>成员数量</span>
                    <strong>{{ tenantDetail.tenant.memberCount }}</strong>
                  </div>
                  <div class="mini-stat">
                    <span>负责人数量</span>
                    <strong>{{ tenantDetail.tenant.ownerCount }}</strong>
                  </div>
                  <div class="mini-stat">
                    <span>启用成员</span>
                    <strong>{{ tenantDetail.tenant.activeMemberCount }}</strong>
                  </div>
                  <div class="mini-stat">
                    <span>策略数量</span>
                    <strong>{{ tenantRuleCount }}</strong>
                  </div>
                </div>
              </section>

              <section class="panel detail-panel">
                <div class="panel-head">
                  <div>
                    <p class="panel-label">租户成员</p>
                    <h3>成员清单</h3>
                  </div>
                </div>
                <ul class="card-list compact-list">
                  <li v-for="member in tenantDetail.members" :key="member.id">
                    <strong>{{ member.displayName }}</strong>
                    <div class="subtext">{{ member.roleKey }} · {{ member.username }} · {{ member.active ? "启用" : "停用" }}</div>
                  </li>
                </ul>
              </section>

              <section class="panel detail-panel">
                <div class="panel-head">
                  <div>
                    <p class="panel-label">租户策略</p>
                    <h3>模型权限</h3>
                  </div>
                </div>
                <div class="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>规则 ID</th>
                        <th>模型</th>
                        <th>效果</th>
                        <th>说明</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="rule in tenantDetail.rules" :key="rule.id">
                        <td>{{ rule.id }}</td>
                        <td>{{ rule.platformModelAlias }}</td>
                        <td>{{ rule.effect }}</td>
                        <td>{{ rule.scope.type }}:{{ rule.scope.value }}</td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>

              <section class="panel detail-panel">
                <div class="panel-head">
                  <div>
                    <p class="panel-label">支付订单</p>
                    <h3>微信支付模拟订单</h3>
                  </div>
                </div>
                <div class="table-wrap">
                  <table>
                    <thead>
                      <tr>
                        <th>订单 ID</th>
                        <th>状态</th>
                        <th>金额</th>
                        <th>说明</th>
                        <th>操作</th>
                      </tr>
                    </thead>
                    <tbody>
                      <tr v-for="order in tenantDetail.paymentOrders" :key="order.id">
                        <td>{{ order.id }}</td>
                        <td>{{ order.status }}</td>
                        <td>{{ order.amount }}</td>
                        <td>{{ order.description }}</td>
                        <td>
                          <button
                            v-if="order.status === 'PENDING'"
                            class="tiny-button"
                            type="button"
                            @click="runAction(() => mockPayOrder(order.id))"
                          >
                            模拟支付成功
                          </button>
                          <span v-else>已到账</span>
                        </td>
                      </tr>
                    </tbody>
                  </table>
                </div>
              </section>
            </div>
          </section>

          <section v-if="isPlatformAdmin" v-show="activeTabKey === 'accounts'" id="accounts" class="panel">
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
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="user in users" :key="user.id">
                <td>{{ user.username }}</td>
                <td>{{ user.displayName }}</td>
                <td>
                  <select
                    v-model="memberRoleDrafts[user.id]"
                    class="member-role-select"
                    :disabled="session?.userId === user.id"
                  >
                    <option value="platform_admin">平台超管</option>
                    <option value="tenant_owner">租户负责人</option>
                    <option value="tenant_member">租户成员</option>
                  </select>
                </td>
                <td>{{ user.tenantId ?? "-" }}</td>
                <td>{{ user.active ? "启用" : "停用" }}</td>
                <td class="inline-actions">
                  <button
                    class="tiny-button"
                    type="button"
                    :disabled="session?.userId === user.id || memberRoleDrafts[user.id] === user.roleKey"
                    @click="runAction(() => updateMemberRole(user))"
                  >保存角色</button>
                  <button
                    class="tiny-button"
                    type="button"
                    :disabled="session?.userId === user.id || user.active"
                    @click="runAction(() => updateMemberStatus(user, true))"
                  >启用</button>
                  <button
                    class="tiny-button danger-button"
                    type="button"
                    :disabled="session?.userId === user.id || !user.active"
                    @click="runAction(() => updateMemberStatus(user, false))"
                  >停用</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="isPlatformAdmin" v-show="activeTabKey === 'policies'" id="policies" class="panel">
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

      <section v-show="activeTabKey === 'finance'" id="finance" class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">{{ isPlatformAdmin ? "平台侧租户运营" : "租户资金" }}</p>
            <h2>{{ isPlatformAdmin ? "租户充值" : "钱包与额度" }}</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form v-if="canRechargeTenant" class="form-stack" @submit.prevent="runAction(rechargeWallet)">
            <h3>微信支付下单</h3>
            <input v-model="rechargeForm.orderId" placeholder="支付订单 ID" required>
            <input v-model="rechargeForm.amount" placeholder="充值金额" required>
            <input v-model="rechargeForm.description" placeholder="说明" required>
            <input v-model="rechargeForm.referenceId" placeholder="引用 ID" required>
            <button class="action-button" type="submit">创建微信支付订单</button>
          </form>

          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(saveQuota)">
            <h3>额度配置</h3>
            <input v-model="quotaForm.allocationId" placeholder="额度 ID" required>
            <select v-model="quotaForm.scopeType" @change="syncQuotaScope">
              <option value="project">项目</option>
              <option value="user">用户</option>
            </select>
            <select v-model="quotaForm.scopeId">
              <option v-for="option in quotaScopeOptions" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
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
        <div v-if="canViewTenantWorkspace" class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>额度 ID</th>
                <th>范围类型</th>
                <th>范围对象</th>
                <th>维度</th>
                <th>上限</th>
                <th>已用</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="allocation in quotaAllocations" :key="allocation.id">
                <td>{{ allocation.id }}</td>
                <td>{{ quotaScopeTypeLabel(allocation.scopeType) }}</td>
                <td>{{ quotaScopeLabel(allocation) }}</td>
                <td>{{ allocation.dimension }}</td>
                <td>{{ allocation.limit }}</td>
                <td>{{ allocation.used }}</td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="table-wrap" v-if="paymentOrders.length > 0 && !isPlatformAdmin">
          <table>
            <thead>
              <tr>
                <th>支付订单</th>
                <th>状态</th>
                <th>金额</th>
                <th>说明</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="order in paymentOrders" :key="order.id">
                <td>{{ order.id }}</td>
                <td>{{ order.status }}</td>
                <td>{{ order.amount }}</td>
                <td>{{ order.description }}</td>
                <td>
                  <button
                    v-if="order.status === 'PENDING' && canRechargeTenant"
                    class="tiny-button"
                    type="button"
                    @click="runAction(() => mockPayOrder(order.id))"
                  >
                    模拟支付成功
                  </button>
                  <span v-else>已到账</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <section v-if="canViewTenantWorkspace" v-show="activeTabKey === 'members'" id="members" class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">租户协作</p>
            <h2>项目与成员</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(createProject)">
            <h3>创建项目</h3>
            <input v-model="projectForm.projectId" placeholder="项目 ID" required>
            <input v-model="projectForm.name" placeholder="项目名称" required>
            <button class="action-button" type="submit">创建项目</button>
          </form>

          <form v-if="canManageTenantWorkspace" class="form-stack" @submit.prevent="runAction(createMember)">
            <h3>创建成员</h3>
            <input v-model="memberForm.userId" placeholder="成员 ID" required>
            <input v-model="memberForm.username" placeholder="登录名" required>
            <input v-model="memberForm.displayName" placeholder="显示名" required>
            <input v-model="memberForm.password" placeholder="初始密码" required>
            <select v-model="memberForm.roleKey">
              <option value="tenant_owner">租户负责人</option>
              <option value="tenant_member">租户成员</option>
            </select>
            <button class="action-button" type="submit">创建成员</button>
          </form>

          <div class="form-stack">
            <h3>租户成员</h3>
            <ul class="card-list">
              <li v-for="member in members" :key="member.id">
                <strong>{{ member.displayName }}</strong>
                <div class="subtext">{{ member.roleKey }} · {{ member.username }} · {{ member.active ? "启用" : "停用" }}</div>
                <div v-if="canManageTenantWorkspace" class="inline-actions">
                  <select
                    v-model="memberRoleDrafts[member.id]"
                    class="member-role-select"
                    :disabled="session?.userId === member.id"
                  >
                    <option value="tenant_owner">租户负责人</option>
                    <option value="tenant_member">租户成员</option>
                  </select>
                  <button
                    class="tiny-button"
                    type="button"
                    :disabled="session?.userId === member.id || memberRoleDrafts[member.id] === member.roleKey"
                    @click="runAction(() => updateMemberRole(member))"
                  >
                    保存角色
                  </button>
                  <button
                    class="tiny-button"
                    type="button"
                    :disabled="session?.userId === member.id || member.active"
                    @click="runAction(() => updateMemberStatus(member, true))"
                  >
                    启用
                  </button>
                  <button
                    class="tiny-button danger-button"
                    type="button"
                    :disabled="session?.userId === member.id || !member.active"
                    @click="runAction(() => updateMemberStatus(member, false))"
                  >
                    停用
                  </button>
                </div>
              </li>
            </ul>
          </div>
        </div>
        <div class="assets-layout">
          <div>
            <h3>项目列表</h3>
            <ul class="card-list">
              <li v-for="project in projects" :key="project.id">
                <strong>{{ project.name }}</strong>
                <div class="subtext">{{ project.id }}</div>
              </li>
            </ul>
          </div>
        </div>
      </section>

      <section v-if="canViewTenantWorkspace" v-show="activeTabKey === 'assets'" id="assets" class="panel">
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
      </section>
    </div>
  </div>
</template>
