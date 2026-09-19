package com.crs.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * 课程评价提交请求 DTO。
 */
@Getter
@Setter
public class ReviewSubmitRequest {

    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最低为 1")
    @Max(value = 5, message = "评分最高为 5")
    private Integer rating;

    @Size(max = 500, message = "评论最长 500 字")
    private String comment;
}
