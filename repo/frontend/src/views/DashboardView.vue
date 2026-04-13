<template>
  <div class="dashboard">
    <header class="dashboard-header">
      <h1>Culinary Coach</h1>
      <div class="header-actions">
        <span class="username">{{ auth.displayName || auth.username }}</span>
        <router-link to="/notifications" class="nav-link">Notifications</router-link>
        <router-link to="/mfa-setup" class="nav-link">MFA</router-link>
        <router-link to="/change-password" class="nav-link">Password</router-link>
        <button @click="handleLogout" class="logout-btn">Logout</button>
      </div>
    </header>

    <main class="dashboard-content">
      <div class="welcome-card">
        <h2>Welcome, {{ auth.displayName || auth.username }}!</h2>
        <p>Your cooking study workstation is ready.</p>

        <div class="info-grid">
          <div class="info-item">
            <strong>Roles</strong>
            <span>{{ auth.roles.join(', ') }}</span>
          </div>
          <div class="info-item">
            <strong>Status</strong>
            <span class="status-badge active">Active</span>
          </div>
        </div>

        <div v-if="auth.forcePasswordChange" class="warning-banner">
          You must change your password before continuing.
          <router-link to="/change-password">Change password now</router-link>
        </div>
      </div>

      <!-- Navigation Cards -->
      <div class="nav-grid">
        <router-link to="/audio" class="nav-card">
          <div class="nav-icon">&#9835;</div>
          <h3>Audio Library</h3>
          <p>Browse audio assets, manage playlists, and favorites</p>
        </router-link>

        <router-link to="/cooking" class="nav-card">
          <div class="nav-icon">&#127859;</div>
          <h3>Cooking Sessions</h3>
          <p>Start and manage step-by-step cooking sessions with timers</p>
        </router-link>

        <router-link to="/study" class="nav-card">
          <div class="nav-icon">&#128218;</div>
          <h3>Study</h3>
          <p>Answer questions and test your culinary knowledge</p>
        </router-link>

        <router-link to="/notebook" class="nav-card">
          <div class="nav-icon">&#128221;</div>
          <h3>Notebook</h3>
          <p>Review wrong answers, add notes, and launch practice drills</p>
        </router-link>

        <router-link to="/checkout" class="nav-card">
          <div class="nav-icon">&#128722;</div>
          <h3>Checkout</h3>
          <p>Browse bundles and manage purchases</p>
        </router-link>

        <router-link to="/notifications" class="nav-card">
          <div class="nav-icon">&#128276;</div>
          <h3>Notifications</h3>
          <p>View alerts, timer notifications, and system messages</p>
        </router-link>
      </div>

      <!-- Parent/Coach Section -->
      <div v-if="auth.isParentCoach || auth.isAdmin" class="coach-section">
        <h2>Parent / Coach</h2>
        <div class="nav-grid">
          <router-link to="/review" class="nav-card coach">
            <div class="nav-icon">&#128269;</div>
            <h3>Student Review</h3>
            <p>Review assigned students' notebooks, attempts, and cooking history</p>
          </router-link>
        </div>
      </div>

      <!-- Admin Section -->
      <div v-if="auth.isAdmin" class="admin-section">
        <h2>Administration</h2>
        <div class="nav-grid admin-grid">
          <router-link to="/admin/users" class="nav-card admin">
            <div class="nav-icon">&#128101;</div>
            <h3>User Management</h3>
            <p>Create, enable, and disable user accounts</p>
          </router-link>

          <router-link to="/admin/observability" class="nav-card admin">
            <div class="nav-icon">&#128200;</div>
            <h3>Observability</h3>
            <p>Monitor jobs, alerts, capacity, and KPI metrics</p>
          </router-link>

          <router-link to="/admin/tips" class="nav-card admin">
            <div class="nav-icon">&#128161;</div>
            <h3>Tip Card Configuration</h3>
            <p>Enable, disable, and configure tip card display modes</p>
          </router-link>

          <router-link to="/admin/reconciliation" class="nav-card admin">
            <div class="nav-icon">&#128202;</div>
            <h3>Reconciliation Exports</h3>
            <p>Run daily transaction reconciliation and view export history</p>
          </router-link>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()

async function handleLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.dashboard { min-height: 100vh; background: #f5f5f5; }

.dashboard-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 2rem;
  background: white;
  box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}

.dashboard-header h1 { font-size: 1.3rem; color: #333; }

.header-actions {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.username { color: #666; font-weight: 500; }

.nav-link {
  color: #667eea;
  text-decoration: none;
  font-size: 0.9rem;
}

.logout-btn {
  padding: 0.4rem 1rem;
  background: #eee;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.9rem;
}

.logout-btn:hover { background: #ddd; }

.dashboard-content { padding: 2rem; max-width: 1000px; margin: 0 auto; }

.welcome-card {
  background: white;
  padding: 2rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  margin-bottom: 2rem;
}

.welcome-card h2 { margin-bottom: 0.5rem; }
.welcome-card p { color: #666; }

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1rem;
  margin: 1.5rem 0;
}

.info-item { padding: 1rem; background: #f8f9fa; border-radius: 6px; }
.info-item strong { display: block; color: #333; margin-bottom: 0.3rem; }
.info-item span { color: #666; font-size: 0.9rem; }

.status-badge.active {
  color: #28a745;
  font-weight: 500;
}

.warning-banner {
  background: #fff3cd;
  color: #856404;
  padding: 1rem;
  border-radius: 6px;
  margin: 1rem 0;
}

.warning-banner a { color: #533f03; font-weight: 600; }

/* Navigation Grid */
.nav-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 1rem;
  margin-bottom: 2rem;
}

.nav-card {
  background: white;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
  text-decoration: none;
  color: inherit;
  transition: box-shadow 0.2s, transform 0.2s;
  border: 2px solid transparent;
}

.nav-card:hover {
  box-shadow: 0 4px 12px rgba(0,0,0,0.12);
  transform: translateY(-2px);
  border-color: #667eea;
}

.nav-card.admin { border-left: 4px solid #667eea; }
.nav-card.admin:hover { border-color: #667eea; }

.nav-icon { font-size: 2rem; margin-bottom: 0.5rem; }

.nav-card h3 {
  font-size: 1.05rem;
  color: #333;
  margin-bottom: 0.3rem;
}

.nav-card p {
  font-size: 0.85rem;
  color: #888;
  margin: 0;
  line-height: 1.4;
}

/* Coach Section */
.coach-section {
  border-top: 1px solid #ddd;
  padding-top: 1.5rem;
}

.coach-section h2 {
  font-size: 1.1rem;
  color: #555;
  margin-bottom: 1rem;
}

.nav-card.coach { border-left: 4px solid #28a745; }
.nav-card.coach:hover { border-color: #28a745; }

/* Admin Section */
.admin-section {
  border-top: 1px solid #ddd;
  padding-top: 1.5rem;
}

.admin-section h2 {
  font-size: 1.1rem;
  color: #555;
  margin-bottom: 1rem;
}

.admin-grid {
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
}
</style>
