import api from "./axios";
//取得全部申請
export function getApplications(params){
    return api.get('/api/admin/card-applications',{params})
}
//取得單一申請
export function getApplicationById(id){
    return api.get(`/api/admin/applications/${id}`)
}
//更新申請狀態(後台)
export function updateApplicationStatus(id, status) {
  return api.put(`/api/admin/card-applications/${id}/status`, {
    status
  })
}
//刪除申請
export function deleteApplication(id){
    return api.delete(`/api/admin/card-applications/${id}`)
}
// 使用者送出申請（前台）
export function createApplication(data) {
  return api.post('/api/card-applications', data)
}
//修改備註
export function updateApplicationRemark(id, remark) {
  return api.put(`/api/admin/card-applications/${id}/remark`, {
    remark:remark
  })
}

