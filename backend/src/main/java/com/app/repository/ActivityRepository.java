package com.app.repository;

import com.app.model.Activity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    List<Activity> findByNameContainingIgnoreCaseOrUserNameContainingIgnoreCaseOrStatusContainingIgnoreCase(
        String name, String userName, String status);
}
