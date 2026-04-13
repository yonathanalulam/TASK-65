<template>
  <div class="admin-container">
    <header class="admin-header">
      <h1>User Management</h1>
      <router-link to="/" class="back-link">Back to Dashboard</router-link>
    </header>

    <div class="content">
      <!-- Create User Form -->
      <div class="card">
        <h2>Create New User</h2>
        <div v-if="createError" class="error-message">{{ createError }}</div>
        <div v-if="createSuccess" class="success-message">User created successfully!</div>

        <form @submit.prevent="handleCreate">
          <div class="form-row">
            <div class="form-group">
              <label>Username</label>
              <input v-model="newUser.username" type="text" required minlength="3" maxlength="64"
                     pattern="[a-zA-Z0-9._-]+" />
            </div>
            <div class="form-group">
              <label>Display Name</label>
              <input v-model="newUser.displayName" type="text" />
            </div>
          </div>
          <div class="form-row">
            <div class="form-group">
              <label>Password</label>
              <input v-model="newUser.password" type="password" required minlength="12" />
            </div>
            <div class="form-group">
              <label>Role</label>
              <select v-model="newUser.role">
                <option value="ROLE_USER">User</option>
                <option value="ROLE_PARENT_COACH">Parent/Coach</option>
                <option value="ROLE_ADMIN">Admin</option>
              </select>
            </div>
          </div>
          <button type="submit" :disabled="creating" class="primary-btn">
            {{ creating ? 'Creating...' : 'Create User' }}
          </button>
        </form>
      </div>

      <!-- User List -->
      <div class="card">
        <h2>Users</h2>
        <div v-if="loadError" class="error-message">{{ loadError }}</div>

        <table v-if="users.length">
          <thead>
            <tr>
              <th>Username</th>
              <th>Display Name</th>
              <th>Status</th>
              <th>Roles</th>
              <th>MFA</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in users" :key="user.id">
              <td>{{ user.username }}</td>
              <td>{{ user.displayName || '-' }}</td>
              <td>
                <span :class="['status-badge', user.status.toLowerCase()]">
                  {{ user.status }}
                </span>
              </td>
              <td>{{ user.roles.join(', ') }}</td>
              <td>{{ user.mfaEnabled ? 'Enabled' : 'Disabled' }}</td>
              <td>
                <button v-if="user.status === 'ACTIVE'" @click="handleDisable(user.id)"
                        class="action-btn danger">Disable</button>
                <button v-if="user.status === 'DISABLED'" @click="handleEnable(user.id)"
                        class="action-btn">Enable</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else-if="!loadError" class="empty-state">No users found.</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { listUsers, createUser, disableUser, enableUser } from '@/api/users'
import type { User } from '@/types/user'

const users = ref<User[]>([])
const loadError = ref<string | null>(null)
const createError = ref<string | null>(null)
const createSuccess = ref(false)
const creating = ref(false)

const newUser = reactive({
  username: '',
  displayName: '',
  password: '',
  role: 'ROLE_USER',
})

onMounted(loadUsers)

async function loadUsers() {
  try {
    const page = await listUsers()
    users.value = page.content
  } catch (e: any) {
    loadError.value = e.message ?? 'Failed to load users'
  }
}

async function handleCreate() {
  creating.value = true
  createError.value = null
  createSuccess.value = false
  try {
    await createUser({
      username: newUser.username,
      password: newUser.password,
      displayName: newUser.displayName || undefined,
      roles: [newUser.role],
    })
    createSuccess.value = true
    newUser.username = ''
    newUser.displayName = ''
    newUser.password = ''
    newUser.role = 'ROLE_USER'
    await loadUsers()
  } catch (e: any) {
    createError.value = e.response?.data?.error?.message ?? e.message ?? 'Failed to create user'
  } finally {
    creating.value = false
  }
}

async function handleDisable(userId: number) {
  if (!confirm('Are you sure you want to disable this user?')) return
  try {
    await disableUser(userId)
    await loadUsers()
  } catch (e: any) {
    loadError.value = e.message
  }
}

async function handleEnable(userId: number) {
  try {
    await enableUser(userId)
    await loadUsers()
  } catch (e: any) {
    loadError.value = e.message
  }
}
</script>

<style scoped>
.admin-container { min-height: 100vh; background: #f5f5f5; }
.admin-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.admin-header h1 { font-size: 1.3rem; }
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 900px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }
.form-row { display: grid; grid-template-columns: 1fr 1fr; gap: 1rem; }
.form-group { margin-bottom: 1rem; }
label { display: block; margin-bottom: 0.3rem; color: #555; font-size: 0.9rem; font-weight: 500; }
input, select {
  width: 100%; padding: 0.6rem; border: 1px solid #ddd;
  border-radius: 6px; font-size: 0.95rem; box-sizing: border-box;
}
.primary-btn {
  padding: 0.6rem 1.5rem; background: #667eea; color: white;
  border: none; border-radius: 6px; cursor: pointer;
}
.primary-btn:disabled { opacity: 0.6; }
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.9rem; }
th { font-weight: 600; color: #555; }
.status-badge { padding: 0.15rem 0.5rem; border-radius: 12px; font-size: 0.8rem; }
.status-badge.active { background: #d4edda; color: #155724; }
.status-badge.disabled { background: #f8d7da; color: #721c24; }
.status-badge.locked { background: #fff3cd; color: #856404; }
.action-btn {
  padding: 0.3rem 0.8rem; border: 1px solid #ddd; border-radius: 4px;
  background: white; cursor: pointer; font-size: 0.85rem;
}
.action-btn.danger { color: #dc3545; border-color: #dc3545; }
.action-btn:hover { background: #f8f9fa; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.success-message { background: #d4edda; color: #155724; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
