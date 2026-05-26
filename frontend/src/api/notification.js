import api from './axios'

export function getCustomerNotifications() {
  return api.get('/api/customer/notifications')
}

export function getCustomerUnreadNotificationCount() {
  return api.get('/api/customer/notifications/unread-count')
}

export function markCustomerNotificationAsRead(notificationId) {
  return api.patch(`/api/customer/notifications/${notificationId}/read`)
}

export function markAllCustomerNotificationsAsRead() {
  return api.patch('/api/customer/notifications/read-all')
}
