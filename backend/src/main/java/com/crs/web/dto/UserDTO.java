package com.crs.web.dto;

import com.crs.domain.Role;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户视图 DTO(注册主任维护学生/教授用例用)。
 * 敏感字段(密码哈希、SSN)只在服务端处理,不通过此 DTO 暴露。
 * SSN 经过脱敏处理: 仅显示最后 4 位,其余用 * 替代。
 */
@Getter
@Setter
@AllArgsConstructor
public class UserDTO {
    private Long id;
    private String username;
    private Role role;
    private String name;
    private String department;
    private LocalDate dob;
    private LocalDate graduationDate;
    private String status;  // ACTIVE / INACTIVE
    private String ssn;     // 脱敏后的 SSN

    /**
     * SSN 脱敏处理。
     * <p>
     * 将完整 SSN (如 "123-45-6789") 转换为脱敏格式 ("***-**-6789")。
     * 若 SSN 为 null 或长度不足,则返回原值。
     *
     * @param ssn 完整 SSN
     * @return 脱敏后的 SSN
     */
    public static String maskSSN(String ssn) {
        if (ssn == null || ssn.isEmpty()) {
            return ssn;
        }
        // 移除可能的格式化字符 (连字符等)
        String cleanSSN = ssn.replaceAll("[^0-9]", "");
        if (cleanSSN.length() < 4) {
            return ssn; // 长度不足,返回原值
        }
        // 保留最后 4 位,其余用 * 替代
        String lastFour = cleanSSN.substring(cleanSSN.length() - 4);
        return "***-**-" + lastFour;
    }

    /**
     * 无参构造 (供 Jackson 反序列化等场景使用)。
     */
    public UserDTO() {
    }
}
