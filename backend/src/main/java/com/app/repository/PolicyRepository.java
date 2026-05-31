package com.app.repository;

import com.app.model.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PolicyRepository extends JpaRepository<Policy, Long> {
    List<Policy> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrStatusContainingIgnoreCase(
        String name, String category, String status);
}
