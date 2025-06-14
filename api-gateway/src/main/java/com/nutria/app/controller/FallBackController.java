package com.nutria.app.controller;

import com.nutria.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FallBackController {
    @RequestMapping("/fallbackCall")
    public ResponseEntity<ApiResponse<?>> authFallback() {
        return ResponseEntity.ok(ApiResponse.error(HttpStatus.SERVICE_UNAVAILABLE, "Lo sentimos, el servicio se encuentra en mantenimiento. Por favor, intente nuevamente dentro de un momento."));
    }
}
