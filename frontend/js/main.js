let currentProduct = null;
let modalQuantity = 1;
let allToppings = [];
let selectedCategoryId = null;

document.addEventListener('DOMContentLoaded', async () => {
  await loadCategories();
  await loadToppings();
  await loadProducts();
  setupCartDrawer();
});

// Load Categories
async function loadCategories() {
  const container = document.getElementById('categoryBar');
  if (!container) return;

  try {
    const categories = await fetchAPI('/categories');
    let html = `<button class="category-btn active" onclick="filterCategory(null, this)">Tất Cả</button>`;
    categories.forEach(cat => {
      html += `<button class="category-btn" onclick="filterCategory(${cat.id}, this)">${cat.name}</button>`;
    });
    container.innerHTML = html;
  } catch (err) {
    console.error("Lỗi nạp danh mục:", err);
  }
}

// Load Toppings
async function loadToppings() {
  try {
    allToppings = await fetchAPI('/toppings');
  } catch (err) {
    allToppings = [];
  }
}

// Filter Category
async function filterCategory(categoryId, btn) {
  document.querySelectorAll('.category-btn').forEach(b => b.classList.remove('active'));
  if (btn) btn.classList.add('active');
  selectedCategoryId = categoryId;
  await loadProducts();
}

function searchProducts(event) {
  event.preventDefault();
  loadProducts();
}

// Load Products
async function loadProducts() {
  const container = document.getElementById('productGrid');
  if (!container) return;

  container.innerHTML = '<p>Đang tải sản phẩm...</p>';

  try {
    const keyword = document.getElementById('productSearch')?.value.trim() || '';
    const query = new URLSearchParams();
    if (selectedCategoryId) query.set('categoryId', selectedCategoryId);
    if (keyword) query.set('keyword', keyword);
    const endpoint = `/products${query.size ? `?${query}` : ''}`;
    const products = await fetchAPI(endpoint);

    if (!products || products.length === 0) {
      container.innerHTML = '<p>Chưa có sản phẩm nào trong danh mục này.</p>';
      return;
    }

    let html = '';
    products.forEach(p => {
      html += `
        <div class="product-card">
          <img src="${p.imageUrl || 'https://placehold.co/260x200?text=Moc+Nhien+Coffee'}" class="product-img" alt="${p.name}">
          <div class="product-info">
            <h3 class="product-name">${p.name}</h3>
            <p class="product-desc">${p.description || ''}</p>
            <div class="product-bottom">
              <span class="product-price">${formatVND(p.basePrice)}</span>
              <button class="btn-add" onclick="openProductModal(${p.id})">+ Thêm</button>
            </div>
          </div>
        </div>
      `;
    });
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = '<p style="color:red">Lỗi tải danh sách sản phẩm. Hãy chắc chắn Backend đang chạy!</p>';
  }
}

// Open Product Modal
async function openProductModal(productId) {
  try {
    currentProduct = await fetchAPI(`/products/${productId}`);
    modalQuantity = 1;

    document.getElementById('modalProductName').textContent = currentProduct.name;
    document.getElementById('modalProductDesc').textContent = currentProduct.description || '';
    document.getElementById('modalQty').textContent = modalQuantity;

    // Render Sizes
    const sizesContainer = document.getElementById('modalSizes');
    let sizesHtml = '';
    currentProduct.sizes.forEach((s, idx) => {
      const extraText = s.extraPrice > 0 ? ` (+${formatVND(s.extraPrice)})` : '';
      sizesHtml += `
        <div class="option-item">
          <label>
            <input type="radio" name="productSize" value="${s.sizeName}" data-price="${s.extraPrice}" ${idx === 0 ? 'checked' : ''} onchange="updateModalPrice()">
            <span>Size ${s.sizeName}${extraText}</span>
          </label>
        </div>
      `;
    });
    sizesContainer.innerHTML = sizesHtml;

    // Render Toppings
    const toppingsContainer = document.getElementById('modalToppings');
    let toppingsHtml = '';
    allToppings.forEach(t => {
      toppingsHtml += `
        <div class="option-item">
          <label>
            <input type="checkbox" name="productTopping" value="${t.name}" data-price="${t.price}" onchange="updateModalPrice()">
            <span>${t.name} (+${formatVND(t.price)})</span>
          </label>
        </div>
      `;
    });
    toppingsContainer.innerHTML = toppingsHtml;

    updateModalPrice();
    document.getElementById('productModal').classList.add('active');
  } catch (err) {
    alert('Không thể tải chi tiết sản phẩm!');
  }
}

