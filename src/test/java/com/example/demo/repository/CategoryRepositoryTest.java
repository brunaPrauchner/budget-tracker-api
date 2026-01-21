package com.example.demo.repository;

import com.example.demo.model.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class CategoryRepositoryTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void existsByNameIgnoreCase_respectsCaseInsensitivity() {
        Category saved = categoryRepository.save(category("Groceries"));

        assertTrue(categoryRepository.existsByNameIgnoreCase("groceries"));
        assertTrue(categoryRepository.existsByNameIgnoreCase(saved.getName()));
        assertFalse(categoryRepository.existsByNameIgnoreCase("other"));
    }

    @Test
    void findByNameIgnoreCase_returnsMatch() {
        Category saved = categoryRepository.save(category("Utilities"));

        Optional<Category> found = categoryRepository.findByNameIgnoreCase("utilities");

        assertTrue(found.isPresent());
        assertEquals(saved.getId(), found.get().getId());
        assertEquals(saved.getName(), found.get().getName());
    }

    private Category category(String name) { //factory to create a valid Category entity for the tests
        Category category = new Category();
        category.setName(name);
        category.setMonthlyBudgetLimit(new BigDecimal("100.00"));
        return category;
    }
}
