import api from './axios'

const unwrap = (res) => res.data.data

// 客戶：新增附件
export async function addMyApplicationDocument(applicationId, data) {
  const res = await api.post(
    `/api/user/card-application-documents/${applicationId}/documents`,
    data
  )
  return unwrap(res)
}

// 客戶：查自己的附件
export async function getMyApplicationDocuments(applicationId) {
  const res = await api.get(
    `/api/user/card-application-documents/${applicationId}/documents`
  )
  return unwrap(res)
}

// 客戶：刪自己的附件
export async function deleteMyApplicationDocument(documentId) {
  const res = await api.delete(
    `/api/user/card-application-documents/documents/${documentId}`
  )
  return unwrap(res)
}
