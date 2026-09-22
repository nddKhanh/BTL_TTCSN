const CART_KEY = 'highlands_cart';

const CartManager = {
  getCart() {
    const raw = localStorage.getItem(CART_KEY);
    return raw ? JSON.parse(raw) : [];
  },

  saveCart(cart) {
    localStorage.setItem(CART_KEY, JSON.stringify(cart));
    this.updateCartBadge();
  },

  addItem(item) {
    const cart = this.getCart();
    // Check if identical item exists (same productId, size, and toppings)
    const existingIndex = cart.findIndex(c => 
      c.productId === item.productId &&
      c.sizeName === item.sizeName &&
      JSON.stringify(c.toppings.sort()) === JSON.stringify(item.toppings.sort())
    );

    if (existingIndex > -1) {
      cart[existingIndex].quantity += item.quantity;
      cart[existingIndex].subtotal = cart[existingIndex].quantity * cart[existingIndex].unitPrice;
    } else {
      cart.push(item);
    }

    this.saveCart(cart);
  },

  removeItem(index) {
    const cart = this.getCart();
    cart.splice(index, 1);
    this.saveCart(cart);
  },

  clearCart() {
    localStorage.removeItem(CART_KEY);
    this.updateCartBadge();
  },

  getTotal() {
    const cart = this.getCart();
    return cart.reduce((total, item) => total + (item.subtotal || 0), 0);
  },

  updateCartBadge() {
    const badge = document.getElementById('cartBadge');
    if (badge) {
      const cart = this.getCart();
      const totalQty = cart.reduce((sum, i) => sum + i.quantity, 0);
      badge.textContent = totalQty;
    }
  }
};

// Initialize badge on load
document.addEventListener('DOMContentLoaded', () => {
  CartManager.updateCartBadge();
});
