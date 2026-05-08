import api from "./axios";
//共用解包function
const unwrap = (res) => res.data.data
//建立一個
const BASE_URL = '/api/admin/card-bills'
//查全部
export const getBills = (page=0, size=10) => {
    return api.get(BASE_URL,{
        params:{
            page,
            size
        },
    }).then(unwrap);

};