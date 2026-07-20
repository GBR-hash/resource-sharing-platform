<template>
  <div class="monitor-page">
    <div v-if="isDev" class="dev-placeholder">
      <div class="dev-placeholder-inner">
        <el-icon :size="64" color="#484f58"><VideoCamera /></el-icon>
        <h2>系统监控面板</h2>
        <p>监控面板需部署后查看</p>
        <p class="dev-hint">当前为本地开发环境，树莓派监控服务不可用</p>
      </div>
    </div>

    <template v-else>
      <!-- ====== 摄像头实时画面 ====== -->
      <div class="stream-wrap">
        <img ref="streamImg" alt="Camera Stream" style="display:none" />
        <div class="no-cam" ref="noCamEl">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2" ry="2"/><line x1="1" y1="1" x2="23" y2="23"/></svg>
          <span>未接入摄像头</span>
        </div>
        <div class="fps-badge">
          <span class="fps-dot"></span>
          <span class="fps-num" ref="fpsEl">--</span>
          <span class="fps-unit">fps</span>
        </div>
      </div>

      <!-- ====== ECharts 数据面板 ====== -->
      <div class="dashboard">
        <div class="chart-row">
          <div class="chart-card chart-card--cpu">
            <div class="chart-card__header">
              <el-icon :size="16"><Cpu /></el-icon>
              <span>CPU 使用率</span>
              <span class="chart-card__unit">{{ stats.cpu_percent }}%</span>
            </div>
            <div ref="cpuChartRef" class="chart-card__body"></div>
          </div>
          <div class="chart-card chart-card--temp" :class="{ 'chart-card--alert': stats.cpu_temp >= 50 }">
            <div class="chart-card__header">
              <el-icon :size="16"><Sunny /></el-icon>
              <span>CPU 温度</span>
              <span class="chart-card__unit">{{ stats.cpu_temp }}°C</span>
              <span v-if="stats.cpu_temp >= 50" class="temp-alert-badge">
                <el-icon :size="14"><WarningFilled /></el-icon>高温预警
              </span>
            </div>
            <div ref="tempChartRef" class="chart-card__body"></div>
          </div>
        </div>

        <div class="chart-row chart-row--triple">
          <div class="chart-card chart-card--mem">
            <div class="chart-card__header">
              <el-icon :size="16"><Odometer /></el-icon>
              <span>内存使用</span>
              <span class="chart-card__unit">{{ stats.memory_percent }}%</span>
            </div>
            <div ref="memChartRef" class="chart-card__body"></div>
            <div class="mem-detail">{{ stats.memory_used_gb }} / {{ stats.memory_total_gb }} GB</div>
          </div>
          <div class="chart-card chart-card--disk">
            <div class="chart-card__header">
              <el-icon :size="16"><Coin /></el-icon>
              <span>磁盘使用</span>
            </div>
            <div ref="diskChartRef" class="chart-card__body"></div>
          </div>
          <div class="chart-card chart-card--uptime">
            <div class="chart-card__header">
              <el-icon :size="16"><Timer /></el-icon>
              <span>运行时长</span>
            </div>
            <div class="uptime-display">
              <div class="uptime-segment">
                <span class="uptime-segment__num">{{ uptimeParts.days }}</span>
                <span class="uptime-segment__label">天</span>
              </div>
              <span class="uptime-colon">:</span>
              <div class="uptime-segment">
                <span class="uptime-segment__num">{{ uptimeParts.hours }}</span>
                <span class="uptime-segment__label">时</span>
              </div>
              <span class="uptime-colon">:</span>
              <div class="uptime-segment">
                <span class="uptime-segment__num">{{ uptimeParts.minutes }}</span>
                <span class="uptime-segment__label">分</span>
              </div>
            </div>
            <div class="uptime-sub">自上次启动以来</div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onActivated, onDeactivated, onBeforeUnmount, computed, nextTick } from 'vue'
import { Cpu, Sunny, Odometer, Coin, Timer, VideoCamera, WarningFilled } from '@element-plus/icons-vue'
import * as echarts from 'echarts'

const isDev = import.meta.env.DEV

// ===== ECharts refs =====
const cpuChartRef = ref(null)
const tempChartRef = ref(null)
const memChartRef = ref(null)
const diskChartRef = ref(null)
let cpuChart = null, tempChart = null, memChart = null, diskChart = null

