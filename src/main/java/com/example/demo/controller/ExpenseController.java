package com.example.demo.controller;

import com.example.demo.dto.ExpenseRequest;
import com.example.demo.dto.ExpenseResponse;
import com.example.demo.dto.MonthlyCategoryTotalResponse;
import com.example.demo.dto.ExpensePatchRequest;
import com.example.demo.service.ExpenseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ExpenseController {

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping("/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse createExpense(@Valid @RequestBody ExpenseRequest request) {
        return expenseService.createExpense(request);
    }

    @PutMapping("/expenses/{expenseId}")
    public ExpenseResponse updateExpense(@PathVariable UUID expenseId, @Valid @RequestBody ExpenseRequest request) {
        return expenseService.updateExpense(expenseId, request);
    }

    @PatchMapping("/expenses/{expenseId}")
    public ExpenseResponse patchExpense(@PathVariable UUID expenseId, @Valid @RequestBody ExpensePatchRequest request) {
        return expenseService.patchExpense(expenseId, request);
    }

    @GetMapping("/expenses/recent")
    public List<ExpenseResponse> recentExpenses(@RequestParam(defaultValue = "10") int limit) {
        return expenseService.listRecentExpenses(limit);
    }

    @GetMapping("/categories/{categoryId}/expenses/recent")
    public List<ExpenseResponse> recentExpensesByCategory(@PathVariable UUID categoryId,
                                                          @RequestParam(defaultValue = "10") int limit) {
        return expenseService.listRecentExpensesByCategory(categoryId, limit);
    }

    @GetMapping("/summary/monthly")
    public List<MonthlyCategoryTotalResponse> monthlyTotals(@RequestParam int year, @RequestParam int month) {
        return expenseService.calculateMonthlyTotals(year, month);
    }

    @DeleteMapping("/expenses/{expenseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExpense(@PathVariable UUID expenseId) {
        expenseService.deleteExpense(expenseId);
    }
}
