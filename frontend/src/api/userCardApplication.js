import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/api/user/card-applications'

//使用者送出申請（前台）
export async function createCardApplication(data) {
  const res = await api.post(BASE_URL, data)
  return unwrap(res)
}
//使用者查詢自己的申請紀錄
export async function getMyApplications(params) {
  const res = await api.get(BASE_URL, { params })
  return unwrap(res)
}
