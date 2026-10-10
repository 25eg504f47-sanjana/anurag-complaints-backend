package edu.anurag.complaints.service;

import edu.anurag.complaints.dto.Dtos.*;
import edu.anurag.complaints.model.*;
import edu.anurag.complaints.repository.ComplaintRepository;
import edu.anurag.complaints.repository.UserRepository;
import edu.anurag.complaints.security.JwtService.AuthUser;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class ComplaintService {

    private final ComplaintRepository complaints;
    private final UserRepository users;

    public ComplaintService(ComplaintRepository complaints, UserRepository users) {
        this.complaints = complaints;
        this.users = users;
    }

    public ComplaintResponse create(AuthUser me, ComplaintRequest req) {
        User student = users.findById(me.id())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User no longer exists"));
        Complaint c = new Complaint();
        c.setTitle(req.title().trim());
        c.setDescription(req.description().trim());
        c.setCategory(req.category());
        c.setPriority(req.priority() == null ? Priority.MEDIUM : req.priority());
        c.setStudent(student);
        return ComplaintResponse.from(complaints.save(c));
    }

    @Transactional(readOnly = true)
    public List<ComplaintResponse> list(AuthUser me, Status statusFilter) {
        List<Complaint> found;
        if (me.role() == Role.ADMIN) {
            found = statusFilter == null
                    ? complaints.findAllByOrderByCreatedAtDesc()
                    : complaints.findByStatusOrderByCreatedAtDesc(statusFilter);
        } else {
            found = complaints.findByStudentIdOrderByCreatedAtDesc(me.id());
        }
        return found.stream().map(ComplaintResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ComplaintResponse get(AuthUser me, Long id) {
        return ComplaintResponse.from(loadVisible(me, id));
    }

    public ComplaintResponse updateStatus(Long id, StatusUpdateRequest req) {
        Complaint c = complaints.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        c.setStatus(req.status());
        if (req.adminResponse() != null) {
            c.setAdminResponse(req.adminResponse().trim());
        }
        // flush so @PreUpdate sets updatedAt before the response is built
        return ComplaintResponse.from(complaints.saveAndFlush(c));
    }

    /** Admin can delete anything; a student can delete their own complaint only while it is still OPEN. */
    public void delete(AuthUser me, Long id) {
        Complaint c = loadVisible(me, id);
        if (me.role() != Role.ADMIN && c.getStatus() != Status.OPEN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only open complaints can be withdrawn");
        }
        complaints.delete(c);
    }

    @Transactional(readOnly = true)
    public StatsResponse stats() {
        Map<Status, Long> byStatus = new EnumMap<>(Status.class);
        long total = 0;
        for (Status s : Status.values()) {
            long n = complaints.countByStatus(s);
            byStatus.put(s, n);
            total += n;
        }
        return new StatsResponse(total, byStatus);
    }

    /** Loads a complaint; students get 404 for other students' complaints so ids cannot be probed. */
    private Complaint loadVisible(AuthUser me, Long id) {
        Complaint c = complaints.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (me.role() != Role.ADMIN && !c.getStudent().getId().equals(me.id())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Complaint not found");
        }
        return c;
    }
}
