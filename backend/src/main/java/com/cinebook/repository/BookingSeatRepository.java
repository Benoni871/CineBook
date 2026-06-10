package com.cinebook.repository;

import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.SeatStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BookingSeatRepository extends JpaRepository<BookingSeat, Long> {

    List<BookingSeat> findByBookingId(Long bookingId);

    List<BookingSeat> findByBookingIdAndStatus(Long bookingId, SeatStatus status);

    // Supports future show-level seat-availability checks across a set of bookings.
    List<BookingSeat> findByBookingIdInAndStatus(List<Long> bookingIds, SeatStatus status);
}
