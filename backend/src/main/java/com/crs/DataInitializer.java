package com.crs;

import com.crs.domain.Course;
import com.crs.domain.CourseOffering;
import com.crs.domain.Enrollment;
import com.crs.domain.EnrollmentStatus;
import com.crs.domain.Grade;
import com.crs.domain.OfferingStatus;
import com.crs.domain.Role;
import com.crs.domain.User;
import com.crs.repository.CourseOfferingRepository;
import com.crs.domain.SystemSetting;
import com.crs.repository.CourseRepository;
import com.crs.repository.EnrollmentRepository;
import com.crs.repository.GradeRepository;
import com.crs.repository.SystemSettingRepository;
import com.crs.repository.UserRepository;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化: 创建默认测试账号与示例课程数据。
 * <p>
 * 默认账号(密码与用户名相同):
 * - registrar / registrar
 * - professor / professor
 * - student / student
 * <p>
 * 参考 AI 生成,经人工核验与调整。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseOfferingRepository offeringRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final GradeRepository gradeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SystemSettingRepository systemSettingRepository;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("数据已存在,跳过初始化");
            return;
        }
        initUsers();
        initCatalogAndOfferings();

        // 初始化注册周期状态
        if (systemSettingRepository.findBySettingKey("registration.phase").isEmpty()) {
            SystemSetting phaseSetting = new SystemSetting();
            phaseSetting.setSettingKey("registration.phase");
            phaseSetting.setSettingValue("open");
            systemSettingRepository.save(phaseSetting);
            log.info("初始化注册周期状态: open");
        }

        log.info("========== 初始化完成 ==========");
        log.info("默认账号:");
        log.info("  Registrar:  registrar / registrar");
        log.info("  Professor:  professor / professor");
        log.info("  Student:   student / student");
        log.info("================================");
    }

    private void initUsers() {
        // Registrar
        User registrar = new User();
        registrar.setUsername("registrar");
        registrar.setPasswordHash(passwordEncoder.encode("registrar"));
        registrar.setRole(Role.REGISTRAR);
        registrar.setName("Alice Registrar");
        registrar.setSsn("123-45-6789");
        registrar.setDob(LocalDate.of(1980, 1, 1));
        userRepository.save(registrar);

        // Professor
        User professor = new User();
        professor.setUsername("professor");
        professor.setPasswordHash(passwordEncoder.encode("professor"));
        professor.setRole(Role.PROFESSOR);
        professor.setName("Bob Smith");
        professor.setDepartment("Computer Science");
        professor.setSsn("234-56-7890");
        professor.setDob(LocalDate.of(1975, 6, 15));
        User savedProf = userRepository.save(professor);

        // Student
        User student = new User();
        student.setUsername("student");
        student.setPasswordHash(passwordEncoder.encode("student"));
        student.setRole(Role.STUDENT);
        student.setName("Charlie Brown");
        student.setSsn("345-67-8901");
        student.setDob(LocalDate.of(2003, 9, 1));
        student.setGraduationDate(LocalDate.of(2027, 6, 30));
        userRepository.save(student);

        // 更多学生(用于后续测试容量限制)
        for (int i = 2; i <= 12; i++) {
            User s = new User();
            s.setUsername("student" + i);
            s.setPasswordHash(passwordEncoder.encode("student" + i));
            s.setRole(Role.STUDENT);
            s.setName("Student No." + i);
            s.setDob(LocalDate.of(2003, 1, 1));
            s.setGraduationDate(LocalDate.of(2027, 6, 30));
            userRepository.save(s);
        }

        log.info("已初始化 {} 个用户(含 1 registrar, 1 professor, 12 students)",
                1 + 1 + 12);
    }

    private void initCatalogAndOfferings() {
        // 课程目录(模拟遗留 Ingres Catalog)
        Course cs101 = course("CS101", "Introduction to Programming", "Computer Science", 3, null);
        Course cs201 = course("CS201", "Data Structures", "Computer Science", 3, "CS101");
        Course cs301 = course("CS301", "Algorithms", "Computer Science", 3, "CS201");
        Course math101 = course("MATH101", "Calculus I", "Mathematics", 4, null);
        Course eng101 = course("ENG101", "English Composition", "English", 3, null);

        courseRepository.save(cs101);
        courseRepository.save(cs201);
        courseRepository.save(cs301);
        courseRepository.save(math101);
        courseRepository.save(eng101);

        // 找到教授 ID
        Long profId = userRepository.findByUsername("professor")
                .map(User::getId).orElse(null);

        // 课程开设(本学期 2026-FALL)
        CourseOffering fallCs101 = offering(cs101.getCourseId(), "2026-FALL", profId, "MWF", "09:00-10:00", "A101");
        CourseOffering fallCs201 = offering(cs201.getCourseId(), "2026-FALL", profId, "TR", "11:00-12:30", "B202");
        offering(math101.getCourseId(), "2026-FALL", null, "MWF", "13:00-14:00", "C303");
        offering(eng101.getCourseId(), "2026-FALL", null, "TR", "15:00-16:30", "D404");

        // 上学期开设(2026-SPRING,用于 Submit Grades / View Report Card 测试)
        CourseOffering springCs101 = offering(cs101.getCourseId(), "2026-SPRING", profId, "MWF", "09:00-10:00", "A101");
        CourseOffering springMath101 = offering(math101.getCourseId(), "2026-SPRING", profId, "MWF", "13:00-14:00", "C303");

        // 上学期学生选课记录(用于 Submit Grades 测试)
        Long studentId = userRepository.findByUsername("student").map(User::getId).orElse(null);
        if (studentId != null) {
            enrollment(springCs101.getId(), studentId);
            enrollment(springMath101.getId(), studentId);
        }

        log.info("已初始化 5 门课程, 6 个课程开设(4 本学期 + 2 上学期)");
    }

    private Course course(String id, String title, String dept, int credits, String prereq) {
        Course c = new Course();
        c.setCourseId(id);
        c.setTitle(title);
        c.setDepartment(dept);
        c.setCredits(credits);
        c.setPrerequisiteId(prereq);
        return c;
    }

    private CourseOffering offering(String courseId, String semester, Long profId,
                          String days, String times, String room) {
        CourseOffering o = new CourseOffering();
        o.setCourseId(courseId);
        o.setSemester(semester);
        o.setProfessorId(profId);
        o.setDays(days);
        o.setTimes(times);
        o.setRoom(room);
        o.setMaxCapacity(10);
        o.setStatus(OfferingStatus.OPEN);
        return offeringRepository.save(o);
    }

    private void enrollment(Long offeringId, Long studentId) {
        Enrollment e = new Enrollment();
        e.setOfferingId(offeringId);
        e.setStudentId(studentId);
        e.setStatus(EnrollmentStatus.ENROLLED);
        enrollmentRepository.save(e);
    }
}
