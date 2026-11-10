let categoriesCache = [];
let couponsCache = [];
let adminOrdersCache = [];
const productPaging = { page: 0, size: 20, totalPages: 0, totalElements: 0, loading: false, requestId: 0 };

document.addEventListener('DOMContentLoaded', async () => {
  const user = getAuthUser();
  if (!user || user.role !== 'ADMIN') {
    alert('Quyền truy cập bị từ chối! Bạn cần đăng nhập tài khoản ADMIN.');
    window.location.href = 'login.html';
    return;
  }

  await loadAdminDashboard();
  await loadCategoriesCache();
  setupFormListeners();
});

function switchTab(tabId, btn) {
  document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
  document.querySelectorAll('.admin-section').forEach(s => s.classList.remove('active'));

  btn.classList.add('active');
  document.getElementById(tabId).classList.add('active');

  if (tabId === 'dashboardTab') loadAdminDashboard();
  if (tabId === 'ordersTab') loadAdminOrders();
  if (tabId === 'couponsTab') loadAdminCoupons();
  if (tabId === 'productsTab') loadAdminProducts();
  if (tabId === 'categoriesTab') loadAdminCategories();
  if (tabId === 'toppingsTab') loadAdminToppings();
}

async function loadCategoriesCache() {
  try {
    categoriesCache = await fetchAPI('/categories');
  } catch (err) {
    categoriesCache = [];
  }
}

