package com.example.demo.service;

import com.example.demo.dto.CategoryRequest;
import com.example.demo.dto.CategoryResponse;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

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
