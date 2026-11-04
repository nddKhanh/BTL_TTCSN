const API_BASE_URL = 'http://localhost:8081/api/v1';

// Auth Token & User Helpers
function getAuthToken() {
  return localStorage.getItem('authToken');
}

function getAuthUser() {
  const user = localStorage.getItem('authUser');
  return user ? JSON.parse(user) : null;
}

function setAuth(token, user, refreshToken = null) {
  localStorage.setItem('authToken', token);
  localStorage.setItem('authUser', JSON.stringify(user));
  if (refreshToken) localStorage.setItem('refreshToken', refreshToken);
}

async function logout() {
  const refreshToken = localStorage.getItem('refreshToken');
  if (refreshToken) {
    try {
      await fetch(`${API_BASE_URL}/auth/logout`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
    } catch (_) { /* local logout must always succeed */ }
  }
  localStorage.removeItem('authToken');
  localStorage.removeItem('authUser');
  localStorage.removeItem('refreshToken');
  window.location.href = 'index.html';
}

async function fetchAPI(endpoint, options = {}, retried = false) {
  try {
    const token = getAuthToken();
    const headers = { ...options.headers };

    // GET requests should stay CORS-simple. Sending application/json without
    // a request body forces an unnecessary preflight in some browsers.
    if (options.body != null) {
      headers['Content-Type'] = 'application/json';
    }

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers
    });

    const result = await response.json();
    if (response.status === 401 && !retried && localStorage.getItem('refreshToken')) {
      const refreshResponse = await fetch(`${API_BASE_URL}/auth/refresh`, {
        method: 'POST', headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: localStorage.getItem('refreshToken') })
      });
      const refreshResult = await refreshResponse.json();
      if (refreshResponse.ok && refreshResult.success) {
        setAuth(refreshResult.data.accessToken, refreshResult.data.user, refreshResult.data.refreshToken);
        return fetchAPI(endpoint, options, true);
      }
    }
    if (!response.ok || !result.success) {
      throw new Error(result.message || 'Có lỗi xảy ra!');
    }
    return result.data;
  } catch (error) {
    console.error(`[API Error] ${endpoint}:`, error);
    throw error;
  }
}

async function logoutAll() {
  await fetchAPI('/auth/logout-all', { method: 'POST' });
  localStorage.clear();
  window.location.href = 'login.html';
}

// Global Currency Formatter for VND
function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
}

function escapeHtml(value) {
  const element = document.createElement('div');
  element.textContent = value == null ? '' : String(value);
  return element.innerHTML;
}

function showToast(message, type = 'success') {
  let container = document.getElementById('toastContainer');
  if (!container) {
    container = document.createElement('div');
    container.id = 'toastContainer';
    container.className = 'toast-container';
    container.setAttribute('aria-live', 'polite');
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  container.appendChild(toast);

  requestAnimationFrame(() => toast.classList.add('visible'));
  window.setTimeout(() => {
    toast.classList.remove('visible');
    window.setTimeout(() => toast.remove(), 250);
  }, 2800);
}

// Render dynamic User Header state
function renderHeaderAuth() {
  const user = getAuthUser();
  const navContainer = document.querySelector('.nav-links');
  if (!navContainer) return;

  const existingAuthGroup = document.getElementById('navAuthGroup');
  if (existingAuthGroup) existingAuthGroup.remove();

  const authDiv = document.createElement('div');
  authDiv.id = 'navAuthGroup';
  authDiv.style.display = 'inline-flex';
  authDiv.style.alignItems = 'center';
  authDiv.style.gap = '12px';

  if (user) {
    const isAdmin = user.role === 'ADMIN';
    authDiv.innerHTML = `
      <a href="my-orders.html" class="nav-link" style="color:var(--primary-red); font-weight:bold;">📦 Đơn của tôi</a>
      ${isAdmin ? '<a href="admin.html" class="nav-link" style="color:darkblue; font-weight:bold;">⚙️ Admin</a>' : ''}
      <span style="font-size:14px; font-weight:600; color:#333;">👤 ${user.fullName}</span>
      <button onclick="logout()" style="background:none; border:1px solid #ccc; padding:4px 10px; border-radius:4px; cursor:pointer; font-size:13px;">Đăng xuất</button>
    `;
  } else {
    authDiv.innerHTML = `
      <a href="login.html" class="nav-link">Đăng nhập</a>
      <a href="register.html" class="nav-link" style="background:var(--primary-red); color:#fff; padding:6px 14px; border-radius:4px; text-decoration:none;">Đăng ký</a>
    `;
  }

  navContainer.appendChild(authDiv);
}

document.addEventListener('DOMContentLoaded', () => {
  renderHeaderAuth();
});
