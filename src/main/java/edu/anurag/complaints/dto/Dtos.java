package edu.anurag.complaints.dto;

import edu.anurag.complaints.model.*;
import jakarta.validation.constraints.*;

import java.time.Instant;
import java.util.Map;

public final class Dtos {

    private Dtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            @Size(max = 30) String rollNumber,
            @Size(max = 100) String department) {}

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password) {}

    public record UserResponse(Long id, String name, String email, Role role,
                               String rollNumber, String department) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(),
                    u.getRollNumber(), u.getDepartment());
        }
    }

    public record AuthResponse(String token, UserResponse user) {}

    public record ComplaintRequest(
            @NotBlank @Size(max = 150) String title,
            @NotBlank @Size(max = 2000) String description,
            @NotNull Category category,
            Priority priority) {}

    public record StatusUpdateRequest(
            @NotNull Status status,
            @Size(max = 2000) String adminResponse) {}

    public record ComplaintResponse(
            Long id, String title, String description,
            Category category, Priority priority, Status status,
            String adminResponse,
            Long studentId, String studentName, String studentEmail,
            String rollNumber, String department,
            Instant createdAt, Instant updatedAt) {
        public static ComplaintResponse from(Complaint c) {
            User s = c.getStudent();
            return new ComplaintResponse(c.getId(), c.getTitle(), c.getDescription(),
                    c.getCategory(), c.getPriority(), c.getStatus(), c.getAdminResponse(),
                    s.getId(), s.getName(), s.getEmail(), s.getRollNumber(), s.getDepartment(),
                    c.getCreatedAt(), c.getUpdatedAt());
        }
    }

    public record StatsResponse(long total, Map<Status, Long> byStatus) {}
}
