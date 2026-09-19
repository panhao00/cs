<template>
  <div class="timetable-wrapper">
    <div class="timetable-toolbar" v-if="items.length > 0">
      <el-button size="small" type="primary" plain @click="exportPdf" :loading="exporting">
        <el-icon><Download /></el-icon>
        导出课表
      </el-button>
    </div>
    <div class="timetable-scroll">
      <div class="timetable-grid">
        <!-- Corner cell -->
        <div class="grid-corner"></div>

        <!-- Day headers -->
        <div v-for="day in DAYS" :key="day.key" class="day-header">
          {{ day.label }}
        </div>

        <!-- Time labels + cells (two rows per hour) -->
        <template v-for="(hour, hIdx) in hours" :key="'h' + hour">
          <div
            class="time-label"
            :style="{ gridRow: (hIdx * 2 + 1) + ' / ' + (hIdx * 2 + 3) }"
          >
            {{ String(hour).padStart(2, '0') }}:00
          </div>
          <template v-for="d in 5" :key="'c' + hour + '-' + d">
            <div
              class="grid-cell"
              :style="{ gridColumn: d + 1, gridRow: hIdx * 2 + 1 }"
            ></div>
            <div
              class="grid-cell cell-half"
              :style="{ gridColumn: d + 1, gridRow: hIdx * 2 + 2 }"
            ></div>
          </template>
        </template>

        <!-- Course blocks -->
        <div
          v-for="block in positionedBlocks"
          :key="block.key"
          class="course-block"
          :class="block.classes"
          :style="block.style"
        >
          <el-tooltip
            :content="block.tooltip"
            placement="top"
            :show-after="200"
            popper-class="timetable-tooltip"
          >
            <div class="block-inner">
              <div class="block-course-id">{{ block.item.courseId }}</div>
              <div class="block-title">{{ block.item.courseTitle }}</div>
              <div class="block-meta">
                <span class="block-room">{{ block.item.room }}</span>
                <span class="block-sep" v-if="block.item.room &amp;&amp; block.item.professorName">&middot;</span>
                <span class="block-prof">{{ block.item.professorName }}</span>
              </div>
              <div class="block-capacity" v-if="block.item.enrolledCount != null">
                {{ block.item.enrolledCount }}/{{ block.item.maxCapacity }}
              </div>
            </div>
          </el-tooltip>
          <div v-if="block.hasConflict" class="conflict-badge">
            <el-icon :size="14"><WarningFilled /></el-icon>
          </div>
        </div>
      </div>
    </div>

    <!-- Conflict alert -->
    <el-alert
      v-if="showConflicts &amp;&amp; conflicts.length > 0"
      :title="conflicts.length + ' 个时间冲突'"
      type="error"
      show-icon
      :closable="false"
      class="conflict-alert"
    >
      <div class="conflict-list">
        <div v-for="(c, i) in conflicts" :key="i" class="conflict-item">
          <strong>{{ c.item1.courseId }}</strong> ({{ c.item1.times }})
          &#8596;
          <strong>{{ c.item2.courseId }}</strong> ({{ c.item2.times }})
          <span class="conflict-day">{{ c.day }}</span>
        </div>
      </div>
    </el-alert>
  </div>
</template>

<script setup>
import { computed, watch, ref } from 'vue'
import { WarningFilled, Download } from '@element-plus/icons-vue'

const props = defineProps({
  items: { type: Array, default: function () { return [] } },
  showConflicts: { type: Boolean, default: true }
})

const emit = defineEmits(['conflicts-detected'])

var exporting = ref(false)

