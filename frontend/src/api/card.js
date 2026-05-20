import api from "./axios";
// 建立一個 axios 實例，統一設定後端 API 的基礎 URL
const BASE_URL = '/api/admin/cards'

//查全部
export const getCards = (params) => {
    return api.get(BASE_URL, { params });
}
//查單筆
export const getCardById = (id) => {
    return api.get(`${BASE_URL}/${id}`);
}
//新增
export const createCard = (data) => {
    return api.post(BASE_URL, data);
}
//更新
export const updateCard = (id, data) => {
    return api.put(`${BASE_URL}/${id}`, data);
}
//刪除
export const deleteCard = (id) => {
    return api.delete(`${BASE_URL}/${id}`);
}
//停用
export const blockCard = (id) => {
    return api.patch(`${BASE_URL}/${id}/block`);
}
//啟用
export const unblockCard = (id) => {
    return api.patch(`${BASE_URL}/${id}/unblock`);
}

