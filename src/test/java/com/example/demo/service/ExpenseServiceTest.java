package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.ExpenseRequest;
import com.example.demo.dto.ExpenseResponse;
import com.example.demo.dto.MonthlyCategoryTotalResponse;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    private Category food;
    private Category transport;

    @BeforeEach
    void setup() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();

        food = createCategory("Food Out");
        transport = createCategory("Transport");
    }

    @Test
    void createExpense_persistsWithLocation() {
        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(food.getId());
        request.setName("Sushi");
        request.setAmount(new BigDecimal("18.50"));
        request.setCurrency("USD");
        request.setSpentAt(OffsetDateTime.of(2025, 1, 3, 18, 0, 0, 0, ZoneOffset.ofHours(-5)));
        request.setLocation("Downtown Market");

        ExpenseResponse response = expenseService.createExpense(request);

        assertNotNull(response.getId());
        assertEquals(food.getId(), response.getCategoryId());
        assertEquals("Sushi", response.getName());
        assertEquals(new BigDecimal("18.50"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(request.getSpentAt(), response.getSpentAt());
        assertEquals("Downtown Market", response.getLocation());
    }

    @Test
    void listRecentExpenses_respectsLimit() {
        createExpense(food.getId(), "A", new BigDecimal("5.00"), OffsetDateTime.now());
        createExpense(food.getId(), "B", new BigDecimal("6.00"), OffsetDateTime.now().plusMinutes(1));
        createExpense(food.getId(), "C", new BigDecimal("7.00"), OffsetDateTime.now().plusMinutes(2));

        List<ExpenseResponse> responses = expenseService.listRecentExpenses(2);
        assertEquals(2, responses.size());
        assertEquals("C", responses.get(0).getName());
        assertEquals("B", responses.get(1).getName());
    }

    @Test
    void monthlyTotals_returnsSumPerCategory() {
        OffsetDateTime jan1 = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        createExpense(food.getId(), "Lunch", new BigDecimal("12.00"), jan1.plusDays(1));
        createExpense(food.getId(), "Dinner", new BigDecimal("20.00"), jan1.plusDays(2));
        createExpense(transport.getId(), "Bus", new BigDecimal("5.00"), jan1.plusDays(3));

        // Outside month should not be counted
        createExpense(food.getId(), "Late", new BigDecimal("50.00"), jan1.plusMonths(1));

        List<MonthlyCategoryTotalResponse> totals = expenseService.calculateMonthlyTotals(2025, 1);

        assertEquals(2, totals.size());
        assertEquals(new BigDecimal("32.00"), totals.stream().filter(t -> t.getCategoryId().equals(food.getId())).findFirst().orElseThrow().getTotal());
        assertEquals(new BigDecimal("5.00"), totals.stream().filter(t -> t.getCategoryId().equals(transport.getId())).findFirst().orElseThrow().getTotal());
    }

    private Category createCategory(String name) {
        CategoryRequest request = new CategoryRequest();
        request.setName(name + "-" + UUID.randomUUID());
        request.setMonthlyBudgetLimit(new BigDecimal("100.00"));
        return categoryRepository.findByNameIgnoreCase(categoryService.createCategory(request).getName()).orElseThrow();
    }

    private ExpenseResponse createExpense(UUID categoryId, String name, BigDecimal amount, OffsetDateTime spentAt) {
        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(categoryId);
        request.setName(name);
        request.setAmount(amount);
        request.setCurrency("USD");
        request.setSpentAt(spentAt);
        return expenseService.createExpense(request);
    }
}