const stats = reactive({
  cpu_percent: 0, cpu_temp: 0,
  memory_percent: 0, memory_used_gb: 0, memory_total_gb: 0,
  disk_root_percent: 0, disk_nvme_percent: 0,
  uptime_seconds: 0,
})

const tempHistory = ref([])
const MAX_TEMP_POINTS = 360
const TEMP_CACHE_KEY = 'monitor_temp_history'
let statsTimer = null

const uptimeParts = computed(() => {
  const s = stats.uptime_seconds || 0
  if (!s) return { days: '--', hours: '--', minutes: '--' }
  return {
    days: String(Math.floor(s / 86400)).padStart(2, '0'),
    hours: String(Math.floor((s % 86400) / 3600)).padStart(2, '0'),
    minutes: String(Math.floor((s % 3600) / 60)).padStart(2, '0'),
  }
})

// ===== 摄像头逻辑（源自 index-video-only.html） =====
const streamImg = ref(null)
const fpsEl = ref(null)
const noCamEl = ref(null)

const B = '/monitor'
const U = B + '/frame.jpg'
let C = 3, P = 0, R = 0, active = false, camTimer = null
let fpsTimes = []

function tic() {
  const n = performance.now()
  fpsTimes.push(n)
  while (fpsTimes.length && n - fpsTimes[0] > 3000) fpsTimes.shift()
  if (fpsTimes.length > 1 && fpsEl.value) {
    fpsEl.value.textContent = ((fpsTimes.length - 1) / ((n - fpsTimes[0]) / 1000)).toFixed(1)
  }
}

function fir() {
  if (!active || P >= C) return
  P++
  fetch(U + '?_=' + Date.now()).then(r => {
    if (!r.ok) { P--; setTimeout(fir, 50); return null }
    return r.blob()
  }).then(b => {
    if (!b) return
    P--
    const n = performance.now()
    if (n > R) {
      R = n
      const u = URL.createObjectURL(b)
      const img = streamImg.value
      if (!img) return
      const o = img.src
      img.src = u
      if (o && o.startsWith('blob:')) URL.revokeObjectURL(o)
      tic()
    }
    fir()
  }).catch(() => { P--; setTimeout(fir, 50) })
}

function startFetch() {
  if (!active) { active = true; P = 0; for (let i = 0; i < C; i++) fir() }
}

function stopFetch() {
  active = false
}

function checkCam() {
  fetch(B + '/api/camera-status').then(r => r.json()).then(d => {
    if (!noCamEl.value || !streamImg.value) return
    if (d.available) {
      noCamEl.value.classList.add('hidden')
      streamImg.value.style.display = 'block'
      startFetch()
    } else {
      noCamEl.value.classList.remove('hidden')
      streamImg.value.style.display = 'none'
      stopFetch()
      if (fpsEl.value) fpsEl.value.textContent = '--'
    }
  }).catch(() => {})
}

// ===== ECharts 初始化 =====
const darkText = '#8b949e'
const bgColor = '#161b22'

function initCpuChart() {
  if (!cpuChartRef.value) return
  cpuChart = echarts.init(cpuChartRef.value, null, { renderer: 'canvas' })
  cpuChart.setOption({ series: [{ type: 'gauge', startAngle: 210, endAngle: -30, center: ['50%', '54%'], radius: '92%', min: 0, max: 100, splitNumber: 10, axisLine: { lineStyle: { width: 16, color: [[0.3, '#3fb950'], [0.6, '#d29922'], [0.8, '#f0883e'], [1, '#f85149']] } }, pointer: { length: '55%', width: 6, itemStyle: { color: '#e6edf3' } }, axisTick: { length: 8, lineStyle: { color: 'auto', width: 1 } }, splitLine: { length: 18, lineStyle: { color: 'auto', width: 3 } }, axisLabel: { color: darkText, fontSize: 10, distance: 20 }, detail: { valueAnimation: true, fontSize: 30, fontWeight: 'bold', offsetCenter: [0, '56%'], formatter: '{value}%', color: '#e6edf3' }, data: [{ value: 0 }] }] })
}

