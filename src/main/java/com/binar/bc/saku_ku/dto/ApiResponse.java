package com.binar.bc.saku_ku.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ApiResponse<T> {

    private Integer statusCode;
    private String message;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private T data;

    public static <T> ApiResponse<T> success(T data, String message) {
        return success(200, data, message);
    }

    public static <T> ApiResponse<T> success(Integer statusCode, T data, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatusCode(statusCode);
        response.setData(data);
        response.setMessage(message);
        return response;
    }
}