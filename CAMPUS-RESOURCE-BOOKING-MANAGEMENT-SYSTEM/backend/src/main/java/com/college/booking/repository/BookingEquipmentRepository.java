package com.college.booking.repository;

import com.college.booking.entity.BookingEquipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingEquipmentRepository extends JpaRepository<BookingEquipment, Long> {
    List<BookingEquipment> findByBookingId(Long bookingId);
}
