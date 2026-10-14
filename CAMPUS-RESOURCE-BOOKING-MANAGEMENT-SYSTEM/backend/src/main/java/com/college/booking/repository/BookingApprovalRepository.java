package com.college.booking.repository;

import com.college.booking.entity.BookingApproval;
import com.college.booking.enums.ApprovalStatus;
import com.college.booking.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BookingApprovalRepository extends JpaRepository<BookingApproval, Long> {
    List<BookingApproval> findByBookingId(Long bookingId);
    List<BookingApproval> findByRequiredRoleAndStatus(Role role, ApprovalStatus status);
    Optional<BookingApproval> findByBookingIdAndRequiredRole(Long bookingId, Role role);
    long countByRequiredRoleAndStatus(Role role, ApprovalStatus status);
}
