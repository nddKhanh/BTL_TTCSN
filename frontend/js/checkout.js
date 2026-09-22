document.addEventListener('DOMContentLoaded', () => {
  renderCheckoutSummary();
  setupCheckoutForm();
});

function renderCheckoutSummary() {
  const container = document.getElementById('checkoutItems');
  const totalEl = document.getElementById('checkoutTotal');
  const cart = CartManager.getCart();

  if (!container) return;

  if (cart.length === 0) {
    container.innerHTML = '<p style="color:red">Giỏ hàng trống! Hãy chọn món trước khi đặt hàng.</p>';
    if (totalEl) totalEl.textContent = formatVND(0);
    return;
  }

  let html = '';
  cart.forEach(item => {
    const toppingsText = item.toppings && item.toppings.length > 0 ? ` (+ ${item.toppings.join(', ')})` : '';
    html += `
      <div style="display:flex; justify-content:space-between; margin-bottom:12px; padding-bottom:8px; border-bottom:1px dashed var(--border-color)">
        <div>
          <strong>${item.productName}</strong> x ${item.quantity}
          <div style="font-size:12px; color:var(--text-muted)">Size: ${item.sizeName} ${toppingsText}</div>
        </div>
        <div style="font-weight:bold; color:var(--primary-red)">${formatVND(item.subtotal)}</div>
      </div>
    `;
  });

  container.innerHTML = html;
  if (totalEl) totalEl.textContent = formatVND(CartManager.getTotal());
}

function setupCheckoutForm() {
  const form = document.getElementById('checkoutForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const cart = CartManager.getCart();
    if (cart.length === 0) {
      alert('Giỏ hàng trống!');
      return;
    }

    const payload = {
      customerName: document.getElementById('customerName').value.trim(),
      customerPhone: document.getElementById('customerPhone').value.trim(),
      deliveryAddress: document.getElementById('deliveryAddress').value.trim(),
      note: document.getElementById('note').value.trim(),
      items: cart.map(i => ({
        productId: i.productId,
        sizeName: i.sizeName,
        toppings: i.toppings,
        quantity: i.quantity
      }))
    };

    try {
      const response = await fetch('http://localhost:8080/api/v1/orders', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });

      const resData = await response.json();

      if (resData.success) {
        CartManager.clearCart();
        alert('Đặt hàng thành công!');
        window.location.href = `order-status.html?code=${resData.data.orderCode}`;
      } else {
        alert('Đặt hàng thất bại: ' + resData.message);
      }
    } catch (err) {
      console.error(err);
      alert('Không thể kết nối đến Backend Server! Vui lòng kiểm tra lại Spring Boot.');
    }
  });
}
