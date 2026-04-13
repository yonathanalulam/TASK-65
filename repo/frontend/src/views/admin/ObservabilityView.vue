<template>
  <div class="observability-page">
    <header class="page-header">
      <h1>Observability Dashboard</h1>
      <div class="header-actions">
        <span v-if="openAlertCount > 0" class="alert-badge">{{ openAlertCount }} open alerts</span>
        <router-link to="/" class="back-link">Back to Dashboard</router-link>
      </div>
    </header>

    <main class="content">
      <div v-if="error" class="error-message">{{ error }}</div>

      <!-- KPI Summary -->
      <div class="card">
        <h2>KPI Summary</h2>
        <div v-if="loadingKpis" class="loading">Loading KPIs...</div>
        <div v-else-if="kpis" class="kpi-grid">
          <div class="kpi-item">
            <span class="kpi-label">Request Throughput</span>
            <span class="kpi-value">{{ kpis.requestThroughput ?? '-' }}</span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">Error Rate</span>
            <span class="kpi-value" :class="{ danger: (kpis.errorRate ?? 0) > 5 }">
              {{ kpis.errorRate != null ? kpis.errorRate + '%' : '-' }}
            </span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">P50 Latency</span>
            <span class="kpi-value">{{ kpis.p50Latency != null ? kpis.p50Latency + 'ms' : '-' }}</span>
          </div>
          <div class="kpi-item">
            <span class="kpi-label">P95 Latency</span>
            <span class="kpi-value" :class="{ warning: (kpis.p95Latency ?? 0) > 1000 }">
              {{ kpis.p95Latency != null ? kpis.p95Latency + 'ms' : '-' }}
            </span>
          </div>
        </div>
        <p v-else class="empty-state">No KPI data available.</p>
      </div>

      <!-- Capacity Report -->
      <div class="card">
        <h2>Capacity Report</h2>
        <div v-if="loadingCapacity" class="loading">Loading capacity data...</div>
        <div v-else-if="capacity" class="capacity-grid">
          <div class="capacity-item">
            <span class="capacity-label">Total Users</span>
            <span class="capacity-value">{{ capacity.totalUsers }}</span>
          </div>
          <div class="capacity-item">
            <span class="capacity-label">Active Sessions</span>
            <span class="capacity-value">{{ capacity.activeSessions }}</span>
          </div>
          <div class="capacity-item">
            <span class="capacity-label">Audio Cache</span>
            <span class="capacity-value">{{ formatBytes(capacity.totalAudioCacheBytes) }}</span>
          </div>
          <div class="capacity-item">
            <span class="capacity-label">Total Transactions</span>
            <span class="capacity-value">{{ capacity.totalTransactions }}</span>
          </div>
          <div class="capacity-item">
            <span class="capacity-label">Pending Notifications</span>
            <span class="capacity-value">{{ capacity.pendingNotifications }}</span>
          </div>
          <div class="capacity-item">
            <span class="capacity-label">Report Time</span>
            <span class="capacity-value small">{{ formatDate(capacity.reportTime) }}</span>
          </div>
        </div>
      </div>

      <!-- Anomaly Alerts -->
      <div class="card">
        <h2>Anomaly Alerts</h2>
        <div v-if="loadingAlerts" class="loading">Loading alerts...</div>
        <div v-else-if="alerts.length" class="alerts-list">
          <div v-for="alert in alerts" :key="alert.id" class="alert-item"
               :class="alert.severity.toLowerCase()">
            <div class="alert-header">
              <span class="alert-type">{{ alert.alertType }}</span>
              <span :class="['severity-badge', alert.severity.toLowerCase()]">{{ alert.severity }}</span>
              <span :class="['status-badge', alert.status.toLowerCase()]">{{ alert.status }}</span>
              <span class="alert-date">{{ formatDate(alert.createdAt) }}</span>
            </div>
            <p class="alert-message">{{ alert.message }}</p>
            <div class="alert-metrics">
              <span>Metric: {{ alert.metricName }}</span>
              <span>Threshold: {{ alert.thresholdValue }}</span>
              <span>Actual: {{ alert.actualValue }}</span>
            </div>
            <div v-if="alert.acknowledgedBy" class="alert-ack">
              Acknowledged by {{ alert.acknowledgedBy }}
              <span v-if="alert.acknowledgedAt"> at {{ formatDate(alert.acknowledgedAt) }}</span>
            </div>
            <div class="alert-actions">
              <button v-if="alert.status === 'OPEN'" @click="handleAcknowledgeAlert(alert.id)"
                      class="action-btn">Acknowledge</button>
              <button v-if="alert.status !== 'RESOLVED'" @click="handleResolveAlert(alert.id)"
                      class="action-btn resolve">Resolve</button>
            </div>
          </div>
        </div>
        <p v-else class="empty-state">No alerts.</p>
      </div>

      <!-- Scheduled Jobs -->
      <div class="card">
        <h2>Scheduled Jobs</h2>
        <div v-if="loadingJobs" class="loading">Loading jobs...</div>
        <table v-else-if="jobs.length">
          <thead>
            <tr>
              <th>Job Name</th>
              <th>Description</th>
              <th>Enabled</th>
              <th>Latest Status</th>
              <th>Latest Run</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in jobs" :key="job.id">
              <td class="job-name">{{ job.jobName }}</td>
              <td>{{ job.description ?? '-' }}</td>
              <td>
                <span :class="job.enabled ? 'status-on' : 'status-off'">
                  {{ job.enabled ? 'Yes' : 'No' }}
                </span>
              </td>
              <td>
                <span v-if="job.latestRunStatus" :class="['run-status', job.latestRunStatus.toLowerCase()]">
                  {{ job.latestRunStatus }}
                </span>
                <span v-else>-</span>
              </td>
              <td>{{ job.latestRunAt ? formatDate(job.latestRunAt) : '-' }}</td>
              <td>
                <button @click="loadJobRuns(job.jobName)" class="small-btn">View Runs</button>
              </td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty-state">No scheduled jobs found.</p>
      </div>

      <!-- Job Runs Detail -->
      <div v-if="selectedJobName" class="card">
        <div class="runs-header">
          <h2>Runs: {{ selectedJobName }}</h2>
          <button @click="selectedJobName = null" class="close-btn">Close</button>
        </div>
        <div v-if="loadingRuns" class="loading">Loading runs...</div>
        <table v-else-if="jobRuns.length">
          <thead>
            <tr>
              <th>Status</th>
              <th>Started</th>
              <th>Ended</th>
              <th>Affected Rows</th>
              <th>Retries</th>
              <th>Error</th>
              <th>Trace ID</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="run in jobRuns" :key="run.id">
              <td>
                <span :class="['run-status', run.status.toLowerCase()]">{{ run.status }}</span>
              </td>
              <td>{{ formatDate(run.startedAt) }}</td>
              <td>{{ run.endedAt ? formatDate(run.endedAt) : '-' }}</td>
              <td>{{ run.affectedRows }}</td>
              <td>{{ run.retryCount }}</td>
              <td class="error-cell">{{ run.errorSummary ?? '-' }}</td>
              <td class="trace-id">{{ run.traceId ?? '-' }}</td>
            </tr>
          </tbody>
        </table>
        <p v-else class="empty-state">No runs found for this job.</p>
      </div>

      <!-- Metrics Trend Charts -->
      <div class="card">
        <h2>Metrics Trends</h2>
        <div v-if="loadingSnapshots" class="loading">Loading metric snapshots...</div>
        <div v-else-if="metricSnapshots.length" class="charts-container">
          <div class="chart-section">
            <h3>Error Rate Trend</h3>
            <div class="chart-area">
              <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="trend-chart">
                <line v-for="(tick, i) in yTicks" :key="'grid-' + i"
                      :x1="chartPadding" :y1="tick.y" :x2="chartWidth - chartPadding" :y2="tick.y"
                      stroke="#eee" stroke-width="1" />
                <text v-for="(tick, i) in yTicks" :key="'label-' + i"
                      :x="chartPadding - 4" :y="tick.y + 4" text-anchor="end"
                      fill="#999" font-size="10">{{ tick.label }}</text>
                <polyline v-if="errorRatePoints.length > 1"
                          :points="errorRatePoints.map(p => p.x + ',' + p.y).join(' ')"
                          fill="none" stroke="#dc3545" stroke-width="2" />
                <circle v-for="(p, i) in errorRatePoints" :key="'err-' + i"
                        :cx="p.x" :cy="p.y" r="3" fill="#dc3545" />
              </svg>
            </div>
          </div>
          <div class="chart-section">
            <h3>Throughput Trend</h3>
            <div class="chart-area">
              <svg :viewBox="`0 0 ${chartWidth} ${chartHeight}`" class="trend-chart">
                <line v-for="(tick, i) in throughputYTicks" :key="'tgrid-' + i"
                      :x1="chartPadding" :y1="tick.y" :x2="chartWidth - chartPadding" :y2="tick.y"
                      stroke="#eee" stroke-width="1" />
                <text v-for="(tick, i) in throughputYTicks" :key="'tlabel-' + i"
                      :x="chartPadding - 4" :y="tick.y + 4" text-anchor="end"
                      fill="#999" font-size="10">{{ tick.label }}</text>
                <polyline v-if="throughputPoints.length > 1"
                          :points="throughputPoints.map(p => p.x + ',' + p.y).join(' ')"
                          fill="none" stroke="#667eea" stroke-width="2" />
                <circle v-for="(p, i) in throughputPoints" :key="'tp-' + i"
                        :cx="p.x" :cy="p.y" r="3" fill="#667eea" />
              </svg>
            </div>
          </div>
        </div>
        <p v-else class="empty-state">No metric snapshot data available for charting.</p>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import client from '@/api/client'
