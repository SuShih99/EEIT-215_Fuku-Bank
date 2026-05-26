import api from './axios'

export function getCustomerNotificationPreferences() {
  return api.get('/api/customer/notification-preferences')
}

export function updateCustomerNotificationPreference(payload) {
  return api.patch('/api/customer/notification-preferences', payload)
}
