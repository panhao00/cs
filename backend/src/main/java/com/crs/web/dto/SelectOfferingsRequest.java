package com.crs.web.dto;

import com.crs.domain.ItemKind;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * 选择课程开设请求 DTO。
 * 对应用例 7.2 Select Offerings: 学生从可用开设列表中选 4 门主选 + 2 门备选。
 */
@Getter
@Setter
public class SelectOfferingsRequest {

    @NotEmpty(message = "至少选择一门课程")
    private List<OfferingSelection> selections;

    @Getter
    @Setter
    public static class OfferingSelection {
        @NotNull(message = "offeringId 不能为空")
        private Long offeringId;

        @NotNull(message = "kind 不能为空")
        private ItemKind kind;  // PRIMARY / ALTERNATE
    }
}