function initTempChart() {
  if (!tempChartRef.value) return
  tempChart = echarts.init(tempChartRef.value, null, { renderer: 'canvas' })
  const times = []
  for (let i = MAX_TEMP_POINTS - 1; i >= 0; i--) {
    times.push(new Date(Date.now() - i * 3000).toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }))
  }
  tempChart.setOption({
    grid: { left: 48, right: 24, top: 20, bottom: 32 },
    xAxis: { type: 'category', data: times, axisLabel: { color: darkText, fontSize: 9, interval: Math.max(1, Math.floor(times.length / 6) - 1), formatter: v => v.slice(0, 5) }, axisLine: { lineStyle: { color: '#21262d' } }, axisTick: { show: false } },
    yAxis: { type: 'value', name: '°C', nameTextStyle: { color: darkText, fontSize: 10 }, min: 0, max: 100, splitLine: { lineStyle: { color: '#21262d', type: 'dashed' } }, axisLabel: { color: darkText, fontSize: 10, formatter: '{value}°C' } },
    series: [{ type: 'line', data: new Array(MAX_TEMP_POINTS).fill(null), smooth: true, symbol: 'none', lineStyle: { color: '#f0883e', width: 2 }, areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: 'rgba(240,136,62,0.32)' }, { offset: 1, color: 'rgba(240,136,62,0.02)' }]) }, markLine: { silent: true, symbol: 'none', lineStyle: { color: '#f85149', type: 'dashed', width: 1.5 }, label: { color: '#f85149', fontSize: 10, formatter: '50°C 预警线' }, data: [{ yAxis: 50 }] } }],
  })
}

