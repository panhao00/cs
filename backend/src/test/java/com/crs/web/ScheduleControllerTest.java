package com.crs.web;

import com.crs.common.ApiException;
import com.crs.common.GlobalExceptionHandler;
import com.crs.domain.ItemKind;
import com.crs.domain.ItemStatus;
import com.crs.domain.ScheduleState;
import com.crs.service.AcademicSummaryService;
import com.crs.service.ScheduleService;
import com.crs.web.dto.ScheduleResponse;
import com.crs.web.dto.SelectOfferingsRequest;
import com.crs.web.dto.SubmitResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * ScheduleController 控制器层测试。
 * <p>
 * 使用 MockMvc standalone 模式,Mock ScheduleService 和 RoleGuard,
 * 包含 GlobalExceptionHandler 以验证异常 → HTTP 映射。
 * <p>
 * 覆盖:
 * - GET    /schedules/me         获取当前学生课表
 * - POST   /schedules            创建课表
 * - POST   /schedules/offerings  选择课程开设
 * - DELETE /schedules/offerings/{offeringId}  移除课程
 * - POST   /schedules/save       保存课表
 * - POST   /schedules/submit     提交课表
 * - DELETE /schedules            删除课表
 * - 权限: 非学生角色被拒绝
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ScheduleController 集成测试")
class ScheduleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ScheduleService scheduleService;

    @Mock
    private AcademicSummaryService academicSummaryService;

    @Mock
    private RoleGuard roleGuard;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final Long STUDENT_ID = 1L;
    private static final String SEMESTER = "2026-FALL";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ScheduleController(scheduleService, academicSummaryService, roleGuard))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ==================== 辅助方法 ====================

    /** 构造默认的 ScheduleResponse 测试数据 */
    private ScheduleResponse buildScheduleResponse() {
        ScheduleResponse.ScheduleItemDTO item = new ScheduleResponse.ScheduleItemDTO(
                1L, 10L, "CS101", "Introduction to Computer Science",
                "MWF", "09:00-10:00", "Room 101",
                ItemKind.PRIMARY, ItemStatus.SELECTED,
                2L, "Bob Smith", 5, 10
        );
        List<ScheduleResponse.ScheduleItemDTO> items = new ArrayList<>();
        items.add(item);
        return new ScheduleResponse(
                100L, STUDENT_ID, SEMESTER,
                ScheduleState.DRAFT, items,
                1, 0
        );
    }

    // ==================== GET /schedules/me ====================

    @Test
    @DisplayName("获取课表成功 - 返回 200 和课表数据")
    void getMySchedule_success() throws Exception {
        ScheduleResponse response = buildScheduleResponse();
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.getMySchedule(STUDENT_ID, SEMESTER))
                .thenReturn(java.util.Optional.of(response));

        mockMvc.perform(get("/schedules/me")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(100))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.semester").value(SEMESTER))
                .andExpect(jsonPath("$.state").value("DRAFT"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.primaryCount").value(1))
                .andExpect(jsonPath("$.alternateCount").value(0));
    }

    @Test
    @DisplayName("获取课表 - 不存在时返回 404")
    void getMySchedule_notFound() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.getMySchedule(STUDENT_ID, SEMESTER))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/schedules/me")
                        .param("semester", SEMESTER))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("获取课表 - 默认使用当前学期")
    void getMySchedule_defaultSemester() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.getMySchedule(eq(STUDENT_ID), any(String.class)))
                .thenReturn(java.util.Optional.empty());

        mockMvc.perform(get("/schedules/me"))
                .andExpect(status().isNotFound());
    }

    // ==================== POST /schedules ====================

    @Test
    @DisplayName("创建课表成功 - 返回 201 和新课表")
    void createSchedule_success() throws Exception {
        ScheduleResponse response = buildScheduleResponse();
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.createSchedule(STUDENT_ID, SEMESTER))
                .thenReturn(response);

        mockMvc.perform(post("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scheduleId").value(100))
                .andExpect(jsonPath("$.studentId").value(STUDENT_ID))
                .andExpect(jsonPath("$.semester").value(SEMESTER))
                .andExpect(jsonPath("$.state").value("DRAFT"));
    }

    @Test
    @DisplayName("创建课表失败 - 课表已存在返回 409")
    void createSchedule_conflict() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.createSchedule(STUDENT_ID, SEMESTER))
                .thenThrow(ApiException.conflict("本学期课表已存在,请使用更新接口"));

        mockMvc.perform(post("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("本学期课表已存在,请使用更新接口"));
    }

    // ==================== POST /schedules/offerings ====================

    @Test
    @DisplayName("选择课程开设成功 - 返回 200 和更新后课表")
    void selectOfferings_success() throws Exception {
        ScheduleResponse response = buildScheduleResponse();
        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection selection =
                new SelectOfferingsRequest.OfferingSelection();
        selection.setOfferingId(10L);
        selection.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(selection));

        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.selectOfferings(eq(STUDENT_ID), eq(SEMESTER),
                any(SelectOfferingsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/schedules/offerings")
                        .param("semester", SEMESTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(100))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].courseId").value("CS101"));
    }

    @Test
    @DisplayName("选择课程开设失败 - 课表不存在返回 404")
    void selectOfferings_scheduleNotFound() throws Exception {
        SelectOfferingsRequest request = new SelectOfferingsRequest();
        SelectOfferingsRequest.OfferingSelection selection =
                new SelectOfferingsRequest.OfferingSelection();
        selection.setOfferingId(10L);
        selection.setKind(ItemKind.PRIMARY);
        request.setSelections(List.of(selection));

        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.selectOfferings(eq(STUDENT_ID), eq(SEMESTER),
                any(SelectOfferingsRequest.class)))
                .thenThrow(ApiException.notFound("课表不存在"));

        mockMvc.perform(post("/schedules/offerings")
                        .param("semester", SEMESTER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("课表不存在"));
    }

    // ==================== DELETE /schedules/offerings/{offeringId} ====================

    @Test
    @DisplayName("移除课程成功 - 返回 200 和更新后课表")
    void removeOffering_success() throws Exception {
        ScheduleResponse response = buildScheduleResponse();
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.removeOffering(STUDENT_ID, SEMESTER, 10L))
                .thenReturn(response);

        mockMvc.perform(delete("/schedules/offerings/10")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(100));
    }

    @Test
    @DisplayName("移除课程失败 - 开设不在课表中返回 404")
    void removeOffering_notFound() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.removeOffering(STUDENT_ID, SEMESTER, 999L))
                .thenThrow(ApiException.notFound("开设不在课表中"));

        mockMvc.perform(delete("/schedules/offerings/999")
                        .param("semester", SEMESTER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("开设不在课表中"));
    }

    // ==================== POST /schedules/save ====================

    @Test
    @DisplayName("保存课表成功 - 返回 200")
    void saveSchedule_success() throws Exception {
        ScheduleResponse response = buildScheduleResponse();
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.saveSchedule(STUDENT_ID, SEMESTER))
                .thenReturn(response);

        mockMvc.perform(post("/schedules/save")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scheduleId").value(100))
                .andExpect(jsonPath("$.state").value("DRAFT"));
    }

    // ==================== POST /schedules/submit ====================

    @Test
    @DisplayName("提交课表成功 - 返回 200 和成功结果")
    void submitSchedule_success() throws Exception {
        SubmitResult result = new SubmitResult(
                true, "课表提交成功",
                List.of(new SubmitResult.EnrollmentResult(
                        10L, "CS101", true, "选课成功")),
                List.of()
        );
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.submitSchedule(STUDENT_ID, SEMESTER))
                .thenReturn(result);

        mockMvc.perform(post("/schedules/submit")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("课表提交成功"))
                .andExpect(jsonPath("$.results.length()").value(1))
                .andExpect(jsonPath("$.results[0].offeringId").value(10))
                .andExpect(jsonPath("$.results[0].enrolled").value(true))
                .andExpect(jsonPath("$.errors.length()").value(0));
    }

    @Test
    @DisplayName("提交课表部分失败 - 返回校验错误")
    void submitSchedule_withErrors() throws Exception {
        SubmitResult result = new SubmitResult(
                false, "部分课程校验失败",
                List.of(new SubmitResult.EnrollmentResult(
                        10L, "CS101", true, "选课成功")),
                List.of(new SubmitResult.ValidationError(
                        11L, "MATH101", "PREREQUISITE_NOT_MET", "先修课未通过"))
        );
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.submitSchedule(STUDENT_ID, SEMESTER))
                .thenReturn(result);

        mockMvc.perform(post("/schedules/submit")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].errorType").value("PREREQUISITE_NOT_MET"))
                .andExpect(jsonPath("$.errors[0].message").value("先修课未通过"));
    }

    @Test
    @DisplayName("提交课表失败 - 课表为空返回 400")
    void submitSchedule_emptySchedule() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        when(scheduleService.submitSchedule(STUDENT_ID, SEMESTER))
                .thenThrow(ApiException.badRequest("课表为空,请先选择课程"));

        mockMvc.perform(post("/schedules/submit")
                        .param("semester", SEMESTER))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("课表为空,请先选择课程"));
    }

    // ==================== DELETE /schedules ====================

    @Test
    @DisplayName("删除课表成功 - 返回 200 和确认消息")
    void deleteSchedule_success() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        doNothing().when(scheduleService).deleteSchedule(STUDENT_ID, SEMESTER);

        mockMvc.perform(delete("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("课表已删除"));
    }

    @Test
    @DisplayName("删除课表失败 - 课表不存在返回 404")
    void deleteSchedule_notFound() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        doThrow(ApiException.notFound("课表不存在"))
                .when(scheduleService).deleteSchedule(STUDENT_ID, SEMESTER);

        mockMvc.perform(delete("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("课表不存在"));
    }

    @Test
    @DisplayName("删除课表失败 - 注册已关闭返回 409")
    void deleteSchedule_closed() throws Exception {
        when(roleGuard.requireStudent()).thenReturn(STUDENT_ID);
        doThrow(ApiException.conflict("注册已关闭,无法删除课表"))
                .when(scheduleService).deleteSchedule(STUDENT_ID, SEMESTER);

        mockMvc.perform(delete("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("注册已关闭,无法删除课表"));
    }

    // ==================== 权限测试 ====================

    @Test
    @DisplayName("权限 - 非学生角色获取课表被拒绝返回 403")
    void getMySchedule_forbidden() throws Exception {
        when(roleGuard.requireStudent())
                .thenThrow(ApiException.forbidden("只有学生可以操作课表"));

        mockMvc.perform(get("/schedules/me")
                        .param("semester", SEMESTER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("只有学生可以操作课表"));
    }

    @Test
    @DisplayName("权限 - 未登录获取课表返回 401")
    void getMySchedule_unauthorized() throws Exception {
        when(roleGuard.requireStudent())
                .thenThrow(ApiException.unauthorized("未登录"));

        mockMvc.perform(get("/schedules/me")
                        .param("semester", SEMESTER))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("未登录"));
    }

    @Test
    @DisplayName("权限 - 非学生角色创建课表被拒绝返回 403")
    void createSchedule_forbidden() throws Exception {
        when(roleGuard.requireStudent())
                .thenThrow(ApiException.forbidden("只有学生可以操作课表"));

        mockMvc.perform(post("/schedules")
                        .param("semester", SEMESTER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("权限 - 非学生角色提交课表被拒绝返回 403")
    void submitSchedule_forbidden() throws Exception {
        when(roleGuard.requireStudent())
                .thenThrow(ApiException.forbidden("只有学生可以操作课表"));

        mockMvc.perform(post("/schedules/submit")
                        .param("semester", SEMESTER))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }
}
