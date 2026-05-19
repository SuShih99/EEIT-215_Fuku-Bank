<template>
  <div class="page-container">
    <div class="page-header">
      <h2 class="page-title">客戶管理</h2>
    </div>

    <div class="action-bar">
      <div class="search-group">
        <a-input
          v-model:value="keyword"
          placeholder="搜尋客戶姓名或身分證..."
          class="rounded-input search-input"
          allow-clear
          @press-enter="handleSearch"
        >
          <template #prefix>
            <SearchOutlined style="color: #bfbfbf"/>
          </template>
        </a-input>
        <a-button type="primary" class="rounded-btn" @click="handleSearch">查詢</a-button>
        <a-button class="rounded-btn btn-ghost" @click="handleClear">清除</a-button>
        <a-button type="primary" class="rounded-btn" @click="goCreate"> 新增客戶</a-button>
      </div>
    </div>

    <a-table
      :columns="columns"
      :data-source="customers"
      :loading="loading"
      :scroll="{ x: 1720 }"
      row-key="customerId"
      class="custom-table"
      :pagination="{ pageSize: 10, showSizeChanger: false }"
      :locale="{ triggerDesc: '點擊降冪排序', triggerAsc: '點擊升冪排序', cancelSort: '取消排序' }"
      @resizeColumn="handleResizeColumn"
    >
      <template #emptyText>
        <div class="customer-empty-state">
          <div class="customer-empty-mark" aria-hidden="true"></div>
          <strong>{{ keyword ? '查無符合條件的客戶' : '目前尚無客戶資料' }}</strong>
          <span>{{ keyword ? '請調整關鍵字後重新查詢。' : '建立客戶後，資料會顯示在這裡。' }}</span>
        </div>
      </template>

      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'name'">
          <div class="emp-name-cell">
            <div class="emp-avatar">{{ firstChar(record.name) }}</div>
            <div class="emp-info">
              <span class="emp-name-text">{{ displayValue(record.name) }}</span>
              <span class="emp-id-text">{{ displayValue(record.customerId) }}</span>
            </div>
          </div>
        </template>
        <template v-else-if="column.key === 'gender'">
          {{ genderMap[record.gender] || displayValue(record.gender) }}
        </template>
        <template v-else-if="column.key === 'currentAddress'">
          <a-tooltip :title="record.currentAddress || record.address || '-'">
            <span class="truncate-cell">{{ record.currentAddress || record.address || '-' }}</span>
          </a-tooltip>
        </template>
        <template v-else-if="column.key === 'occupationInfo'">
          <div class="stack-cell">
            <span class="primary-text">{{ displayValue(record.occupation) }}</span>
            <span class="secondary-text">{{ displayValue(record.employer) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'compliance'">
          <div class="tag-stack">
            <a-tag :color="record.isPep ? 'red' : 'green'">
              PEP {{ record.isPep ? '是' : '否' }}
            </a-tag>
            <span class="secondary-text">{{ displayCountry(record.taxResidency) }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'latestApplication'">
          <div class="stack-cell">
            <span
              :class="[
                'application-status',
                applicationStatusClass(record.latestAccountApplicationStatus),
              ]"
            >
              {{
                applicationStatusMap[record.latestAccountApplicationStatus] ||
                displayValue(record.latestAccountApplicationStatus)
              }}
            </span>
            <span class="secondary-text">{{
                displayValue(record.latestAccountApplicationNo)
              }}</span>
          </div>
        </template>
        <template v-else-if="column.key === 'status'">
          <div :class="['status-tag', `status-${String(record.status || '').toLowerCase()}`]">
            <span class="status-dot"></span>
            {{ statusMap[record.status] || displayValue(record.status) }}
          </div>
        </template>
        <template v-else-if="column.key === 'action'">
          <div class="action-cell">
            <a-button type="link" class="action-btn edit-btn" @click="openEditModal(record)">
              編輯
            </a-button>
            <a-divider type="vertical"/>

            <template v-if="String(record.status || '').toUpperCase() === 'LOCKED'">
              <a-button
                type="link"
                class="action-btn resume-btn"
                style="color: #2f54eb; font-weight: 600"
                @click="handleUnlock(record)"
              >
                解除鎖定
              </a-button>
            </template>

            <template v-else-if="record.status === 'INACTIVE' || record.status === 'DEACTIVATED'">
              <a-button type="link" class="action-btn resume-btn" @click="handleActivate(record)">
                啟用
              </a-button>
            </template>

            <template v-else>
              <a-button
                type="link"
                class="action-btn suspend-btn"
                @click="handleDeactivate(record)"
              >
                停用
              </a-button>
            </template>
          </div>
        </template>
      </template>
      <template #expandedRowRender="{ record }">
        <div class="customer-detail-grid">
          <section class="detail-section">
            <h4>基本與地址</h4>
            <dl>
              <div v-if="String(record.status || '').toUpperCase() === 'LOCKED'">
                <dt>資安狀態</dt>
                <dd>
                  <a-button type="primary" size="small" @click="handleUnlock(record)">
                    解除帳戶鎖定
                  </a-button>
                </dd>
              </div>
              <div>
                <dt>帳戶狀態</dt>
                <dd>{{ statusMap[record.status] || displayValue(record.status) }}</dd>
              </div>
              <div>
                <dt>國籍</dt>
                <dd>{{ displayCountry(record.nationality) }}</dd>
              </div>
              <div>
                <dt>生日</dt>
                <dd>{{ displayValue(record.birthday) }}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{{ displayValue(record.email) }}</dd>
              </div>
              <div>
                <dt>戶籍地址</dt>
                <dd>{{ displayValue(record.registeredAddress) }}</dd>
              </div>
              <div>
                <dt>現居地址</dt>
                <dd>{{ displayValue(record.currentAddress || record.address) }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h4>職業與法遵</h4>
            <dl>
              <div>
                <dt>職業</dt>
                <dd>{{ displayValue(record.occupation) }}</dd>
              </div>
              <div>
                <dt>風控職業</dt>
                <dd>{{ displayValue(record.job) }}</dd>
              </div>
              <div>
                <dt>任職機構</dt>
                <dd>{{ displayValue(record.employer) }}</dd>
              </div>
              <div>
                <dt>年收入</dt>
                <dd>{{ formatAnnualIncome(record.annualIncome) }}</dd>
              </div>
              <div>
                <dt>預估月交易量</dt>
                <dd>{{ formatMonthlyTx(record.estimatedMonthlyTx) }}</dd>
              </div>
              <div>
                <dt>風險等級</dt>
                <dd>{{ displayValue(record.riskLevel) }}</dd>
              </div>
              <div>
                <dt>開戶目的</dt>
                <dd>
                  {{
                    accountPurposeMap[record.accountPurpose] || displayValue(record.accountPurpose)
                  }}
                </dd>
              </div>
              <div>
                <dt>資金來源</dt>
                <dd>{{ fundSourceMap[record.fundSource] || displayValue(record.fundSource) }}</dd>
              </div>
              <div>
                <dt>稅務居民</dt>
                <dd>{{ displayCountry(record.taxResidency) }}</dd>
              </div>
            </dl>
          </section>

          <section class="detail-section">
            <h4>最近開戶申請</h4>
            <dl>
              <div>
                <dt>申請編號</dt>
                <dd>{{ displayValue(record.latestAccountApplicationNo) }}</dd>
              </div>
              <div>
                <dt>申請狀態</dt>
                <dd>
                  {{
                    applicationStatusMap[record.latestAccountApplicationStatus] ||
                    displayValue(record.latestAccountApplicationStatus)
                  }}
                </dd>
              </div>
              <div>
                <dt>帳戶類型</dt>
                <dd>
                  {{
                    accountTypeMap[record.latestAppliedAccountType] ||
                    displayValue(record.latestAppliedAccountType)
                  }}
                </dd>
              </div>
              <div>
                <dt>幣別</dt>
                <dd>{{ displayValue(record.latestAppliedCurrency) }}</dd>
              </div>
              <div>
                <dt>風險標記</dt>
                <dd>
                  {{
                    riskFlagMap[record.latestAccountApplicationRiskFlag] ||
                    displayValue(record.latestAccountApplicationRiskFlag)
                  }}
                </dd>
              </div>
              <div>
                <dt>審核人員</dt>
                <dd>{{ displayValue(record.latestAccountApplicationReviewedBy) }}</dd>
              </div>
              <div>
                <dt>審核時間</dt>
                <dd>{{ displayValue(record.latestAccountApplicationReviewedAt) }}</dd>
              </div>
              <div>
                <dt>駁回/補件原因</dt>
                <dd>{{ displayValue(record.latestAccountApplicationRejectReason) }}</dd>
              </div>
              <div>
                <dt>建立帳號</dt>
                <dd>{{ displayValue(record.createdAccountNumber) }}</dd>
              </div>
              <div>
                <dt>同步時間</dt>
                <dd>{{ displayValue(record.accountApplicationSyncedAt) }}</dd>
              </div>
            </dl>
          </section>
        </div>
      </template>
    </a-table>

    <!-- 編輯 Modal -->
    <a-modal
      v-model:open="showEditModal"
      title="編輯客戶資料"
      :width="920"
      @ok="handleSubmitEdit"
      :confirm-loading="submitLoading"
      @cancel="resetEditForm"
      ok-text="儲存變更"
      cancel-text="取消"
    >
      <a-form layout="vertical" class="customer-edit-form">
        <section class="edit-section">
          <h4>識別資料</h4>
          <div class="edit-grid">
            <a-form-item label="Customer ID">
              <a-input :value="editForm.customerId" disabled/>
            </a-form-item>
            <a-form-item label="CIF">
              <a-input :value="editForm.cif" disabled/>
            </a-form-item>
            <a-form-item label="身分證字號">
              <a-input v-model:value="editForm.idNumber" placeholder="請輸入身分證字號"/>
            </a-form-item>
            <a-form-item label="姓名">
              <a-input v-model:value="editForm.name" placeholder="請輸入姓名"/>
            </a-form-item>
            <a-form-item label="生日">
              <a-date-picker
                v-model:value="editForm.birthday"
                style="width: 100%"
                value-format="YYYY-MM-DD"
                placeholder="請選擇生日"
              />
            </a-form-item>
            <a-form-item label="性別">
              <a-input
                :value="genderMap[editForm.gender] || displayValue(editForm.gender)"
                disabled
              />
            </a-form-item>
            <a-form-item label="顧客狀態">
              <a-select v-model:value="editForm.status" placeholder="請選擇狀態">
                <a-select-option value="ACTIVE">正常</a-select-option>
                <a-select-option value="INACTIVE">停用</a-select-option>
                <a-select-option value="DEACTIVATED">已停用</a-select-option>
                <a-select-option value="PENDING">待審核</a-select-option>
                <a-select-option value="FROZEN">凍結</a-select-option>
                <a-select-option value="LOCKED" disabled>資安鎖定 (請透過列表解鎖)</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="大頭照 URL">
              <a-input v-model:value="editForm.avatarUrl" placeholder="請輸入大頭照 URL"/>
            </a-form-item>
          </div>
        </section>

        <section class="edit-section">
          <h4>聯絡與地址</h4>
          <div class="edit-grid">
            <a-form-item label="Email">
              <a-input v-model:value="editForm.email" placeholder="請輸入 Email"/>
            </a-form-item>
            <a-form-item label="電話">
              <a-input v-model:value="editForm.phone" placeholder="請輸入電話"/>
            </a-form-item>
            <a-form-item label="國籍">
              <a-select v-model:value="editForm.nationality" placeholder="請選擇國籍" allow-clear>
                <a-select-option
                  v-for="option in countryOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="稅務居民">
              <a-select
                v-model:value="editForm.taxResidency"
                placeholder="請選擇稅務居民"
                allow-clear
              >
                <a-select-option
                  v-for="option in countryOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="通訊地址" class="full-width">
              <a-input v-model:value="editForm.address" placeholder="請輸入通訊地址"/>
            </a-form-item>
            <a-form-item label="戶籍地址" class="full-width">
              <a-input v-model:value="editForm.registeredAddress" placeholder="請輸入戶籍地址"/>
            </a-form-item>
            <a-form-item label="現居地址" class="full-width">
              <a-input v-model:value="editForm.currentAddress" placeholder="請輸入現居地址"/>
            </a-form-item>
          </div>
        </section>

        <section class="edit-section">
          <h4>職業與法遵</h4>
          <div class="edit-grid">
            <a-form-item label="職業">
              <a-input v-model:value="editForm.occupation" placeholder="請輸入職業"/>
            </a-form-item>
            <a-form-item label="Job / 風控職業">
              <a-input v-model:value="editForm.job" placeholder="請輸入風控職業欄位"/>
            </a-form-item>
            <a-form-item label="任職機構">
              <a-input v-model:value="editForm.employer" placeholder="請輸入任職機構"/>
            </a-form-item>
            <a-form-item label="預估月交易量（萬元）">
              <a-input-number
                v-model:value="editForm.estimatedMonthlyTx"
                style="width: 100%"
                :min="0"
              />
            </a-form-item>
            <a-form-item label="年收入（萬元）">
              <a-select
                v-model:value="editForm.annualIncome"
                placeholder="請選擇年收入級距"
                allow-clear
              >
                <a-select-option
                  v-for="option in annualIncomeOptions"
                  :key="option.value"
                  :value="option.value"
                >
                  {{ option.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="開戶目的">
              <a-select
                v-model:value="editForm.accountPurpose"
                placeholder="請選擇開戶目的"
                allow-clear
              >
                <a-select-option
                  v-for="(label, value) in accountPurposeMap"
                  :key="value"
                  :value="value"
                >
                  {{ label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="資金來源">
              <a-select
                v-model:value="editForm.fundSource"
                placeholder="請選擇資金來源"
                allow-clear
              >
                <a-select-option
                  v-for="(label, value) in fundSourceMap"
                  :key="value"
                  :value="value"
                >
                  {{ label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="風險等級">
              <a-select v-model:value="editForm.riskLevel" placeholder="請選擇風險等級" allow-clear>
                <a-select-option value="LOW">LOW</a-select-option>
                <a-select-option value="MEDIUM">MEDIUM</a-select-option>
                <a-select-option value="HIGH">HIGH</a-select-option>
              </a-select>
            </a-form-item>
            <a-form-item label="PEP">
              <a-switch
                v-model:checked="editForm.isPep"
                checked-children="是"
                un-checked-children="否"
              />
            </a-form-item>
          </div>
        </section>

        <section class="edit-section">
          <h4>證件與開戶同步資料</h4>
          <div class="document-upload-grid">
            <div v-for="doc in documentFields" :key="doc.field" class="document-upload-card">
              <div class="document-preview">
                <img
                  v-if="documentPreviewUrl(doc.field)"
                  :src="documentPreviewUrl(doc.field)"
                  :alt="doc.label"
                />
                <div v-else class="document-placeholder">
                  <span></span>
                </div>
              </div>
              <div class="document-meta">
                <strong>{{ doc.label }}</strong>
                <small>{{
                    documentFileNames[doc.field] || documentPathLabel(editForm[doc.field])
                  }}</small>
              </div>
              <label class="document-upload-btn">
                選擇檔案
                <input
                  type="file"
                  accept="image/jpeg,image/png"
                  @change="handleDocumentFile(doc.field, $event)"
                />
              </label>
              <a-button
                v-if="editForm[doc.field]"
                type="link"
                size="small"
                @click="openDocument(editForm[doc.field])"
              >
                檢視目前檔案
              </a-button>
            </div>
          </div>

          <div class="edit-grid">
            <a-form-item label="最近申請 ID">
              <a-input :value="displayValue(editForm.latestAccountApplicationId)" disabled/>
            </a-form-item>
            <a-form-item label="最近申請編號">
              <a-input :value="displayValue(editForm.latestAccountApplicationNo)" disabled/>
            </a-form-item>
            <a-form-item label="最近申請狀態">
              <a-input
                :value="
                  applicationStatusMap[editForm.latestAccountApplicationStatus] ||
                  displayValue(editForm.latestAccountApplicationStatus)
                "
                disabled
              />
            </a-form-item>
            <a-form-item label="最近帳戶類型">
              <a-input
                :value="
                  accountTypeMap[editForm.latestAppliedAccountType] ||
                  displayValue(editForm.latestAppliedAccountType)
                "
                disabled
              />
            </a-form-item>
            <a-form-item label="最近申請幣別">
              <a-input :value="displayValue(editForm.latestAppliedCurrency)" disabled/>
            </a-form-item>
            <a-form-item label="最近風險標記">
              <a-input
                :value="
                  riskFlagMap[editForm.latestAccountApplicationRiskFlag] ||
                  displayValue(editForm.latestAccountApplicationRiskFlag)
                "
                disabled
              />
            </a-form-item>
            <a-form-item label="建立帳號">
              <a-input :value="displayValue(editForm.createdAccountNumber)" disabled/>
            </a-form-item>
            <a-form-item label="審核時間">
              <a-input
                :value="displayValue(editForm.latestAccountApplicationReviewedAt)"
                disabled
              />
            </a-form-item>
            <a-form-item label="審核人員">
              <a-input
                :value="displayValue(editForm.latestAccountApplicationReviewedBy)"
                disabled
              />
            </a-form-item>
            <a-form-item label="駁回/補件原因" class="full-width">
              <a-textarea
                :value="displayValue(editForm.latestAccountApplicationRejectReason)"
                disabled
                :rows="2"
              />
            </a-form-item>
            <a-form-item label="同步時間">
              <a-input :value="displayValue(editForm.accountApplicationSyncedAt)" disabled/>
            </a-form-item>
            <a-form-item label="建立時間">
              <a-input :value="displayValue(editForm.createdAt)" disabled/>
            </a-form-item>
            <a-form-item label="更新時間">
              <a-input :value="displayValue(editForm.updatedAt)" disabled/>
            </a-form-item>
          </div>
        </section>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import {ref, reactive, onMounted} from 'vue'
import {useRouter} from 'vue-router'
import {message, Modal} from 'ant-design-vue'
import {SearchOutlined} from '@ant-design/icons-vue'
import {BASE_URL} from '@/api/axios'
import {
  getCustomers,
  updateCustomer,
  deactivateCustomer,
  activateCustomer,
  uploadCustomerDocument,
} from '@/api/customer'
import {unlockCustomer} from "@/api/customerAuth.js";

const router = useRouter()

const statusMap = {
  ACTIVE: '正常',
  DEACTIVATED: '已停用',
  PENDING: '待審核',
  FROZEN: '凍結',
  INACTIVE: '停用',
  LOCKED: '資安鎖定',
}

const genderMap = {
  M: '男',
  F: '女',
}

const countryOptions = [
  {value: 'TW', label: '中華民國（TW）'},
  {value: 'US', label: '美國（US）'},
  {value: 'JP', label: '日本（JP）'},
  {value: 'CN', label: '中國（CN）'},
  {value: 'HK', label: '香港（HK）'},
  {value: 'MO', label: '澳門（MO）'},
  {value: 'KR', label: '韓國（KR）'},
  {value: 'SG', label: '新加坡（SG）'},
  {value: 'MY', label: '馬來西亞（MY）'},
  {value: 'TH', label: '泰國（TH）'},
  {value: 'VN', label: '越南（VN）'},
  {value: 'PH', label: '菲律賓（PH）'},
  {value: 'ID', label: '印尼（ID）'},
  {value: 'AU', label: '澳洲（AU）'},
  {value: 'CA', label: '加拿大（CA）'},
  {value: 'GB', label: '英國（GB）'},
  {value: 'DE', label: '德國（DE）'},
  {value: 'FR', label: '法國（FR）'},
  {value: 'OTHER', label: '其他（OTHER）'},
]

const countryMap = Object.fromEntries(countryOptions.map((option) => [option.value, option.label]))

const annualIncomeOptions = [
  {value: 50, label: '50 萬元以下'},
  {value: 100, label: '51 - 100 萬元'},
  {value: 200, label: '101 - 200 萬元'},
  {value: 500, label: '201 - 500 萬元'},
  {value: 1000, label: '501 - 1000 萬元'},
  {value: 1001, label: '1001 萬元以上'},
]

const annualIncomeMap = Object.fromEntries(
  annualIncomeOptions.map((option) => [option.value, option.label]),
)

const applicationStatusMap = {
  PENDING: '待審核',
  SUPPLEMENT_REQUIRED: '需補件',
  APPROVED: '已核准',
  REJECTED: '已駁回',
  CANCELLED: '已取消',
}

const accountTypeMap = {
  CHECKING: '活期帳戶',
  SAVINGS: '儲蓄帳戶',
  TIME_DEPOSIT: '定期存款',
  LOAN: '貸款帳戶',
  SUB_ACCOUNT: '子帳戶',
}

const accountPurposeMap = {
  SALARY: '薪轉',
  SAVINGS: '儲蓄',
  INVESTMENT: '投資',
  PAYMENT: '支付',
  DAILY_EXPENSE: '日常支出',
  BUSINESS: '商業用途',
  FOREIGN_EXCHANGE: '外匯',
  LOAN: '貸款',
  OTHER: '其他',
}

const fundSourceMap = {
  SALARY: '薪資',
  BUSINESS_INCOME: '營業收入',
  INVESTMENT: '投資收益',
  INHERITANCE: '繼承',
  RETIREMENT: '退休金',
  SAVINGS: '存款',
  OTHER: '其他',
}

const riskFlagMap = {
  NORMAL: '一般',
  WATCH: '觀察',
  PEP: 'PEP',
  HIGH_RISK: '高風險',
  HIGH_FREQUENCY: '高頻申請',
  PEP_HIGH_FREQUENCY: 'PEP + 高頻',
}

const keyword = ref('')
const customers = ref([])
const loading = ref(false)

const columns = ref([
  {
    title: '客戶資訊',
    dataIndex: 'name',
    key: 'name',
    width: 190,
    fixed: 'left',
    resizable: true,
    sorter: (a, b) => (a.name || '').localeCompare(b.name || ''),
  },
  {
    title: 'CIF',
    dataIndex: 'cif',
    key: 'cif',
    width: 125,
    resizable: true,
    sorter: (a, b) => (a.cif || '').localeCompare(b.cif || ''),
  },
  {
    title: '身分證字號',
    dataIndex: 'idNumber',
    key: 'idNumber',
    width: 130,
    resizable: true,
    sorter: (a, b) => (a.idNumber || '').localeCompare(b.idNumber || ''),
  },
  {
    title: '國籍',
    dataIndex: 'nationality',
    key: 'nationality',
    width: 75,
    resizable: true,
    sorter: (a, b) => (a.nationality || '').localeCompare(b.nationality || ''),
  },
  {
    title: '性別',
    dataIndex: 'gender',
    key: 'gender',
    width: 65,
    resizable: true,
    sorter: (a, b) => (a.gender || '').localeCompare(b.gender || ''),
  },
  {
    title: '電話',
    dataIndex: 'phone',
    key: 'phone',
    width: 130,
    resizable: true,
    sorter: (a, b) => (a.phone || '').localeCompare(b.phone || ''),
  },
  {
    title: '現居地址',
    dataIndex: 'currentAddress',
    key: 'currentAddress',
    width: 230,
    resizable: true,
    sorter: (a, b) =>
      (a.currentAddress || a.address || '').localeCompare(b.currentAddress || b.address || ''),
  },
  {
    title: '職業/任職機構',
    key: 'occupationInfo',
    width: 180,
    resizable: true,
    sorter: (a, b) => (a.occupation || '').localeCompare(b.occupation || ''),
  },
  {
    title: '法遵',
    key: 'compliance',
    width: 115,
    resizable: true,
    sorter: (a, b) => Number(b.isPep || 0) - Number(a.isPep || 0),
  },
  {
    title: '最近開戶申請',
    key: 'latestApplication',
    width: 170,
    resizable: true,
    sorter: (a, b) =>
      (a.latestAccountApplicationStatus || '').localeCompare(
        b.latestAccountApplicationStatus || '',
      ),
  },
  {
    title: '建立帳號',
    dataIndex: 'createdAccountNumber',
    key: 'createdAccountNumber',
    width: 130,
    resizable: true,
    sorter: (a, b) => (a.createdAccountNumber || '').localeCompare(b.createdAccountNumber || ''),
  },
  {
    title: '顧客狀態',
    dataIndex: 'status',
    key: 'status',
    width: 105,
    resizable: true,
    sorter: (a, b) => (a.status || '').localeCompare(b.status || ''),
  },
  {title: '操作', key: 'action', width: 140, fixed: 'right'},
])

function handleResizeColumn(w, col) {
  col.width = w
}

async function fetchData() {
  loading.value = true
  try {
    const res = await getCustomers(keyword.value || undefined)
    customers.value = res.data.data
  } catch (err) {
    message.error(err.response?.data?.message || '查詢失敗')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  fetchData()
}

function handleClear() {
  keyword.value = ''
  customers.value = []
}

function goCreate() {
  router.push({name: 'admin-customers-create'})
}

function displayValue(value) {
  return value === null || value === undefined || value === '' ? '-' : value
}

function firstChar(value) {
  return value ? String(value).charAt(0) : '?'
}

function formatMonthlyTx(value) {
  if (value === null || value === undefined || value === '') return '-'
  return `${value} 萬元`
}

function formatAnnualIncome(value) {
  if (value === null || value === undefined || value === '') return '-'
  return annualIncomeMap[value] || `${value} 萬元`
}

function displayCountry(value) {
  return countryMap[value] || displayValue(value)
}

function normalizeAnnualIncomeOption(value) {
  if (value === null || value === undefined || value === '') return undefined
  const amount = Number(value)
  if (Number.isNaN(amount)) return undefined
  if (amount <= 50) return 50
  if (amount <= 100) return 100
  if (amount <= 200) return 200
  if (amount <= 500) return 500
  if (amount <= 1000) return 1000
  return 1001
}

function applicationStatusClass(status) {
  return String(status || 'none')
    .toLowerCase()
    .replace('_', '-')
}

// ===========================
// 編輯 Modal
// ===========================
const showEditModal = ref(false)
const submitLoading = ref(false)
const editingCustomerId = ref('')

const documentFields = [
  {field: 'idFrontUrl', label: '身分證正面'},
  {field: 'idBackUrl', label: '身分證反面'},
  {field: 'secondIdUrl', label: '第二證件'},
]

const documentFiles = reactive({
  idFrontUrl: null,
  idBackUrl: null,
  secondIdUrl: null,
})

const documentFileNames = reactive({
  idFrontUrl: '',
  idBackUrl: '',
  secondIdUrl: '',
})

const documentPreviewUrls = reactive({
  idFrontUrl: '',
  idBackUrl: '',
  secondIdUrl: '',
})

function createEmptyEditForm() {
  return {
    customerId: '',
    cif: '',
    idNumber: '',
    name: '',
    birthday: null,
    gender: undefined,
    email: '',
    phone: '',
    address: '',
    nationality: '',
    registeredAddress: '',
    currentAddress: '',
    occupation: '',
    employer: '',
    estimatedMonthlyTx: null,
    accountPurpose: undefined,
    fundSource: undefined,
    taxResidency: '',
    isPep: false,
    idFrontUrl: '',
    idBackUrl: '',
    secondIdUrl: '',
    latestAccountApplicationId: null,
    latestAccountApplicationNo: '',
    latestAppliedAccountType: '',
    latestAppliedCurrency: '',
    latestAccountApplicationStatus: '',
    latestAccountApplicationRiskFlag: '',
    latestAccountApplicationReviewedAt: '',
    latestAccountApplicationReviewedBy: '',
    latestAccountApplicationRejectReason: '',
    createdAccountNumber: '',
    accountApplicationSyncedAt: '',
    avatarUrl: '',
    status: 'ACTIVE',
    createdAt: '',
    updatedAt: '',
    job: '',
    annualIncome: null,
    riskLevel: undefined,
  }
}

const editForm = reactive(createEmptyEditForm())

function resetEditForm() {
  Object.assign(editForm, createEmptyEditForm())
  clearDocumentSelections()
  editingCustomerId.value = ''
}

function openEditModal(record) {
  clearDocumentSelections()
  editingCustomerId.value = record.customerId
  Object.assign(editForm, createEmptyEditForm(), {
    customerId: record.customerId || '',
    cif: record.cif || '',
    idNumber: record.idNumber || '',
    name: record.name || '',
    birthday: record.birthday || null,
    gender: record.gender || undefined,
    email: record.email || '',
    phone: record.phone || '',
    address: record.address || '',
    nationality: record.nationality || '',
    registeredAddress: record.registeredAddress || '',
    currentAddress: record.currentAddress || '',
    occupation: record.occupation || '',
    employer: record.employer || '',
    estimatedMonthlyTx: record.estimatedMonthlyTx ?? null,
    accountPurpose: record.accountPurpose || undefined,
    fundSource: record.fundSource || undefined,
    taxResidency: record.taxResidency || '',
    isPep: Boolean(record.isPep),
    idFrontUrl: record.idFrontUrl || '',
    idBackUrl: record.idBackUrl || '',
    secondIdUrl: record.secondIdUrl || '',
    latestAccountApplicationId: record.latestAccountApplicationId ?? null,
    latestAccountApplicationNo: record.latestAccountApplicationNo || '',
    latestAppliedAccountType: record.latestAppliedAccountType || '',
    latestAppliedCurrency: record.latestAppliedCurrency || '',
    latestAccountApplicationStatus: record.latestAccountApplicationStatus || '',
    latestAccountApplicationRiskFlag: record.latestAccountApplicationRiskFlag || '',
    latestAccountApplicationReviewedAt: record.latestAccountApplicationReviewedAt || '',
    latestAccountApplicationReviewedBy: record.latestAccountApplicationReviewedBy || '',
    latestAccountApplicationRejectReason: record.latestAccountApplicationRejectReason || '',
    createdAccountNumber: record.createdAccountNumber || '',
    accountApplicationSyncedAt: record.accountApplicationSyncedAt || '',
    avatarUrl: record.avatarUrl || '',
    status: record.status || 'ACTIVE',
    createdAt: record.createdAt || '',
    updatedAt: record.updatedAt || '',
    job: record.job || '',
    annualIncome: normalizeAnnualIncomeOption(record.annualIncome),
    riskLevel: record.riskLevel || undefined,
  })
  showEditModal.value = true
}

async function handleSubmitEdit() {
  submitLoading.value = true
  try {
    await uploadPendingDocuments()
    await updateCustomer(editingCustomerId.value, {
      idNumber: editForm.idNumber,
      name: editForm.name,
      birthday: editForm.birthday,
      email: editForm.email,
      phone: editForm.phone,
      address: editForm.address,
      nationality: editForm.nationality,
      registeredAddress: editForm.registeredAddress,
      currentAddress: editForm.currentAddress,
      occupation: editForm.occupation,
      employer: editForm.employer,
      estimatedMonthlyTx: editForm.estimatedMonthlyTx,
      accountPurpose: editForm.accountPurpose,
      fundSource: editForm.fundSource,
      taxResidency: editForm.taxResidency,
      isPep: editForm.isPep,
      idFrontUrl: editForm.idFrontUrl,
      idBackUrl: editForm.idBackUrl,
      secondIdUrl: editForm.secondIdUrl,
      avatarUrl: editForm.avatarUrl,
      status: editForm.status,
      job: editForm.job,
      annualIncome: editForm.annualIncome,
      riskLevel: editForm.riskLevel,
    })
    message.success('客戶資料已更新')
    showEditModal.value = false
    resetEditForm()
    await fetchData()
  } catch (err) {
    message.error(err.response?.data?.message || '修改失敗')
  } finally {
    submitLoading.value = false
  }
}

function clearDocumentSelections() {
  documentFields.forEach(({field}) => {
    if (documentPreviewUrls[field]) {
      URL.revokeObjectURL(documentPreviewUrls[field])
    }
    documentFiles[field] = null
    documentFileNames[field] = ''
    documentPreviewUrls[field] = ''
  })
}

function handleDocumentFile(field, event) {
  const file = event.target.files?.[0]
  event.target.value = ''
  if (!file) return

  if (!['image/jpeg', 'image/png'].includes(file.type)) {
    message.error('僅支援 JPG / PNG 格式')
    return
  }

  if (file.size > 5 * 1024 * 1024) {
    message.error('檔案大小不可超過 5MB')
    return
  }

  if (documentPreviewUrls[field]) {
    URL.revokeObjectURL(documentPreviewUrls[field])
  }
  documentFiles[field] = file
  documentFileNames[field] = file.name
  documentPreviewUrls[field] = URL.createObjectURL(file)
}

async function uploadPendingDocuments() {
  for (const {field} of documentFields) {
    const file = documentFiles[field]
    if (!file) continue

    const formData = new FormData()
    formData.append('file', file)
    const result = await uploadCustomerDocument(formData)
    editForm[field] = result.url
  }
}

function resolveDocumentUrl(url) {
  if (!url) return ''
  if (url.startsWith('blob:') || url.startsWith('http')) return url
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}

function documentPreviewUrl(field) {
  return documentPreviewUrls[field] || resolveDocumentUrl(editForm[field])
}

function documentPathLabel(url) {
  if (!url) return '尚未上傳'
  return String(url).split('/').filter(Boolean).pop() || '已上傳'
}

function openDocument(url) {
  const href = resolveDocumentUrl(url)
  if (!href) return
  window.open(href, '_blank', 'noopener,noreferrer')
}

// ===========================
// 停用客戶（帶確認對話框）
// ===========================
function handleDeactivate(record) {
  Modal.confirm({
    title: '確定要停用此客戶嗎？',
    content: `姓名：${record.name}（${record.customerId}），停用後該客戶所有服務將暫停。`,
    okText: '確定停用',
    okType: 'danger',
    cancelText: '取消',
    async onOk() {
      try {
        await deactivateCustomer(record.customerId)
        message.success(`客戶「${record.name}」已停用`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '停用失敗')
      }
    },
  })
}

// ===========================
// 重新啟用客戶
// ===========================
function handleActivate(record) {
  Modal.confirm({
    title: '確定要重新啟用此客戶嗎？',
    content: `姓名：${record.name}（${record.customerId}），解鎖後，客戶即可重新使用原本的密碼嘗試登入，且系統的連續登入失敗計數將重設。`,
    okText: '確定啟用',
    okType: 'primary',
    cancelText: '取消',
    async onOk() {
      try {
        await unlockCustomer(record.customerId)
        message.success(`客戶「${record.name}」已重新啟用`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '啟用失敗')
      }
    },
  })
}

// ===========================
// 解除客戶資安鎖定
// ===========================
function handleUnlock(record) {
  Modal.confirm({
    title: '確定要解除該客戶的資安鎖定嗎？',
    content: `姓名：${record.name}（${record.customerId}）。解鎖後，客戶即可重新使用原本的密碼嘗試登入，且系統的連續登入失敗計數將重設。`,
    okText: '確定解鎖',
    okType: 'primary',
    cancelText: '取消',
    async onOk() {
      try {
        // 呼叫啟用 API 進行解鎖
        await unlockCustomer(record.customerId)
        message.success(`客戶「${record.name}」已成功解除鎖定`)
        await fetchData()
      } catch (err) {
        message.error(err.response?.data?.message || '解鎖失敗')
      }
    },
  })
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.emp-name-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.emp-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background-color: rgba(92, 107, 95, 0.1);
  color: #5c6b5f;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 16px;
  flex-shrink: 0;
}

.emp-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.emp-name-text {
  font-weight: 600;
  color: #1a1a2e;
  font-size: 15px;
}

.emp-id-text {
  font-size: 12px;
  color: #8c8c8c;
  margin-top: 2px;
}

.truncate-cell {
  display: inline-block;
  max-width: 210px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  vertical-align: bottom;
}

.stack-cell {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-width: 0;
}

.primary-text {
  color: #1a1a2e;
  font-weight: 600;
  line-height: 1.35;
}

.secondary-text {
  color: #8c8c8c;
  font-size: 12px;
  line-height: 1.35;
}

.tag-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 4px;
}

.application-status {
  display: inline-flex;
  width: fit-content;
  align-items: center;
  border-radius: 999px;
  padding: 2px 9px;
  font-size: 12px;
  font-weight: 700;
  background: rgba(140, 140, 140, 0.12);
  color: #595959;
}

.application-status.pending {
  background: rgba(250, 140, 22, 0.12);
  color: #d46b08;
}

.application-status.supplement-required {
  background: rgba(250, 173, 20, 0.16);
  color: #ad6800;
}

.application-status.approved {
  background: rgba(82, 196, 26, 0.12);
  color: #389e0d;
}

.application-status.rejected,
.application-status.cancelled {
  background: rgba(255, 77, 79, 0.12);
  color: #cf1322;
}

.customer-empty-state {
  display: flex;
  min-height: 220px;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #6f6a60;
  text-align: center;
}

.customer-empty-state strong {
  color: #2f312b;
  font-size: 18px;
  font-weight: 700;
}

.customer-empty-state span {
  font-size: 13px;
  color: #8a8477;
}

.customer-empty-mark {
  position: relative;
  width: 68px;
  height: 68px;
  border-radius: 22px;
  background: linear-gradient(145deg, rgba(255, 251, 244, 0.96), rgba(242, 233, 216, 0.92));
  border: 1px solid rgba(164, 142, 111, 0.2);
  box-shadow: 0 16px 34px rgba(95, 82, 61, 0.12);
}

.customer-empty-mark::before,
.customer-empty-mark::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 999px;
  background: rgba(111, 102, 85, 0.3);
}

.customer-empty-mark::before {
  top: 18px;
  width: 26px;
  height: 3px;
}

.customer-empty-mark::after {
  top: 30px;
  width: 18px;
  height: 3px;
  box-shadow: 0 10px 0 rgba(111, 102, 85, 0.3);
}

.customer-detail-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  padding: 14px 8px;
}

.detail-section {
  min-width: 0;
}

.detail-section h4 {
  margin: 0 0 10px;
  color: #1a1a2e;
  font-size: 14px;
  font-weight: 700;
}

.detail-section dl {
  display: grid;
  grid-template-columns: minmax(88px, auto) minmax(0, 1fr);
  gap: 8px 12px;
  margin: 0;
}

.detail-section dl > div {
  display: contents;
}

.detail-section dt {
  color: #8c8c8c;
  font-size: 12px;
}

.detail-section dd {
  min-width: 0;
  margin: 0;
  color: #303030;
  overflow-wrap: anywhere;
  font-size: 13px;
}

.status-tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-active {
  background-color: rgba(82, 196, 26, 0.1);
  color: #389e0d;
}

.status-active .status-dot {
  background-color: #52c41a;
}

.status-deactivated,
.status-frozen,
.status-inactive {
  background-color: rgba(255, 77, 79, 0.1);
  color: #d9363e;
}

.status-deactivated .status-dot,
.status-frozen .status-dot,
.status-inactive .status-dot {
  background-color: #ff4d4f;
}

.status-locked {
  background-color: rgba(47, 84, 235, 0.1);
  color: #2f54eb;
}

.status-locked .status-dot {
  background-color: #2f54eb;
}

.status-pending {
  background-color: rgba(250, 140, 22, 0.1);
  color: #fa8c16;
}

.status-pending .status-dot {
  background-color: #fa8c16;
}

.suspend-btn {
  color: #ff4d4f;
}

.suspend-btn:hover {
  color: #d9363e;
  background-color: rgba(255, 77, 79, 0.05);
}

.resume-btn {
  color: #52c41a;
}

.resume-btn:hover {
  color: #389e0d;
  background-color: rgba(82, 196, 26, 0.05);
}

.customer-edit-form {
  max-height: 68vh;
  overflow-y: auto;
  padding-right: 8px;
}

.edit-section {
  padding: 14px 0 4px;
  border-top: 1px solid rgba(92, 107, 95, 0.12);
}

.edit-section:first-child {
  padding-top: 0;
  border-top: 0;
}

.edit-section h4 {
  margin: 0 0 14px;
  color: #1a1a2e;
  font-size: 15px;
  font-weight: 700;
}

.edit-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.edit-grid .full-width {
  grid-column: 1 / -1;
}

.document-upload-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
  margin-bottom: 20px;
}

.document-upload-card {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 12px;
  padding: 16px;
  border: 1px solid rgba(92, 107, 95, 0.14);
  border-radius: 18px;
  background: linear-gradient(180deg, rgba(255, 251, 246, 0.9), rgba(250, 245, 238, 0.82));
}

.document-preview {
  display: flex;
  aspect-ratio: 16 / 10;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px dashed rgba(120, 118, 102, 0.26);
}

.document-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.document-placeholder {
  position: relative;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle at 20% 24%, rgba(197, 180, 150, 0.16), transparent 34%),
  linear-gradient(180deg, rgba(255, 250, 242, 0.9), rgba(245, 237, 226, 0.65));
}

.document-placeholder span {
  position: absolute;
  inset: 0;
  margin: auto;
  width: 42px;
  height: 52px;
  border-radius: 12px;
  border: 1.4px solid rgba(141, 125, 100, 0.4);
  background: rgba(255, 255, 255, 0.82);
}

.document-placeholder span::before,
.document-placeholder span::after {
  content: '';
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
  border-radius: 999px;
  background: rgba(141, 125, 100, 0.35);
}

.document-placeholder span::before {
  top: 14px;
  width: 18px;
  height: 2px;
  box-shadow: 0 8px 0 rgba(141, 125, 100, 0.35);
}

.document-placeholder span::after {
  top: 30px;
  width: 12px;
  height: 2px;
}

.document-meta {
  display: flex;
  min-width: 0;
  flex-direction: column;
  gap: 4px;
}

.document-meta strong {
  color: #28312a;
  font-size: 14px;
  font-weight: 700;
}

.document-meta small {
  overflow: hidden;
  color: #857f72;
  font-size: 12px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.document-upload-btn {
  position: relative;
  display: inline-flex;
  width: fit-content;
  align-items: center;
  justify-content: center;
  padding: 9px 14px;
  border-radius: 999px;
  background: #5c6b5f;
  color: #fffdf7;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.2s ease,
  transform 0.2s ease;
}

.document-upload-btn:hover {
  background: #4f5d52;
  transform: translateY(-1px);
}

.document-upload-btn input {
  display: none;
}

@media (max-width: 1100px) {
  .customer-detail-grid {
    grid-template-columns: 1fr;
  }

  .document-upload-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 760px) {
  .edit-grid {
    grid-template-columns: 1fr;
  }
}
</style>
