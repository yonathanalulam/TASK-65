<template>
  <div class="notifications-page">
    <header class="page-header">
      <h1>
        Notifications
        <span v-if="unreadCount > 0" class="unread-badge">{{ unreadCount }}</span>
      </h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <main class="content">
      <div v-if="error" class="error-message">{{ error }}</div>
      <div v-if="loading" class="loading">Loading notifications...</div>

      <div v-if="notifications.length" class="notifications-list">
        <div v-for="notification in notifications" :key="notification.id"
             class="notification-item" :class="{ unread: isUnread(notification.status) }">
          <div class="notification-header">
            <span class="notification-type" :class="notification.type.toLowerCase()">
              {{ notification.type }}
            </span>
            <span class="notification-priority" v-if="notification.priority > 1">
              Priority: {{ notification.priority }}
            </span>
            <span class="notification-date">{{ formatDate(notification.createdAt) }}</span>
          </div>
          <h3 class="notification-title">{{ notification.title }}</h3>
          <p v-if="notification.message" class="notification-message">{{ notification.message }}</p>
          <div class="notification-actions">
            <button v-if="canMarkRead(notification.status)" @click="handleMarkRead(notification.id)"
                    class="action-btn">Mark Read</button>
            <button v-if="canDismiss(notification.status)" @click="handleDismiss(notification.id)"
                    class="action-btn dismiss">Dismiss</button>
            <span v-if="notification.status === 'DISMISSED'" class="dismissed-label">Dismissed</span>
          </div>
        </div>
      </div>
      <div v-else-if="!loading" class="card empty-state">
        <p>No notifications.</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import * as questionsApi from '@/api/questions'
import { isUnread, canMarkRead, canDismiss } from '@/api/notificationStatus'
import type { Notification } from '@/types/study'

const notifications = ref<Notification[]>([])
const unreadCount = ref(0)
const loading = ref(false)
const error = ref<string | null>(null)

onMounted(async () => {
  await Promise.all([loadNotifications(), loadUnreadCount()])
})

async function loadNotifications() {
  loading.value = true
  error.value = null
  try {
    const page = await questionsApi.listNotifications()
    notifications.value = page.content
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load notifications'
  } finally {
    loading.value = false
  }
}

async function loadUnreadCount() {
  try {
    unreadCount.value = await questionsApi.getUnreadCount()
  } catch {
    // Non-blocking
  }
}

async function handleMarkRead(id: number) {
  try {
    await questionsApi.markNotificationRead(id)
    await Promise.all([loadNotifications(), loadUnreadCount()])
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleDismiss(id: number) {
  try {
    await questionsApi.dismissNotification(id)
    await Promise.all([loadNotifications(), loadUnreadCount()])
  } catch (e: any) {
    error.value = e.message
  }
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.notifications-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; display: flex; align-items: center; gap: 0.5rem; }
.unread-badge {
  display: inline-flex; align-items: center; justify-content: center;
  background: #dc3545; color: white; border-radius: 50%; min-width: 22px; height: 22px;
  font-size: 0.75rem; font-weight: 600; padding: 0 4px;
}
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 800px; margin: 0 auto; }

.notifications-list { display: flex; flex-direction: column; gap: 0.75rem; }
.notification-item {
  background: white; padding: 1.25rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); border-left: 4px solid #ddd;
}
.notification-item.unread { border-left-color: #667eea; background: #fafbff; }
.notification-header { display: flex; align-items: center; gap: 0.75rem; margin-bottom: 0.5rem; }
.notification-type {
  padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.75rem;
  font-weight: 500; text-transform: uppercase;
}
.notification-type.timer_alert { background: #fff3cd; color: #856404; }
.notification-type.drill_complete { background: #d4edda; color: #155724; }
.notification-type.system { background: #e9ecef; color: #555; }
.notification-type.checkout { background: #cce5ff; color: #004085; }
.notification-priority { font-size: 0.75rem; color: #dc3545; font-weight: 500; }
.notification-date { margin-left: auto; font-size: 0.8rem; color: #999; }
.notification-title { font-size: 1rem; margin-bottom: 0.3rem; color: #333; }
.notification-message { font-size: 0.9rem; color: #666; margin-bottom: 0.75rem; line-height: 1.4; }
.notification-actions { display: flex; gap: 0.5rem; }
.action-btn {
  padding: 0.3rem 0.8rem; border: 1px solid #667eea; color: #667eea;
  border-radius: 4px; background: white; cursor: pointer; font-size: 0.85rem;
}
.action-btn:hover { background: #667eea; color: white; }
.action-btn.dismiss { border-color: #6c757d; color: #6c757d; }
.action-btn.dismiss:hover { background: #6c757d; color: white; }
.dismissed-label { font-size: 0.8rem; color: #999; font-style: italic; }

.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}
.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
