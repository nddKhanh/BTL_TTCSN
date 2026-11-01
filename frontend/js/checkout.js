let appliedCoupon = null;

document.addEventListener('DOMContentLoaded', () => {
  prefillUserInfo();
  renderCheckoutSummary();
  setupCheckoutForm();
});

function prefillUserInfo() {
  const user = getAuthUser();
  if (user) {
    const nameEl = document.getElementById('customerName');
    const phoneEl = document.getElementById('customerPhone');
    if (nameEl && !nameEl.value) nameEl.value = user.fullName || '';
    if (phoneEl && !phoneEl.value) phoneEl.value = user.phone || '';
  }
}

function renderCheckoutSummary() {
  const container = document.getElementById('checkoutItems');
  const totalEl = document.getElementById('checkoutTotal');
  const subtotalEl = document.getElementById('checkoutSubtotal');
  const discountRow = document.getElementById('discountRow');
  const discountEl = document.getElementById('checkoutDiscount');
  const shippingEl = document.getElementById('checkoutShipping');
  const cart = CartManager.getCart();

  if (!container) return;

  if (cart.length === 0) {
    container.innerHTML = '<p style="color:red">Giỏ hàng trống! Hãy chọn món trước khi đặt hàng.</p>';
    if (totalEl) totalEl.textContent = formatVND(0);
    if (subtotalEl) subtotalEl.textContent = formatVND(0);
    if (shippingEl) shippingEl.textContent = formatVND(0);
    if (discountRow) discountRow.style.display = 'none';
    return;
  }

  let html = '';
  cart.forEach(item => {
    const opts = [
      `Size: ${item.sizeName}`,
      item.iceLevel,
      item.sugarLevel,
      item.toppings && item.toppings.length > 0 ? `Topping: ${item.toppings.join(', ')}` : null,
      item.note ? `Ghi chú: ${item.note}` : null
    ].filter(Boolean).join(' | ');

    html += `
      <div style="display:flex; justify-content:space-between; margin-bottom:12px; padding-bottom:8px; border-bottom:1px dashed var(--border-color)">
        <div>
          <strong>${item.productName}</strong> x ${item.quantity}
          <div style="font-size:12px; color:var(--text-muted)">${opts}</div>
        </div>
        <div style="font-weight:bold; color:var(--primary-red)">${formatVND(item.subtotal)}</div>
      </div>
    `;
  });

  container.innerHTML = html;
  const subtotal = CartManager.getTotal();
  const discount = appliedCoupon ? appliedCoupon.discountAmount : 0;
  const netSubtotal = Math.max(0, subtotal - discount);
  const shipping = subtotal >= 200000 ? 0 : 15000;

  if (subtotalEl) subtotalEl.textContent = formatVND(subtotal);

  if (appliedCoupon && discount > 0) {
    if (discountRow) discountRow.style.display = 'flex';
    if (discountEl) discountEl.textContent = `-${formatVND(discount)}`;
  } else {
    if (discountRow) discountRow.style.display = 'none';
  }

  if (shippingEl) shippingEl.textContent = shipping === 0 ? 'Miễn phí' : formatVND(shipping);
  if (totalEl) totalEl.textContent = formatVND(netSubtotal + shipping);
}

async function applyCoupon() {
  const inputEl = document.getElementById('couponCodeInput');
  const msgEl = document.getElementById('couponMsg');
  const code = inputEl ? inputEl.value.trim().toUpperCase() : '';

  if (!code) {
    if (msgEl) {
      msgEl.style.color = 'red';
      msgEl.textContent = 'Vui lòng nhập mã giảm giá';
    }
    return;
  }

  const subtotal = CartManager.getTotal();
  if (subtotal <= 0) {
    if (msgEl) {
      msgEl.style.color = 'red';
      msgEl.textContent = 'Giỏ hàng trống!';
    }
    return;
  }

  try {
    const data = await fetchAPI('/coupons/validate', {
      method: 'POST',
      body: JSON.stringify({ code, subtotal })
    });

    appliedCoupon = data;
    if (msgEl) {
      msgEl.style.color = 'green';
      msgEl.textContent = `Áp dụng thành công mã ${data.code}! Giảm ${formatVND(data.discountAmount)}`;
    }
    renderCheckoutSummary();
  } catch (err) {
    appliedCoupon = null;
    if (msgEl) {
      msgEl.style.color = 'red';
      msgEl.textContent = err.message || 'Mã giảm giá không hợp lệ!';
    }
    renderCheckoutSummary();
  }
}

function setupCheckoutForm() {
  const form = document.getElementById('checkoutForm');
  if (!form) return;

  form.addEventListener('submit', async (e) => {
    e.preventDefault();

    const phone = document.getElementById('customerPhone').value.trim();
    const address = document.getElementById('deliveryAddress').value.trim();
    if (!/^(\+84|0)(3|5|7|8|9)\d{8}$/.test(phone)) {
      alert('Vui lòng nhập số điện thoại Việt Nam hợp lệ.');
      return;
    }
    if (address.length < 10) {
      alert('Địa chỉ giao hàng cần ít nhất 10 ký tự.');
      return;
    }

    const cart = CartManager.getCart();
    if (cart.length === 0) {
      alert('Giỏ hàng trống!');
      return;
    }

    const payload = {
      customerName: document.getElementById('customerName').value.trim(),
      customerPhone: phone,
      deliveryAddress: document.getElementById('deliveryAddress').value.trim(),
      note: document.getElementById('note').value.trim(),
      couponCode: appliedCoupon ? appliedCoupon.code : null,
      items: cart.map(i => ({
        productId: i.productId,
        sizeName: i.sizeName,
        iceLevel: i.iceLevel || null,
        sugarLevel: i.sugarLevel || null,
        toppings: i.toppings || [],
        note: i.note || null,
        quantity: i.quantity
      }))
    };

    try {
      const order = await fetchAPI('/orders', {
        method: 'POST',
        body: JSON.stringify(payload)
      });

      CartManager.clearCart();
      alert('Đặt hàng thành công!');
      window.location.href = `order-status.html?code=${encodeURIComponent(order.orderCode)}&phone=${encodeURIComponent(payload.customerPhone)}`;
    } catch (err) {
      console.error(err);
      alert(`Đặt hàng thất bại: ${err.message}`);
    }
  });
}
