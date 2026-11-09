<!--
  ============================================================
  待办清单主页面

  这一个文件就串起了整条请求链路：
    用户操作
      → Element Plus 组件（el-table / el-dialog / el-pagination）
      → src/api/task.js 里的方法封装
      → src/api/request.js 的 axios 实例 + 拦截器
      → Vite proxy（开发）或 Nginx proxy_pass（Docker）
      → Spring Boot TaskController
      → TaskServiceImpl（读写 Redis 缓存）
      → MyBatis-Plus BaseMapper → MySQL

  页面上所有数据都来自后端，前端不自行计算统计数字，
  这样才能真实演示「Redis 缓存」与「数据库回源」的差别。
  ============================================================
-->
<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createTask, deleteTask, getStats, pageTasks, toggleTask, updateTask } from '../api/task'

/* ==================== 一、列表状态 ==================== */

/** 表格是否正在加载，绑定到 v-loading 显示遮罩 */
const loading = ref(false)

/** 当前页的任务数据（后端分页结果里的 records） */
const list = ref([])

/** 满足筛选条件的总条数，供分页组件计算页数 */
const total = ref(0)

/**
 * 查询条件：字段与后端 dto/TaskQuery 一一对应。
 * 用 reactive 可以让 query.keyword = 'xxx' 这种直接赋值也具备响应式。
 */
const query = reactive({
  pageNum: 1,
  pageSize: 5,
  keyword: '',
  finished: null // null=全部，0=未完成，1=已完成
})

/**
 * 统计卡片数据：字段与后端 dto/TaskStats 一一对应。
 * fromCache / ttl 是专门为演示 Redis 缓存加的，界面上会显示命中状态。
 */
const stats = reactive({
  total: 0,
  finished: 0,
  unfinished: 0,
  fromCache: false,
  ttl: 0
})

/* ==================== 二、弹窗表单状态 ==================== */

/** 弹窗控制对象：visible 控制显隐，saving 控制按钮 loading，id 为 null 表示新增 */
const dialog = reactive({ visible: false, saving: false, id: null })

/** el-form 组件实例，用来调用 validate() 触发校验 */
const formRef = ref()

/** 表单数据：新增和编辑共用同一套字段 */
const form = reactive({
  title: '',
  description: '',
  priority: 2,
  dueDate: null
})

/** 表单校验规则，交给 el-form 自动执行 */
const rules = {
  title: [
    { required: true, message: '请输入任务标题', trigger: 'blur' },
    { max: 100, message: '标题不能超过 100 个字符', trigger: 'blur' }
  ]
}

/** 优先级字典：把数字翻译成中文文案和标签颜色，避免在模板里写 if-else */
const PRIORITY_MAP = {
  1: { text: '低', type: 'info' },
  2: { text: '中', type: 'warning' },
  3: { text: '高', type: 'danger' }
}

/**
 * 根据优先级数字取出展示所需的文案与标签类型。
 *
 * @param {number} priority 优先级 1/2/3
 * @returns {{text: string, type: string}} 用于 el-tag 的文案和颜色类型
 */
function priorityMeta(priority) {
  return PRIORITY_MAP[priority] || PRIORITY_MAP[1]
}

/* ==================== 三、数据加载 ==================== */

/**
 * 加载当前页的任务列表。
 *
 * 把 query 整体作为参数传给后端，后端用 LambdaQueryWrapper 拼 WHERE 条件。
 * 注意 axios 会自动忽略值为 null 的参数，所以 finished=null 时不会出现在 URL 上。
 */
async function loadList() {
  loading.value = true
  try {
    const data = await pageTasks({ ...query })
    list.value = data.records
    total.value = Number(data.total)
  } finally {
    // 无论成功失败都要关闭 loading，否则界面会一直转圈
    loading.value = false
  }
}

/**
 * 加载统计信息。
 *
 * 这个请求的响应里带有 fromCache 和 ttl，
 * 前端据此在工具栏显示「Redis 命中」还是「Redis 回源」。
 */
async function loadStats() {
  const data = await getStats()
  Object.assign(stats, data)
}

/**
 * 同时刷新「列表」和「统计」。
 *
 * 任何写操作之后都必须调用它：因为后端在写操作里已经删除了 Redis 缓存，
 * 重新请求才会拿到最新的数字。
 */
