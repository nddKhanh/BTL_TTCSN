const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const source = fs.readFileSync(path.join(__dirname, '../js/admin.js'), 'utf8');
function admin(fetchAPI) {
  const nodes = Object.fromEntries(['adminProductsList', 'productPreviousPage', 'productNextPage', 'productPageStatus']
    .map(id => [id, { innerHTML: '', textContent: '', disabled: false, attributes: {},
      setAttribute(key, value) { this.attributes[key] = value; } }]));
  const escapeHtml = value => String(value ?? '').replaceAll('&', '&amp;').replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#39;');
  const context = vm.createContext({ fetchAPI, URLSearchParams, console,
    document: { addEventListener() {}, getElementById: id => nodes[id] || null },
    escapeHtml, formatVND: amount => String(amount), alert() {}, confirm: () => true });
  vm.runInContext(source, context);
  return { context, nodes };
}
const productPage = (page, name = `Product ${page}`) => ({ number: page, totalPages: 3, totalElements: 42,
  content: [{ id: page + 1, name, basePrice: 10000, isActive: true }] });

test('product navigation requests bounded server pages and updates accessible status', async () => {
  const requested = [];
  const { context, nodes } = admin(async endpoint => {
    const params = new URL(endpoint, 'http://example.test').searchParams;
    requested.push(Number(params.get('page')));
    assert.equal(params.get('size'), '20');
    return productPage(Number(params.get('page')));
  });
  await context.loadAdminProducts();
  assert.equal(nodes.productPreviousPage.disabled, true);
  assert.equal(nodes.productNextPage.disabled, false);
  await context.changeAdminProductPage(1);
  assert.match(nodes.productPageStatus.textContent, /Trang 2 \/ 3/);
  assert.match(nodes.adminProductsList.innerHTML, /Product 1/);
  await context.changeAdminProductPage(1);
  assert.equal(nodes.productNextPage.disabled, true);
  await context.changeAdminProductPage(1);
  await context.changeAdminProductPage(-1);
  assert.deepEqual(requested, [0, 1, 2, 1]);
  assert.equal(nodes.adminProductsList.attributes['aria-busy'], 'false');
});

test('a stale response cannot overwrite a more recent page', async () => {
  let finishOld;
  const old = new Promise(resolve => { finishOld = resolve; });
  const { context, nodes } = admin(async endpoint => endpoint.endsWith('page=0') ? old : productPage(1, 'Newest'));
  const pendingOld = context.loadAdminProducts(0);
  await context.loadAdminProducts(1);
  finishOld(productPage(0, 'Outdated'));
  await pendingOld;
  assert.match(nodes.adminProductsList.innerHTML, /Newest/);
  assert.doesNotMatch(nodes.adminProductsList.innerHTML, /Outdated/);
  assert.match(nodes.productPageStatus.textContent, /Trang 2 \/ 3/);
});

test('controls are disabled while loading and an empty catalog has no next page', async () => {
  let release;
  const gate = new Promise(resolve => { release = resolve; });
  let requests = 0;
  const { context, nodes } = admin(async () => { requests++; return gate; });
  const pending = context.loadAdminProducts();
  assert.equal(nodes.productPreviousPage.disabled, true);
  assert.equal(nodes.productNextPage.disabled, true);
  await context.changeAdminProductPage(1);
  assert.equal(requests, 1);
  release({ number: 0, totalPages: 0, totalElements: 0, content: [] });
  await pending;
  assert.match(nodes.adminProductsList.innerHTML, /Chưa có sản phẩm/);
  assert.equal(nodes.productNextPage.disabled, true);
  assert.equal(nodes.productPreviousPage.disabled, true);
});

test('reload keeps the current page and falls back when its last item disappears', async () => {
  const pages = [];
  let lastPageLoads = 0;
  const { context, nodes } = admin(async endpoint => {
    const page = Number(new URL(endpoint, 'http://example.test').searchParams.get('page'));
    pages.push(page);
    if (page === 2 && lastPageLoads++ === 0) return productPage(2);
    if (page === 2) return { number: 2, totalPages: 2, totalElements: 40, content: [] };
    return { ...productPage(1, 'Remaining'), totalPages: 2, totalElements: 40 };
  });
  await context.loadAdminProducts(2);
  await context.loadAdminProducts();
  assert.deepEqual(pages, [2, 2, 1]);
  assert.match(nodes.adminProductsList.innerHTML, /Remaining/);
  assert.match(nodes.productPageStatus.textContent, /Trang 2 \/ 2/);
  assert.equal(nodes.productNextPage.disabled, true);
});

test('failed page requests restore controls and escape errors for a safe retry', async () => {
  let fail = false;
  const { context, nodes } = admin(async () => {
    if (fail) throw new Error('<img src=x onerror=alert(1)>');
    return productPage(1, '<b>Product</b>');
  });
  await context.loadAdminProducts(1);
  assert.match(nodes.adminProductsList.innerHTML, /&lt;b&gt;Product&lt;\/b&gt;/);
  fail = true;
  await context.loadAdminProducts();
  assert.match(nodes.adminProductsList.innerHTML, /role="alert"/);
  assert.match(nodes.adminProductsList.innerHTML, /&lt;img/);
  assert.match(nodes.adminProductsList.innerHTML, /Thử lại/);
  assert.equal(nodes.productPreviousPage.disabled, false);
  assert.equal(nodes.productNextPage.disabled, false);
  assert.equal(nodes.adminProductsList.attributes['aria-busy'], 'false');
});
