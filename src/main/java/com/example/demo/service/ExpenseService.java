package com.example.demo.service;

import com.example.demo.dto.ExpenseRequest;
import com.example.demo.dto.ExpenseResponse;
import com.example.demo.dto.MonthlyCategoryTotalResponse;
import com.example.demo.holiday.HolidayService;
import com.example.demo.model.Category;
import com.example.demo.model.Expense;
import com.example.demo.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Service
public class ExpenseService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ExpenseRepository expenseRepository;
    private final CategoryService categoryService;
    private final HolidayService holidayService;

    public ExpenseService(ExpenseRepository expenseRepository, CategoryService categoryService, HolidayService holidayService) {
        this.expenseRepository = expenseRepository;
        this.categoryService = categoryService;
        this.holidayService = holidayService;
    }

    public ExpenseResponse createExpense(ExpenseRequest request) {
        Category category = categoryService.getCategory(request.getCategoryId());

        Expense expense = new Expense();
        expense.setCategory(category);
        expense.setName(request.getName());
        expense.setAmount(request.getAmount());
        expense.setCurrency(request.getCurrency().toUpperCase());
        expense.setSpentAt(request.getSpentAt());
        expense.setLocation(request.getLocation());

        holidayService.findHoliday(request.getSpentAt().toLocalDate())
                .ifPresentOrElse(name -> {
                    expense.setHoliday(true);
                    expense.setHolidayName(name);
                }, () -> {
                    expense.setHoliday(false);
                    expense.setHolidayName(null);
                });

        Expense saved = expenseRepository.save(expense);
        return toResponse(saved);
    }

    public List<ExpenseResponse> listRecentExpenses(int limit) {
        Pageable pageable = PageRequest.of(0, clampLimit(limit), Sort.by(Sort.Direction.DESC, "spentAt", "createdAt"));
        Page<Expense> page = expenseRepository.findByOrderBySpentAtDesc(pageable);
        return page.map(this::toResponse).toList();
    }

    public List<ExpenseResponse> listRecentExpensesByCategory(UUID categoryId, int limit) {
        Category category = categoryService.getCategory(categoryId);
        Pageable pageable = PageRequest.of(0, clampLimit(limit), Sort.by(Sort.Direction.DESC, "spentAt", "createdAt"));
        Page<Expense> page = expenseRepository.findByCategoryOrderBySpentAtDesc(category, pageable);
        return page.map(this::toResponse).toList();
    }

    public List<MonthlyCategoryTotalResponse> calculateMonthlyTotals(int year, int month) {
        if (month < 1 || month > 12) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Month must be between 1 and 12");
        }
        LocalDate startDate = LocalDate.of(year, month, 1);
        OffsetDateTime start = startDate.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = startDate.plusMonths(1).atStartOfDay().atOffset(ZoneOffset.UTC);

        List<ExpenseRepository.CategoryMonthlyTotalView> totals = expenseRepository.findCategoryTotalsBetween(start, end);
        return totals.stream().map(view -> {
            MonthlyCategoryTotalResponse response = new MonthlyCategoryTotalResponse();
            response.setCategoryId(view.getCategoryId());
            response.setCategoryName(view.getCategoryName());
            response.setYear(year);
            response.setMonth(month);
            response.setTotal(view.getTotal() == null ? BigDecimal.ZERO : view.getTotal());
            return response;
        }).toList();
    }

    public void deleteExpense(UUID id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Expense not found"));
        expenseRepository.delete(expense);
    }

    private ExpenseResponse toResponse(Expense expense) {
        ExpenseResponse response = new ExpenseResponse();
        response.setId(expense.getId());
        response.setCategoryId(expense.getCategory().getId());
        response.setCategoryName(expense.getCategory().getName());
        response.setName(expense.getName());
        response.setAmount(expense.getAmount());
        response.setCurrency(expense.getCurrency());
        response.setSpentAt(expense.getSpentAt());
        response.setLocation(expense.getLocation());
        response.setHoliday(expense.isHoliday());
        response.setHolidayName(expense.getHolidayName());
        response.setCreatedAt(expense.getCreatedAt());
        response.setUpdatedAt(expense.getUpdatedAt());
        return response;
    }

    private int clampLimit(int limit) {
        if (limit <= 0) {
            return 10;
        }
        return Math.min(limit, MAX_PAGE_SIZE);
    }
}
