package com.javaeasybank.notification.service;

import com.javaeasybank.common.exception.BusinessException;
import com.javaeasybank.notification.dto.NotificationPreferenceResponseDTO;
import com.javaeasybank.notification.entity.NotificationPreference;
import com.javaeasybank.notification.enums.NotificationType;
import com.javaeasybank.notification.repository.NotificationPreferenceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPreferenceServiceTest {

    @Mock
    private NotificationPreferenceRepository notificationPreferenceRepository;

    @InjectMocks
    private NotificationPreferenceServiceImpl service;

    @Test
    @DisplayName("未設定偏好時，預設全部啟用且 SECURITY 鎖定")
    void getPreferences_defaultsEnabled() {
        when(notificationPreferenceRepository.findByCustomerId("C001")).thenReturn(List.of());

        var result = service.getPreferences("C001");

        assertEquals(NotificationType.values().length, result.size());
        assertTrue(result.stream().allMatch(NotificationPreferenceResponseDTO::getEnabled));
        assertTrue(result.stream()
                .filter(dto -> dto.getType() == NotificationType.SECURITY)
                .findFirst()
                .orElseThrow()
                .getLocked());
    }

    @Test
    @DisplayName("SECURITY 不可關閉")
    void updatePreference_securityCannotDisable() {
        assertThrows(BusinessException.class, () ->
                service.updatePreference("C001", NotificationType.SECURITY, false));
    }

    @Test
    @DisplayName("可正常關閉一般通知")
    void updatePreference_updatesNormalType() {
        when(notificationPreferenceRepository.findByCustomerIdAndType("C001", NotificationType.LOAN))
                .thenReturn(Optional.empty());
        when(notificationPreferenceRepository.save(any(NotificationPreference.class)))
                .thenAnswer(inv -> {
                    NotificationPreference pref = inv.getArgument(0);
                    pref.setId(1L);
                    pref.setUpdatedAt(LocalDateTime.now());
                    return pref;
                });

        var result = service.updatePreference("C001", NotificationType.LOAN, false);

        assertEquals(NotificationType.LOAN, result.getType());
        assertFalse(result.getEnabled());
        verify(notificationPreferenceRepository).save(any(NotificationPreference.class));
    }

    @Test
    @DisplayName("isNotificationEnabled 對關閉的通知回傳 false")
    void isNotificationEnabled_respectsSavedPreference() {
        NotificationPreference pref = new NotificationPreference();
        pref.setCustomerId("C001");
        pref.setType(NotificationType.TRANSFER);
        pref.setEnabled(false);
        pref.setUpdatedAt(LocalDateTime.now());

        when(notificationPreferenceRepository.findByCustomerIdAndType("C001", NotificationType.TRANSFER))
                .thenReturn(Optional.of(pref));

        assertFalse(service.isNotificationEnabled("C001", NotificationType.TRANSFER));
        assertTrue(service.isNotificationEnabled("C001", NotificationType.SECURITY));
    }
}
