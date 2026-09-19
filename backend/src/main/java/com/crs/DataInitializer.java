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
import org.springframework.core.env.Environment;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化: 创建默认测试账号与示例课程数据。
 * <p>
 * 默认账号(密码与用户名相同):
 * - registrar / registrar
 * - professor / professor
 * - student / student
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
    private final Environment environment;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("数据已存在,跳过初始化");
            return;
        }
        initUsers();
        initCatalogAndOfferings();

        // 生产环境默认密码安全检查
        if (isProductionProfile()) {
            log.warn("=================================================================");
            log.warn("⚠️  生产环境安全警告: 当前使用默认测试密码!");
            log.warn("   默认账号(registrar, professor, student 等)密码与用户名相同,");
            log.warn("   请立即通过系统设置修改所有默认密码!");
            log.warn("   路径: 系统设置 → 修改密码");
            log.warn("=================================================================");
        }

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
        log.info("  Registrar:  registrar / [configured]");
        log.info("  Professor:  professor / [configured]");
        log.info("  Student:   student / [configured]");
        log.info("================================");
    }

    private void initUsers() {
        // ========== Registrar ==========
        User registrar = new User();
        registrar.setUsername("registrar");
        registrar.setPasswordHash(passwordEncoder.encode("registrar"));
        registrar.setRole(Role.REGISTRAR);
        registrar.setName("Alice Registrar");
        registrar.setSsn("123-45-6789");
        registrar.setDob(LocalDate.of(1980, 1, 1));
        userRepository.save(registrar);

        // ========== Professors (5人) ==========
        String[][] professors = {
            {"professor", "professor", "Bob Smith", "Computer Science", "234-56-7890", "1975-06-15"},
            {"professor2", "professor2", "Diana Johnson", "Mathematics", "456-78-9012", "1972-03-20"},
            {"professor3", "professor3", "Edward Williams", "English", "567-89-0123", "1968-11-08"},
            {"professor4", "professor4", "Fiona Davis", "Physics", "678-90-1234", "1979-07-22"},
            {"professor5", "professor5", "George Miller", "Computer Science", "789-01-2345", "1971-09-30"}
        };
        for (String[] p : professors) {
            User prof = new User();
            prof.setUsername(p[0]);
            prof.setPasswordHash(passwordEncoder.encode(p[1]));
            prof.setRole(Role.PROFESSOR);
            prof.setName(p[2]);
            prof.setDepartment(p[3]);
            prof.setSsn(p[4]);
            prof.setDob(LocalDate.parse(p[5]));
            userRepository.save(prof);
        }

        // ========== Students (30人) ==========
        // 主测试账号
        User student = new User();
        student.setUsername("student");
        student.setPasswordHash(passwordEncoder.encode("student"));
        student.setRole(Role.STUDENT);
        student.setName("Charlie Brown");
        student.setSsn("345-67-8901");
        student.setDob(LocalDate.of(2003, 9, 1));
        student.setGraduationDate(LocalDate.of(2027, 6, 30));
        student.setDepartment("Computer Science");
        userRepository.save(student);

        // 更多学生 - 真实姓名
        String[][] students = {
            {"student2", "Hannah Wilson", "2003-02-14", "Computer Science"},
            {"student3", "Ian Taylor", "2002-07-22", "Mathematics"},
            {"student4", "Julia Anderson", "2003-04-05", "Physics"},
            {"student5", "Kevin Thomas", "2002-11-18", "English"},
            {"student6", "Laura Martinez", "2003-06-30", "Computer Science"},
            {"student7", "Michael Garcia", "2002-09-12", "Mathematics"},
            {"student8", "Nancy Robinson", "2003-01-25", "Physics"},
            {"student9", "Oscar Clark", "2002-08-08", "English"},
            {"student10", "Patricia Lewis", "2003-03-17", "Computer Science"},
            {"student11", "Quinn Walker", "2002-12-03", "Mathematics"},
            {"student12", "Rachel Hall", "2003-05-21", "Physics"},
            {"student13", "Samuel Allen", "2002-10-14", "English"},
            {"student14", "Tina Young", "2003-08-09", "Computer Science"},
            {"student15", "Ulysses King", "2002-06-27", "Mathematics"},
            {"student16", "Victoria Wright", "2003-02-08", "Physics"},
            {"student17", "Walter Scott", "2002-04-19", "English"},
            {"student18", "Xena Adams", "2003-07-11", "Computer Science"},
            {"student19", "Yusuf Baker", "2002-01-30", "Mathematics"},
            {"student20", "Zoe Nelson", "2003-09-23", "Physics"},
            {"student21", "Aaron Hill", "2002-03-15", "English"},
            {"student22", "Bella Green", "2003-11-02", "Computer Science"},
            {"student23", "Carlos Carter", "2002-05-28", "Mathematics"},
            {"student24", "Diana Mitchell", "2003-01-13", "Physics"},
            {"student25", "Ethan Roberts", "2002-09-07", "English"},
            {"student26", "Grace Turner", "2003-04-26", "Computer Science"},
            {"student27", "Henry Phillips", "2002-12-19", "Mathematics"},
            {"student28", "Iris Campbell", "2003-06-04", "Physics"},
            {"student29", "Jack Parker", "2002-08-16", "English"},
            {"student30", "Katie Evans", "2003-03-29", "Computer Science"}
        };
        for (String[] s : students) {
            User stu = new User();
            stu.setUsername(s[0]);
            stu.setPasswordHash(passwordEncoder.encode(s[0]));
            stu.setRole(Role.STUDENT);
            stu.setName(s[1]);
            stu.setSsn(generateSSN());
            stu.setDob(LocalDate.parse(s[2]));
            stu.setGraduationDate(LocalDate.of(2027, 6, 30));
            stu.setDepartment(s[3]);
            userRepository.save(stu);
        }

        log.info("已初始化 {} 个用户(含 1 registrar, 5 professors, 30 students)",
                1 + 5 + 30);
    }

    /** 生成随机 SSN (避免重复) */
    private String generateSSN() {
        int area = 100 + (int)(Math.random() * 899);
        int group = 10 + (int)(Math.random() * 89);
        int serial = 1000 + (int)(Math.random() * 8999);
        return String.format("%03d-%02d-%04d", area, group, serial);
    }

    /** 检查是否为生产环境 */
    private boolean isProductionProfile() {
        String[] activeProfiles = environment.getActiveProfiles();
        for (String profile : activeProfiles) {
            if ("prod".equals(profile) || "production".equals(profile)) {
                return true;
            }
        }
        return false;
    }

    private void initCatalogAndOfferings() {
        // ========== 课程目录 (15门课程) ==========
        // Computer Science
        Course cs101 = course("CS101", "Introduction to Programming", "Computer Science", 3, null);
        Course cs102 = course("CS102", "Object-Oriented Programming", "Computer Science", 3, "CS101");
        Course cs201 = course("CS201", "Data Structures", "Computer Science", 3, "CS102");
        Course cs202 = course("CS202", "Computer Organization", "Computer Science", 3, "CS102");
        Course cs301 = course("CS301", "Algorithms", "Computer Science", 3, "CS201");
        Course cs302 = course("CS302", "Operating Systems", "Computer Science", 3, "CS202");
        Course cs303 = course("CS303", "Database Systems", "Computer Science", 3, "CS201");

        // Mathematics
        Course math101 = course("MATH101", "Calculus I", "Mathematics", 4, null);
        Course math102 = course("MATH102", "Calculus II", "Mathematics", 4, "MATH101");
        Course math201 = course("MATH201", "Linear Algebra", "Mathematics", 3, "MATH102");
        Course math301 = course("MATH301", "Probability & Statistics", "Mathematics", 3, "MATH201");

        // English
        Course eng101 = course("ENG101", "English Composition", "English", 3, null);
        Course eng201 = course("ENG201", "Technical Writing", "English", 3, "ENG101");

        // Physics
        Course phys101 = course("PHYS101", "General Physics I", "Physics", 4, "MATH101");
        Course phys201 = course("PHYS201", "General Physics II", "Physics", 4, "PHYS101");

        // 保存所有课程
        Course[] allCourses = {cs101, cs102, cs201, cs202, cs301, cs302, cs303,
                               math101, math102, math201, math301,
                               eng101, eng201, phys101, phys201};
        for (Course c : allCourses) {
            courseRepository.save(c);
        }

        // ========== 获取教授 ID ==========
        Long prof1 = userRepository.findByUsername("professor").map(User::getId).orElse(null);
        Long prof2 = userRepository.findByUsername("professor2").map(User::getId).orElse(null);
        Long prof3 = userRepository.findByUsername("professor3").map(User::getId).orElse(null);
        Long prof4 = userRepository.findByUsername("professor4").map(User::getId).orElse(null);
        Long prof5 = userRepository.findByUsername("professor5").map(User::getId).orElse(null);

        // ========== 本学期课程开设 (2026-FALL) ==========
        // CS 课程 - Bob Smith (prof1)
        offering(cs101.getCourseId(), "2026-FALL", prof1, "MWF", "09:00-10:00", "A101");
        offering(cs201.getCourseId(), "2026-FALL", prof1, "TR", "11:00-12:30", "B202");
        offering(cs301.getCourseId(), "2026-FALL", prof1, "MWF", "14:00-15:00", "A103");

        // CS 课程 - George Miller (prof5)
        offering(cs102.getCourseId(), "2026-FALL", prof5, "TR", "09:00-10:30", "A102");
        offering(cs202.getCourseId(), "2026-FALL", prof5, "MWF", "11:00-12:00", "B201");
        offering(cs302.getCourseId(), "2026-FALL", prof5, "TR", "14:00-15:30", "B203");
        offering(cs303.getCourseId(), "2026-FALL", prof5, "MWF", "15:00-16:00", "A104");

        // Mathematics - Diana Johnson (prof2)
        offering(math101.getCourseId(), "2026-FALL", prof2, "MWF", "08:00-09:00", "C301");
        offering(math102.getCourseId(), "2026-FALL", prof2, "TR", "10:00-11:30", "C302");
        offering(math201.getCourseId(), "2026-FALL", prof2, "MWF", "13:00-14:00", "C303");
        offering(math301.getCourseId(), "2026-FALL", prof2, "TR", "16:00-17:30", "C304");

        // English - Edward Williams (prof3)
        offering(eng101.getCourseId(), "2026-FALL", prof3, "MWF", "10:00-11:00", "D401");
        offering(eng201.getCourseId(), "2026-FALL", prof3, "TR", "13:00-14:30", "D402");

        // Physics - Fiona Davis (prof4)
        offering(phys101.getCourseId(), "2026-FALL", prof4, "TR", "08:00-09:30", "E501");
        offering(phys201.getCourseId(), "2026-FALL", prof4, "MWF", "16:00-17:00", "E502");

        // ========== 上学期课程开设 (2026-SPRING, 用于成绩测试) ==========
        CourseOffering springCs101 = offering(cs101.getCourseId(), "2026-SPRING", prof1, "MWF", "09:00-10:00", "A101");
        CourseOffering springCs201 = offering(cs201.getCourseId(), "2026-SPRING", prof1, "TR", "11:00-12:30", "B202");
        CourseOffering springMath101 = offering(math101.getCourseId(), "2026-SPRING", prof2, "MWF", "13:00-14:00", "C303");
        CourseOffering springEng101 = offering(eng101.getCourseId(), "2026-SPRING", prof3, "TR", "15:00-16:30", "D401");
        CourseOffering springPhys101 = offering(phys101.getCourseId(), "2026-SPRING", prof4, "MWF", "11:00-12:00", "E501");

        // ========== 上学期学生选课记录 ==========
        // Charlie Brown (student) - CS101, MATH101
        Long studentId = userRepository.findByUsername("student").map(User::getId).orElse(null);
        if (studentId != null) {
            enrollment(springCs101.getId(), studentId);
            enrollment(springMath101.getId(), studentId);
        }

        // Hannah Wilson (student2) - CS101, CS201, ENG101
        Long student2Id = userRepository.findByUsername("student2").map(User::getId).orElse(null);
        if (student2Id != null) {
            enrollment(springCs101.getId(), student2Id);
            enrollment(springCs201.getId(), student2Id);
            enrollment(springEng101.getId(), student2Id);
        }

        // Ian Taylor (student3) - MATH101, PHYS101
        Long student3Id = userRepository.findByUsername("student3").map(User::getId).orElse(null);
        if (student3Id != null) {
            enrollment(springMath101.getId(), student3Id);
            enrollment(springPhys101.getId(), student3Id);
        }

        log.info("已初始化 15 门课程, 22 个课程开设(17 本学期 + 5 上学期), 3 个学生选课记录");
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