async function exportPdf() {
  exporting.value = true
  try {
    var h2c = window.html2canvas
    var jsPDF = window.jspdf && window.jspdf.jsPDF
    if (!h2c || !jsPDF) {
      console.error('PDF libraries not loaded')
      return
    }

    var grid = document.querySelector('.timetable-scroll')
    if (!grid) return

    // Use higher scale for crisp text rendering
    var canvas = await h2c(grid, {
      scale: 3,
      useCORS: true,
      backgroundColor: '#ffffff',
      logging: false,
      allowTaint: true,
      removeContainer: true,
      imageTimeout: 15000
    })

    var imgData = canvas.toDataURL('image/png', 1.0)
    var imgWidth = canvas.width
    var imgHeight = canvas.height

    // A4 landscape for wide timetable
    var pdf = new jsPDF({
      orientation: 'landscape',
      unit: 'mm',
      format: 'a4'
    })

    var pdfWidth = pdf.internal.pageSize.getWidth()
    var pdfHeight = pdf.internal.pageSize.getHeight()

    // Header section
    var headerHeight = 18
    var margin = 12
    var footerHeight = 10
    var availableWidth = pdfWidth - margin * 2
    var availableHeight = pdfHeight - headerHeight - footerHeight - margin * 2

    // Title
    pdf.setFont('helvetica', 'bold')
    pdf.setFontSize(16)
    pdf.setTextColor(30, 41, 59)
    pdf.text('Course Schedule', margin, 12)

    // Subtitle with semester info
    pdf.setFont('helvetica', 'normal')
    pdf.setFontSize(9)
    pdf.setTextColor(100, 116, 139)
    pdf.text('Generated: ' + new Date().toLocaleDateString('zh-CN'), margin, 16)

    // Scale image to fit available area
    var ratio = Math.min(availableWidth / imgWidth, availableHeight / imgHeight)
    var scaledWidth = imgWidth * ratio
    var scaledHeight = imgHeight * ratio

    // Center horizontally
    var x = margin + (availableWidth - scaledWidth) / 2
    var y = headerHeight + margin

    pdf.addImage(imgData, 'PNG', x, y, scaledWidth, scaledHeight)

    // Footer
    pdf.setFontSize(8)
    pdf.setTextColor(150, 150, 150)
    pdf.text('Wylie College - Course Registration System', margin, pdfHeight - 5)
    pdf.text('Page 1 of 1', pdfWidth - margin - 20, pdfHeight - 5)

    pdf.save('\u8bfe\u8868.pdf')
  } catch (e) {
    console.error('PDF export failed:', e)
  } finally {
    exporting.value = false
  }
}

var START_HOUR = 8
var END_HOUR = 17

var DAY_TO_COL = { M: 1, T: 2, W: 3, R: 4, F: 5 }

var DAYS = [
  { key: 'Mon', abbr: 'M', label: '\u5468\u4e00', col: 1 },
  { key: 'Tue', abbr: 'T', label: '\u5468\u4e8c', col: 2 },
  { key: 'Wed', abbr: 'W', label: '\u5468\u4e09', col: 3 },
  { key: 'Thu', abbr: 'R', label: '\u5468\u56db', col: 4 },
  { key: 'Fri', abbr: 'F', label: '\u5468\u4e94', col: 5 }
]

var PALETTE = [
  { bg: '#dbeafe', border: '#3b82f6', text: '#1e40af' },
  { bg: '#dcfce7', border: '#22c55e', text: '#166534' },
  { bg: '#fed7aa', border: '#f97316', text: '#9a3412' },
  { bg: '#e9d5ff', border: '#a855f7', text: '#6b21a8' },
  { bg: '#ccfbf1', border: '#14b8a6', text: '#115e59' },
  { bg: '#fecdd3', border: '#f43f5e', text: '#9f1239' },
  { bg: '#c7d2fe', border: '#6366f1', text: '#3730a3' },
  { bg: '#d9f99d', border: '#84cc16', text: '#3f6212' },
  { bg: '#fde68a', border: '#eab308', text: '#854d0e' },
  { bg: '#a7f3d0', border: '#10b981', text: '#065f46' }
]

function parseDays(days) {
  if (!days) return []
  var result = []
  for (var i = 0; i < days.length; i++) {
    var col = DAY_TO_COL[days[i]]
    if (col != null) result.push(col)
  }
  return result
}

function parseTimes(times) {
  if (!times) return { start: START_HOUR, end: START_HOUR }
  var parts = times.split('-')
  if (parts.length !== 2) return { start: START_HOUR, end: START_HOUR }
  function toHour(s) {
    var p = s.split(':')
    return parseInt(p[0], 10) + (parseInt(p[1], 10) || 0) / 60
  }
  return { start: toHour(parts[0]), end: toHour(parts[1]) }
}

function hashCode(str) {
  var hash = 0
  for (var i = 0; i < str.length; i++) {
    hash = ((hash << 5) - hash + str.charCodeAt(i)) | 0
  }
  return Math.abs(hash)
}

function formatHour(h) {
  var hh = Math.floor(h)
  var mm = Math.round((h - hh) * 60)
  return String(hh).padStart(2, '0') + ':' + String(mm).padStart(2, '0')
}

function statusLabel(status) {
  var m = { SELECTED: '\u5df2\u9009', ENROLLED_IN: '\u5df2\u6ce8\u518c', CANCELLED: '\u5df2\u53d6\u6d88' }
  return m[status] || status
}

var hours = computed(function () {
  var r = []
  for (var h = START_HOUR; h < END_HOUR; h++) r.push(h)
  return r
})

