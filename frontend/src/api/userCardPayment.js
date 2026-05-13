import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/user/card-payments'
export const payCard = (data)=>{
    return api.post(BASE_URL, data).then(unwrap);
}
export const getPaymentAccounts = ()=>{
    return api.get(`${BASE_URL}/payment-accounts`).then(unwrap);
}