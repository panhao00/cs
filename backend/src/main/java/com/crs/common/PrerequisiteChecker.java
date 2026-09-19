package com.crs.common;

import com.crs.domain.CourseOffering;
import com.crs.domain.Grade;
import com.crs.repository.CourseOfferingRepository;
import com.crs.repository.GradeRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 先修课校验工具: 消除 ScheduleService / WaitlistService 中的重复代码。
 * <p>
 * 查询 Grade 表中是否有该课程及格成绩(A/B/C/D, 排除 F/I/NULL)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PrerequisiteChecker {

    private final GradeRepository gradeRepository;
    private final CourseOfferingRepository offeringRepository;

    /**
     * 检查学生是否通过指定先修课。
     *
     * @param studentId           学生 ID
     * @param prerequisiteCourseId 先修课 courseId
     * @return true 表示已通过
     */
    public boolean hasPassedPrerequisite(Long studentId, String prerequisiteCourseId) {
        List<Grade> grades = gradeRepository.findByStudentId(studentId);
        Set<String> passedCourses = new HashSet<>();

        for (Grade g : grades) {
            String grade = g.getGrade();
            if (grade != null && !grade.isEmpty()
                    && !grade.equals("F") && !grade.equals("I")) {
                offeringRepository.findById(g.getOfferingId())
                        .ifPresent(o -> passedCourses.add(o.getCourseId()));
            }
        }

        return passedCourses.contains(prerequisiteCourseId);
    }
}
