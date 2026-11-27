let currentProduct = null;
let modalQuantity = 1;
let allToppings = [];
let selectedCategoryId = null;
let searchTimer = null;
let lastFocusedElement = null;
const PRODUCT_PLACEHOLDER = 'https://placehold.co/400x300?text=Moc+Nhien+Coffee';

// Giá thấp nhất; thêm "Từ" khi món có size phụ thu.
function priceLabel(product) {
  const hasExtra = (product.sizes || []).some(s => s.extraPrice > 0);
  return `${hasExtra ? 'Từ ' : ''}${formatVND(product.basePrice)}`;
}

function sizeSummary(sizes) {
  const names = (sizes || []).map(s => s.sizeName);
  return names.length > 1 ? `Size ${names.join(' · ')}` : '';
}

document.addEventListener('DOMContentLoaded', async () => {
  const params = new URLSearchParams(window.location.search);
  selectedCategoryId = params.get('categoryId') ? Number(params.get('categoryId')) : null;
  const initialSearch = document.getElementById('productSearch');
  if (initialSearch) initialSearch.value = params.get('keyword') || '';
  await loadCategories();
  await loadToppings();
  await loadProducts();
  setupCartDrawer();
  setupLiveSearch();
});

function setupLiveSearch() {
  const searchInput = document.getElementById('productSearch');
  if (!searchInput) return;

  searchInput.addEventListener('input', () => {
    window.clearTimeout(searchTimer);
    searchTimer = window.setTimeout(loadProducts, 300);
  });
}

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
    const url = new URL(window.location.href);
    url.search = query.toString();
    window.history.replaceState({}, '', url);
    const endpoint = `/products${query.size ? `?${query}` : ''}`;
    const products = await fetchAPI(endpoint);

    if (!products || products.length === 0) {
      container.innerHTML = '<p>Chưa có sản phẩm nào trong danh mục này.</p>';
      return;
    }

    let html = '';
    products.forEach(p => {
      const sizes = sizeSummary(p.sizes);
      html += `
        <div class="product-card" onclick="openProductModal(${p.id})">
          <img src="${p.imageUrl || PRODUCT_PLACEHOLDER}" class="product-img" alt="${p.name}" loading="lazy" onerror="this.onerror=null;this.src=PRODUCT_PLACEHOLDER">
          <div class="product-info">
            ${p.category ? `<span class="product-category">${p.category.name}</span>` : ''}
            <h3 class="product-name">${p.name}</h3>
            <p class="product-desc">${p.description || ''}</p>
            ${sizes ? `<p class="product-sizes">${sizes}</p>` : ''}
            <div class="product-bottom">
              <span class="product-price">${priceLabel(p)}</span>
              <button class="btn-add" type="button" onclick="event.stopPropagation(); openProductModal(${p.id})">+ Thêm</button>
            </div>
          </div>
        </div>
      `;
    });
    container.innerHTML = html;
  } catch (err) {
    container.innerHTML = '<p style="color:red">Lỗi tải danh sách sản phẩm. <button type="button" onclick="loadProducts()">Thử lại</button></p>';
  }
}

// Open Product Modal
async function openProductModal(productId) {
  try {
    currentProduct = await fetchAPI(`/products/${productId}`);
    modalQuantity = 1;

    document.getElementById('modalProductName').textContent = currentProduct.name;
    const modalImage = document.getElementById('modalProductImage');
    modalImage.onerror = () => { modalImage.onerror = null; modalImage.src = PRODUCT_PLACEHOLDER; };
    modalImage.src = currentProduct.imageUrl || PRODUCT_PLACEHOLDER;
    modalImage.alt = currentProduct.name;
    const modalCategory = document.getElementById('modalProductCategory');
    modalCategory.textContent = currentProduct.category?.name || '';
    modalCategory.hidden = !currentProduct.category;
    document.getElementById('modalProductPrice').textContent = priceLabel(currentProduct);
    renderProductSpecs(currentProduct);
    document.getElementById('modalProductDesc').textContent = currentProduct.description || '';
    document.getElementById('modalDescGroup').hidden = !currentProduct.description;
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

    // Reset options
    const defaultIce = document.querySelector('input[name="productIce"][value="100% Đá"]');
    if (defaultIce) defaultIce.checked = true;
    const defaultSugar = document.querySelector('input[name="productSugar"][value="100% Đường"]');
    if (defaultSugar) defaultSugar.checked = true;
    const noteInput = document.getElementById('modalItemNote');
    if (noteInput) noteInput.value = '';

    updateModalPrice();
    lastFocusedElement = document.activeElement;
    const modal = document.getElementById('productModal');
    modal.hidden = false;
    modal.classList.add('active');
    modal.querySelector('.modal-close')?.focus();
  } catch (err) {
    alert('Không thể tải chi tiết sản phẩm!');
  }
}

