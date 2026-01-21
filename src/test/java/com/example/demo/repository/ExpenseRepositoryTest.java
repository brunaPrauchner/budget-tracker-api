package com.example.demo.repository;

import com.example.demo.model.Category;
import com.example.demo.model.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class ExpenseRepositoryTest {

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Category food;
    private Category travel;

    @BeforeEach
    void setup() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        food = categoryRepository.save(category("Food"));
        travel = categoryRepository.save(category("Travel"));
    }

    @Test
    void findByOrderBySpentAtDesc_returnsOrderedPage() {
        OffsetDateTime base = OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC);
        expenseRepository.save(expense(food, "A", base));
        expenseRepository.save(expense(food, "B", base.plusMinutes(1)));
        expenseRepository.save(expense(food, "C", base.plusMinutes(2)));

        Page<Expense> page = expenseRepository.findByOrderBySpentAtDesc(PageRequest.of(0, 2, Sort.by("spentAt").descending()));

        assertEquals(2, page.getNumberOfElements());
        assertEquals("C", page.getContent().get(0).getName());
        assertEquals("B", page.getContent().get(1).getName());
    }

    @Test
    void findByCategoryOrderBySpentAtDesc_filtersByCategory() {
        OffsetDateTime now = OffsetDateTime.now();
        expenseRepository.save(expense(food, "FoodA", now));
        expenseRepository.save(expense(travel, "TravelA", now.plusMinutes(1)));
        expenseRepository.save(expense(travel, "TravelB", now.plusMinutes(2)));

        Page<Expense> page = expenseRepository.findByCategoryOrderBySpentAtDesc(travel, PageRequest.of(0, 5));

        assertEquals(2, page.getNumberOfElements());
        assertTrue(page.getContent().stream().allMatch(e -> e.getCategory().getId().equals(travel.getId())));
        assertEquals("TravelB", page.getContent().get(0).getName());
    }

    @Test
    void existsByCategory_returnsTrueWhenExpensePresent() {
        expenseRepository.save(expense(food, "Any", OffsetDateTime.now()));

        assertTrue(expenseRepository.existsByCategory(food));
        assertFalse(expenseRepository.existsByCategory(travel));
    }

    @Test
    void findCategoryTotalsBetween_sumsByCategory() {
        OffsetDateTime jan = OffsetDateTime.of(2025, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC);
        expenseRepository.save(expense(food, "Lunch", jan.plusDays(1), new BigDecimal("10.00")));
        expenseRepository.save(expense(food, "Dinner", jan.plusDays(2), new BigDecimal("15.00")));
        expenseRepository.save(expense(travel, "Taxi", jan.plusDays(3), new BigDecimal("20.00")));
        // Outside range
        expenseRepository.save(expense(food, "NextMonth", jan.plusMonths(1), new BigDecimal("50.00")));

        List<ExpenseRepository.CategoryMonthlyTotalView> totals = expenseRepository.findCategoryTotalsBetween(jan, jan.plusMonths(1));

        assertEquals(2, totals.size());
        ExpenseRepository.CategoryMonthlyTotalView foodTotal = totals.stream()
                .filter(t -> t.getCategoryId().equals(food.getId()))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("25.00"), foodTotal.getTotal());
        assertEquals(food.getName(), foodTotal.getCategoryName());
    }

    private Category category(String name) {
        Category category = new Category();
        category.setName(name + "-" + UUID.randomUUID());
        category.setMonthlyBudgetLimit(new BigDecimal("100.00"));
        return category;
    }

    private Expense expense(Category category, String name, OffsetDateTime spentAt) {
        return expense(category, name, spentAt, new BigDecimal("5.00"));
    }

    private Expense expense(Category category, String name, OffsetDateTime spentAt, BigDecimal amount) {
        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setName(name);
        expense.setAmount(amount);
        expense.setCurrency("USD");
        expense.setSpentAt(spentAt);
        expense.setHoliday(false);
        return expense;
    }
}
