package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.CategoryResponse;
import com.example.demo.model.Category;
import com.example.demo.model.Expense;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(CategoryService.class)
class CategoryServiceTest {

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

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

    @Test
    void listCategories_returnsCreatedCategory() {
        CategoryResponse created = categoryService.createCategory(requestWithName("ListCat"));

        List<CategoryResponse> categories = categoryService.listCategories();

        assertFalse(categories.isEmpty());
        assertTrue(categories.stream().anyMatch(c -> c.getId().equals(created.getId()) && c.getName().equals(created.getName())));
    }

    @Test
    void getCategory_returnsExisting() {
        CategoryResponse created = categoryService.createCategory(requestWithName("GetCat"));

        Category found = categoryService.getCategory(created.getId());

        assertEquals(created.getId(), found.getId());
        assertEquals(created.getName(), found.getName());
    }

    @Test
    void getCategory_unknownId_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.getCategory(unknown));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteCategory_succeedsWhenEmpty() {
        CategoryResponse created = categoryService.createCategory(requestWithName("DeleteEmpty"));

        categoryService.deleteCategory(created.getId());

        assertFalse(categoryRepository.findById(created.getId()).isPresent());
    }

    @Test
    void deleteCategory_unknown_throwsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(unknown));
        assertEquals(404, ex.getStatusCode().value());
    }

    @Test
    void deleteCategory_withExpenses_throwsConflict() {
        Category category = categoryRepository.save(toCategory("WithExpense"));
        expenseRepository.save(toExpense(category));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> categoryService.deleteCategory(category.getId()));
        assertEquals(409, ex.getStatusCode().value());
    }

    private CategoryRequest requestWithName(String prefix) {
        CategoryRequest request = new CategoryRequest();
        request.setName(prefix + "-" + System.nanoTime());
        request.setMonthlyBudgetLimit(new BigDecimal("100.00"));
        return request;
    }

    private Category toCategory(String name) {
        Category category = new Category();
        category.setName(name + "-" + System.nanoTime());
        category.setMonthlyBudgetLimit(new BigDecimal("50.00"));
        return category;
    }

    private Expense toExpense(Category category) {
        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setName("TestExpense");
        expense.setAmount(new BigDecimal("10.00"));
        expense.setCurrency("USD");
        expense.setSpentAt(OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC));
        expense.setHoliday(false);
        return expense;
    }
}
