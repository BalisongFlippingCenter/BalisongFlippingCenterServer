package com.example.BalisongFlipping.repositories;

import com.example.BalisongFlipping.enums.reports.ReportStatus;
import com.example.BalisongFlipping.enums.reports.TargetType;
import com.example.BalisongFlipping.modals.reports.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterAccountIdAndTargetTypeAndTargetId(Long reporterAccountId, TargetType targetType, Long targetId);

    Page<Report> findAll(Pageable pageable);
    Page<Report> findByStatus(ReportStatus status, Pageable pageable);
    Page<Report> findByTargetType(TargetType targetType, Pageable pageable);
    Page<Report> findByStatusAndTargetType(ReportStatus status, TargetType targetType, Pageable pageable);
}
