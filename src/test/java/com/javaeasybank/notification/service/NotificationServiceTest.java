package com.javaeasybank.notification.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.notification.entity.Notification;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.repository.NotificationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationPreferenceService notificationPreferenceService;

    @InjectMocks
    private NotificationServiceImpl service;

    @Test
    @DisplayName("markAsRead 只能標記自己的通知")
    void markAsRead_onlyOwnNotification() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setCustomerId("C26050001");
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("測試");
        notification.setMessage("通知內容");
        notification.setIsRead(false);

        when(notificationRepository.findByIdAndCustomerId(1L, "C26050001"))
                .thenReturn(Optional.of(notification));

        service.markAsRead(1L, "C26050001");

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertEquals(true, captor.getValue().getIsRead());
    }

    @Test
    @DisplayName("markAsRead 找不到自己的通知時會丟例外")
    void markAsRead_rejectsOtherCustomerNotification() {
        when(notificationRepository.findByIdAndCustomerId(1L, "C26050001"))
                .thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> service.markAsRead(1L, "C26050001"));
    }

    @Test
    @DisplayName("getUserNotifications 依建立時間回傳自己的通知")
    void getUserNotifications_returnsOwnNotifications() {
        Notification notification = new Notification();
        notification.setId(1L);
        notification.setCustomerId("C26050001");
        notification.setType(NotificationType.SYSTEM);
        notification.setTitle("測試");
        notification.setMessage("通知內容");
        notification.setIsRead(false);

        when(notificationRepository.findByCustomerIdOrderByCreatedAtDesc("C26050001"))
                .thenReturn(List.of(notification));

        var result = service.getUserNotifications("C26050001");

        assertEquals(1, result.size());
        assertEquals("測試", result.get(0).getTitle());
    }

    @Test
    @DisplayName("createNotification 會尊重通知偏好，非 SECURITY 被關閉時不建立")
    void createNotification_respectsPreference() {
        when(notificationPreferenceService.isNotificationEnabled("C26050001", NotificationType.LOAN))
                .thenReturn(false);

        var result = service.createNotification(
                "C26050001",
                NotificationType.LOAN,
                "測試",
                "內容",
                "/user/home");

        assertEquals(null, result);
    }

    @Test
    @DisplayName("SECURITY 通知永遠會建立")
    void createNotification_securityAlwaysCreates() {
        var result = service.createNotification(
                "C26050001",
                NotificationType.SECURITY,
                "測試",
                "內容",
                "/user/home");

        assertEquals("測試", result.getTitle());
        verify(notificationRepository).save(org.mockito.ArgumentMatchers.any(Notification.class));
    }
}