var parsedBlocks = computed(function () {
  var blocks = []
  for (var idx = 0; idx < props.items.length; idx++) {
    var item = props.items[idx]
    var dayCols = parseDays(item.days)
    var tr = parseTimes(item.times)
    var ci = hashCode(item.courseId || '') % PALETTE.length
    var color = PALETTE[ci]
    for (var di = 0; di < dayCols.length; di++) {
      var col = dayCols[di]
      blocks.push({
        key: item.itemId + '-' + item.offeringId + '-' + col,
        item: item,
        col: col,
        startHour: tr.start,
        endHour: tr.end,
        color: color,
        rowStart: (tr.start - START_HOUR) * 2 + 1,
        rowSpan: Math.max(1, Math.round((tr.end - tr.start) * 2))
      })
    }
  }
  return blocks
})

var conflicts = computed(function () {
  if (!props.showConflicts) return []
  var result = []
  var byDay = {}
  var pb = parsedBlocks.value
  for (var i = 0; i < pb.length; i++) {
    var b = pb[i]
    if (!byDay[b.col]) byDay[b.col] = []
    byDay[b.col].push(b)
  }
  var keys = Object.keys(byDay)
  for (var ki = 0; ki < keys.length; ki++) {
    var arr = byDay[keys[ki]]
    for (var i = 0; i < arr.length; i++) {
      for (var j = i + 1; j < arr.length; j++) {
        if (arr[i].startHour < arr[j].endHour && arr[j].startHour < arr[i].endHour) {
          var oStart = Math.max(arr[i].startHour, arr[j].startHour)
          var oEnd = Math.min(arr[i].endHour, arr[j].endHour)
          var dLabel = ''
          for (var d = 0; d < DAYS.length; d++) {
            if (DAYS[d].col === Number(keys[ki])) { dLabel = DAYS[d].label; break }
          }
          result.push({
            item1: arr[i].item,
            item2: arr[j].item,
            day: dLabel,
            overlapStart: formatHour(oStart),
            overlapEnd: formatHour(oEnd)
          })
        }
      }
    }
  }
  return result
})

watch(conflicts, function (val) { emit('conflicts-detected', val) }, { immediate: true })

var conflictingItemIds = computed(function () {
  var ids = new Set()
  for (var i = 0; i < conflicts.value.length; i++) {
    ids.add(conflicts.value[i].item1.itemId)
    ids.add(conflicts.value[i].item2.itemId)
  }
  return ids
})

var positionedBlocks = computed(function () {
  return parsedBlocks.value.map(function (block) {
    var isConflict = conflictingItemIds.value.has(block.item.itemId)
    var isAlt = block.item.kind === 'ALTERNATE'
    var isCancel = block.item.status === 'CANCELLED'
    var isSel = block.item.status === 'SELECTED'
    var isEnroll = block.item.status === 'ENROLLED_IN'

    var style = {
      gridColumn: (block.col + 1) + ' / ' + (block.col + 2),
      gridRow: block.rowStart + ' / ' + (block.rowStart + block.rowSpan),
      '--block-bg': block.color.bg,
      '--block-border': block.color.border,
      '--block-text': block.color.text
    }

    var classes = {
      'block-alternate': isAlt,
      'block-cancelled': isCancel,
      'block-selected': isSel,
      'block-enrolled': isEnroll,
      'block-conflict': isConflict
    }

    var tip = block.item.courseId + ' - ' + block.item.courseTitle + '\n'
      + '\u65f6\u95f4: ' + block.item.days + ' ' + block.item.times + '\n'
      + '\u6559\u5ba4: ' + (block.item.room || '-') + '\n'
      + '\u6559\u6388: ' + (block.item.professorName || '-') + '\n'
      + '\u7c7b\u578b: ' + (block.item.kind === 'PRIMARY' ? '\u4e3b\u9009' : '\u5907\u9009') + '\n'
      + '\u72b6\u6001: ' + statusLabel(block.item.status) + '\n'
      + '\u5bb9\u91cf: ' + block.item.enrolledCount + '/' + block.item.maxCapacity

    return {
      key: block.key,
      item: block.item,
      col: block.col,
      style: style,
      classes: classes,
      tooltip: tip,
      hasConflict: isConflict
    }
  })
})
</script>

<style scoped>
.timetable-toolbar {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.timetable-wrapper {
  width: 100%;
}

.timetable-scroll {
  overflow-x: auto;
  -webkit-overflow-scrolling: touch;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-bg-elevated);
}

.timetable-grid {
  display: grid;
  grid-template-columns: 60px repeat(5, 1fr);
  grid-auto-rows: 28px;
  min-width: 580px;
  position: relative;
}

