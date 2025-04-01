package com.nutria.app.controller;

import com.nutria.app.model.MacrosData;
import com.nutria.app.service.MacrosDataService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class MacrosDataController {

    private final MacrosDataService macrosDataService;

    @GetMapping
    public List<MacrosData> getAll() { return macrosDataService.getAll(); }

    @GetMapping("/find/{id}")
    public ResponseEntity<ApiResponse<MacrosData>> getById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(macrosDataService.getById(id)));
    }

    @PostMapping("/delete/{id}")
    public ResponseEntity<ApiResponse<MacrosData>> deleteById(@PathVariable("id") String id) {
        return ResponseEntity.ok(ApiResponse.success(macrosDataService.delete(id)));
    }





}