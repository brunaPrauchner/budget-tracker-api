package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class CategoryResponse {

    private UUID id;
    private String name;
    private BigDecimal monthlyBudgetLimit;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