.grid-corner {
  position: sticky;
  left: 0;
  z-index: 3;
  background: var(--color-bg-elevated);
  border-bottom: 2px solid var(--color-border);
  border-right: 1px solid var(--color-border);
}

.day-header {
  position: sticky;
  top: 0;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  height: 40px;
  background: #f8fafc;
  border-bottom: 2px solid var(--color-border);
  border-right: 1px solid var(--color-border-light);
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.day-header:last-child {
  border-right: none;
}

.time-label {
  position: sticky;
  left: 0;
  z-index: 1;
  display: flex;
  align-items: flex-start;
  justify-content: flex-end;
  padding: 2px 8px 0 0;
  background: var(--color-bg-elevated);
  border-right: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border-light);
  font-size: var(--font-size-xs);
  font-family: var(--font-mono);
  color: var(--color-text-muted);
  line-height: 1;
}

.grid-cell {
  border-bottom: 1px solid var(--color-border-light);
  border-right: 1px solid var(--color-border-light);
  background: var(--color-bg-elevated);
  min-height: 28px;
}

.grid-cell.cell-half {
  border-bottom: 1px dashed var(--color-border-light);
}

.course-block {
  position: relative;
  z-index: 1;
  margin: 2px 3px;
  padding: 0;
  border-radius: var(--radius-sm);
  background: var(--block-bg, #dbeafe);
  border-left: 3px solid var(--block-border, #3b82f6);
  color: var(--block-text, #1e40af);
  overflow: hidden;
  cursor: default;
  transition: transform var(--transition-fast), box-shadow var(--transition-fast);
}

.course-block:hover {
  transform: scale(1.02);
  box-shadow: var(--shadow-md);
  z-index: 5;
}

.block-inner {
  padding: 4px 6px;
  height: 100%;
  display: flex;
  flex-direction: column;
  gap: 1px;
  overflow: hidden;
}

.block-course-id {
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-bold);
  line-height: var(--line-height-tight);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.block-title {
  font-size: 10px;
  font-weight: var(--font-weight-medium);
  line-height: var(--line-height-tight);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  opacity: 0.85;
}

.block-meta {
  font-size: 9px;
  line-height: var(--line-height-tight);
  opacity: 0.7;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.block-sep {
  margin: 0 2px;
}

.block-capacity {
  font-size: 9px;
  opacity: 0.6;
  margin-top: auto;
}

.block-alternate {
  opacity: 0.7;
  border-left-style: dashed;
}

.block-cancelled {
  opacity: 0.35;
}

.block-cancelled .block-course-id,
.block-cancelled .block-title {
  text-decoration: line-through;
}

.block-selected {
  opacity: 0.85;
}

.block-enrolled {
  border-left-width: 4px;
  box-shadow: var(--shadow-xs);
}

.block-conflict {
  border-left-color: var(--color-danger) !important;
  box-shadow: 0 0 0 2px var(--color-danger), var(--shadow-sm);
  animation: conflict-pulse 2s ease-in-out infinite;
}

@keyframes conflict-pulse {
  0%, 100% { box-shadow: 0 0 0 2px var(--color-danger), var(--shadow-sm); }
  50% { box-shadow: 0 0 0 3px rgba(220, 38, 38, 0.4), var(--shadow-md); }
}

.conflict-badge {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  background: var(--color-danger);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  line-height: 1;
}

.conflict-alert {
  margin-top: var(--space-3);
}

.conflict-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
}

.conflict-item {
  font-size: var(--font-size-sm);
  line-height: var(--line-height-normal);
  color: var(--color-text-secondary);
}

.conflict-day {
  display: inline-block;
  margin-left: var(--space-2);
  padding: 0 6px;
  border-radius: var(--radius-sm);
  background: var(--color-danger-bg);
  color: var(--color-danger);
  font-size: var(--font-size-xs);
  font-weight: var(--font-weight-medium);
}

@media (max-width: 768px) {
  .timetable-grid {
    grid-template-columns: 44px repeat(5, 1fr);
    grid-auto-rows: 22px;
    min-width: 400px;
  }

  .time-label {
    font-size: 10px;
    padding: 1px 4px 0 0;
  }

  .day-header {
    font-size: 11px;
    height: 32px;
  }

  .grid-cell {
    min-height: 22px;
  }

  .block-inner {
    padding: 2px 4px;
    gap: 0;
  }

  .block-course-id {
    font-size: 10px;
  }

  .block-title {
    font-size: 9px;
  }

  .block-meta,
  .block-capacity {
    display: none;
  }
}
</style>