import type { ApiResponse, PageResponse } from '@/types/api'

interface KpiSummary {
  requestThroughput: number | null
  errorRate: number | null
  p50Latency: number | null
  p95Latency: number | null
  windowStart: string | null
  windowEnd: string | null
}

interface CapacityReport {
  totalUsers: number
  activeSessions: number
  totalAudioCacheBytes: number
  totalTransactions: number
  pendingNotifications: number
  reportTime: string
}

interface AnomalyAlert {
  id: number
  alertType: string
  severity: string
  message: string
  metricName: string
  thresholdValue: number
  actualValue: number
  status: string
  acknowledgedBy: string | null
  acknowledgedAt: string | null
  resolvedAt: string | null
  createdAt: string
}

interface ScheduledJob {
  id: number
  jobName: string
  description: string | null
  enabled: boolean
  latestRunStatus: string | null
  latestRunAt: string | null
}

interface JobRun {
  id: number
  jobName: string
  status: string
  startedAt: string
  endedAt: string | null
  affectedRows: number
  affectedFiles: number
  errorSummary: string | null
  retryCount: number
  traceId: string | null
}

const kpis = ref<KpiSummary | null>(null)
const capacity = ref<CapacityReport | null>(null)
const alerts = ref<AnomalyAlert[]>([])
const jobs = ref<ScheduledJob[]>([])
const jobRuns = ref<JobRun[]>([])
const selectedJobName = ref<string | null>(null)
const openAlertCount = ref(0)

