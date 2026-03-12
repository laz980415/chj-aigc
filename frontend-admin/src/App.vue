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

const healthStatus = ref("loading");
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
    return "Pending";
  }
  return dbInfo.value.passwordConfigured ? "Configured" : "Password Missing";
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
  success.value = `Rule ${ruleForm.ruleId} created`;
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
  success.value = `Wallet recharged by ${rechargeForm.amount}`;
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
  success.value = `Quota ${quotaForm.allocationId} saved`;
  await loadDashboard();
}

async function runAction(action: () => Promise<void>) {
  try {
    await action();
  } catch (err) {
    error.value = err instanceof Error ? err.message : "Unknown error";
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
        <h1>Admin Console</h1>
        <p class="hero-copy">
          Frontend and backend are now separated. Vue 3 handles the operator UI,
          Spring Boot stays focused on API and domain orchestration.
        </p>
      </div>
      <div class="hero-status">
        <div class="status-card">
          <span>Backend</span>
          <strong>{{ healthStatus }}</strong>
        </div>
        <div class="status-card">
          <span>Database</span>
          <strong>{{ dbSummary }}</strong>
        </div>
      </div>
    </header>

    <main class="dashboard-grid">
      <section class="panel panel-wide">
        <div class="panel-head">
          <div>
            <p class="panel-label">Overview</p>
            <h2>Platform Snapshot</h2>
          </div>
          <button class="action-button" type="button" @click="runAction(loadDashboard)">
            Refresh
          </button>
        </div>
        <div class="stats-grid">
          <article class="stat-card">
            <span>Policies</span>
            <strong>{{ summary.policies }}</strong>
          </article>
          <article class="stat-card">
            <span>Audit Events</span>
            <strong>{{ summary.auditEvents }}</strong>
          </article>
          <article class="stat-card">
            <span>Wallet Balance</span>
            <strong>{{ wallet.balance }}</strong>
          </article>
          <article class="stat-card">
            <span>User Tokens</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </article>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">Admin</p>
            <h2>Model Access Rules</h2>
          </div>
        </div>
        <form class="form-stack" @submit.prevent="runAction(createRule)">
          <div class="form-grid">
            <input v-model="ruleForm.ruleId" placeholder="rule id" required>
            <input v-model="ruleForm.actorId" placeholder="actor id" required>
            <input v-model="ruleForm.platformModelAlias" placeholder="model alias" required>
            <select v-model="ruleForm.scopeType">
              <option value="tenant">tenant</option>
              <option value="project">project</option>
              <option value="role">role</option>
            </select>
            <input v-model="ruleForm.scopeValue" placeholder="scope value" required>
            <select v-model="ruleForm.effect">
              <option value="allow">allow</option>
              <option value="deny">deny</option>
            </select>
            <input v-model="ruleForm.reason" class="full-span" placeholder="reason" required>
          </div>
          <button class="action-button" type="submit">Create Rule</button>
        </form>
        <div class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Rule</th>
                <th>Model</th>
                <th>Scope</th>
                <th>Effect</th>
                <th>Actor</th>
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
            <p class="panel-label">Tenant Finance</p>
            <h2>Wallet And Quotas</h2>
          </div>
        </div>
        <div class="form-grid split-forms">
          <form class="form-stack" @submit.prevent="runAction(rechargeWallet)">
            <h3>Recharge Wallet</h3>
            <input v-model="rechargeForm.entryId" placeholder="entry id" required>
            <input v-model="rechargeForm.amount" placeholder="amount" required>
            <input v-model="rechargeForm.description" placeholder="description" required>
            <input v-model="rechargeForm.referenceId" placeholder="reference id" required>
            <button class="action-button" type="submit">Recharge</button>
          </form>

          <form class="form-stack" @submit.prevent="runAction(saveQuota)">
            <h3>Save Quota</h3>
            <input v-model="quotaForm.allocationId" placeholder="allocation id" required>
            <select v-model="quotaForm.scopeType">
              <option value="project">project</option>
              <option value="user">user</option>
            </select>
            <input v-model="quotaForm.scopeId" placeholder="scope id" required>
            <select v-model="quotaForm.dimension">
              <option value="image_count">image_count</option>
              <option value="tokens">tokens</option>
              <option value="video_seconds">video_seconds</option>
            </select>
            <input v-model="quotaForm.limit" placeholder="limit" required>
            <input v-model="quotaForm.used" placeholder="used" required>
            <button class="action-button" type="submit">Save Quota</button>
          </form>
        </div>
        <div class="quota-strip">
          <div class="mini-stat">
            <span>Project Images</span>
            <strong>{{ quotas.projectImageRemaining }}</strong>
          </div>
          <div class="mini-stat">
            <span>User Tokens</span>
            <strong>{{ quotas.userTokenRemaining }}</strong>
          </div>
        </div>
      </section>

      <section class="panel">
        <div class="panel-head">
          <div>
            <p class="panel-label">Brand Grounding</p>
            <h2>Clients And Assets</h2>
          </div>
        </div>
        <div class="assets-layout">
          <div>
            <h3>Clients</h3>
            <ul class="card-list">
              <li v-for="client in clients" :key="client.id">
                <strong>{{ client.name }}</strong>
                <div class="subtext">{{ client.id }}</div>
              </li>
            </ul>
          </div>
          <div>
            <h3>Assets</h3>
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
      <span v-if="loading">Loading dashboard...</span>
      <span v-else-if="error" class="error-text">{{ error }}</span>
      <span v-else-if="success" class="success-text">{{ success }}</span>
      <span v-else>Ready</span>
    </footer>
  </div>
</template>
