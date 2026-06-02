import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/api/user/card-txns'
//查全部
export const getTransactions = (page=0, size=10) => {
    return api.get(BASE_URL,{
        params:{
            page,
            size
        },
    }).then(unwrap);

};
//新增交易
export const createTransaction = (data) => {
    return api.post(BASE_URL, data).then(unwrap);
}
//取得商店名稱
export const getMerchantNames = () => {
    return api.get(`${BASE_URL}/merchants`).then(unwrap);
}

export const requestLinePay = (data) => {
    return api.post('/api/linepay/request', data).then(unwrap);
}
export const confirmLinePay = (transactionId,orderId) => {
    return api.get('/api/linepay/confirm', {
        params: {
            transactionId,
            orderId
        }
    }).then(unwrap);
}