async function loadAll() {
  try {
    await Promise.all([loadList(), loadStats()])
  } catch (e) {
    // 错误提示已由 axios 响应拦截器统一弹出，这里只避免未捕获的 Promise 异常
  }
}

/* ==================== 四、交互动作 ==================== */

/**
 * 点击「查询」按钮或搜索框回车。
 *
 * 必须把页码重置为 1：否则用户在第 3 页搜索时，
 * 结果可能只有 1 页，表格会显示空白，看起来像「搜不到」。
 */
function handleSearch() {
  query.pageNum = 1
  loadList()
}

/**
 * 分页组件的页码改变回调。
 *
 * @param {number} page 目标页码
 */
function onPageChange(page) {
  query.pageNum = page
  loadList()
}

/**
 * 分页组件的每页条数改变回调。
 *
 * @param {number} size 新的每页条数
 */
function onSizeChange(size) {
  query.pageSize = size
  query.pageNum = 1 // 条数变了，页码必须回到第 1 页，否则可能越界
  loadList()
}

/** 打开「新增任务」弹窗，并把表单重置为干净状态 */
function openCreate() {
  dialog.id = null
  Object.assign(form, { title: '', description: '', priority: 2, dueDate: null })
  dialog.visible = true
  // 清掉上一次打开时残留的红色校验提示（?. 防御：弹窗刚打开时组件可能还没挂载）
  formRef.value?.clearValidate()
}

/**
 * 打开「编辑任务」弹窗，用当前行的数据回填表单。
 *
 * @param {object} row 表格中该行的任务对象
 */
function openEdit(row) {
  dialog.id = row.id
  Object.assign(form, {
    title: row.title,
    description: row.description ?? '',
    priority: row.priority ?? 2,
    dueDate: row.dueDate ?? null
  })
  dialog.visible = true
  formRef.value?.clearValidate()
}

/**
 * 保存表单：根据 dialog.id 判断是「新增」还是「编辑」。
 *
 * 流程：表单校验 → 调接口 → 关弹窗 → 刷新列表与统计。
 */
async function handleSave() {
  // validate() 校验失败会 reject，这里捕获并转成 false
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  dialog.saving = true
  try {
    if (dialog.id) {
      await updateTask(dialog.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createTask({ ...form })
      ElMessage.success('新增成功')
    }
    dialog.visible = false
    await loadAll()
  } catch (e) {
    // 失败提示由拦截器统一处理
  } finally {
    dialog.saving = false
  }
}

/**
 * 删除任务：先二次确认，再调用后端的逻辑删除接口。
 *
 * @param {object} row 表格中该行的任务对象
 */
async function handleDelete(row) {
  try {
    await ElMessageBox.confirm(`确定要删除任务「${row.title}」吗？`, '删除确认', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消'
    })
  } catch {
    return // 用户点了「取消」，ElMessageBox 会 reject，这里直接返回
  }

  try {
    await deleteTask(row.id)
    ElMessage.success('删除成功')

    // 如果删掉的是当前页的最后一条，页码会越界导致空白页，需要回退一页
    if (list.value.length === 1 && query.pageNum > 1) {
      query.pageNum -= 1
    }
    await loadAll()
  } catch (e) {
    // 失败提示由拦截器统一处理
  }
}

/**
 * 切换任务的完成状态。
 *
 * 这里用 :model-value 绑定而不是 v-model：
 * 开关的显示状态完全由后端返回的 row.finished 决定，
 * 用户点击后先调接口，成功再重新拉列表，避免前端状态与数据库不一致。
 *
 * @param {object} row 表格中该行的任务对象
 */
async function handleToggle(row) {
  try {
    await toggleTask(row.id)
    await loadAll()
  } catch (e) {
    // 失败提示由拦截器统一处理
  }
}

/* ==================== 五、初始化 ==================== */

// 组件挂载完成后立刻加载第一页数据
onMounted(loadAll)
</script>

