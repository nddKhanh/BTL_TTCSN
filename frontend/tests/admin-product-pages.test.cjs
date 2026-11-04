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