const loadingKpis = ref(false)
const loadingCapacity = ref(false)
const loadingAlerts = ref(false)
const loadingJobs = ref(false)
const loadingRuns = ref(false)
const loadingSnapshots = ref(false)
const error = ref<string | null>(null)

interface MetricSnapshot {
  id: number
  metricName: string
  metricValue: number
  recordedAt: string
}

const metricSnapshots = ref<MetricSnapshot[]>([])

const chartWidth = 500
const chartHeight = 200
const chartPadding = 50

function computePoints(data: { value: number }[], maxVal: number) {
  if (data.length === 0) return []
  const usableWidth = chartWidth - chartPadding * 2
  const usableHeight = chartHeight - 40
  const effectiveMax = maxVal > 0 ? maxVal : 1
  return data.map((d, i) => ({
    x: chartPadding + (data.length > 1 ? (i / (data.length - 1)) * usableWidth : usableWidth / 2),
    y: 20 + usableHeight - (d.value / effectiveMax) * usableHeight,
  }))
}

function computeYTicks(maxVal: number) {
  const usableHeight = chartHeight - 40
  const effectiveMax = maxVal > 0 ? maxVal : 1
  const steps = [0, 0.25, 0.5, 0.75, 1]
  return steps.map(s => ({
    y: 20 + usableHeight - s * usableHeight,
    label: (s * effectiveMax).toFixed(1),
  }))
}

