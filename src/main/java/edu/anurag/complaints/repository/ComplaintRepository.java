package edu.anurag.complaints.repository;

import edu.anurag.complaints.model.Complaint;
import edu.anurag.complaints.model.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByStatusOrderByCreatedAtDesc(Status status);
    long countByStatus(Status status);
}
