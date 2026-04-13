<template>
  <div class="checkout-page">
    <header class="page-header">
      <h1>Checkout</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="successMessage" class="success-message">{{ successMessage }}</div>

      <!-- Bundles -->
      <div class="card">
        <h2>Available Bundles</h2>
        <div v-if="loadingBundles" class="loading">Loading bundles...</div>
        <div v-else-if="bundles.length" class="bundle-grid">
          <div v-for="bundle in bundles" :key="bundle.id" class="bundle-card"
               :class="{ selected: cart.includes(bundle.id) }">
            <h3>{{ bundle.name }}</h3>
            <p v-if="bundle.description" class="bundle-desc">{{ bundle.description }}</p>
            <div class="bundle-price">${{ bundle.price.toFixed(2) }}</div>
            <button @click="toggleCart(bundle.id)"
                    :class="cart.includes(bundle.id) ? 'remove-btn' : 'add-btn'">
              {{ cart.includes(bundle.id) ? 'Remove' : 'Add to Cart' }}
            </button>
          </div>
        </div>
        <p v-else class="empty-state">No bundles available.</p>
      </div>

      <!-- Cart & Checkout -->
      <div v-if="cart.length" class="card">
        <h2>Cart ({{ cart.length }} item{{ cart.length > 1 ? 's' : '' }})</h2>
        <div class="cart-items">
          <div v-for="bundleId in cart" :key="bundleId" class="cart-item">
            <span>{{ getBundleName(bundleId) }}</span>
            <span>${{ getBundlePrice(bundleId).toFixed(2) }}</span>
          </div>
          <div class="cart-total">
            <strong>Total:</strong>
            <strong>${{ cartTotal.toFixed(2) }}</strong>
          </div>
        </div>
        <div class="checkout-actions">
          <button v-if="!pendingTransaction" @click="handleInitiate" class="primary-btn"
                  :disabled="processing">
            {{ processing ? 'Processing...' : 'Initiate Checkout' }}
          </button>
          <template v-if="pendingTransaction">
            <p class="pending-info">
              Transaction #{{ pendingTransaction.id }} - Status: {{ pendingTransaction.status }}
            </p>
            <button @click="handleComplete" class="success-btn" :disabled="processing">
              {{ processing ? 'Completing...' : 'Complete Payment' }}
            </button>
          </template>
        </div>
      </div>

      <!-- Transaction History -->
      <div class="card">
        <h2>Transaction History</h2>
        <div v-if="loadingTransactions" class="loading">Loading transactions...</div>
        <table v-else-if="transactions.length">
          <thead>
            <tr>
              <th>ID</th>
              <th>Status</th>
              <th>Amount</th>
              <th>Receipt</th>
              <th>Date</th>
              <th>Items</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="tx in transactions" :key="tx.id">
              <td>#{{ tx.id }}</td>
              <td>
                <span :class="['status-badge', tx.status.toLowerCase()]">{{ tx.status }}</span>
              </td>
              <td>${{ tx.totalAmount.toFixed(2) }}</td>
              <td>{{ tx.receiptNumber ?? '-' }}</td>
              <td>{{ formatDate(tx.initiatedAt) }}</td>
              <td>
                <span v-for="(item, i) in tx.items" :key="i" class="item-tag">
                  {{ item.bundleName }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty-state">No transactions yet.</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import * as checkoutApi from '@/api/checkout'
import type { Bundle, Transaction } from '@/types/finance'

const bundles = ref<Bundle[]>([])
const transactions = ref<Transaction[]>([])
const cart = ref<number[]>([])
const pendingTransaction = ref<Transaction | null>(null)
const loadingBundles = ref(false)
const loadingTransactions = ref(false)
const processing = ref(false)
const error = ref<string | null>(null)
const successMessage = ref<string | null>(null)

const cartTotal = computed(() => {
  return cart.value.reduce((sum, id) => sum + getBundlePrice(id), 0)
})

onMounted(async () => {
  await Promise.all([loadBundles(), loadTransactions()])
})

async function loadBundles() {
  loadingBundles.value = true
  try {
    bundles.value = await checkoutApi.listBundles()
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load bundles'
  } finally {
    loadingBundles.value = false
  }
}

async function loadTransactions() {
  loadingTransactions.value = true
  try {
    const page = await checkoutApi.listTransactions()
    transactions.value = page.content
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load transactions'
  } finally {
    loadingTransactions.value = false
  }
}

function toggleCart(bundleId: number) {
  const index = cart.value.indexOf(bundleId)
  if (index >= 0) {
    cart.value.splice(index, 1)
  } else {
    cart.value.push(bundleId)
  }
  pendingTransaction.value = null
}

function getBundleName(id: number): string {
  return bundles.value.find(b => b.id === id)?.name ?? `Bundle #${id}`
}

function getBundlePrice(id: number): number {
  return bundles.value.find(b => b.id === id)?.price ?? 0
}

async function handleInitiate() {
  processing.value = true
  error.value = null
  successMessage.value = null
  try {
    pendingTransaction.value = await checkoutApi.initiateCheckout(cart.value)
  } catch (e: any) {
    error.value = e.message ?? 'Failed to initiate checkout'
  } finally {
    processing.value = false
  }
}

async function handleComplete() {
  if (!pendingTransaction.value) return
  processing.value = true
  error.value = null
  try {
    const completed = await checkoutApi.completeCheckout(pendingTransaction.value.id)
    successMessage.value = `Payment completed! Receipt: ${completed.receiptNumber ?? 'N/A'}`
    pendingTransaction.value = null
    cart.value = []
    await loadTransactions()
  } catch (e: any) {
    error.value = e.message ?? 'Failed to complete checkout'
  } finally {
    processing.value = false
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.checkout-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 1000px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }

/* Bundles */
.bundle-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(220px, 1fr)); gap: 1rem; }
.bundle-card {
  border: 2px solid #eee; border-radius: 8px; padding: 1.25rem; text-align: center;
  transition: border-color 0.2s;
}
.bundle-card.selected { border-color: #667eea; background: #f0f3ff; }
.bundle-card h3 { font-size: 1rem; margin-bottom: 0.5rem; }
.bundle-desc { font-size: 0.85rem; color: #666; margin-bottom: 0.75rem; }
.bundle-price { font-size: 1.5rem; font-weight: 700; color: #333; margin-bottom: 0.75rem; }
.add-btn {
  padding: 0.4rem 1rem; background: #667eea; color: white; border: none;
  border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}
.remove-btn {
  padding: 0.4rem 1rem; background: #dc3545; color: white; border: none;
  border-radius: 6px; cursor: pointer; font-size: 0.9rem;
}

/* Cart */
.cart-items { margin-bottom: 1rem; }
.cart-item {
  display: flex; justify-content: space-between; padding: 0.5rem 0;
  border-bottom: 1px solid #eee; font-size: 0.95rem;
}
.cart-total {
  display: flex; justify-content: space-between; padding: 0.75rem 0;
  font-size: 1.1rem; border-top: 2px solid #333;
}
.checkout-actions { display: flex; flex-direction: column; gap: 0.5rem; }
.primary-btn {
  padding: 0.6rem 1.5rem; background: #667eea; color: white;
  border: none; border-radius: 6px; cursor: pointer; font-size: 1rem;
}
.primary-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.success-btn {
  padding: 0.6rem 1.5rem; background: #28a745; color: white;
  border: none; border-radius: 6px; cursor: pointer; font-size: 1rem;
}
.success-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.pending-info { font-size: 0.9rem; color: #667eea; margin: 0; }

/* Transactions */
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.9rem; }
th { font-weight: 600; color: #555; }
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.75rem; }
.status-badge.completed { background: #d4edda; color: #155724; }
.status-badge.pending { background: #fff3cd; color: #856404; }
.status-badge.initiated { background: #cce5ff; color: #004085; }
.status-badge.failed { background: #f8d7da; color: #721c24; }
.item-tag { display: inline-block; padding: 0.1rem 0.4rem; background: #e9ecef; border-radius: 4px; font-size: 0.75rem; margin-right: 0.3rem; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.success-message { background: #d4edda; color: #155724; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