function closeModal() {
  document.getElementById('productModal').classList.remove('active');
}

function changeQty(delta) {
  modalQuantity += delta;
  if (modalQuantity < 1) modalQuantity = 1;
  document.getElementById('modalQty').textContent = modalQuantity;
  updateModalPrice();
}

function updateModalPrice() {
  if (!currentProduct) return;

  let unitPrice = currentProduct.basePrice;

  // Selected Size
  const selectedSizeRadio = document.querySelector('input[name="productSize"]:checked');
  if (selectedSizeRadio) {
    unitPrice += parseFloat(selectedSizeRadio.dataset.price || 0);
  }

  // Selected Toppings
  document.querySelectorAll('input[name="productTopping"]:checked').forEach(cb => {
    unitPrice += parseFloat(cb.dataset.price || 0);
  });

  const totalPrice = unitPrice * modalQuantity;
  document.getElementById('modalBtnAddText').textContent = `Thêm vào giỏ hàng - ${formatVND(totalPrice)}`;
}

function confirmAddToCart() {
  if (!currentProduct) return;

  const selectedSizeRadio = document.querySelector('input[name="productSize"]:checked');
  const sizeName = selectedSizeRadio ? selectedSizeRadio.value : 'S';
  const sizeExtra = selectedSizeRadio ? parseFloat(selectedSizeRadio.dataset.price || 0) : 0;

  let unitPrice = currentProduct.basePrice + sizeExtra;
  const selectedToppings = [];

  document.querySelectorAll('input[name="productTopping"]:checked').forEach(cb => {
    selectedToppings.push(cb.value);
    unitPrice += parseFloat(cb.dataset.price || 0);
  });

  const cartItem = {
    productId: currentProduct.id,
    productName: currentProduct.name,
    sizeName: sizeName,
    toppings: selectedToppings,
    unitPrice: unitPrice,
    quantity: modalQuantity,
    subtotal: unitPrice * modalQuantity
  };

  CartManager.addItem(cartItem);
  closeModal();
  renderCartDrawer();
  toggleCartDrawer(true);
}

// Cart Drawer Setup
function setupCartDrawer() {
  renderCartDrawer();
}

function toggleCartDrawer(open) {
  const drawer = document.getElementById('cartDrawer');
  if (open === true) {
    drawer.classList.add('active');
  } else if (open === false) {
    drawer.classList.remove('active');
  } else {
    drawer.classList.toggle('active');
  }
}

function renderCartDrawer() {
  const container = document.getElementById('cartItemsList');
  const cart = CartManager.getCart();

  if (!container) return;

  if (cart.length === 0) {
    container.innerHTML = '<p style="text-align:center; color: var(--text-muted); margin-top: 40px;">Giỏ hàng của bạn đang trống.</p>';
    document.getElementById('cartTotalAmount').textContent = formatVND(0);
    return;
  }

  let html = '';
  cart.forEach((item, index) => {
    const toppingsText = item.toppings && item.toppings.length > 0 ? `+ Topping: ${item.toppings.join(', ')}` : '';
    html += `
      <div class="cart-item">
        <div>
          <div class="cart-item-name">${item.productName} (Size ${item.sizeName})</div>
          <div class="cart-item-sub">SL: ${item.quantity} | ${toppingsText}</div>
          <div class="cart-item-price">${formatVND(item.subtotal)}</div>
        </div>
        <button style="color:red; border:none; background:none; cursor:pointer; font-weight:bold;" onclick="CartManager.removeItem(${index}); renderCartDrawer();">Xóa</button>
      </div>
    `;
  });

  container.innerHTML = html;
  document.getElementById('cartTotalAmount').textContent = formatVND(CartManager.getTotal());
}