<template>
  <div class="todo-page">
    <!-- ① 统计卡片：数据来自 GET /api/tasks/stats，可能命中 Redis 缓存 -->
    <el-row :gutter="16" class="stat-row">
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">任务总数</div>
          <div class="stat-value">{{ stats.total }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">未完成</div>
          <div class="stat-value is-warning">{{ stats.unfinished }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">已完成</div>
          <div class="stat-value is-success">{{ stats.finished }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- ② 主卡片：工具栏 + 表格 + 分页 -->
    <el-card shadow="never" class="main-card">
      <div class="toolbar">
        <div class="toolbar-left">
          <el-input
            v-model="query.keyword"
            placeholder="搜索任务标题"
            clearable
            style="width: 240px"
            @keyup.enter="handleSearch"
            @clear="handleSearch"
          />
          <el-select
            v-model="query.finished"
            placeholder="全部状态"
            clearable
            style="width: 140px"
            @change="handleSearch"
          >
            <el-option label="未完成" :value="0" />
            <el-option label="已完成" :value="1" />
          </el-select>
          <el-button type="primary" @click="handleSearch">查询</el-button>
        </div>

        <div class="toolbar-right">
          <!-- Redis 缓存状态：命中为绿色，回源为橙色，演示时一眼可见 -->
          <el-tag :type="stats.fromCache ? 'success' : 'warning'" effect="plain">
            {{ stats.fromCache ? `Redis 命中 · 剩余 ${stats.ttl}s` : 'Redis 回源（已写入缓存 60s）' }}
          </el-tag>
          <el-button type="primary" @click="openCreate">＋ 新增任务</el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="list"
        row-key="id"
        stripe
        style="width: 100%"
      >
        <el-table-column prop="id" label="ID" width="70" align="center" />

        <el-table-column label="任务标题" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">
            <!-- 已完成的任务加删除线并置灰，视觉上更好区分 -->
            <span :class="{ 'is-done': row.finished === 1 }">{{ row.title }}</span>
          </template>
        </el-table-column>

        <el-table-column label="优先级" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="priorityMeta(row.priority).type" size="small">
              {{ priorityMeta(row.priority).text }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="截止日期" width="120" align="center">
          <template #default="{ row }">{{ row.dueDate || '—' }}</template>
        </el-table-column>

        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.finished === 1" @change="handleToggle(row)" />
          </template>
        </el-table-column>

        <el-table-column prop="updateTime" label="更新时间" width="170" />

        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!--
        分页组件：:total 告诉它总条数，它据此算出总页数。
        @current-change 换页、@size-change 换每页条数，两者都会重新请求后端。
      -->
      <el-pagination
        class="pager"
        background
        layout="total, sizes, prev, pager, next"
        :total="total"
        :page-size="query.pageSize"
        :current-page="query.pageNum"
        :page-sizes="[5, 10, 20]"
        @current-change="onPageChange"
        @size-change="onSizeChange"
      />
    </el-card>

    <!-- ③ 新增 / 编辑共用弹窗：dialog.id 为空表示新增 -->
    <el-dialog
      v-model="dialog.visible"
      :title="dialog.id ? '编辑任务' : '新增任务'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input
            v-model="form.title"
            maxlength="100"
            show-word-limit
            placeholder="请输入任务标题"
          />
        </el-form-item>

        <el-form-item label="优先级">
          <el-radio-group v-model="form.priority">
            <el-radio :value="1">低</el-radio>
            <el-radio :value="2">中</el-radio>
            <el-radio :value="3">高</el-radio>
          </el-radio-group>
        </el-form-item>

        <el-form-item label="截止日期">
          <!-- value-format 让 v-model 直接存 "2026-11-10" 这样的字符串，
               与后端 LocalDate + @JsonFormat 的格式对齐 -->
          <el-date-picker
            v-model="form.dueDate"
            type="date"
            value-format="YYYY-MM-DD"
            placeholder="请选择日期"
            style="width: 100%"
          />
        </el-form-item>

        <el-form-item label="备注">
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
            placeholder="补充说明（可选）"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialog.visible = false">取消</el-button>
        <el-button type="primary" :loading="dialog.saving" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.todo-page {
  max-width: 1200px;
  margin: 0 auto;
}

.stat-row {
  margin-bottom: 16px;
}

.stat-card {
  border-radius: 10px;
}

.stat-label {
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}

.stat-value {
  font-size: 30px;
  font-weight: 600;
  line-height: 1.2;
}

.stat-value.is-warning {
  color: #e6a23c;
}

.stat-value.is-success {
  color: #67c23a;
}

.main-card {
  border-radius: 10px;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-left,
.toolbar-right {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

/* 已完成任务的标题样式：置灰 + 删除线 */
.is-done {
  color: #a8abb2;
  text-decoration: line-through;
}

.pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
