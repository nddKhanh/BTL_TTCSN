let categoriesCache = [];
let couponsCache = [];

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
            <th>Trạng Thái</th>
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
          <td>${getStatusBadge(o.status)}</td>
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
    const orders = await fetchAPI('/orders/admin/all');

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
            <th>Chi Tiết Món</th>
            <th>Giảm Giá</th>
            <th>Tổng Tiền</th>
            <th>Trạng Thái</th>
            <th>Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    orders.forEach(o => {
      const dateStr = new Date(o.createdAt).toLocaleString('vi-VN');
      const itemsText = o.items ? o.items.map(i => {
        const opts = [i.sizeName, i.iceLevel, i.sugarLevel, i.toppings, i.note].filter(Boolean).join('/');
        return `• ${i.productName || 'Món'} x${i.quantity} (${opts})`;
      }).join('<br>') : '';

      const discountText = o.couponCode ? `${o.couponCode} (-${formatVND(o.discountAmount || 0)})` : 'Không';

      html += `
        <tr>
          <td><strong>${o.orderCode}</strong><br><small style="color:gray">${dateStr}</small></td>
          <td>${o.customerName}</td>
          <td>${o.customerPhone}<br><small>${o.deliveryAddress}</small></td>
          <td style="font-size:12px;">${itemsText}</td>
          <td style="font-size:12px; color:green;">${discountText}</td>
          <td style="color:var(--primary-red); font-weight:bold;">${formatVND(o.totalAmount)}</td>
          <td>${getStatusBadge(o.status)}</td>
          <td>
            <select onchange="updateOrderStatus(${o.id}, this.value)" style="padding:6px; border-radius:4px;">
              <option value="PENDING" ${o.status === 'PENDING' ? 'selected' : ''}>Chờ xác nhận</option>
              <option value="CONFIRMED" ${o.status === 'CONFIRMED' ? 'selected' : ''}>Đã xác nhận</option>
              <option value="DELIVERING" ${o.status === 'DELIVERING' ? 'selected' : ''}>Đang giao</option>
              <option value="COMPLETED" ${o.status === 'COMPLETED' ? 'selected' : ''}>Hoàn thành</option>
              <option value="CANCELLED" ${o.status === 'CANCELLED' ? 'selected' : ''}>Hủy đơn</option>
            </select>
          </td>
        </tr>
      `;
    });

    html += '</tbody></table>';
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp đơn hàng: ${err.message}</p>`;
  }
}

function getStatusBadge(status) {
  const map = {
    'PENDING': '<span style="background:#fff3cd; color:#856404; padding:4px 8px; border-radius:12px; font-size:12px; font-weight:bold;">Chờ xác nhận</span>',
    'CONFIRMED': '<span style="background:#cce5ff; color:#004085; padding:4px 8px; border-radius:12px; font-size:12px; font-weight:bold;">Đã xác nhận</span>',
    'DELIVERING': '<span style="background:#d4edda; color:#155724; padding:4px 8px; border-radius:12px; font-size:12px; font-weight:bold;">Đang giao</span>',
    'COMPLETED': '<span style="background:#28a745; color:#fff; padding:4px 8px; border-radius:12px; font-size:12px; font-weight:bold;">Hoàn thành</span>',
    'CANCELLED': '<span style="background:#dc3545; color:#fff; padding:4px 8px; border-radius:12px; font-size:12px; font-weight:bold;">Đã hủy</span>'
  };
  return map[status] || status;
}

async function updateOrderStatus(orderId, newStatus) {
  try {
    await fetchAPI(`/orders/admin/${orderId}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ status: newStatus })
    });
    alert('Cập nhật trạng thái thành công!');
    await loadAdminOrders();
    await loadAdminDashboard();
  } catch (err) {
    alert('Không thể cập nhật trạng thái đơn: ' + err.message);
  }
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
async function loadAdminProducts() {
  const container = document.getElementById('adminProductsList');
  if (!container) return;

  try {
    const products = await fetchAPI('/admin/products');

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
      const catName = p.category ? p.category.name : 'N/A';
      const statusBadge = p.isActive ? '<span class="badge-active">Đang bán</span>' : '<span class="badge-hidden">Tạm ẩn</span>';
      const img = p.imageUrl || 'https://placehold.co/50x50?text=Food';

      html += `
        <tr>
          <td><img src="${img}" style="width:48px; height:48px; object-fit:cover; border-radius:6px;"></td>
          <td><strong>${p.name}</strong><br><small style="color:var(--text-muted);">${p.description || ''}</small></td>
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
    container.innerHTML = `<p style="color:red; padding:20px;">Lỗi nạp sản phẩm: ${err.message}</p>`;
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
      await loadAdminProducts();
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
