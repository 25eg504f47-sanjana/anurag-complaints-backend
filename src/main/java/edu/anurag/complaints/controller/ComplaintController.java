package edu.anurag.complaints.controller;

import edu.anurag.complaints.dto.Dtos.*;
import edu.anurag.complaints.model.Status;
import edu.anurag.complaints.security.JwtService.AuthUser;
import edu.anurag.complaints.service.ComplaintService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintService service;

    public ComplaintController(ComplaintService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    @ResponseStatus(HttpStatus.CREATED)
    public ComplaintResponse create(@AuthenticationPrincipal AuthUser me, @Valid @RequestBody ComplaintRequest req) {
        return service.create(me, req);
    }

    @GetMapping
    public List<ComplaintResponse> list(@AuthenticationPrincipal AuthUser me,
                                        @RequestParam(required = false) Status status) {
        return service.list(me, status);
    }

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public StatsResponse stats() {
        return service.stats();
    }

    @GetMapping("/{id}")
    public ComplaintResponse get(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        return service.get(me, id);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return service.updateStatus(id, req);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AuthUser me, @PathVariable Long id) {
        service.delete(me, id);
    }
}