function initMemChart() {
  if (!memChartRef.value) return
  memChart = echarts.init(memChartRef.value, null, { renderer: 'canvas' })
  memChart.setOption({
    series: [
      { type: 'pie', radius: ['62%', '82%'], center: ['50%', '52%'], avoidLabelOverlap: false, itemStyle: { borderRadius: 4, borderColor: bgColor, borderWidth: 3 }, label: { show: false }, emphasis: { scale: false }, data: [{ value: 0, name: '已使用', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#d2a8ff' }, { offset: 1, color: '#8957e5' }]) } }, { value: 100, name: '可用', itemStyle: { color: '#21262d' } }] },
      { type: 'pie', radius: ['0%', '58%'], center: ['50%', '52%'], label: { show: false }, silent: true, data: [{ value: 1, itemStyle: { color: 'rgba(210,168,255,0.08)' } }], z: 0 },
    ],
  })
}

function initDiskChart() {
  if (!diskChartRef.value) return
  diskChart = echarts.init(diskChartRef.value, null, { renderer: 'canvas' })
  diskChart.setOption({
    grid: { left: 80, right: 60, top: 10, bottom: 20 },
    xAxis: { type: 'value', min: 0, max: 100, axisLabel: { color: darkText, fontSize: 10, formatter: '{value}%' }, splitLine: { lineStyle: { color: '#21262d' } } },
    yAxis: { type: 'category', data: ['SD 卡', 'NVMe'], axisLabel: { color: '#c9d1d9', fontSize: 12, fontWeight: 600, margin: 12 }, axisLine: { show: false }, axisTick: { show: false } },
    series: [{ type: 'bar', data: [{ value: 0, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#7ee787' }, { offset: 1, color: '#3fb950' }]), borderRadius: [0, 5, 5, 0] } }, { value: 0, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#a5d6ff' }, { offset: 1, color: '#58a6ff' }]), borderRadius: [0, 5, 5, 0] } }], barWidth: 16, label: { show: true, position: 'right', color: '#c9d1d9', fontSize: 12, fontWeight: 600, formatter: '{c}%' }, barGap: '50%' }],
  })
}

// ===== 数据获取 =====
async function fetchStats() {
  try {
    const res = await fetch('/monitor/api/stats')
    if (!res.ok) return
    const data = await res.json()
    Object.assign(stats, data)

    tempHistory.value.push({ time: new Date().toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' }), value: Math.max(0, data.cpu_temp) })
    if (tempHistory.value.length > MAX_TEMP_POINTS) tempHistory.value = tempHistory.value.slice(-MAX_TEMP_POINTS)
  try { sessionStorage.setItem(TEMP_CACHE_KEY, JSON.stringify(tempHistory.value)) } catch {}

    if (cpuChart) cpuChart.setOption({ series: [{ data: [{ value: data.cpu_percent }] }] })

    if (tempChart && tempHistory.value.length > 1) {
      const values = tempHistory.value.map(p => p.value)
      const labels = tempHistory.value.map(p => p.time)
      tempChart.setOption({ xAxis: { data: labels, axisLabel: { interval: Math.max(1, Math.floor(labels.length / 6) - 1), formatter: v => v.slice(0, 5) } }, series: [{ data: values }] })
    }

    if (memChart) {
      memChart.setOption({ series: [{ data: [{ value: data.memory_percent, name: '已使用', itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [{ offset: 0, color: '#d2a8ff' }, { offset: 1, color: '#8957e5' }]) } }, { value: Math.max(0, 100 - data.memory_percent), name: '可用', itemStyle: { color: '#21262d' } }] }] })
    }

    if (diskChart) {
      diskChart.setOption({ series: [{ data: [{ value: data.disk_root_percent, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#7ee787' }, { offset: 1, color: '#3fb950' }]), borderRadius: [0, 5, 5, 0] } }, { value: data.disk_nvme_percent, itemStyle: { color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [{ offset: 0, color: '#a5d6ff' }, { offset: 1, color: '#58a6ff' }]), borderRadius: [0, 5, 5, 0] } }] }] })
    }
  } catch {}
}

// ===== 生命周期 =====
let inited = false

onMounted(() => {
  if (isDev) return

  if (!inited) {
    inited = true
    const cached = sessionStorage.getItem(TEMP_CACHE_KEY)
    if (cached) {
      try {
        const parsed = JSON.parse(cached)
        if (Array.isArray(parsed) && parsed.length > 0) {
          tempHistory.value = parsed
        } else {
          for (let i = 0; i < MAX_TEMP_POINTS; i++) tempHistory.value.push({ time: '', value: null })
        }
      } catch { for (let i = 0; i < MAX_TEMP_POINTS; i++) tempHistory.value.push({ time: '', value: null }) }
    } else {
      for (let i = 0; i < MAX_TEMP_POINTS; i++) tempHistory.value.push({ time: '', value: null })
    }
    initCpuChart()
    initTempChart()
    initMemChart()
    initDiskChart()
    fetchStats()
    statsTimer = setInterval(fetchStats, 3000)
  }

  nextTick(() => {
    checkCam()
    camTimer = setInterval(checkCam, 2000)
  })

  const handleResize = () => { cpuChart?.resize(); tempChart?.resize(); memChart?.resize(); diskChart?.resize() }
  window.addEventListener('resize', handleResize)
  onBeforeUnmount(() => window.removeEventListener('resize', handleResize))
})

onActivated(() => {
  if (isDev) return
  nextTick(() => {
    checkCam()
    camTimer = setInterval(checkCam, 2000)
  })
  cpuChart?.resize(); tempChart?.resize(); memChart?.resize(); diskChart?.resize()
})

onDeactivated(() => {
  stopFetch()
  if (camTimer) { clearInterval(camTimer); camTimer = null }
})

onBeforeUnmount(() => {
  stopFetch()
  if (statsTimer) clearInterval(statsTimer)
  if (camTimer) clearInterval(camTimer)
  cpuChart?.dispose(); tempChart?.dispose(); memChart?.dispose(); diskChart?.dispose()
})
</script>

<style scoped>
.monitor-page { min-height: calc(100vh - 60px); background: #0d1117; padding: 12px; }

.dev-placeholder { display: flex; align-items: center; justify-content: center; min-height: calc(100vh - 84px); }
.dev-placeholder-inner { text-align: center; color: #8b949e; }
.dev-placeholder-inner h2 { margin: 16px 0 8px; font-size: 22px; font-weight: 600; color: #c9d1d9; }
.dev-placeholder-inner p { font-size: 14px; margin-bottom: 4px; }
.dev-hint { margin-top: 12px; font-size: 12px; color: #484f58; }

/* ===== 摄像头画面 ===== */
.stream-wrap { width: 100%; max-width: 1200px; margin: 0 auto 14px; background: #000; line-height: 0; position: relative; min-height: 30dvh; border-radius: 8px; overflow: hidden; border: 1px solid #21262d; }
.stream-wrap img { width: 100%; display: block; object-fit: contain; max-height: 55dvh; }
.no-cam { position: absolute; inset: 0; display: flex; flex-direction: column; align-items: center; justify-content: center; background: #161b22; opacity: 1; transition: opacity 0.3s; }
.no-cam.hidden { opacity: 0; pointer-events: none; }
.no-cam svg { width: 40px; height: 40px; color: #484f58; margin-bottom: 8px; }
.no-cam span { font-size: 13px; color: #484f58; }

.fps-badge { position: absolute; top: 6px; left: 6px; display: flex; align-items: center; gap: 4px; background: rgba(0,0,0,0.55); backdrop-filter: blur(4px); -webkit-backdrop-filter: blur(4px); padding: 4px 8px; border-radius: 5px; pointer-events: none; z-index: 2; }
.fps-dot { width: 6px; height: 6px; border-radius: 50%; background: #3fb950; animation: pulse 1s ease-in-out infinite; }
@keyframes pulse { 0%,100% { opacity: 1; } 50% { opacity: 0.3; } }
.fps-num { font-size: 12px; font-weight: 600; color: #e6edf3; font-variant-numeric: tabular-nums; }
.fps-unit { font-size: 10px; color: #8b949e; font-weight: 400; }

/* ===== ECharts 面板 ===== */
.dashboard { max-width: 1200px; margin: 0 auto; }
.chart-row { display: grid; gap: 12px; margin-bottom: 12px; grid-template-columns: 1fr 2fr; }
.chart-row--triple { grid-template-columns: 1fr 1fr 1fr; }

.chart-card { background: #161b22; border: 1px solid #21262d; border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }
.chart-card__header { display: flex; align-items: center; gap: 7px; padding: 10px 14px 4px; font-size: 13px; font-weight: 600; color: #c9d1d9; letter-spacing: 0.3px; flex-shrink: 0; flex-wrap: wrap; }
.chart-card__unit { margin-left: auto; font-size: 18px; font-weight: 700; font-variant-numeric: tabular-nums; }
.chart-card--cpu .chart-card__header .el-icon { color: #f78166; }
.chart-card--cpu .chart-card__unit { color: #f78166; }
.chart-card--temp .chart-card__header .el-icon { color: #f0883e; }
.chart-card--temp .chart-card__unit { color: #f0883e; }
.chart-card--alert { border-color: #f85149; box-shadow: 0 0 16px rgba(248,81,73,0.12); }
.temp-alert-badge { display: inline-flex; align-items: center; gap: 4px; background: rgba(248,81,73,0.15); color: #f85149; font-size: 11px; font-weight: 600; padding: 2px 8px; border-radius: 4px; animation: alert-blink 1s ease-in-out infinite; }
@keyframes alert-blink { 0%,100% { opacity: 1; } 50% { opacity: 0.6; } }
.chart-card--mem .chart-card__header .el-icon { color: #d2a8ff; }
.chart-card--mem .chart-card__unit { color: #d2a8ff; }
.chart-card--disk .chart-card__header .el-icon { color: #7ee787; }
.chart-card--uptime .chart-card__header .el-icon { color: #58a6ff; }
.chart-card__body { flex: 1; min-height: 200px; }
.mem-detail { text-align: center; font-size: 12px; color: #8b949e; padding-bottom: 10px; margin-top: -8px; }

/* ===== 运行时长 ===== */
.uptime-display { display: flex; align-items: center; justify-content: center; gap: 6px; padding: 20px 0 0; }
.uptime-segment { display: flex; flex-direction: column; align-items: center; background: #0d1117; border: 1px solid #21262d; border-radius: 8px; padding: 10px 14px; min-width: 56px; }
.uptime-segment__num { font-size: 32px; font-weight: 700; color: #58a6ff; font-variant-numeric: tabular-nums; line-height: 1; }
.uptime-segment__label { font-size: 11px; color: #8b949e; margin-top: 4px; letter-spacing: 0.5px; }
.uptime-colon { font-size: 28px; font-weight: 300; color: #30363d; }
.uptime-sub { margin-top: 12px; text-align: center; font-size: 11px; color: #484f58; }

@media (max-width: 768px) {
  .monitor-page { padding: 8px; }
  .chart-row { grid-template-columns: 1fr; }
  .chart-row--triple { grid-template-columns: 1fr 1fr; }
  .chart-card__body { min-height: 170px; }
  .uptime-segment { min-width: 44px; padding: 8px 10px; }
  .uptime-segment__num { font-size: 24px; }
  .uptime-colon { font-size: 20px; }
}
</style>


