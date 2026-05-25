<template>
  <div class="create-page">
    <!-- 頁頭工具列 -->
    <div class="create-toolbar">
      <a-button class="back-btn" @click="router.push({ name: 'admin-employees' })">
        返回員工列表
      </a-button>
      <a-button type="primary" class="demo-btn" @click="fillNextDemo">
        一鍵帶入
      </a-button>
    </div>

    <!-- 主要卡片 -->
    <div class="create-card">
      <!-- 表單 -->
      <a-form
        ref="formRef"
        :model="form"
        layout="vertical"
        class="create-form"
      >
        <div class="form-grid">
          <!-- 員工編號 -->
          <a-form-item
            label="員工編號"
            name="empId"
            :rules="[{ required: true, message: '請輸入員工編號' }]"
          >
            <a-input v-model:value="form.empId" placeholder="如：E26101" class="form-input" />
          </a-form-item>

          <!-- 姓名 -->
          <a-form-item
            label="姓名"
            name="empName"
            :rules="[{ required: true, message: '請輸入姓名' }]"
          >
            <a-input v-model:value="form.empName" placeholder="請輸入中文姓名" class="form-input" />
          </a-form-item>

          <!-- 部門 -->
          <a-form-item
            label="部門"
            name="deptId"
            :rules="[{ required: true, message: '請選擇部門' }]"
          >
            <a-select
              v-model:value="form.deptId"
              placeholder="請選擇部門"
              @change="handleDeptChange"
              class="form-input"
            >
              <a-select-option value="DPT001">DPT001 消費金融部</a-select-option>
              <a-select-option value="DPT002">DPT002 客戶服務部</a-select-option>
              <a-select-option value="DPT003">DPT003 授信審查部</a-select-option>
              <a-select-option value="DPT004">DPT004 營運企劃部</a-select-option>
              <a-select-option value="DPT005">DPT005 資訊安全部</a-select-option>
            </a-select>
          </a-form-item>

          <!-- 角色 -->
          <a-form-item
            label="角色"
            name="roleId"
            :rules="[{ required: true, message: '請選擇角色' }]"
          >
            <a-select
              v-model:value="form.roleId"
              :placeholder="form.deptId ? '請選擇角色' : '請先選擇部門'"
              :disabled="!form.deptId"
              class="form-input"
            >
              <a-select-option v-for="r in filteredRoles" :key="r.id" :value="r.id">
                {{ r.id }} · {{ r.code }} · {{ r.name }}
              </a-select-option>
            </a-select>
          </a-form-item>

          <!-- Email -->
          <a-form-item
            label="Email"
            name="email"
            :rules="[
              { required: true, message: '請輸入 Email' },
              { type: 'email', message: '請輸入正確 Email 格式' }
            ]"
          >
            <a-input v-model:value="form.email" placeholder="name@javabank.com" class="form-input" />
          </a-form-item>

          <!-- 密碼 -->
          <a-form-item
            label="初始密碼"
            name="password"
            :rules="[{ required: true, message: '請輸入密碼' }]"
          >
            <a-input-password v-model:value="form.password" placeholder="預設 123456" class="form-input" />
          </a-form-item>

          <!-- 合約到期日 -->
          <a-form-item label="合約到期日" name="contractEndDate">
            <a-date-picker
              v-model:value="form.contractEndDate"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="請選擇合約到期日"
              class="form-input"
            />
          </a-form-item>

          <!-- 權限到期日 -->
          <a-form-item
            label="權限到期日"
            name="permissionExpire"
            :rules="[{ required: true, message: '請選擇權限到期日' }]"
          >
            <a-date-picker
              v-model:value="form.permissionExpire"
              style="width: 100%"
              value-format="YYYY-MM-DDTHH:mm:ss"
              placeholder="請選擇權限到期日"
              class="form-input"
            />
          </a-form-item>
        </div>

        <!-- 底部操作 -->
        <div class="form-footer">
          <a-button class="cancel-btn" @click="router.push({ name: 'admin-employees' })">
            取消
          </a-button>
          <a-button
            type="primary"
            class="submit-btn"
            :loading="submitLoading"
            @click="handleSubmit"
          >
            確認新增
          </a-button>
        </div>
      </a-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'
import { message } from 'ant-design-vue'
import { createEmployee } from '@/api/auth'

const router = useRouter()
const formRef = ref(null)
const submitLoading = ref(false)
// 10 筆固定輪播示範資料
const demoEmployees = [
  { empId: 'E26101', empName: '周政廷', deptId: 'DPT001', roleId: 'R001', email: 'jason.chou@javabank.com',   password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26102', empName: '許家瑩', deptId: 'DPT002', roleId: 'R003', email: 'karen.hsu@javabank.com',    password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26103', empName: '楊宗翰', deptId: 'DPT003', roleId: 'R005', email: 'harry.yang@javabank.com',   password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26104', empName: '賴怡君', deptId: 'DPT004', roleId: 'R008', email: 'grace.lai@javabank.com',    password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26105', empName: '方建宏', deptId: 'DPT001', roleId: 'R002', email: 'ben.fang@javabank.com',     password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26106', empName: '曾婉茹', deptId: 'DPT002', roleId: 'R004', email: 'wenru.tseng@javabank.com',  password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26107', empName: '廖偉翔', deptId: 'DPT003', roleId: 'R006', email: 'victor.liao@javabank.com',  password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26108', empName: '卓佩樺', deptId: 'DPT004', roleId: 'R009', email: 'peggy.cho@javabank.com',    password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26109', empName: '游志偉', deptId: 'DPT005', roleId: 'R010', email: 'will.yu@javabank.com',      password: '123456', permissionExpire: '2026-12-31T00:00:00' },
  { empId: 'E26110', empName: '施雅萍', deptId: 'DPT001', roleId: 'R001', email: 'pinky.shih@javabank.com',   password: '123456', permissionExpire: '2026-12-31T00:00:00' },
]

