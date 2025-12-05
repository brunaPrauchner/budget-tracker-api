package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.CategoryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Test
    void createCategory_succeeds() {
        CategoryRequest request = new CategoryRequest();
        request.setName("Groceries" + System.nanoTime());
        request.setMonthlyBudgetLimit(new BigDecimal("150.00"));

        CategoryResponse response = categoryService.createCategory(request);

        assertNotNull(response.getId());
        assertEquals(request.getName(), response.getName());
        assertEquals(request.getMonthlyBudgetLimit(), response.getMonthlyBudgetLimit());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    void createCategory_duplicateName_throwsConflict() {
        String name = "Food Out" + System.nanoTime();

        CategoryRequest first = new CategoryRequest();
        first.setName(name);
        first.setMonthlyBudgetLimit(new BigDecimal("200.00"));
        categoryService.createCategory(first);

        CategoryRequest duplicate = new CategoryRequest();
        duplicate.setName(name);
        duplicate.setMonthlyBudgetLimit(new BigDecimal("150.00"));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.createCategory(duplicate));
        assertEquals(409, ex.getStatusCode().value());
    }
}
