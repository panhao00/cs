package com.crs.web.dto;

import com.crs.domain.Role;
import com.crs.domain.User;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * SSN 脱敏功能单元测试。
 * <p>
 * 测试 UserDTO.maskSSN() 方法的各种输入场景。
 */
class SsnMaskingTest {

    @Test
    @DisplayName("maskSSN - 标准格式 SSN → 脱敏显示")
    void maskSSN_standardFormat_masksCorrectly() {
        // Arrange
        String ssn = "123-45-6789";

        // Act
        String masked = UserDTO.maskSSN(ssn);

        // Assert
        assertEquals("***-**-6789", masked);
    }

    @Test
    @DisplayName("maskSSN - 无格式 SSN → 脱敏显示")
    void maskSSN_noFormat_masksCorrectly() {
        // Arrange
        String ssn = "123456789";

        // Act
        String masked = UserDTO.maskSSN(ssn);

        // Assert
        assertEquals("***-**-6789", masked);
    }

    @Test
    @DisplayName("maskSSN - null SSN → 返回 null")
    void maskSSN_null_returnsNull() {
        // Act
        String masked = UserDTO.maskSSN(null);

        // Assert
        assertNull(masked);
    }

    @Test
    @DisplayName("maskSSN - 空字符串 → 返回空字符串")
    void maskSSN_empty_returnsEmpty() {
        // Act
        String masked = UserDTO.maskSSN("");

        // Assert
        assertEquals("", masked);
    }

    @Test
    @DisplayName("maskSSN - 长度不足 4 位 → 返回原值")
    void maskSSN_tooShort_returnsOriginal() {
        // Arrange
        String ssn = "123";

        // Act
        String masked = UserDTO.maskSSN(ssn);

        // Assert
        assertEquals("123", masked);
    }

    @Test
    @DisplayName("maskSSN - 恰好 4 位 → 脱敏显示")
    void maskSSN_exactlyFour_maksCorrectly() {
        // Arrange
        String ssn = "6789";

        // Act
        String masked = UserDTO.maskSSN(ssn);

        // Assert
        // 4位数字,全部被保留为后4位
        assertEquals("***-**-6789", masked);
    }

    @Test
    @DisplayName("UserDTO 包含脱敏后 SSN 字段")
    void userDTO_containsMaskedSSN() {
        // Arrange
        UserDTO dto = new UserDTO(
                1L,
                "charlie",
                Role.STUDENT,
                "Charlie Brown",
                null,
                LocalDate.of(2000, 5, 15),
                LocalDate.of(2028, 6, 15),
                "ACTIVE",
                UserDTO.maskSSN("123-45-6789")
        );

        // Assert
        assertEquals("***-**-6789", dto.getSsn());
    }

    @Test
    @DisplayName("UserDTO 包含 null SSN 字段")
    void userDTO_containsNullSSN() {
        // Arrange
        UserDTO dto = new UserDTO(
                1L,
                "charlie",
                Role.STUDENT,
                "Charlie Brown",
                null,
                LocalDate.of(2000, 5, 15),
                LocalDate.of(2028, 6, 15),
                "ACTIVE",
                UserDTO.maskSSN(null)
        );

        // Assert
        assertNull(dto.getSsn());
    }

    @Test
    @DisplayName("maskSSN - 各种边界情况综合测试")
    void maskSSN_variousEdgeCases() {
        // 标准 9 位
        assertEquals("***-**-6789", UserDTO.maskSSN("123-45-6789"));

        // 无格式 9 位
        assertEquals("***-**-6789", UserDTO.maskSSN("123456789"));

        // 5 位 (只保留最后 4 位)
        assertEquals("***-**-2345", UserDTO.maskSSN("12345"));

        // 10 位
        assertEquals("***-**-3456", UserDTO.maskSSN("123453456"));

        // 8 位
        assertEquals("***-**-4567", UserDTO.maskSSN("1234567"));
    }
}