const errorRateData = computed(() =>
  metricSnapshots.value
    .filter(s => s.metricName === 'error_rate')
    .map(s => ({ value: s.metricValue }))
)

const throughputData = computed(() =>
  metricSnapshots.value
    .filter(s => s.metricName === 'request_throughput')
    .map(s => ({ value: s.metricValue }))
)

const errorRateMax = computed(() => Math.max(1, ...errorRateData.value.map(d => d.value)))
const throughputMax = computed(() => Math.max(1, ...throughputData.value.map(d => d.value)))

const errorRatePoints = computed(() => computePoints(errorRateData.value, errorRateMax.value))
const throughputPoints = computed(() => computePoints(throughputData.value, throughputMax.value))
const yTicks = computed(() => computeYTicks(errorRateMax.value))
const throughputYTicks = computed(() => computeYTicks(throughputMax.value))

onMounted(async () => {
  await Promise.all([loadKpis(), loadCapacity(), loadAlerts(), loadJobs(), loadAlertCount(), loadMetricSnapshots()])
})

async function loadKpis() {
  loadingKpis.value = true
  try {
    const { data } = await client.get<ApiResponse<KpiSummary>>('/admin/dashboard/kpis')
    if (data.success && data.data) kpis.value = data.data
  } catch (e: any) {
    console.error('Failed to load KPIs', e)
  } finally {
    loadingKpis.value = false
  }
}

async function loadCapacity() {
  loadingCapacity.value = true
  try {
    const { data } = await client.get<ApiResponse<CapacityReport>>('/admin/dashboard/capacity')
    if (data.success && data.data) capacity.value = data.data
  } catch (e: any) {
    console.error('Failed to load capacity', e)
  } finally {
    loadingCapacity.value = false
  }
}

async function loadAlerts() {
  loadingAlerts.value = true
  try {
    const { data } = await client.get<ApiResponse<PageResponse<AnomalyAlert>>>('/admin/dashboard/alerts')
    if (data.success && data.data) alerts.value = data.data.content
  } catch (e: any) {
    console.error('Failed to load alerts', e)
  } finally {
    loadingAlerts.value = false
  }
}

async function loadAlertCount() {
  try {
    const { data } = await client.get<ApiResponse<{ openAlerts: number }>>('/admin/dashboard/alerts/count')
    if (data.success && data.data) openAlertCount.value = data.data.openAlerts
  } catch {
    // Non-blocking
  }
}

async function loadMetricSnapshots() {
  loadingSnapshots.value = true
  try {
    const { data } = await client.get<ApiResponse<MetricSnapshot[]>>('/admin/dashboard/metric-snapshots')
    if (data.success && data.data) metricSnapshots.value = data.data
  } catch (e: any) {
    console.error('Failed to load metric snapshots', e)
  } finally {
    loadingSnapshots.value = false
  }
}

