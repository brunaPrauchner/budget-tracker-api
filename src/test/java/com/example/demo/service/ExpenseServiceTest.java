package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.ExpenseRequest;
import com.example.demo.dto.ExpenseResponse;
import com.example.demo.dto.MonthlyCategoryTotalResponse;
import com.example.demo.holiday.HolidayService;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ExpenseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@DataJpaTest
@Import({ExpenseService.class, CategoryService.class})
class ExpenseServiceTest {

    @Autowired
    private ExpenseService expenseService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @MockBean
    private HolidayService holidayService;

    private Category food;
    private Category transport;

    @BeforeEach
    void setup() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        when(holidayService.findHoliday(any())).thenReturn(java.util.Optional.empty());

        food = createCategory("Food Out");
        transport = createCategory("Transport");
    }

    @Test
    void createExpense_persistsWithLocation() {
        //Arrange
        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(food.getId());
        request.setName("Sushi");
        request.setAmount(new BigDecimal("18.50"));
        request.setCurrency("USD");
        request.setSpentAt(OffsetDateTime.of(2025, 1, 3, 18, 0, 0, 0, ZoneOffset.ofHours(-5)));
        request.setLocation("Downtown Market");

        //Act
        ExpenseResponse response = expenseService.createExpense(request);

        //Assert
        assertNotNull(response.getId());
        assertEquals(food.getId(), response.getCategoryId());
        assertEquals("Sushi", response.getName());
        assertEquals(new BigDecimal("18.50"), response.getAmount());
        assertEquals("USD", response.getCurrency());
        assertEquals(request.getSpentAt(), response.getSpentAt());
        assertEquals("Downtown Market", response.getLocation());
        assertFalse(response.isHoliday());
        assertNull(response.getHolidayName());
    }

    @Test
    void createExpense_setsHolidayFieldsWhenHolidayFound() {
        when(holidayService.findHoliday(any())).thenReturn(java.util.Optional.of("Test Holiday"));
        ExpenseRequest request = new ExpenseRequest();
        request.setCategoryId(food.getId());
        request.setName("Holiday Meal");
        request.setAmount(new BigDecimal("25.00"));
        request.setCurrency("usd");
        request.setSpentAt(OffsetDateTime.of(2025, 12, 25, 12, 0, 0, 0, ZoneOffset.UTC));

        ExpenseResponse response = expenseService.createExpense(request);

        assertTrue(response.isHoliday());
        assertEquals("Test Holiday", response.getHolidayName());
        assertEquals("USD", response.getCurrency());
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
    void listRecentExpensesByCategory_filtersAndOrders() {
        createExpense(food.getId(), "Food-A", new BigDecimal("5.00"), OffsetDateTime.now());
        createExpense(transport.getId(), "Bus-A", new BigDecimal("3.00"), OffsetDateTime.now().plusMinutes(1));
        createExpense(transport.getId(), "Bus-B", new BigDecimal("4.00"), OffsetDateTime.now().plusMinutes(2));

        List<ExpenseResponse> responses = expenseService.listRecentExpensesByCategory(transport.getId(), 1);

        assertEquals(1, responses.size());
        assertEquals("Bus-B", responses.get(0).getName());
        assertEquals(transport.getId(), responses.get(0).getCategoryId());
    }

    @Test
    void listRecentExpensesByCategory_unknownCategory_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> expenseService.listRecentExpensesByCategory(unknown, 5));
        assertEquals(404, ex.getStatusCode().value());
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

    @Test
    void monthlyTotals_invalidMonth_throwsBadRequest() {
        ResponseStatusException tooLow = assertThrows(ResponseStatusException.class, () -> expenseService.calculateMonthlyTotals(2025, 0));
        ResponseStatusException tooHigh = assertThrows(ResponseStatusException.class, () -> expenseService.calculateMonthlyTotals(2025, 13));
        assertEquals(400, tooLow.getStatusCode().value());
        assertEquals(400, tooHigh.getStatusCode().value());
    }

    @Test
    void monthlyTotals_noExpenses_returnsEmptyList() {
        List<MonthlyCategoryTotalResponse> totals = expenseService.calculateMonthlyTotals(2030, 1);
        assertTrue(totals.isEmpty());
    }

    @Test
    void deleteExpense_removesExisting() {
        ExpenseResponse expense = createExpense(food.getId(), "Temp", new BigDecimal("8.00"), OffsetDateTime.now());

        expenseService.deleteExpense(expense.getId());

        assertFalse(expenseRepository.findById(expense.getId()).isPresent());
    }

    @Test
    void deleteExpense_unknownId_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> expenseService.deleteExpense(unknown));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void updateExpense_updatesAllFields() {
        ExpenseResponse existing = createExpense(food.getId(), "Orig", new BigDecimal("10.00"), OffsetDateTime.now());
        OffsetDateTime newDate = OffsetDateTime.of(2026, 1, 1, 10, 0, 0, 0, ZoneOffset.UTC);
        ExpenseRequest update = new ExpenseRequest();
        update.setCategoryId(transport.getId());
        update.setName("Updated");
        update.setAmount(new BigDecimal("20.00"));
        update.setCurrency("usd");
        update.setSpentAt(newDate);
        update.setLocation("NewLoc");

        ExpenseResponse updated = expenseService.updateExpense(existing.getId(), update);

        assertEquals(transport.getId(), updated.getCategoryId());
        assertEquals("Updated", updated.getName());
        assertEquals(new BigDecimal("20.00"), updated.getAmount());
        assertEquals("USD", updated.getCurrency());
        assertEquals(newDate, updated.getSpentAt());
        assertEquals("NewLoc", updated.getLocation());
    }

    @Test
    void patchExpense_updatesSubsetOrRejectsEmpty() {
        ExpenseResponse existing = createExpense(food.getId(), "Orig", new BigDecimal("10.00"), OffsetDateTime.now());

        com.example.demo.dto.ExpensePatchRequest empty = new com.example.demo.dto.ExpensePatchRequest();
        ResponseStatusException emptyEx = assertThrows(ResponseStatusException.class, () -> expenseService.patchExpense(existing.getId(), empty));
        assertEquals(400, emptyEx.getStatusCode().value());

        com.example.demo.dto.ExpensePatchRequest patch = new com.example.demo.dto.ExpensePatchRequest();
        patch.setLocation("PatchedLoc");
        patch.setAmount(new BigDecimal("15.00"));

        ExpenseResponse patched = expenseService.patchExpense(existing.getId(), patch);
        assertEquals("PatchedLoc", patched.getLocation());
        assertEquals(new BigDecimal("15.00"), patched.getAmount());
        assertEquals(existing.getName(), patched.getName());
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
