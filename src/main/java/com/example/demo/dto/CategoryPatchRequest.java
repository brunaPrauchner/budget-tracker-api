package com.example.demo.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public class CategoryPatchRequest {

    @Size(max = 100)
    private String name;

    @PositiveOrZero
    @Digits(integer = 17, fraction = 2)
    private BigDecimal monthlyBudgetLimit;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getMonthlyBudgetLimit() {
        return monthlyBudgetLimit;
    }

    public void setMonthlyBudgetLimit(BigDecimal monthlyBudgetLimit) {
        this.monthlyBudgetLimit = monthlyBudgetLimit;
    }

    public boolean isEmpty() {
        return name == null && monthlyBudgetLimit == null;
    }
}
