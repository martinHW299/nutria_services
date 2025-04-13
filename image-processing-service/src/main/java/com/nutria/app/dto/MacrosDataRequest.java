package com.nutria.app.dto;

import com.nutria.app.model.MacrosData;
import lombok.Data;

@Data
public class MacrosDataRequest {

    private String image;
    private MacrosData macrosData;
}
