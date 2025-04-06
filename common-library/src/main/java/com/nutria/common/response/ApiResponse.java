package com.nutria.common.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T>{
    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public static <T> ApiResponse<T> success(T data) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(HttpStatus.OK.value());
        response.setMessage("Success");
        response.setData(data);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
    public static <T> ApiResponse<T> error(HttpStatus status, String message) {
        ApiResponse<T> response = new ApiResponse<>();
        response.setStatus(status.value());
        response.setMessage(message);
        response.setData(null);
        response.setTimestamp(LocalDateTime.now());
        return response;
    }
}
