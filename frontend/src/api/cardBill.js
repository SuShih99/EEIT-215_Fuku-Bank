import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/api/admin/card-bills'
//查全部
export const getBills = (params) => {
  return api.get(BASE_URL, {
    params
  }).then(unwrap)
}

//產生帳單
export const generateBills = () => {
  return api.post(BASE_URL + '/generate').then(unwrap)
}