const demoIndex = ref(0)

function fillNextDemo() {
  const demo = demoEmployees[demoIndex.value]
  form.empId = demo.empId
  form.empName = demo.empName
  form.deptId = demo.deptId
  form.roleId = demo.roleId
  form.email = demo.email
  form.password = demo.password
  form.contractEndDate = null
  form.permissionExpire = demo.permissionExpire
  demoIndex.value = (demoIndex.value + 1) % demoEmployees.length
}
// 部門 & 角色對應
const allRoles = [
  { id: 'R001', deptId: 'DPT001', code: 'CFSO',    name: '消金業務專員' },
  { id: 'R002', deptId: 'DPT001', code: 'CFDM',    name: '消金部經理' },
  { id: 'R003', deptId: 'DPT002', code: 'CSVO',    name: '客服照會專員' },
  { id: 'R004', deptId: 'DPT002', code: 'CSDM',    name: '客服部經理' },
  { id: 'R005', deptId: 'DPT003', code: 'JCRO',    name: '初階授信審查員' },
  { id: 'R006', deptId: 'DPT003', code: 'CRDM',    name: '授信部經理' },
  { id: 'R007', deptId: 'DPT003', code: 'CRO',     name: '風控長' },
  { id: 'R008', deptId: 'DPT004', code: 'OPS_PA',  name: '營運企劃專員' },
  { id: 'R009', deptId: 'DPT004', code: 'COO',     name: '營運長' },
  { id: 'R010', deptId: 'DPT005', code: 'ISSA',    name: '資安監控分析師' },
  { id: 'R011', deptId: 'DPT005', code: 'CISO',    name: '資安長' },
  { id: 'R012', deptId: 'DPT005', code: 'SYS_STAFF', name: '職員' },
  { id: 'R013', deptId: 'DPT005', code: 'SYS_SUPER', name: '超級管理員' },
]

const filteredRoles = computed(() => {
  if (!form.deptId) return []
  return allRoles.filter(r => r.deptId === form.deptId)
})

function handleDeptChange() {
  form.roleId = undefined
}
// 表單資料
const form = reactive({
  empId: '',
  empName: '',
  deptId: undefined,
  roleId: undefined,
  email: '',
  password: '',
  contractEndDate: null,
  permissionExpire: null,
})

async function handleSubmit() {
  try {
    await formRef.value.validate()
  } catch {
    return
  }

  submitLoading.value = true
  try {
    await createEmployee({
      empId: form.empId,
      empName: form.empName,
      deptId: form.deptId,
      roleId: form.roleId,
      email: form.email,
      password: form.password,
      contractEndDate: form.contractEndDate,
      permissionExpire: form.permissionExpire,
    })
    message.success(`員工「${form.empName}」已成功新增`)
    router.push({ name: 'admin-employees' })
  } catch (err) {
    message.error(err.response?.data?.message || '新增失敗，請確認資料是否重複')
  } finally {
    submitLoading.value = false
  }
}
</script>

<style scoped>
.create-page {
  max-width: 900px;
  margin: 0 auto;
  padding: 32px 24px;
}

/* 頁頭工具列 */
.create-toolbar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 28px;
}

.back-btn {
  border-radius: 10px;
  height: 38px;
  border-color: #d9d9d9;
  color: #555;
}

.back-btn:hover {
  border-color: #5C6B5F;
  color: #5C6B5F;
}

.page-title {
  margin: 0;
  font-size: 24px;
  font-weight: 700;
  color: #1a1a2e;
  line-height: 1.3;
}

.page-subtitle {
  margin: 4px 0 0;
  font-size: 14px;
  color: #8c8c8c;
}

/* 主卡片 */
.create-card {
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 24px rgba(92, 107, 95, 0.08);
  overflow: hidden;
}

/* 一鍵帶入區 */
.demo-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 28px;
  background: linear-gradient(135deg, rgba(92, 107, 95, 0.06) 0%, rgba(92, 107, 95, 0.02) 100%);
  border-bottom: 1px solid rgba(92, 107, 95, 0.1);
}

.demo-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  color: #5C6B5F;
}

.demo-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.demo-counter {
  font-size: 13px;
  color: #8c8c8c;
  background: rgba(92, 107, 95, 0.08);
  padding: 4px 10px;
  border-radius: 20px;
}

.demo-btn {
  border-radius: 10px;
  background-color: #5C6B5F !important;
  border-color: #5C6B5F !important;
  color: #fff !important;
  font-weight: 600;
}

.demo-btn:hover {
  background-color: #4A574D !important;
  border-color: #4A574D !important;
  opacity: 0.9;
}

/* 表單 */
.create-form {
  padding: 32px 28px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.form-input {
  border-radius: 10px !important;
}

/* 底部按鈕 */
.form-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 8px;
  padding-top: 24px;
  border-top: 1px solid #f0f0f0;
}

.cancel-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 24px;
}

.submit-btn {
  border-radius: 10px;
  height: 40px;
  padding: 0 32px;
  background-color: #5C6B5F;
  border-color: #5C6B5F;
  font-weight: 600;
}

.submit-btn:hover {
  background-color: #4A574D !important;
  border-color: #4A574D !important;
}
</style>
