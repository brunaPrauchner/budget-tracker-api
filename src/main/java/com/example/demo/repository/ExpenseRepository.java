package com.example.demo.repository;

import com.example.demo.model.Category;
import com.example.demo.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface ExpenseRepository extends JpaRepository<Expense, UUID> {

    Page<Expense> findByOrderBySpentAtDesc(Pageable pageable);

    Page<Expense> findByCategoryOrderBySpentAtDesc(Category category, Pageable pageable);

    boolean existsByCategory(Category category);

    @Query("""
            select e.category.id as categoryId,
                   e.category.name as categoryName,
                   sum(e.amount) as total
            from Expense e
            where e.spentAt >= :start and e.spentAt < :end
            group by e.category.id, e.category.name
            order by e.category.name asc
            """)
    List<CategoryMonthlyTotalView> findCategoryTotalsBetween(@Param("start") OffsetDateTime start, @Param("end") OffsetDateTime end);

    interface CategoryMonthlyTotalView {
        UUID getCategoryId();
        String getCategoryName();
        BigDecimal getTotal();
    }
}
