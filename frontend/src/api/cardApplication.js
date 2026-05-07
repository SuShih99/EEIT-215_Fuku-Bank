import api from "./axios";
// 共用解包 function
const unwrap = (res) => res.data.data

//取得全部申請
export async function getApplications(params){
    const res = await api.get('/api/admin/card-applications', { params })
    return unwrap(res)
}

//取得單一申請
export async function getApplicationById(id){
    const res = await api.get(`/api/admin/card-applications/${id}`)
    return unwrap(res)
}

//更新申請狀態(後台)
export async function updateApplicationStatus(id, status) {
  const res = await api.put(`/api/admin/card-applications/${id}/status`, {
    status
  })
  return unwrap(res)
}

//刪除申請
export async function deleteApplication(id){
    const res = await api.delete(`/api/admin/card-applications/${id}`)
    return unwrap(res)
}

//--使用者功能
//使用者查詢自己的申請紀錄
export async function getMyApplications(params) {
  const res = await api.get('/user/card-applications', { params })
  return unwrap(res)
}

// 使用者送出申請（前台）
export async function createApplication(data) {
  const res = await api.post('/user/card-applications', data)
  return unwrap(res)
}


//--申請明細--
//修改備註
export async function updateApplicationRemark(id, remark) {
  const res = await api.put(`/api/admin/card-applications/${id}/remark`, {
    remark
  })
  return unwrap(res)
}

//查詢明細
export async function getApplicationItems(id) {
  const res = await api.get(`/api/admin/card-application-items/${id}`)
  return res.data.data
}
//核准
export async function approveApplicationItem(id) {
  const res = await api.post(
    `/api/admin/card-application-items/${id}/approve`
  )

  return unwrap(res)
}
//拒絕
export async function rejectApplicationItem(id, remark) {
  const res = await api.post(
    `/api/admin/card-application-items/${id}/reject`,
    null,
    {
      params: {
        remark
      }
    }
  )

  return unwrap(res)
}