async function loadJobs() {
  loadingJobs.value = true
  try {
    const { data } = await client.get<ApiResponse<ScheduledJob[]>>('/admin/dashboard/jobs')
    if (data.success && data.data) jobs.value = data.data
  } catch (e: any) {
    console.error('Failed to load jobs', e)
  } finally {
    loadingJobs.value = false
  }
}

async function loadJobRuns(jobName: string) {
  selectedJobName.value = jobName
  loadingRuns.value = true
  try {
    const { data } = await client.get<ApiResponse<PageResponse<JobRun>>>(
      `/admin/dashboard/jobs/${encodeURIComponent(jobName)}/runs`
    )
    if (data.success && data.data) jobRuns.value = data.data.content
  } catch (e: any) {
    error.value = e.message ?? 'Failed to load job runs'
  } finally {
    loadingRuns.value = false
  }
}

async function handleAcknowledgeAlert(id: number) {
  try {
    await client.post(`/admin/dashboard/alerts/${id}/acknowledge`)
    await Promise.all([loadAlerts(), loadAlertCount()])
  } catch (e: any) {
    error.value = e.message
  }
}

async function handleResolveAlert(id: number) {
  try {
    await client.post(`/admin/dashboard/alerts/${id}/resolve`)
    await Promise.all([loadAlerts(), loadAlertCount()])
  } catch (e: any) {
    error.value = e.message
  }
}

function formatBytes(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i]
}

