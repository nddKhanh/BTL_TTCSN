const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');

const source = fs.readFileSync(path.join(__dirname, '../js/api.js'), 'utf8');
const json = (status, data) => new Response(JSON.stringify(data), { status, headers: { 'Content-Type': 'application/json' } });
const unauthorized = () => json(401, { success: false, message: 'expired' });
const rotated = () => json(200, { success: true, data: { accessToken: 'new-access', refreshToken: 'new-refresh', user: { id: 1 } } });
const deferred = () => { let resolve; const promise = new Promise(done => { resolve = done; }); return { promise, resolve }; };
function client(fetch) {
  const store = new Map();
  const localStorage = { getItem: key => store.get(key) ?? null,
    setItem: (key, value) => store.set(key, String(value)), removeItem: key => store.delete(key) };
  const context = vm.createContext({ fetch, localStorage, console: { error() {} }, Intl,
    document: { addEventListener() {} }, window: { location: { href: '' } } });
  vm.runInContext(source, context);
  context.setAuth('old-access', { id: 1 }, 'old-refresh');
  return { context, store };
}

test('concurrent 401 responses share one refresh and replay each request once', async () => {
  let refreshes = 0;
  let requests = 0;
  const gate = deferred();
  const { context, store } = client(async (url, options) => {
    if (url.endsWith('/auth/refresh')) { refreshes++; await gate.promise; return rotated(); }
    requests++;
    return options.headers.Authorization === 'Bearer old-access'
      ? unauthorized() : json(200, { success: true, data: url.split('/').pop() });
  });
  const pending = Promise.all([context.fetchAPI('/one'), context.fetchAPI('/two')]);
  await new Promise(setImmediate);
  assert.equal(refreshes, 1);
  gate.resolve();
  assert.deepEqual(await pending, ['one', 'two']);
  assert.equal(requests, 4);
  assert.equal(store.get('refreshToken'), 'new-refresh');
});

test('a late 401 reuses the newly rotated access token without rotating twice', async () => {
  const late = deferred();
  let refreshes = 0;
  const { context } = client(async (url, options) => {
    if (url.endsWith('/auth/refresh')) { refreshes++; return rotated(); }
    if (options.headers.Authorization === 'Bearer old-access') {
      if (url.endsWith('/late')) return late.promise;
      return unauthorized();
    }
    return json(200, { success: true, data: 'ok' });
  });
  const delayed = context.fetchAPI('/late');
  assert.equal(await context.fetchAPI('/first'), 'ok');
  late.resolve(unauthorized());
  assert.equal(await delayed, 'ok');
  assert.equal(refreshes, 1);
});

test('failed refresh clears only authentication and every caller rejects', async () => {
  let refreshes = 0;
  const gate = deferred();
  const { context, store } = client(async url => {
    if (url.endsWith('/auth/refresh')) { refreshes++; await gate.promise; return unauthorized(); }
    return unauthorized();
  });
  store.set('cart', 'keep-this-cart');
  const pending = Promise.allSettled([context.fetchAPI('/one'), context.fetchAPI('/two')]);
  await new Promise(setImmediate);
  gate.resolve();
  assert.ok((await pending).every(result => result.status === 'rejected'));
  assert.equal(refreshes, 1);
  for (const key of ['authToken', 'authUser', 'refreshToken']) assert.equal(store.has(key), false);
  assert.equal(store.get('cart'), 'keep-this-cart');
});

test('a replayed request cannot refresh again and login failures never refresh', async () => {
  let refreshes = 0;
  let requests = 0;
  const { context } = client(async url => {
    if (url.endsWith('/auth/refresh')) { refreshes++; return rotated(); }
    requests++; return unauthorized();
  });
  await assert.rejects(context.fetchAPI('/protected'), /expired/);
  assert.equal(refreshes, 1);
  assert.equal(requests, 2);
  await assert.rejects(context.fetchAPI('/auth/login', { method: 'POST', body: '{}' }), /expired/);
  assert.equal(refreshes, 1);
  assert.equal(requests, 3);
});

test('204 responses succeed and logout-all keeps cart data', async () => {
  const { context, store } = client(async () => new Response(null, { status: 204 }));
  store.set('cart', 'saved');
  assert.equal(await context.fetchAPI('/admin/products/1', { method: 'DELETE' }), null);
  await context.logoutAll();
  assert.equal(store.get('cart'), 'saved');
  assert.equal(store.has('authToken'), false);
  assert.equal(context.window.location.href, 'login.html');
});

test('logout during refresh cannot restore the signed-out session', async () => {
  const gate = deferred();
  const { context, store } = client(async url => {
    if (url.endsWith('/auth/refresh')) { await gate.promise; return rotated(); }
    if (url.endsWith('/auth/logout')) return json(200, { success: true, data: null });
    return unauthorized();
  });
  const pending = assert.rejects(context.fetchAPI('/protected'), /expired/);
  await new Promise(setImmediate);
  await context.logout();
  gate.resolve();
  await pending;
  assert.equal(store.has('authToken'), false);
  assert.equal(store.has('refreshToken'), false);
});

test('an old refresh failure cannot clear a newer login', async () => {
  const gate = deferred();
  const { context, store } = client(async url => {
    if (url.endsWith('/auth/refresh')) { await gate.promise; throw new Error('network unavailable'); }
    return unauthorized();
  });
  const pending = assert.rejects(context.fetchAPI('/protected'), /expired/);
  await new Promise(setImmediate);
  context.setAuth('other-access', { id: 2 }, 'other-refresh');
  gate.resolve();
  await pending;
  assert.equal(store.get('authToken'), 'other-access');
  assert.equal(store.get('refreshToken'), 'other-refresh');
});
