package com.traymate.backend.admin.assignment;

import com.traymate.backend.admin.assignment.dto.AssignResidentRequest;
import com.traymate.backend.admin.assignment.dto.AssignmentStatsDto;
import com.traymate.backend.auth.model.User;
import com.traymate.backend.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/assignments")
@RequiredArgsConstructor
public class AdminAssignmentController {

    private final AdminAssignmentService assignmentService;
    private final UserRepository userRepository;

    @PostMapping
    public void assignResident(@RequestBody AssignResidentRequest request) {
        assignmentService.assignResident(
                request.getResidentId(),
                request.getCaregiverId()
        );
    }

    @GetMapping("/stats")
    public AssignmentStatsDto stats() {
        return assignmentService.getStats();
    }

    @GetMapping("/caregivers")
    public List<User> caregivers() {
        return userRepository.findByRole("ROLE_CAREGIVER");
    }
}
