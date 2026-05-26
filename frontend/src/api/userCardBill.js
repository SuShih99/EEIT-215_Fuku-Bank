import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/user/card-bills'
//查全部
export const getBills = (page=0, size=10) => {
    return api.get(BASE_URL,{
        params:{
            page,
            size
        },
    }).then(unwrap);

};
//查未出帳
export const getUnbilledBills = (page=0, size=10) => {
    return api.get(`${BASE_URL}/unbilled`,{
        params:{
            page,
            size
        },
    }).then(unwrap);

};

//取得出帳交易明細
export const getBilledTransactions = (billId, cardId) => {
  return api.get(`${BASE_URL}/${billId}/transactions`, {
    params: {
      cardId,
    },
  }).then(unwrap)
}