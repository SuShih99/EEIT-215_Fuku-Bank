import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個URL
const CARD_URL = '/user/cards/my-cards'

//查持有卡片
export const getMyCards = () => {
    return api.get(CARD_URL).then(unwrap);
}
//開卡
export const activateCard = (cardId) => {
    return api.patch(`/user/cards/${cardId}/activate`).then(unwrap);
}