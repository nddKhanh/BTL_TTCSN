const API_BASE_URL = 'http://localhost:8080/api/v1';

// Auth Token & User Helpers
function getAuthToken() {
  return localStorage.getItem('authToken');
}

function getAuthUser() {
  const user = localStorage.getItem('authUser');
  return user ? JSON.parse(user) : null;
}

function setAuth(token, user) {
  localStorage.setItem('authToken', token);
  localStorage.setItem('authUser', JSON.stringify(user));
}

function logout() {
  localStorage.removeItem('authToken');
  localStorage.removeItem('authUser');
  window.location.href = 'index.html';
}

async function fetchAPI(endpoint, options = {}) {
  try {
    const token = getAuthToken();
    const headers = {
      'Content-Type': 'application/json',
      ...options.headers
    };

    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers
    });

    const result = await response.json();
    if (!response.ok || !result.success) {
      throw new Error(result.message || 'Có lỗi xảy ra!');
    }
    return result.data;
  } catch (error) {
    console.error(`[API Error] ${endpoint}:`, error);
    throw error;
  }
}

// Global Currency Formatter for VND
function formatVND(amount) {
  return new Intl.NumberFormat('vi-VN', { style: 'currency', currency: 'VND' }).format(amount);
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

