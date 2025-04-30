package com.nutria.app.dto;


import lombok.Builder;
import lombok.Data;

import java.util.Date;

@Data
@Builder
public class MacrosSummary {
    private String tag;
    private double totalCalories;
    private double totalProteins;
    private double totalCarbs;
    private double totalFats;
    private double weightStatus;
    private boolean gotRecords;
    private Date dateInit;
    private Date dateEnd;
}