package com.example.demo;

import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ExpenseRepository;
import com.example.demo.holiday.HolidayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @MockBean
    private HolidayService holidayService;

    @BeforeEach
    void clean() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
        when(holidayService.findHoliday(any())).thenReturn(java.util.Optional.of("Test Holiday"));
    }

    @Test
    void createExpense_and_fetchRecent_and_summary() {
        // create category
        String categoryName = "Food Out" + UUID.randomUUID();
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", categoryName, "monthlyBudgetLimit", 200),
                CategoryDto.class);
        assertEquals(HttpStatus.CREATED, catResp.getStatusCode());
        assertNotNull(catResp.getBody());
        UUID categoryId = catResp.getBody().id;

        // create expense
        OffsetDateTime spentAt = OffsetDateTime.of(2025, 1, 3, 18, 0, 0, 0, ZoneOffset.UTC);
        ResponseEntity<ExpenseDto> expResp = restTemplate.postForEntity(
                "/api/expenses",
                Map.of(
                        "categoryId", categoryId,
                        "name", "Sushi",
                        "amount", 18.50,
                        "currency", "USD",
                        "spentAt", spentAt.toString(),
                        "location", "Downtown Market"
                ),
                ExpenseDto.class);
        assertEquals(HttpStatus.CREATED, expResp.getStatusCode());
        assertNotNull(expResp.getBody());
        assertEquals("Sushi", expResp.getBody().name);
        assertEquals("Downtown Market", expResp.getBody().location);
        assertTrue(expResp.getBody().holiday);
        assertEquals("Test Holiday", expResp.getBody().holidayName);

        // recent expenses
        ResponseEntity<List<ExpenseDto>> recentResp = restTemplate.exchange(
                "/api/expenses/recent?limit=5",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ExpenseDto>>() {});
        assertEquals(HttpStatus.OK, recentResp.getStatusCode());
        assertNotNull(recentResp.getBody());
        assertFalse(recentResp.getBody().isEmpty());
        assertEquals("Sushi", recentResp.getBody().get(0).name);

        // monthly summary
        ResponseEntity<List<MonthlyTotalDto>> summaryResp = restTemplate.exchange(
                "/api/summary/monthly?year=2025&month=1",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<MonthlyTotalDto>>() {});
        assertEquals(HttpStatus.OK, summaryResp.getStatusCode());
        assertNotNull(summaryResp.getBody());
        assertFalse(summaryResp.getBody().isEmpty());

        MonthlyTotalDto summary = summaryResp.getBody().stream()
                .filter(it -> categoryId.equals(it.categoryId))
                .findFirst()
                .orElseThrow();
        assertEquals(new BigDecimal("18.50"), summary.total);
    }

    @Test
    void createDuplicateCategory_returnsConflict() {
        String name = "UniqueCat-" + UUID.randomUUID();

        ResponseEntity<CategoryDto> first = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", name, "monthlyBudgetLimit", 100),
                CategoryDto.class);
        assertEquals(HttpStatus.CREATED, first.getStatusCode());

        ResponseEntity<Map> dup = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", name, "monthlyBudgetLimit", 50),
                Map.class);
        assertEquals(HttpStatus.CONFLICT, dup.getStatusCode());
        assertNotNull(dup.getBody());
        assertEquals("Category name already exists", dup.getBody().get("message"));
    }

    @Test
    void deleteExpense_succeeds() {
        // create category
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "TempCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 100),
                CategoryDto.class);
        UUID categoryId = catResp.getBody().id;

        // create expense
        ResponseEntity<ExpenseDto> expResp = restTemplate.postForEntity(
                "/api/expenses",
                Map.of(
                        "categoryId", categoryId,
                        "name", "DeleteMe",
                        "amount", 10.00,
                        "currency", "USD",
                        "spentAt", OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString(),
                        "location", "X"
                ),
                ExpenseDto.class);
        UUID expenseId = expResp.getBody().id;

        // delete
        ResponseEntity<String> delResp = restTemplate.exchange(
                "/api/expenses/" + expenseId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);
        assertEquals(HttpStatus.NO_CONTENT, delResp.getStatusCode(), () -> "Delete body: " + delResp.getBody());

        // verify absent
        ResponseEntity<List<ExpenseDto>> recent = restTemplate.exchange(
                "/api/expenses/recent?limit=5",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<ExpenseDto>>() {});
        assertTrue(recent.getBody().stream().noneMatch(e -> e.id.equals(expenseId)));
    }

    // Simple DTOs for deserialization
    static class CategoryDto {
        public UUID id;
        public String name;
        public BigDecimal monthlyBudgetLimit;
    }

    static class ExpenseDto {
        public UUID id;
        public UUID categoryId;
        public String categoryName;
        public String name;
        public BigDecimal amount;
        public String currency;
        public String location;
        public OffsetDateTime spentAt;
        public boolean holiday;
        public String holidayName;
    }

    static class MonthlyTotalDto {
        public UUID categoryId;
        public String categoryName;
        public int year;
        public int month;
        public BigDecimal total;
    }
}
