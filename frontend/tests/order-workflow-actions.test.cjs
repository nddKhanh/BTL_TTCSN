const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const adminSource = fs.readFileSync(path.join(__dirname, '../js/admin.js'), 'utf8');
const customerSource = fs.readFileSync(path.join(__dirname, '../js/my-orders.js'), 'utf8');

function workflowAdmin(fetchAPI) {
  const escapeHtml = value => String(value ?? '')
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;');
  const context = vm.createContext({
    fetchAPI,
    URLSearchParams,
    console,
    document: { addEventListener() {}, getElementById() { return null; } },
    window: { prompt: () => 'Hết nguyên liệu' },
    globalThis: {},
    escapeHtml,
    formatVND: amount => String(amount),
    showToast() {},
    alert() {}
  });
  vm.runInContext(adminSource, context);
  context.loadAdminOrders = async () => {};
  context.loadAdminDashboard = async () => {};
  return context;
}

test('admin renders only actions supplied by the backend', () => {
  const context = workflowAdmin(async () => null);
  const html = context.renderOrderActions({
    id: 7,
    approvalVersion: 3,
    version: 5,
    availableApprovalActions: ['approve', 'reject'],
    availableFulfillmentActions: []
  });

  assert.match(html, /approve/);
  assert.match(html, /reject/);
  assert.doesNotMatch(html, /dispatch/);
  assert.doesNotMatch(html, /<select/);
});

test('admin renders dynamic action metadata without hardcoding its name', () => {
  const context = workflowAdmin(async () => null);
  const html = context.renderOrderActions({
    id: 7,
    approvalVersion: 3,
    version: 5,
    availableApprovalActions: ['start_review'],
    availableApprovalActionMetadata: [
      { name: 'start_review', label: 'Tiếp nhận hồ sơ', reasonRequired: false }
    ],
    availableFulfillmentActions: []
  });

  assert.match(html, /start_review/);
  assert.match(html, /Tiếp nhận hồ sơ/);
  assert.doesNotMatch(html, /approve/);
});

test('workflow command sends request ID and expected version to semantic endpoint', async () => {
  const calls = [];
  const context = workflowAdmin(async (endpoint, options) => {
    calls.push({ endpoint, options });
    return {};
  });

  await context.executeOrderAction(9, 'approval', 'reject', 4);

  assert.equal(calls.length, 1);
  assert.equal(calls[0].endpoint, '/orders/9/approval/actions/reject');
  const body = JSON.parse(calls[0].options.body);
  assert.equal(body.expectedVersion, 4);
  assert.equal(body.reason, 'Hết nguyên liệu');
  assert.match(body.requestId, /^workflow-/);
});

test('dynamic action prompts when metadata requires a reason', async () => {
  const calls = [];
  const context = workflowAdmin(async (endpoint, options) => {
    calls.push({ endpoint, options });
    return {};
  });

  await context.executeOrderAction(9, 'approval', 'request_changes', 4, true);

  const body = JSON.parse(calls[0].options.body);
  assert.equal(body.reason, 'Hết nguyên liệu');
});

test('customer UI uses approval actions instead of the legacy status endpoint', () => {
  assert.match(customerSource, /approval\/actions\/withdraw/);
  assert.doesNotMatch(adminSource, /orders\/admin\/\$\{orderId\}\/status/);
  assert.doesNotMatch(customerSource, /o\.status === 'PENDING'/);
});