function formatDate(iso: string): string {
  return new Date(iso).toLocaleDateString(undefined, {
    month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}
</script>

<style scoped>
.observability-page { min-height: 100vh; background: #f5f5f5; }
.page-header {
  display: flex; justify-content: space-between; align-items: center;
  padding: 1rem 2rem; background: white; box-shadow: 0 1px 3px rgba(0,0,0,0.1);
}
.page-header h1 { font-size: 1.3rem; color: #333; }
.header-actions { display: flex; align-items: center; gap: 1rem; }
.alert-badge {
  padding: 0.25rem 0.75rem; background: #dc3545; color: white;
  border-radius: 12px; font-size: 0.8rem; font-weight: 500;
}
.back-link { color: #667eea; text-decoration: none; }
.content { padding: 2rem; max-width: 1100px; margin: 0 auto; }
.card {
  background: white; padding: 1.5rem; border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.08); margin-bottom: 1.5rem;
}
.card h2 { margin-bottom: 1rem; font-size: 1.1rem; }

/* KPIs */
.kpi-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 1rem; }
.kpi-item { padding: 1rem; background: #f8f9fa; border-radius: 8px; text-align: center; }
.kpi-label { display: block; font-size: 0.8rem; color: #666; margin-bottom: 0.3rem; text-transform: uppercase; }
.kpi-value { font-size: 1.5rem; font-weight: 700; color: #333; }
.kpi-value.danger { color: #dc3545; }
.kpi-value.warning { color: #ffc107; }

/* Capacity */
.capacity-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(160px, 1fr)); gap: 1rem; }
.capacity-item { padding: 1rem; background: #f8f9fa; border-radius: 8px; text-align: center; }
.capacity-label { display: block; font-size: 0.8rem; color: #666; margin-bottom: 0.3rem; }
.capacity-value { font-size: 1.3rem; font-weight: 600; color: #333; }
.capacity-value.small { font-size: 0.85rem; }

/* Alerts */
.alerts-list { display: flex; flex-direction: column; gap: 0.75rem; }
.alert-item {
  padding: 1rem; border-radius: 8px; border-left: 4px solid #ddd;
  background: #fafafa;
}
.alert-item.critical { border-left-color: #dc3545; background: #fff5f5; }
.alert-item.high { border-left-color: #fd7e14; background: #fff8f0; }
.alert-item.medium { border-left-color: #ffc107; background: #fffdf0; }
.alert-item.low { border-left-color: #28a745; background: #f0fff4; }
.alert-header { display: flex; align-items: center; gap: 0.5rem; margin-bottom: 0.5rem; flex-wrap: wrap; }
.alert-type { font-weight: 600; font-size: 0.85rem; }
.severity-badge { padding: 0.1rem 0.4rem; border-radius: 4px; font-size: 0.7rem; text-transform: uppercase; font-weight: 600; }
.severity-badge.critical { background: #dc3545; color: white; }
.severity-badge.high { background: #fd7e14; color: white; }
.severity-badge.medium { background: #ffc107; color: #333; }
.severity-badge.low { background: #28a745; color: white; }
.status-badge { padding: 0.1rem 0.4rem; border-radius: 4px; font-size: 0.7rem; text-transform: uppercase; }
.status-badge.open { background: #cce5ff; color: #004085; }
.status-badge.acknowledged { background: #fff3cd; color: #856404; }
.status-badge.resolved { background: #d4edda; color: #155724; }
.alert-date { margin-left: auto; font-size: 0.8rem; color: #999; }
.alert-message { font-size: 0.9rem; color: #555; margin-bottom: 0.5rem; }
.alert-metrics { display: flex; gap: 1rem; font-size: 0.8rem; color: #666; margin-bottom: 0.5rem; }
.alert-ack { font-size: 0.8rem; color: #667eea; margin-bottom: 0.5rem; }
.alert-actions { display: flex; gap: 0.5rem; }
.action-btn {
  padding: 0.3rem 0.8rem; border: 1px solid #667eea; color: #667eea;
  border-radius: 4px; background: white; cursor: pointer; font-size: 0.85rem;
}
.action-btn:hover { background: #667eea; color: white; }
.action-btn.resolve { border-color: #28a745; color: #28a745; }
.action-btn.resolve:hover { background: #28a745; color: white; }

/* Jobs Table */
table { width: 100%; border-collapse: collapse; }
th, td { padding: 0.6rem; text-align: left; border-bottom: 1px solid #eee; font-size: 0.85rem; }
th { font-weight: 600; color: #555; }
.job-name { font-weight: 500; font-family: monospace; }
.status-on { color: #28a745; font-weight: 500; }
.status-off { color: #dc3545; font-weight: 500; }
.run-status { padding: 0.1rem 0.4rem; border-radius: 4px; font-size: 0.75rem; font-weight: 500; }
.run-status.success, .run-status.completed { background: #d4edda; color: #155724; }
.run-status.failed { background: #f8d7da; color: #721c24; }
.run-status.running { background: #cce5ff; color: #004085; }
.small-btn {
  padding: 0.3rem 0.7rem; background: #667eea; color: white;
  border: none; border-radius: 4px; cursor: pointer; font-size: 0.8rem;
}
.runs-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem; }
.close-btn { padding: 0.3rem 0.8rem; border: 1px solid #ddd; border-radius: 4px; background: white; cursor: pointer; }
.error-cell { max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; color: #dc3545; }
.trace-id { font-family: monospace; font-size: 0.75rem; color: #666; }

/* Metrics Trend Charts */
.charts-container { display: grid; grid-template-columns: 1fr 1fr; gap: 1.5rem; }
.chart-section { background: #f8f9fa; padding: 1rem; border-radius: 8px; }
.chart-section h3 { font-size: 0.95rem; color: #555; margin-bottom: 0.5rem; }
.chart-area { width: 100%; }
.trend-chart { width: 100%; height: auto; }

.loading { text-align: center; padding: 2rem; color: #666; }
.error-message { background: #fee; color: #c00; padding: 0.6rem; border-radius: 6px; margin-bottom: 1rem; }
.empty-state { color: #999; text-align: center; padding: 2rem; }
</style>