function renderProductSpecs(product) {
  const sizes = (product.sizes || [])
    .map(s => s.extraPrice > 0 ? `${s.sizeName} (+${formatVND(s.extraPrice)})` : s.sizeName)
    .join(', ');
  const rows = [
    ['Danh mục', product.category?.name],
    ['Kích cỡ', sizes]
  ].filter(([, value]) => value);

  const container = document.getElementById('modalSpecs');
  container.replaceChildren(...rows.map(([label, value]) => {
    const row = document.createElement('div');
    row.className = 'spec-row';
    const labelEl = document.createElement('span');
    labelEl.className = 'spec-label';
    labelEl.textContent = label;
    const valueEl = document.createElement('span');
    valueEl.className = 'spec-value';
    valueEl.textContent = value;
    row.append(labelEl, valueEl);
    return row;
  }));
}

function closeModal() {
  const modal = document.getElementById('productModal');
  modal.classList.remove('active');
  modal.hidden = true;
  lastFocusedElement?.focus();
}

document.addEventListener('keydown', event => {
  if (event.key === 'Escape' && document.getElementById('productModal')?.classList.contains('active')) closeModal();
});

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

  const iceLevel = document.querySelector('input[name="productIce"]:checked')?.value || '100% Đá';
  const sugarLevel = document.querySelector('input[name="productSugar"]:checked')?.value || '100% Đường';
  const note = document.getElementById('modalItemNote')?.value.trim() || '';

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
    iceLevel: iceLevel,
    sugarLevel: sugarLevel,
    toppings: selectedToppings,
    note: note,
    unitPrice: unitPrice,
    quantity: modalQuantity,
    subtotal: unitPrice * modalQuantity
  };

  CartManager.addItem(cartItem);
  closeModal();
  renderCartDrawer();
  toggleCartDrawer(true);
  showToast(`Đã thêm ${currentProduct.name} vào giỏ hàng`);
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
    const opts = [
      `Size ${item.sizeName}`,
      item.iceLevel,
      item.sugarLevel,
      item.toppings && item.toppings.length > 0 ? `Topping: ${item.toppings.join(', ')}` : null,
      item.note ? `Ghi chú: ${item.note}` : null
    ].filter(Boolean).join(' | ');

    html += `
      <div class="cart-item">
        <div>
          <div class="cart-item-name">${item.productName}</div>
          <div class="cart-item-sub">SL: <input class="cart-quantity" type="number" min="1" value="${item.quantity}" onchange="updateCartItemQuantity(${index}, this.value)" aria-label="Số lượng ${item.productName}"> | ${opts}</div>
          <div class="cart-item-price">${formatVND(item.subtotal)}</div>
        </div>
        <button style="color:red; border:none; background:none; cursor:pointer; font-weight:bold;" onclick="removeFromCart(${index});">Xóa</button>
      </div>
    `;
  });

  container.innerHTML = html;
  document.getElementById('cartTotalAmount').textContent = formatVND(CartManager.getTotal());
  const progress = CartManager.getShippingProgress();
  const shippingMessage = document.getElementById('shippingProgress');
  if (shippingMessage) shippingMessage.textContent = progress.free
    ? 'Bạn được miễn phí giao hàng.'
    : `Mua thêm ${formatVND(progress.remaining)} để được miễn phí giao hàng.`;
}

function updateCartItemQuantity(index, quantity) {
  CartManager.updateItem(index, quantity);
  renderCartDrawer();
}

function removeFromCart(index) {
  CartManager.removeItem(index);
  renderCartDrawer();
}
