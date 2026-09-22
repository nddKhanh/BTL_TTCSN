document.addEventListener('DOMContentLoaded', async () => {
  await loadAdminOrders();
});

async function loadAdminOrders() {
  const container = document.getElementById('adminOrdersList');
  if (!container) return;

  try {
    const orders = await fetchAPI('/orders/admin/all');

    if (!orders || orders.length === 0) {
      container.innerHTML = '<p>Chưa có đơn hàng nào.</p>';
      return;
    }

    let html = `
      <table style="width:100%; border-collapse:collapse; margin-top:20px; background:#fff; box-shadow:var(--shadow);">
        <thead>
          <tr style="background:var(--primary-red); color:#fff; text-align:left;">
            <th style="padding:12px;">Mã Đơn</th>
            <th style="padding:12px;">Khách Hàng</th>
            <th style="padding:12px;">SĐT / Địa Chỉ</th>
            <th style="padding:12px;">Tổng Tiền</th>
            <th style="padding:12px;">Trạng Thái</th>
            <th style="padding:12px;">Thao Tác</th>
          </tr>
        </thead>
        <tbody>
    `;

    orders.forEach(o => {
      const dateStr = new Date(o.createdAt).toLocaleString('vi-VN');
      html += `
        <tr style="border-bottom:1px solid var(--border-color);">
          <td style="padding:12px;"><strong>${o.orderCode}</strong><br><small style="color:gray">${dateStr}</small></td>
          <td style="padding:12px;">${o.customerName}</td>
          <td style="padding:12px;">${o.customerPhone}<br><small>${o.deliveryAddress}</small></td>
          <td style="padding:12px; color:var(--primary-red); font-weight:bold;">${formatVND(o.totalAmount)}</td>
          <td style="padding:12px;">${getStatusBadge(o.status)}</td>
          <td style="padding:12px;">
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
    container.innerHTML = '<p style="color:red">Lỗi nạp danh sách đơn hàng cho Admin!</p>';
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
    const res = await fetch(`http://localhost:8080/api/v1/orders/admin/${orderId}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status: newStatus })
    });
    const result = await res.json();
    if (result.success) {
      alert('Cập nhật trạng thái thành công!');
      await loadAdminOrders();
    } else {
      alert('Lỗi: ' + result.message);
    }
  } catch (err) {
    alert('Không thể cập nhật trạng thái đơn!');
  }
}
