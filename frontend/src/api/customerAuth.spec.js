import { beforeEach, describe, expect, it, vi } from 'vitest'

const api = {
  delete: vi.fn(),
  get: vi.fn(),
  patch: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
}

vi.mock('./axios', () => ({
  default: api,
}))

const customerAuth = await import('./customerAuth')

describe('customerAuth api', () => {
  beforeEach(() => {
    Object.values(api).forEach((mockFn) => mockFn.mockReset())
  })

  it('calls customer auth endpoints for register, login, profile, and password reset', () => {
    const payload = { username: 'cust0001' }

    customerAuth.customerRegister(payload)
    expect(api.post).toHaveBeenCalledWith('/api/customer/auth/register', payload)

    customerAuth.customerLogin(payload)
    expect(api.post).toHaveBeenCalledWith('/api/customer/auth/login', payload)

    customerAuth.customerGetProfile()
    expect(api.get).toHaveBeenCalledWith('/api/customer/auth/me')

    customerAuth.customerUpdateProfile(payload)
    expect(api.put).toHaveBeenCalledWith('/api/customer/auth/profile', payload)

    customerAuth.customerRequestReset(payload)
    expect(api.post).toHaveBeenCalledWith('/api/customer/auth/request-reset', payload)

    customerAuth.customerResetPassword(payload)
    expect(api.post).toHaveBeenCalledWith('/api/customer/auth/reset-password', payload)
  })

  it('passes verification token as query params', () => {
    customerAuth.customerVerifyEmail('verify-token')

    expect(api.get).toHaveBeenCalledWith('/api/customer/auth/verify-email', {
      params: { token: 'verify-token' },
    })
  })

  it('uses multipart content type for avatar upload', () => {
    const formData = new FormData()

    customerAuth.customerUploadAvatar(formData)

    expect(api.post).toHaveBeenCalledWith('/api/customer/auth/avatar', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  })

  it('returns wrapped security data for login logs and devices', async () => {
    const logs = [{ id: 1 }]
    const devices = [{ deviceId: 10 }]

    api.get.mockResolvedValueOnce({ data: { data: logs } })
    await expect(customerAuth.getCustomerLoginLogs()).resolves.toEqual(logs)
    expect(api.get).toHaveBeenLastCalledWith('/api/customer/security/login-logs')

    api.get.mockResolvedValueOnce({ data: { data: devices } })
    await expect(customerAuth.getCustomerDevices()).resolves.toEqual(devices)
    expect(api.get).toHaveBeenLastCalledWith('/api/customer/security/devices')
  })

  it('calls device revoke and admin unlock endpoints', () => {
    customerAuth.revokeCustomerDevice(10)
    expect(api.delete).toHaveBeenCalledWith('/api/customer/security/devices/10')

    customerAuth.unlockCustomer('CUST0001')
    expect(api.patch).toHaveBeenCalledWith('/api/customer/auth/CUST0001/unlock')
  })
})
