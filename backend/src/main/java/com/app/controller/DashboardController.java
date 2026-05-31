package com.app.controller;

import com.app.dto.SearchResultDto;
import com.app.dto.SummaryDto;
import com.app.model.Activity;
import com.app.model.Policy;
import com.app.model.User;
import com.app.repository.ActivityRepository;
import com.app.repository.PolicyRepository;
import com.app.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final UserRepository     userRepo;
    private final ActivityRepository activityRepo;
    private final PolicyRepository   policyRepo;

    public DashboardController(UserRepository u, ActivityRepository a, PolicyRepository p) {
        this.userRepo     = u;
        this.activityRepo = a;
        this.policyRepo   = p;
    }

    @GetMapping("/summary")
    public SummaryDto summary() {
        long activePolicies = 0;
        for (Policy p : policyRepo.findAll()) {
            if ("Active".equals(p.getStatus())) activePolicies++;
        }
        long failedLogins = 0;
        for (Activity a : activityRepo.findAll()) {
            if ("Failed".equals(a.getStatus())) failedLogins++;
        }
        return new SummaryDto(userRepo.count(), activePolicies, failedLogins, 12);
    }

    @GetMapping("/activities")
    public List<Activity> activities() {
        return activityRepo.findAll();
    }

    @GetMapping("/policies")
    public List<Policy> policies() {
        return policyRepo.findAll();
    }

    @GetMapping("/users")
    public List<User> users() {
        return userRepo.findAll();
    }

    @GetMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(@RequestParam String q) {
        List<SearchResultDto> results = new ArrayList<>();

        for (User u : userRepo.findAll()) {
            String full = u.getFirstName() + " " + u.getLastName();
            if (matches(q, full, u.getRole(), u.getStatus())) {
                results.add(new SearchResultDto("USR-" + u.getId(), full, u.getRole(), "-", u.getStatus()));
            }
        }

        for (Policy p : policyRepo.findAll()) {
            if (matches(q, p.getName(), p.getCategory(), p.getStatus())) {
                results.add(new SearchResultDto(p.getPolicyCode(), p.getName(), "Policy", p.getEffectiveDate(), p.getStatus()));
            }
        }

        for (Activity a : activityRepo.findAll()) {
            if (matches(q, a.getName(), a.getUserName(), a.getStatus())) {
                results.add(new SearchResultDto("ACT-" + a.getId(), a.getName(), "Activity", a.getActivityDate(), a.getStatus()));
            }
        }

        return ResponseEntity.ok(results);
    }

    private boolean matches(String q, String... fields) {
        String lower = q.toLowerCase();
        for (String f : fields) {
            if (f != null && f.toLowerCase().contains(lower)) return true;
        }
        return false;
    }
}
