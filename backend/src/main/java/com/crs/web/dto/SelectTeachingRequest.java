package com.crs.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 教授选择授课课程请求 DTO。
 * offeringIds: 教授下学期欲授课的课程开设列表(全量覆盖)。
 * 对应用例 7.3: 教授选择/取消选择下学期欲授课程。
 */
@Getter
@Setter
public class SelectTeachingRequest {

    @NotEmpty(message = "至少选择一门课程")
    private List<Long> offeringIds;
}
