document.addEventListener('DOMContentLoaded', async () => {
  const user = getAuthUser();
  if (!user) {
    alert('Vui lòng đăng nhập để xem lịch sử đơn hàng!');
    window.location.href = 'login.html';
    return;
  }

  await loadMyOrders();
});

async function loadMyOrders() {
  const container = document.getElementById('myOrdersContainer');
  if (!container) return;

  container.innerHTML = '<p style="text-align:center;">Đang tải lịch sử đơn hàng...</p>';

  try {
    const orders = await fetchAPI('/orders/my-orders');

    if (!orders || orders.length === 0) {
      container.innerHTML = `
        <div style="text-align:center; padding: 40px; background:#fff; border-radius:12px; box-shadow:var(--shadow);">
          <p style="font-size:16px; color:var(--text-muted);">Bạn chưa có đơn hàng nào.</p>
          <a href="index.html" style="display:inline-block; margin-top:16px; background:var(--primary-red); color:#fff; padding:10px 20px; border-radius:20px; text-decoration:none; font-weight:bold;">Đặt hàng ngay</a>
        </div>
      `;
      return;
    }

    let html = '';
    orders.forEach(o => {
      const dateStr = new Date(o.createdAt).toLocaleString('vi-VN');
      let itemsSummary = '';
      o.items.forEach(i => {
        const opts = [
          `Size ${i.sizeName}`,
          i.iceLevel,
          i.sugarLevel,
          i.toppings ? `Topping: ${i.toppings}` : null,
          i.note ? `Ghi chú: ${i.note}` : null
        ].filter(Boolean).join(' | ');

        itemsSummary += `
          <div style="display:flex; justify-content:space-between; margin-bottom:6px; font-size:14px;">
            <div>
              • <strong>${i.productName}</strong> x ${i.quantity}
              <div style="font-size:12px; color:var(--text-muted); margin-left:10px;">${opts}</div>
            </div>
            <span style="font-weight:600;">${formatVND(i.subtotal)}</span>
          </div>
        `;
      });

      html += `
        <div style="background:#fff; border-radius:12px; padding:20px; margin-bottom:20px; box-shadow:var(--shadow); border:1px solid var(--border-color);">
          <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid var(--border-color); padding-bottom:12px; margin-bottom:12px;">
            <div>
              <span style="font-weight:bold; font-size:16px; color:var(--primary-red);">Đơn hàng: ${o.orderCode}</span>
              <div style="font-size:13px; color:var(--text-muted); margin-top:2px;">Ngày đặt: ${dateStr}</div>
            </div>
            <div>
              ${getStatusBadge(o.status)}
            </div>
          </div>

          <div style="margin-bottom:12px;">
            ${itemsSummary}
          </div>

          <div style="display:flex; justify-content:space-between; align-items:center; border-top:1px dashed var(--border-color); padding-top:12px;">
            <div style="font-size:13px; color:var(--text-muted);">
              Địa chỉ: ${o.deliveryAddress}
            </div>
            <div style="text-align:right;">
              <span style="font-size:14px;">Tổng cộng: </span>
              <strong style="font-size:18px; color:var(--primary-red);">${formatVND(o.totalAmount)}</strong>
            </div>
          </div>
          
          <div style="text-align:right; margin-top:12px;">
            <a href="order-status.html?code=${o.orderCode}" style="font-size:13px; color:var(--primary-red); font-weight:600; text-decoration:none;">Xem chi tiết & tra cứu &rarr;</a>
          </div>
        </div>
      `;
    });

    container.innerHTML = html;
  } catch (err) {
    console.error(err);
    container.innerHTML = '<p style="color:red; text-align:center;">Lỗi tải lịch sử đơn hàng. Vui lòng kiểm tra lại đăng nhập!</p>';
  }
}

function getStatusBadge(status) {
  const map = {
    'PENDING': '<span style="background:#fff3cd; color:#856404; padding:4px 10px; border-radius:12px; font-size:13px; font-weight:bold;">Chờ xác nhận</span>',
    'CONFIRMED': '<span style="background:#cce5ff; color:#004085; padding:4px 10px; border-radius:12px; font-size:13px; font-weight:bold;">Đã xác nhận</span>',
    'DELIVERING': '<span style="background:#d4edda; color:#155724; padding:4px 10px; border-radius:12px; font-size:13px; font-weight:bold;">Đang giao hàng</span>',
    'COMPLETED': '<span style="background:#28a745; color:#fff; padding:4px 10px; border-radius:12px; font-size:13px; font-weight:bold;">Hoàn thành</span>',
    'CANCELLED': '<span style="background:#dc3545; color:#fff; padding:4px 10px; border-radius:12px; font-size:13px; font-weight:bold;">Đã hủy</span>'
  };
  return map[status] || status;
}
