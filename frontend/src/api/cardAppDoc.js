import api from './axios'

const unwrap = (res) => res.data.data

// 後台：查某申請單附件
export async function getApplicationDocuments(applicationId) {
  const res = await api.get(
    `/api/admin/card-application-documents/${applicationId}/documents`
  )
  return unwrap(res)
}

// 後台：刪除附件
export async function deleteApplicationDocument(documentId) {
  const res = await api.delete(
    `/api/admin/card-application-documents/documents/${documentId}`
  )
  return unwrap(res)
}

// 後台：退回補件
export async function needSupplement(applicationId, remark) {
  const res = await api.patch(
    `/api/admin/card-application-documents/${applicationId}/need-supplement`,
    { remark }
  )
  return unwrap(res)
}
