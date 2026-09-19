package com.crs.web.dto;

import com.crs.security.DataMaskUtil;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 * 成绩传输对象(DTO): 用于成绩查询的安全增强传输。
 * <p>
 * 对应需求: "student grades are sensitive information, the system must employ extra security measures"
 * <p>
 * 安全特性:
 * - studentName 经过脱敏处理 (DataMaskUtil.maskName)
 * - ssn 经过脱敏处理 (DataMaskUtil.maskSSN),仅显示最后 4 位
 * - 不暴露密码哈希等敏感字段
 * <p>
 * 用于:
 * - GradeController: 教授查看班级名单时 SSN 脱敏
 * - ReportCardController: 学生成绩单中 SSN 脱敏
 */
@Getter
@Setter
@AllArgsConstructor
public class GradeDTO {
    private Long offeringId;
    private String courseId;
    private Long studentId;
    private String studentName;  // 脱敏后的姓名
    private String grade;        // A/B/C/D/F/I 或 null
    private String ssn;          // 脱敏后的 SSN
    private LocalDateTime createdAt;

    /**
     * 无参构造 (供 Jackson 反序列化等场景使用)。
     */
    public GradeDTO() {
    }

    /**
     * 创建脱敏后的 GradeDTO。
     * <p>
     * 自动对 studentName 和 ssn 进行脱敏处理。
     *
     * @param offeringId  开设 ID
     * @param courseId    课程 ID
     * @param studentId   学生 ID
     * @param studentName 完整姓名
     * @param grade       成绩
     * @param ssn         完整 SSN
     * @param createdAt   创建时间
     * @return 脱敏后的 GradeDTO
     */
    public static GradeDTO createMasked(Long offeringId, String courseId,
                                         Long studentId, String studentName,
                                         String grade, String ssn,
                                         LocalDateTime createdAt) {
        return new GradeDTO(
                offeringId,
                courseId,
                studentId,
                DataMaskUtil.maskName(studentName),
                grade,
                DataMaskUtil.maskSSN(ssn),
                createdAt
        );
    }
}
