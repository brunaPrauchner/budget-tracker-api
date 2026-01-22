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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.context.annotation.Import;
import com.example.demo.config.TestSecurityConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

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
@ActiveProfiles("test")
@Import(TestSecurityConfig.class)
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
        restTemplate.getRestTemplate().setRequestFactory(new HttpComponentsClientHttpRequestFactory());
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

        ResponseEntity<Map<String, Object>> dup = restTemplate.exchange(
                "/api/categories",
                HttpMethod.POST,
                new HttpEntity<>(Map.of("name", name, "monthlyBudgetLimit", 50)),
                new ParameterizedTypeReference<Map<String, Object>>() {});
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
                HttpEntity.EMPTY, // exchange requires an entity argument even when it’s empty
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

    @Test
    void listCategories_returnsCreatedCategory() {
        String name = "ListCat-" + UUID.randomUUID();
        restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", name, "monthlyBudgetLimit", 120),
                CategoryDto.class);

        ResponseEntity<List<CategoryDto>> resp = restTemplate.exchange(
                "/api/categories",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<CategoryDto>>() {});

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().stream().anyMatch(c -> name.equals(c.name) && c.monthlyBudgetLimit.intValue() == 120));
    }

    @Test
    void deleteCategory_succeedsWhenNoExpenses() {
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "DeleteCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 80),
                CategoryDto.class);
        UUID categoryId = catResp.getBody().id;

        ResponseEntity<String> delResp = restTemplate.exchange(
                "/api/categories/" + categoryId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                String.class);

        assertEquals(HttpStatus.NO_CONTENT, delResp.getStatusCode());

        ResponseEntity<List<CategoryDto>> listResp = restTemplate.exchange(
                "/api/categories",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<List<CategoryDto>>() {});
        assertTrue(listResp.getBody().stream().noneMatch(c -> categoryId.equals(c.id)));
    }

    @Test
    void deleteCategory_withExpenses_returnsConflict() {
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "HasExpense-" + UUID.randomUUID(), "monthlyBudgetLimit", 200),
                CategoryDto.class);
        UUID categoryId = catResp.getBody().id;

        // create expense to trigger conflict
        restTemplate.postForEntity(
                "/api/expenses",
                Map.of(
                        "categoryId", categoryId,
                        "name", "Meal",
                        "amount", 15.00,
                        "currency", "USD",
                        "spentAt", OffsetDateTime.now().withOffsetSameInstant(ZoneOffset.UTC).toString(),
                        "location", "Cafe"
                ),
                ExpenseDto.class);

        ResponseEntity<Map<String, Object>> delResp = restTemplate.exchange(
                "/api/categories/" + categoryId,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.CONFLICT, delResp.getStatusCode());
        assertNotNull(delResp.getBody());
        assertEquals("Category has expenses and cannot be deleted", delResp.getBody().get("message"));
    }

    @Test
    void createExpense_validationErrors_returnsBadRequest() {
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                "/api/expenses",
                HttpMethod.POST,
                new HttpEntity<>(Map.of(
                        "name", "Invalid",
                        "amount", -5,
                        "currency", "us"
                )),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals(400, resp.getBody().get("status"));
        assertEquals("Validation failed", resp.getBody().get("message"));
    }

    @Test
    void listRecentExpensesByCategory_unknownCategory_returnsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                "/api/categories/" + unknown + "/expenses/recent",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Category not found", resp.getBody().get("message"));
    }

    @Test
    void deleteExpense_unknownId_returnsNotFound() {
        UUID unknown = UUID.randomUUID();
        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                "/api/expenses/" + unknown,
                HttpMethod.DELETE,
                HttpEntity.EMPTY,
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Expense not found", resp.getBody().get("message"));
    }

    @Test
    void updateCategory_put() {
        ResponseEntity<CategoryDto> createResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "PutCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 50),
                CategoryDto.class);
        UUID id = createResp.getBody().id;

        ResponseEntity<CategoryDto> putResp = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "UpdatedName", "monthlyBudgetLimit", 50)),
                CategoryDto.class);
        assertEquals(HttpStatus.OK, putResp.getStatusCode());
        assertEquals("UpdatedName", putResp.getBody().name);
        assertEquals(new BigDecimal("50"), putResp.getBody().monthlyBudgetLimit);
    }

    @Test
    void updateCategory_patch() {
        ResponseEntity<CategoryDto> createResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "PatchCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 50),
                CategoryDto.class);
        UUID id = createResp.getBody().id;

        ResponseEntity<CategoryDto> patchResp = restTemplate.exchange(
                "/api/categories/" + id,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("name", "PatchedName")),
                CategoryDto.class);
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());
        assertEquals("PatchedName", patchResp.getBody().name);
        assertEquals(new BigDecimal("50.00"), patchResp.getBody().monthlyBudgetLimit);
    }

    @Test
    void updateExpense_put() {
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "UpdateExpenseCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 100),
                CategoryDto.class);
        UUID catId = catResp.getBody().id;
        ResponseEntity<ExpenseDto> expResp = restTemplate.postForEntity(
                "/api/expenses",
                Map.of(
                        "categoryId", catId,
                        "name", "Orig",
                        "amount", 10,
                        "currency", "USD",
                        "spentAt", OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toString()
                ),
                ExpenseDto.class);
        UUID expId = expResp.getBody().id;

        ResponseEntity<ExpenseDto> putResp = restTemplate.exchange(
                "/api/expenses/" + expId,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of(
                        "categoryId", catId,
                        "name", "Updated",
                        "amount", 20,
                        "currency", "USD",
                        "spentAt", OffsetDateTime.of(2025, 2, 1, 12, 0, 0, 0, ZoneOffset.UTC).toString()
                )),
                ExpenseDto.class);
        assertEquals(HttpStatus.OK, putResp.getStatusCode());
        assertEquals("Updated", putResp.getBody().name);
        assertEquals(new BigDecimal("20"), putResp.getBody().amount);
    }

    @Test
    void updateExpense_patch() {
        ResponseEntity<CategoryDto> catResp = restTemplate.postForEntity(
                "/api/categories",
                Map.of("name", "PatchExpenseCat-" + UUID.randomUUID(), "monthlyBudgetLimit", 100),
                CategoryDto.class);
        UUID catId = catResp.getBody().id;
        ResponseEntity<ExpenseDto> expResp = restTemplate.postForEntity(
                "/api/expenses",
                Map.of(
                        "categoryId", catId,
                        "name", "Orig",
                        "amount", 10,
                        "currency", "USD",
                        "spentAt", OffsetDateTime.of(2025, 1, 1, 12, 0, 0, 0, ZoneOffset.UTC).toString()
                ),
                ExpenseDto.class);
        UUID expId = expResp.getBody().id;

        ResponseEntity<ExpenseDto> patchResp = restTemplate.exchange(
                "/api/expenses/" + expId,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("location", "PatchedLoc")),
                ExpenseDto.class);
        assertEquals(HttpStatus.OK, patchResp.getStatusCode());
        assertEquals("PatchedLoc", patchResp.getBody().location);
        assertEquals("Orig", patchResp.getBody().name);
        assertEquals(new BigDecimal("10.00"), patchResp.getBody().amount);
    }

    @Test
    void updateCategory_unknownId_putReturnsNotFound() {
        UUID unknown = UUID.randomUUID();

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                "/api/categories/" + unknown,
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", "Nope", "monthlyBudgetLimit", 10)),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Category not found", resp.getBody().get("message"));
    }

    @Test
    void updateExpense_unknownId_patchReturnsNotFound() {
        UUID unknown = UUID.randomUUID();

        ResponseEntity<Map<String, Object>> resp = restTemplate.exchange(
                "/api/expenses/" + unknown,
                HttpMethod.PATCH,
                new HttpEntity<>(Map.of("location", "Nowhere")),
                new ParameterizedTypeReference<Map<String, Object>>() {});

        assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertEquals("Expense not found", resp.getBody().get("message"));
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
