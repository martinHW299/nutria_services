package com.nutria.app.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nutria.app.model.MacrosData;
import com.nutria.app.service.MacrosDataService;
import com.nutria.common.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/ai")
public class MacrosDataController {

    private final MacrosDataService macrosDataService;

    @GetMapping
    public List<MacrosData> getAll() { return macrosDataService.getAll(); }

    @GetMapping("/{id}")
    public Optional<MacrosData> getById(@PathVariable String id) { return macrosDataService.getById(id); }

    @PostMapping("/save")
    public ResponseEntity<ApiResponse<MacrosData>> create(@RequestHeader("Authorization") String token, @RequestBody Map<String, String> payload) throws JsonProcessingException {
        return ResponseEntity.ok(ApiResponse.success(macrosDataService.saveMacros(token, payload.get("image"))));
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable String id) { macrosDataService.delete(id); }
}