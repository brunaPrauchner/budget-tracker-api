package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.CategoryResponse;
import com.example.demo.dto.CategoryPatchRequest;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ExpenseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true) // default for the class
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;

    public CategoryService(CategoryRepository categoryRepository, ExpenseRepository expenseRepository) {
        this.categoryRepository = categoryRepository;
        this.expenseRepository = expenseRepository;
    }

    @Transactional // overrides readOnly=true
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setMonthlyBudgetLimit(request.getMonthlyBudgetLimit());

        Category saved = categoryRepository.save(category);
        return toResponse(saved);
    }

    @Transactional
    public CategoryResponse updateCategory(UUID id, CategoryRequest request) {
        Category category = getCategory(id);
        if (!category.getName().equalsIgnoreCase(request.getName())
                && categoryRepository.existsByNameIgnoreCase(request.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
        }
        category.setName(request.getName());
        category.setMonthlyBudgetLimit(request.getMonthlyBudgetLimit());
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryResponse patchCategory(UUID id, CategoryPatchRequest request) {
        if (request.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No fields to update");
        }
        Category category = getCategory(id);
        if (request.getName() != null) {
            if (!category.getName().equalsIgnoreCase(request.getName())
                    && categoryRepository.existsByNameIgnoreCase(request.getName())) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Category name already exists");
            }
            category.setName(request.getName());
        }
        if (request.getMonthlyBudgetLimit() != null) {
            category.setMonthlyBudgetLimit(request.getMonthlyBudgetLimit());
        }
        return toResponse(categoryRepository.save(category));
    }

    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public Category getCategory(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Category not found"));
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category category = getCategory(id);
        if (expenseRepository.existsByCategory(category)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Category has expenses and cannot be deleted");
        }
        categoryRepository.delete(category);
    }

    private CategoryResponse toResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setMonthlyBudgetLimit(category.getMonthlyBudgetLimit());
        response.setCreatedAt(category.getCreatedAt());
        response.setUpdatedAt(category.getUpdatedAt());
        return response;
    }
}