// ---------------- 0. DASHBOARD STATS ----------------
async function loadAdminDashboard() {
  const recentOrdersContainer = document.getElementById('adminRecentOrdersList');
  try {
    const stats = await fetchAPI('/admin/dashboard');

    document.getElementById('statRevenue').textContent = formatVND(stats.totalRevenue || 0);
    document.getElementById('statOrdersCount').textContent = stats.totalOrders || 0;
    document.getElementById('statOrdersBreakdown').textContent = 
      `Chờ: ${stats.pendingOrders || 0} | Xử lý: ${stats.completedOrders || 0} | Hủy: ${stats.cancelledOrders || 0}`;
    document.getElementById('statProductsCount').textContent = stats.totalProducts || 0;
    document.getElementById('statUsersCount').textContent = stats.totalUsers || 0;

    if (!recentOrdersContainer) return;

    if (!stats.recentOrders || stats.recentOrders.length === 0) {
      recentOrdersContainer.innerHTML = '<p style="padding:20px;">Chưa có đơn hàng nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã Đơn</th>
            <th>Khách Hàng</th>
            <th>SĐT</th>
            <th>Tổng Tiền</th>
            <th>Xét Duyệt / Giao Hàng</th>
          </tr>
        </thead>
        <tbody>
    `;

    stats.recentOrders.forEach(o => {
      const dateStr = new Date(o.createdAt).toLocaleString('vi-VN');
      html += `
        <tr>
          <td><strong>${o.orderCode}</strong><br><small style="color:gray">${dateStr}</small></td>
          <td>${o.customerName}</td>
          <td>${o.customerPhone}</td>
          <td style="color:var(--primary-red); font-weight:bold;">${formatVND(o.totalAmount)}</td>
          <td>${getWorkflowStatus(o)}</td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    recentOrdersContainer.innerHTML = html;
  } catch (err) {
    console.error('Lỗi nạp thống kê:', err);
    if (recentOrdersContainer) {
      recentOrdersContainer.innerHTML = `<p style="color:red; padding:20px;">Không thể tải dữ liệu báo cáo: ${err.message}</p>`;
    }
  }
}

// ---------------- 1. ORDERS MANAGEMENT ----------------
async function loadAdminOrders() {
  const container = document.getElementById('adminOrdersList');
  if (!container) return;

  try {
    const query = new URLSearchParams();
    const status = document.getElementById('orderStatusFilter')?.value;
    const keyword = document.getElementById('orderKeyword')?.value.trim();
    const createdFrom = document.getElementById('orderFromDate')?.value;
    const createdTo = document.getElementById('orderToDate')?.value;
    if (status) query.set('status', status);
    if (keyword) query.set('keyword', keyword);
    if (createdFrom) query.set('createdFrom', createdFrom);
    if (createdTo) query.set('createdTo', createdTo);

    const endpoint = `/orders/admin/all${query.size ? `?${query}` : ''}`;
    const orders = await fetchAPI(endpoint);
    adminOrdersCache = orders || [];

    if (!orders || orders.length === 0) {
      container.innerHTML = '<p style="padding:20px;">Chưa có đơn hàng nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã Đơn</th>
            <th>Khách Hàng</th>
            <th>SĐT / Địa Chỉ</th>
            <th>Chi Tiết</th>
            <th>Giảm Giá</th>
            <th>Tổng Tiền</th>
            <th>Xét Duyệt / Giao Hàng</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    orders.forEach(o => {
      const dateStr = new Date(o.createdAt).toLocaleString('vi-VN');
      const discountText = o.couponCode ? `${o.couponCode} (-${formatVND(o.discountAmount || 0)})` : 'Không';

      html += `
        <tr>
          <td><strong>${o.orderCode}</strong><br><small style="color:gray">${dateStr}</small></td>
          <td>${o.customerName}</td>
          <td>${o.customerPhone}<br><small>${o.deliveryAddress}</small></td>
          <td><button class="action-btn btn-edit" onclick="openOrderDetail(${o.id})">Xem món</button></td>
          <td style="font-size:12px; color:green;">${discountText}</td>
          <td style="color:var(--primary-red); font-weight:bold;">${formatVND(o.totalAmount)}</td>
          <td>${getWorkflowStatus(o)}</td>
          <td>${renderOrderActions(o)}</td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp đơn hàng: ${err.message}</p>`;
  }
}

function applyOrderFilters(event) {
  event.preventDefault();
  loadAdminOrders();
}

function resetOrderFilters() {
  document.getElementById('orderKeyword').value = '';
  document.getElementById('orderStatusFilter').value = '';
  document.getElementById('orderFromDate').value = '';
  document.getElementById('orderToDate').value = '';
  loadAdminOrders();
}

async function openOrderDetail(orderId) {
  const order = adminOrdersCache.find(item => item.id === orderId);
  if (!order) {
    showToast('Không tìm thấy thông tin đơn hàng', 'error');
    return;
  }

  const itemsHtml = (order.items || []).map(item => {
    const options = [
      item.sizeName ? `Size ${item.sizeName}` : null,
      item.iceLevel,
      item.sugarLevel,
      item.toppings ? `Topping: ${item.toppings}` : null,
      item.note ? `Ghi chú: ${item.note}` : null
    ].filter(Boolean).map(escapeHtml).join(' · ');

    return `
      <div class="order-detail-item">
        <div><strong>${escapeHtml(item.productName || 'Món')}</strong> × ${item.quantity}<small>${options}</small></div>
        <strong>${formatVND(item.subtotal)}</strong>
      </div>
    `;
  }).join('');

  const [approvalHistory, fulfillmentHistory] = await Promise.all([
    fetchAPI(`/orders/${orderId}/approval/history`).catch(() => []),
    fetchAPI(`/orders/${orderId}/fulfillment/history`).catch(() => [])
  ]);

  document.getElementById('orderDetailTitle').textContent = `Đơn ${order.orderCode}`;
  document.getElementById('orderDetailBody').innerHTML = `
    <div class="order-detail-customer">
      <p><strong>Khách hàng:</strong> ${escapeHtml(order.customerName)} · ${escapeHtml(order.customerPhone)}</p>
      <p><strong>Giao đến:</strong> ${escapeHtml(order.deliveryAddress)}</p>
      ${order.note ? `<p><strong>Ghi chú:</strong> ${escapeHtml(order.note)}</p>` : ''}
    </div>
    <div class="order-detail-items">${itemsHtml}</div>
    <div style="margin:14px 0;">
      <p><strong>Xét duyệt:</strong> ${escapeHtml(order.approvalStatus || 'Chưa có')}</p>
      <p><strong>Thực hiện:</strong> ${escapeHtml(order.fulfillmentStatus || 'Chưa bắt đầu')}</p>
      ${renderWorkflowHistory('Lịch sử xét duyệt', approvalHistory)}
      ${renderWorkflowHistory('Lịch sử thực hiện', fulfillmentHistory)}
    </div>
    <div class="order-detail-total">
      <span>Tạm tính: ${formatVND(order.subtotal)}</span>
      <span>Giảm: -${formatVND(order.discountAmount || 0)}</span>
      <span>Phí giao: ${formatVND(order.shippingFee)}</span>
      <strong>Tổng cộng: ${formatVND(order.totalAmount)}</strong>
    </div>
  `;
  document.getElementById('orderDetailModal').classList.add('active');
}

function getWorkflowStatus(order) {
  return `<div>${getApprovalBadge(order.approvalStatus, order.approvalWorkflowState)}</div><div style="margin-top:4px;">${getFulfillmentBadge(order.fulfillmentStatus)}</div>`;
}

function getApprovalBadge(status, workflowState) {
  const map = {
    'PENDING': 'Chờ duyệt',
    'ACCEPTED': 'Đã chấp nhận',
    'REJECTED': 'Đã từ chối',
    'WITHDRAWN': 'Đã rút'
  };
  const normalizedState = workflowState ? String(workflowState) : '';
  const stateDetail = normalizedState && normalizedState.toUpperCase() !== status
    ? ` · ${humanizeWorkflowName(normalizedState)}`
    : '';
  return `<span class="badge-hidden">Duyệt: ${escapeHtml((map[status] || status || 'Chưa có') + stateDetail)}</span>`;
}

function getFulfillmentBadge(status) {
  const map = {
    'NOT_STARTED': 'Chưa chuẩn bị',
    'PREPARING': 'Đang chuẩn bị',
    'DELIVERING': 'Đang giao',
    'COMPLETED': 'Hoàn thành',
    'CANCELLED': 'Đã hủy'
  };
  return `<span class="badge-active">Giao hàng: ${escapeHtml(map[status] || status || 'Chưa có')}</span>`;
}

function renderOrderActions(order) {
  const approvalSource = Array.isArray(order.availableApprovalActionMetadata)
      && order.availableApprovalActionMetadata.length > 0
    ? order.availableApprovalActionMetadata
    : (order.availableApprovalActions || []);
  const approval = approvalSource.map(action => workflowActionButton(
    order.id,
    'approval',
    action,
    order.approvalVersion
  ));
  const fulfillment = (order.availableFulfillmentActions || []).map(action => workflowActionButton(
    order.id,
    'fulfillment',
    action,
    order.version
  ));
  return [...approval, ...fulfillment].join(' ') || '<span style="color:var(--text-muted);">Không có action</span>';
}

function workflowActionButton(orderId, workflow, action, version) {
  const labels = {
    approve: 'Chấp nhận',
    reject: 'Từ chối',
    withdraw: 'Rút yêu cầu',
    startPreparation: 'Bắt đầu chuẩn bị',
    dispatch: 'Bắt đầu giao',
    complete: 'Hoàn thành',
    cancel: 'Hủy thực hiện'
  };
  const metadata = typeof action === 'string' ? { name: action } : (action || {});
  const actionName = String(metadata.name || '');
  if (!/^[A-Za-z][A-Za-z0-9_-]{0,63}$/.test(actionName)) return '';
  const label = metadata.label || labels[actionName] || humanizeWorkflowName(actionName);
  const reasonRequired = Boolean(metadata.reasonRequired)
    || actionName === 'reject'
    || actionName === 'cancel';
  const danger = actionName === 'reject' || actionName === 'cancel' ? ' btn-danger' : '';
  return `<button class="action-btn${danger}" onclick="executeOrderAction(${orderId}, '${workflow}', '${actionName}', ${version}, ${reasonRequired})">${escapeHtml(label)}</button>`;
}

function humanizeWorkflowName(value) {
  const text = String(value || '').replaceAll('_', ' ').replaceAll('-', ' ').trim();
  return text ? text.charAt(0).toUpperCase() + text.slice(1) : '';
}

async function executeOrderAction(orderId, workflow, action, expectedVersion, reasonRequired = false) {
  let reason = null;
  if (reasonRequired || action === 'reject' || action === 'cancel') {
    reason = window.prompt('Nhập lý do cho hành động này:');
    if (!reason || !reason.trim()) return;
  }
  try {
    await fetchAPI(`/orders/${orderId}/${workflow}/actions/${action}`, {
      method: 'POST',
      body: JSON.stringify({
        requestId: newWorkflowRequestId(),
        expectedVersion,
        reason
      })
    });
    showToast('Đã cập nhật đơn hàng');
    await loadAdminOrders();
    await loadAdminDashboard();
  } catch (error) {
    showToast(error.message, 'error');
    await loadAdminOrders();
  }
}

function newWorkflowRequestId() {
  if (globalThis.crypto && typeof globalThis.crypto.randomUUID === 'function') {
    return globalThis.crypto.randomUUID();
  }
  return `workflow-${Date.now()}-${Math.random().toString(16).slice(2)}`;
}

function renderWorkflowHistory(title, entries) {
  if (!entries || entries.length === 0) return '';
  const items = entries.map(entry => `
    <li>${escapeHtml(entry.action)} · ${escapeHtml(entry.actorId)} · ${new Date(entry.createdAt).toLocaleString('vi-VN')}${entry.reason ? ` · ${escapeHtml(entry.reason)}` : ''}</li>
  `).join('');
  return `<details><summary>${title}</summary><ul style="padding-left:20px;">${items}</ul></details>`;
}

// ---------------- 2. COUPONS MANAGEMENT ----------------
async function loadAdminCoupons() {
  const container = document.getElementById('adminCouponsList');
  if (!container) return;

  try {
    couponsCache = await fetchAPI('/admin/coupons');

    if (!couponsCache || couponsCache.length === 0) {
      container.innerHTML = '<p style="padding:20px;">Chưa có mã giảm giá nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>Mã (Code)</th>
            <th>Loại Giảm</th>
            <th>Giá Trị</th>
            <th>Đơn Tối Thiểu</th>
            <th>Giảm Tối Đa</th>
            <th>Trạng Thái</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    couponsCache.forEach(c => {
      const typeText = c.discountType === 'PERCENT' ? 'Phần trăm (%)' : 'Số tiền cố định';
      const valText = c.discountType === 'PERCENT' ? `${c.discountValue}%` : formatVND(c.discountValue);
      const minOrderText = c.minOrderAmount ? formatVND(c.minOrderAmount) : '0đ';
      const maxDiscountText = c.maxDiscountAmount ? formatVND(c.maxDiscountAmount) : 'Không giới hạn';
      const statusBadge = c.active ? '<span class="badge-active">Kích hoạt</span>' : '<span class="badge-hidden">Tạm ngưng</span>';

      html += `
        <tr>
          <td><strong>${c.code}</strong></td>
          <td>${typeText}</td>
          <td style="color:var(--primary-red); font-weight:bold;">${valText}</td>
          <td>${minOrderText}</td>
          <td>${maxDiscountText}</td>
          <td>${statusBadge}</td>
          <td>
            <button class="action-btn btn-edit" onclick="editCoupon(${c.id})">✏️ Sửa</button>
            <button class="action-btn btn-danger" onclick="deleteCoupon(${c.id})">🗑️ Xóa</button>
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp danh sách mã giảm giá: ${err.message}</p>`;
  }
}

function openCouponModal(c = null) {
  if (c) {
    document.getElementById('couponModalTitle').textContent = 'Chỉnh Sửa Mã Giảm Giá';
    document.getElementById('couponId').value = c.id;
    document.getElementById('couponCode').value = c.code;
    document.getElementById('couponType').value = c.discountType;
    document.getElementById('couponValue').value = c.discountValue;
    document.getElementById('couponMinOrder').value = c.minOrderAmount || '';
    document.getElementById('couponMaxDiscount').value = c.maxDiscountAmount || '';
    document.getElementById('couponActive').checked = c.active;
  } else {
    document.getElementById('couponModalTitle').textContent = 'Thêm Mã Giảm Giá Mới';
    document.getElementById('couponId').value = '';
    document.getElementById('couponForm').reset();
    document.getElementById('couponActive').checked = true;
  }
  document.getElementById('couponFormModal').classList.add('active');
}

function editCoupon(id) {
  const coupon = couponsCache.find(c => c.id === id);
  if (coupon) openCouponModal(coupon);
}

async function deleteCoupon(id) {
  if (!confirm('Bạn có chắc chắn muốn xóa mã giảm giá này?')) return;
  try {
    const token = getAuthToken();
    const res = await fetch(`${API_BASE_URL}/admin/coupons/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok || res.status === 204) {
      alert('Xóa mã giảm giá thành công!');
      await loadAdminCoupons();
    } else {
      alert('Không thể xóa mã giảm giá!');
    }
  } catch (err) {
    alert('Lỗi: ' + err.message);
  }
}

// ---------------- 3. PRODUCTS MANAGEMENT ----------------
function renderProductPager() {
  const previous = document.getElementById('productPreviousPage');
  const next = document.getElementById('productNextPage');
  const status = document.getElementById('productPageStatus');
  if (previous) previous.disabled = productPaging.loading || productPaging.page === 0;
  if (next) next.disabled = productPaging.loading || productPaging.page + 1 >= productPaging.totalPages;
  if (status) status.textContent = productPaging.loading ? 'Đang tải sản phẩm...'
    : `Trang ${productPaging.page + 1} / ${Math.max(1, productPaging.totalPages)} (${productPaging.totalElements} sản phẩm)`;
}

function changeAdminProductPage(delta) {
  const page = productPaging.page + delta;
  if (productPaging.loading || !Number.isInteger(delta) || page < 0 || page >= productPaging.totalPages) return;
  return loadAdminProducts(page);
}

async function loadAdminProducts(requestedPage = productPaging.page) {
  const container = document.getElementById('adminProductsList');
  if (!container || !Number.isInteger(requestedPage) || requestedPage < 0) return;
  const requestId = ++productPaging.requestId;
  productPaging.loading = true;
  container.setAttribute('aria-busy', 'true');
  renderProductPager();

  try {
    const query = new URLSearchParams({ size: String(productPaging.size), page: String(requestedPage) });
    const page = await fetchAPI(`/admin/products/page?${query}`);
    if (requestId !== productPaging.requestId) return;
    if (!page || !Array.isArray(page.content) || !Number.isInteger(page.totalPages)
        || page.totalPages < 0 || !Number.isInteger(page.totalElements) || page.totalElements < 0)
      throw new Error('Dữ liệu phân trang không hợp lệ');
    const products = page.content;
    productPaging.totalPages = page.totalPages;
    productPaging.totalElements = page.totalElements;
    if (products.length === 0 && requestedPage > 0 && requestedPage >= page.totalPages)
      return loadAdminProducts(Math.max(0, page.totalPages - 1));
    productPaging.page = requestedPage;

    if (!products || products.length === 0) {
      container.innerHTML = '<p style="padding:20px;">Chưa có sản phẩm nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>Hình Ảnh</th>
            <th>Tên Món</th>
            <th>Danh Mục</th>
            <th>Giá Gốc</th>
            <th>Trạng Thái</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    products.forEach(p => {
      const catName = escapeHtml(p.category ? p.category.name : 'N/A');
      const statusBadge = p.isActive ? '<span class="badge-active">Đang bán</span>' : '<span class="badge-hidden">Tạm ẩn</span>';
      const img = escapeHtml(p.imageUrl || 'https://placehold.co/50x50?text=Food');

      html += `
        <tr>
          <td><img src="${img}" style="width:48px; height:48px; object-fit:cover; border-radius:6px;"></td>
          <td><strong>${escapeHtml(p.name)}</strong><br><small style="color:var(--text-muted);">${escapeHtml(p.description || '')}</small></td>
          <td>${catName}</td>
          <td style="color:var(--primary-red); font-weight:bold;">${formatVND(p.basePrice)}</td>
          <td>${statusBadge}</td>
          <td>
            <button class="action-btn btn-edit" onclick="editProduct(${p.id})">✏️ Sửa</button>
            <button class="action-btn btn-danger" onclick="toggleHideProduct(${p.id})">${p.isActive ? '👁️ Ẩn' : '✔️ Hiện'}</button>
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    if (requestId !== productPaging.requestId) return;
    container.innerHTML = `<p role="alert" style="color:red; padding:20px;">Lỗi nạp sản phẩm: ${escapeHtml(err.message)}</p>
      <button type="button" onclick="loadAdminProducts()">Thử lại</button>`;
  } finally {
    if (requestId === productPaging.requestId) {
      productPaging.loading = false;
      container.setAttribute('aria-busy', 'false');
      renderProductPager();
    }
  }
}

async function openProductModal(prod = null) {
  await loadCategoriesCache();
  const select = document.getElementById('prodCategory');
  select.innerHTML = categoriesCache.map(c => `<option value="${c.id}">${c.name}</option>`).join('');

  if (prod) {
    document.getElementById('productModalTitle').textContent = 'Chỉnh Sửa Sản Phẩm';
    document.getElementById('prodId').value = prod.id;
    document.getElementById('prodName').value = prod.name;
    document.getElementById('prodCategory').value = prod.category ? prod.category.id : '';
    document.getElementById('prodPrice').value = prod.basePrice;
    document.getElementById('prodImage').value = prod.imageUrl || '';
    document.getElementById('prodDesc').value = prod.description || '';
    document.getElementById('prodActive').checked = prod.isActive;
  } else {
    document.getElementById('productModalTitle').textContent = 'Thêm Sản Phẩm Mới';
    document.getElementById('prodId').value = '';
    document.getElementById('productForm').reset();
    document.getElementById('prodActive').checked = true;
  }

  document.getElementById('productFormModal').classList.add('active');
}

async function editProduct(id) {
  try {
    const prod = await fetchAPI(`/products/${id}`);
    openProductModal(prod);
  } catch (err) {
    alert('Không thể tải sản phẩm: ' + err.message);
  }
}

async function toggleHideProduct(id) {
  if (!confirm('Bạn có chắc muốn đổi trạng thái ẩn/hiển thị món này?')) return;
  try {
    await fetchAPI(`/admin/products/${id}`, { method: 'DELETE' });
    alert('Đã cập nhật trạng thái sản phẩm!');
    await loadAdminProducts();
  } catch (err) {
    alert('Lỗi: ' + err.message);
  }
}

// ---------------- 4. CATEGORIES MANAGEMENT ----------------
async function loadAdminCategories() {
  const container = document.getElementById('adminCategoriesList');
  if (!container) return;

  try {
    const categories = await fetchAPI('/categories');

    if (!categories || categories.length === 0) {
      container.innerHTML = '<p style="padding:20px;">Chưa có danh mục nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Mã (Code)</th>
            <th>Tên Danh Mục</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    categories.forEach(c => {
      html += `
        <tr>
          <td>${c.id}</td>
          <td><strong>${c.code}</strong></td>
          <td>${c.name}</td>
          <td>
            <button class="action-btn btn-edit" onclick="editCategory(${c.id}, '${c.code}', '${c.name}', '${c.imageUrl || ''}')">✏️ Sửa</button>
            <button class="action-btn btn-danger" onclick="deleteCategory(${c.id})">🗑️ Xóa</button>
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp danh mục: ${err.message}</p>`;
  }
}

function openCategoryModal(cat = null) {
  if (cat) {
    document.getElementById('categoryModalTitle').textContent = 'Chỉnh Sửa Danh Mục';
    document.getElementById('catId').value = cat.id;
    document.getElementById('catCode').value = cat.code;
    document.getElementById('catName').value = cat.name;
    document.getElementById('catImage').value = cat.imageUrl || '';
  } else {
    document.getElementById('categoryModalTitle').textContent = 'Thêm Danh Mục Mới';
    document.getElementById('catId').value = '';
    document.getElementById('categoryForm').reset();
  }
  document.getElementById('categoryFormModal').classList.add('active');
}

function editCategory(id, code, name, imageUrl) {
  openCategoryModal({ id, code, name, imageUrl });
}

async function deleteCategory(id) {
  if (!confirm('Bạn có chắc muốn xóa danh mục này?')) return;
  try {
    const token = getAuthToken();
    const res = await fetch(`${API_BASE_URL}/admin/categories/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok || res.status === 204) {
      alert('Xóa danh mục thành công!');
      await loadAdminCategories();
    } else {
      const data = await res.json();
      alert('Không thể xóa: ' + (data.message || 'Danh mục đang chứa sản phẩm!'));
    }
  } catch (err) {
    alert('Lỗi xóa danh mục: ' + err.message);
  }
}

// ---------------- 5. TOPPINGS MANAGEMENT ----------------
async function loadAdminToppings() {
  const container = document.getElementById('adminToppingsList');
  if (!container) return;

  try {
    const toppings = await fetchAPI('/toppings');

    if (!toppings || toppings.length === 0) {
      container.innerHTML = '<p style="padding:20px;">Chưa có topping nào.</p>';
      return;
    }

    let html = `
      <table class="admin-table">
        <thead>
          <tr>
            <th>ID</th>
            <th>Tên Topping</th>
            <th>Giá Phụ Thu</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    toppings.forEach(t => {
      html += `
        <tr>
          <td>${t.id}</td>
          <td><strong>${t.name}</strong></td>
          <td style="color:var(--primary-red); font-weight:bold;">${formatVND(t.price)}</td>
          <td>
            <button class="action-btn btn-edit" onclick="editTopping(${t.id}, '${t.name}', ${t.price})">✏️ Sửa</button>
            <button class="action-btn btn-danger" onclick="deleteTopping(${t.id})">🗑️ Xóa</button>
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp topping: ${err.message}</p>`;
  }
}

function openToppingModal(top = null) {
  if (top) {
    document.getElementById('toppingModalTitle').textContent = 'Chỉnh Sửa Topping';
    document.getElementById('topId').value = top.id;
    document.getElementById('topName').value = top.name;
    document.getElementById('topPrice').value = top.price;
  } else {
    document.getElementById('toppingModalTitle').textContent = 'Thêm Topping Mới';
    document.getElementById('topId').value = '';
    document.getElementById('toppingForm').reset();
  }
  document.getElementById('toppingFormModal').classList.add('active');
}

function editTopping(id, name, price) {
  openToppingModal({ id, name, price });
}

async function deleteTopping(id) {
  if (!confirm('Bạn có chắc muốn xóa topping này?')) return;
  try {
    const token = getAuthToken();
    const res = await fetch(`${API_BASE_URL}/admin/toppings/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${token}` }
    });
    if (res.ok || res.status === 204) {
      alert('Xóa topping thành công!');
      await loadAdminToppings();
    } else {
      alert('Không thể xóa topping!');
    }
  } catch (err) {
    alert('Lỗi: ' + err.message);
  }
}

function closeAdminModal(modalId) {
  document.getElementById(modalId).classList.remove('active');
}

// ---------------- SETUP FORMS ----------------
function setupFormListeners() {
  // Coupon Submit
  const couponForm = document.getElementById('couponForm');
  if (couponForm) {
    couponForm.addEventListener('submit', async (e) => {
      e.preventDefault();
      const id = document.getElementById('couponId').value;
      const minOrderVal = document.getElementById('couponMinOrder').value;
      const maxDiscountVal = document.getElementById('couponMaxDiscount').value;

      const payload = {
        code: document.getElementById('couponCode').value.trim().toUpperCase(),
        discountType: document.getElementById('couponType').value,
        discountValue: parseFloat(document.getElementById('couponValue').value),
        minOrderAmount: minOrderVal ? parseFloat(minOrderVal) : 0,
        maxDiscountAmount: maxDiscountVal ? parseFloat(maxDiscountVal) : null,
        active: document.getElementById('couponActive').checked
      };

      try {
        if (id) {
          await fetchAPI(`/admin/coupons/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
          alert('Cập nhật mã giảm giá thành công!');
        } else {
          await fetchAPI('/admin/coupons', { method: 'POST', body: JSON.stringify(payload) });
          alert('Thêm mã giảm giá thành công!');
        }
        closeAdminModal('couponFormModal');
        await loadAdminCoupons();
      } catch (err) {
        alert('Lỗi: ' + err.message);
      }
    });
  }

  // Product Submit
  document.getElementById('productForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('prodId').value;
    const payload = {
      name: document.getElementById('prodName').value.trim(),
      categoryId: parseInt(document.getElementById('prodCategory').value),
      basePrice: parseFloat(document.getElementById('prodPrice').value),
      imageUrl: document.getElementById('prodImage').value.trim() || null,
      description: document.getElementById('prodDesc').value.trim() || null,
      isActive: document.getElementById('prodActive').checked
    };

    try {
      if (id) {
        await fetchAPI(`/admin/products/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
        alert('Cập nhật sản phẩm thành công!');
      } else {
        await fetchAPI('/admin/products', { method: 'POST', body: JSON.stringify(payload) });
        alert('Thêm sản phẩm mới thành công!');
      }
      closeAdminModal('productFormModal');
      await loadAdminProducts(id ? productPaging.page : 0);
    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  });

  // Category Submit
  document.getElementById('categoryForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('catId').value;
    const payload = {
      code: document.getElementById('catCode').value.trim().toUpperCase(),
      name: document.getElementById('catName').value.trim(),
      imageUrl: document.getElementById('catImage').value.trim() || null
    };

    try {
      if (id) {
        await fetchAPI(`/admin/categories/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
        alert('Cập nhật danh mục thành công!');
      } else {
        await fetchAPI('/admin/categories', { method: 'POST', body: JSON.stringify(payload) });
        alert('Thêm danh mục mới thành công!');
      }
      closeAdminModal('categoryFormModal');
      await loadAdminCategories();
    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  });

  // Topping Submit
  document.getElementById('toppingForm').addEventListener('submit', async (e) => {
    e.preventDefault();
    const id = document.getElementById('topId').value;
    const payload = {
      name: document.getElementById('topName').value.trim(),
      price: parseFloat(document.getElementById('topPrice').value)
    };

    try {
      if (id) {
        await fetchAPI(`/admin/toppings/${id}`, { method: 'PUT', body: JSON.stringify(payload) });
        alert('Cập nhật topping thành công!');
      } else {
        await fetchAPI('/admin/toppings', { method: 'POST', body: JSON.stringify(payload) });
        alert('Thêm topping mới thành công!');
      }
      closeAdminModal('toppingFormModal');
      await loadAdminToppings();
    } catch (err) {
      alert('Lỗi: ' + err.message);
    }
  });
}
