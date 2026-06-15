package com.cinebook.service;

import com.cinebook.dto.AdminBookingResponse;
import com.cinebook.dto.BookingRequest;
import com.cinebook.dto.MostBookedMovieResponse;
import com.cinebook.dto.SeatAvailabilityResponse;
import com.cinebook.dto.UserBookingResponse;
import com.cinebook.entity.Booking;
import com.cinebook.entity.BookingSeat;
import com.cinebook.entity.BookingStatus;
import com.cinebook.entity.Movie;
import com.cinebook.entity.SeatStatus;
import com.cinebook.entity.Show;
import com.cinebook.entity.Theater;
import com.cinebook.entity.User;
import com.cinebook.exception.ApiException;
import com.cinebook.repository.BookingRepository;
import com.cinebook.repository.BookingSeatRepository;
import com.cinebook.repository.MovieRepository;
import com.cinebook.repository.MovieSeatAggregate;
import com.cinebook.repository.ReviewRepository;
import com.cinebook.repository.ShowRepository;
import com.cinebook.repository.TheaterRepository;
import com.cinebook.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-side service powering the admin "All Bookings" dashboard. Lists bookings
 * for the calling admin's theater and computes the "Most Booked" leaderboard.
 *
 * <p>Theater scope: callers pass in the {@code theaterId} from the authenticated
 * JWT — never trusted from the client — matching the {@link ShowService} pattern.
 */
@Service
public class BookingService {

    private static final int MOST_BOOKED_LIMIT = 5;

    /** Flat GST rate applied to ticket subtotals. Pull into config if business asks. */
    private static final BigDecimal TAX_RATE = new BigDecimal("0.18");

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final ShowRepository showRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;

    public BookingService(BookingRepository bookingRepository,
                          BookingSeatRepository bookingSeatRepository,
                          ShowRepository showRepository,
                          MovieRepository movieRepository,
                          TheaterRepository theaterRepository,
                          UserRepository userRepository,
                          ReviewRepository reviewRepository) {
        this.bookingRepository = bookingRepository;
        this.bookingSeatRepository = bookingSeatRepository;
        this.showRepository = showRepository;
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
        this.userRepository = userRepository;
        this.reviewRepository = reviewRepository;
    }

    /** All bookings for the admin's own theater, enriched with movie + show + user context. */
    @Transactional(readOnly = true)
    public List<AdminBookingResponse> listForTheater(Long theaterId) {
        requireTheater(theaterId);

        List<Booking> bookings = bookingRepository.findAllByTheaterId(theaterId);
        if (bookings.isEmpty()) {
            return List.of();
        }

        // Build id → entity lookups once so we don't re-query inside the per-row mapping loop.
        List<Long> showIds = bookings.stream().map(Booking::getShowId).distinct().toList();
        Map<Long, Show> showsById = showRepository.findAllById(showIds).stream()
                .collect(Collectors.toMap(Show::getId, Function.identity()));

        List<Long> movieIds = showsById.values().stream().map(Show::getMovieId).distinct().toList();
        Map<Long, Movie> moviesById = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        Theater theater = theaterRepository.findById(theaterId).orElse(null);

        List<Long> userIds = bookings.stream().map(Booking::getUserId).distinct().toList();
        Map<Long, User> usersById = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        List<AdminBookingResponse> rows = new ArrayList<>(bookings.size());
        for (Booking booking : bookings) {
            Show show = showsById.get(booking.getShowId());
            Movie movie = show == null ? null : moviesById.get(show.getMovieId());
            User user = usersById.get(booking.getUserId());
            rows.add(toResponse(booking, show, movie, theater, user));
        }
        return rows;
    }

    /** Top {@value #MOST_BOOKED_LIMIT} movies by summed seats_booked for the admin's theater. */
    @Transactional(readOnly = true)
    public List<MostBookedMovieResponse> mostBookedMovies(Long theaterId) {
        requireTheater(theaterId);

        List<MovieSeatAggregate> aggregates = bookingRepository.sumSeatsBookedByMovie(theaterId);
        if (aggregates.isEmpty()) {
            return List.of();
        }

        List<Long> movieIds = aggregates.stream().map(MovieSeatAggregate::getMovieId).toList();
        Map<Long, Movie> moviesById = movieRepository.findAllById(movieIds).stream()
                .filter(movie -> !movie.isDeleted())
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        // Aggregates already arrive ordered by seat count descending; preserve that order
        // and skip movies that have since been soft-deleted from the catalog.
        return aggregates.stream()
                .map(agg -> {
                    Movie movie = moviesById.get(agg.getMovieId());
                    if (movie == null) return null;
                    return new MostBookedMovieResponse(
                            movie.getId(),
                            movie.getTitle(),
                            movie.getPosterUrl(),
                            agg.getTotalSeats(),
                            agg.getTotalBookings()
                    );
                })
                .filter(java.util.Objects::nonNull)
                .limit(MOST_BOOKED_LIMIT)
                .toList();
    }

    private AdminBookingResponse toResponse(Booking booking, Show show, Movie movie,
                                            Theater theater, User user) {
        AdminBookingResponse row = new AdminBookingResponse();
        row.setId(booking.getId());
        row.setUserId(booking.getUserId());
        row.setUsername(user == null ? "User #" + booking.getUserId() : user.getUsername());

        row.setMovieId(movie == null ? null : movie.getId());
        row.setMovieTitle(movie == null ? "(unavailable)" : movie.getTitle());
        row.setMoviePosterUrl(movie == null ? null : movie.getPosterUrl());

        row.setShowId(booking.getShowId());
        row.setShowTime(show == null ? null : show.getShowTime());
        row.setTheaterName(theater == null ? null : theater.getName());
        row.setTheaterLocation(theater == null ? null : theater.getLocation());

        row.setSeatsBooked(booking.getSeatsBooked());
        row.setSeatNumbers(booking.getSeats());
        row.setSubtotal(booking.getSubtotal());
        row.setTaxAmount(booking.getTaxAmount());
        row.setTotalAmount(booking.getTotalAmount());
        row.setRefundAmount(booking.getRefundAmount());
        row.setStatus(booking.getStatus());
        row.setBookingDate(booking.getBookingDate());
        row.setCancelledAt(booking.getCancelledAt());
        return row;
    }

    private void requireTheater(Long theaterId) {
        if (theaterId == null) {
            throw ApiException.forbidden("No theater is associated with this account");
        }
    }

    // ─── User-facing booking flow ─────────────────────────────────────────────

    /** Current user's bookings, newest first, with movie + show + theater context. */
    @Transactional(readOnly = true)
    public List<UserBookingResponse> listForUser(Long userId) {
        List<Booking> bookings = bookingRepository.findByUserIdOrderByBookingDateDesc(userId);
        if (bookings.isEmpty()) {
            return List.of();
        }
        return enrich(bookings);
    }

    /** Single booking, ownership-checked: 404 if missing, 403 if not the caller's. */
    @Transactional(readOnly = true)
    public UserBookingResponse getUserBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("This booking does not belong to you");
        }
        return enrich(List.of(booking)).get(0);
    }

    /** Layout snapshot used by the seat picker — total seats + already-BOOKED labels. */
    @Transactional(readOnly = true)
    public SeatAvailabilityResponse getSeatAvailability(Long showId) {
        Show show = showRepository.findById(showId)
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> ApiException.notFound("Show not found"));
        List<String> booked = bookingSeatRepository
                .findByShowIdAndStatus(showId, SeatStatus.BOOKED)
                .stream()
                .map(BookingSeat::getSeatLabel)
                .toList();
        return new SeatAvailabilityResponse(show.getId(), show.getTotalSeats(), booked);
    }

    /**
     * Atomic booking creation. Validates that none of the requested seats are
     * already BOOKED, computes totals (18% GST), persists the Booking and
     * per-seat BookingSeat rows, decrements the show's availableSeats counter.
     */
    @Transactional
    public UserBookingResponse createBooking(Long userId, BookingRequest request) {
        Show show = showRepository.findById(request.getShowId())
                .filter(s -> !s.isDeleted())
                .orElseThrow(() -> ApiException.notFound("Show not found"));

        // Normalize labels (trim + uppercase) and reject duplicates within the request itself.
        Set<String> requested = new LinkedHashSet<>();
        for (String raw : request.getSeatLabels()) {
            String label = raw == null ? "" : raw.trim().toUpperCase();
            if (label.isEmpty()) {
                throw ApiException.badRequest("Seat labels must not be blank");
            }
            if (!requested.add(label)) {
                throw ApiException.badRequest("Duplicate seat label: " + label);
            }
        }

        if (show.getAvailableSeats() != null && requested.size() > show.getAvailableSeats()) {
            throw ApiException.badRequest("Only " + show.getAvailableSeats() + " seats remain");
        }

        // Conflict check against existing BOOKED rows for this show.
        Set<String> alreadyBooked = bookingSeatRepository
                .findByShowIdAndStatus(show.getId(), SeatStatus.BOOKED)
                .stream()
                .map(BookingSeat::getSeatLabel)
                .collect(Collectors.toSet());
        for (String label : requested) {
            if (alreadyBooked.contains(label)) {
                throw ApiException.badRequest("Seat " + label + " is already booked");
            }
        }

        BigDecimal price = show.getTicketPrice() == null ? BigDecimal.ZERO : show.getTicketPrice();
        BigDecimal subtotal = price.multiply(BigDecimal.valueOf(requested.size()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal tax = subtotal.multiply(TAX_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        Booking booking = new Booking();
        booking.setShowId(show.getId());
        booking.setUserId(userId);
        booking.setSeats(String.join(",", requested));
        booking.setSeatsBooked(requested.size());
        booking.setSubtotal(subtotal);
        booking.setTaxAmount(tax);
        booking.setTotalAmount(total);
        booking.setRefundAmount(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        booking.setStatus(BookingStatus.CONFIRMED);
        booking.setBookingDate(LocalDateTime.now());
        Booking saved = bookingRepository.save(booking);

        for (String label : requested) {
            BookingSeat seat = new BookingSeat();
            seat.setBookingId(saved.getId());
            seat.setSeatLabel(label);
            seat.setPrice(price);
            seat.setStatus(SeatStatus.BOOKED);
            bookingSeatRepository.save(seat);
        }

        if (show.getAvailableSeats() != null) {
            show.setAvailableSeats(show.getAvailableSeats() - requested.size());
            showRepository.save(show);
        }

        return enrich(List.of(saved)).get(0);
    }

    /** Cancel every still-BOOKED seat on a booking, refunding their summed price. */
    @Transactional
    public UserBookingResponse cancelBooking(Long userId, Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("This booking does not belong to you");
        }
        List<BookingSeat> activeSeats = bookingSeatRepository
                .findByBookingIdAndStatus(bookingId, SeatStatus.BOOKED);
        if (activeSeats.isEmpty()) {
            throw ApiException.badRequest("This booking has no active seats to cancel");
        }
        applyCancellation(booking, activeSeats);
        return enrich(List.of(booking)).get(0);
    }

    /** Partial cancel — only the listed seats. Status becomes PARTIALLY_CANCELLED if any seats remain. */
    @Transactional
    public UserBookingResponse cancelSeats(Long userId, Long bookingId, List<String> seatLabels) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ApiException.notFound("Booking not found"));
        if (!booking.getUserId().equals(userId)) {
            throw ApiException.forbidden("This booking does not belong to you");
        }
        Set<String> requested = seatLabels.stream()
                .map(s -> s == null ? "" : s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (requested.isEmpty()) {
            throw ApiException.badRequest("At least one seat label is required");
        }
        List<BookingSeat> activeSeats = bookingSeatRepository
                .findByBookingIdAndStatus(bookingId, SeatStatus.BOOKED);
        List<BookingSeat> toCancel = new ArrayList<>();
        for (String label : requested) {
            BookingSeat match = activeSeats.stream()
                    .filter(s -> s.getSeatLabel().equalsIgnoreCase(label))
                    .findFirst()
                    .orElseThrow(() -> ApiException.badRequest(
                            "Seat " + label + " is not active on this booking"));
            toCancel.add(match);
        }
        applyCancellation(booking, toCancel);
        return enrich(List.of(booking)).get(0);
    }

    /** Common cancel mechanics — set status, refund total, bump Show.availableSeats. */
    private void applyCancellation(Booking booking, List<BookingSeat> seats) {
        LocalDateTime now = LocalDateTime.now();
        BigDecimal refundDelta = BigDecimal.ZERO;
        for (BookingSeat seat : seats) {
            seat.setStatus(SeatStatus.CANCELLED);
            seat.setCancelledAt(now);
            bookingSeatRepository.save(seat);
            if (seat.getPrice() != null) {
                refundDelta = refundDelta.add(seat.getPrice());
            }
        }
        // Refund-only price is the subtotal portion; tax on cancelled seats is also refunded.
        BigDecimal refundWithTax = refundDelta
                .add(refundDelta.multiply(TAX_RATE))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal currentRefund = booking.getRefundAmount() == null
                ? BigDecimal.ZERO : booking.getRefundAmount();
        booking.setRefundAmount(currentRefund.add(refundWithTax).setScale(2, RoundingMode.HALF_UP));

        long remainingActive = bookingSeatRepository
                .findByBookingIdAndStatus(booking.getId(), SeatStatus.BOOKED)
                .size();
        booking.setStatus(remainingActive == 0
                ? BookingStatus.CANCELLED
                : BookingStatus.PARTIALLY_CANCELLED);
        if (remainingActive == 0) {
            booking.setCancelledAt(now);
        }
        bookingRepository.save(booking);

        // Return cancelled seats to the show's pool.
        showRepository.findById(booking.getShowId()).ifPresent(show -> {
            int avail = show.getAvailableSeats() == null ? 0 : show.getAvailableSeats();
            int cap = show.getTotalSeats() == null ? avail + seats.size() : show.getTotalSeats();
            show.setAvailableSeats(Math.min(avail + seats.size(), cap));
            showRepository.save(show);
        });
    }

    /** Shared enrichment: builds UserBookingResponse rows in one shot, batched by id. */
    private List<UserBookingResponse> enrich(List<Booking> bookings) {
        List<Long> showIds = bookings.stream().map(Booking::getShowId).distinct().toList();
        Map<Long, Show> showsById = showRepository.findAllById(showIds).stream()
                .collect(Collectors.toMap(Show::getId, Function.identity()));

        List<Long> movieIds = showsById.values().stream().map(Show::getMovieId).distinct().toList();
        Map<Long, Movie> moviesById = movieRepository.findAllById(movieIds).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));

        List<Long> theaterIds = showsById.values().stream().map(Show::getTheaterId).distinct().toList();
        Map<Long, Theater> theatersById = theaterRepository.findAllById(theaterIds).stream()
                .collect(Collectors.toMap(Theater::getId, Function.identity()));

        List<UserBookingResponse> rows = new ArrayList<>(bookings.size());
        for (Booking booking : bookings) {
            Show show = showsById.get(booking.getShowId());
            Movie movie = show == null ? null : moviesById.get(show.getMovieId());
            Theater theater = show == null ? null : theatersById.get(show.getTheaterId());
            boolean hasReview = reviewRepository.findByBookingId(booking.getId()).isPresent();
            rows.add(toUserResponse(booking, show, movie, theater, hasReview));
        }
        return rows;
    }

    private UserBookingResponse toUserResponse(Booking booking, Show show, Movie movie,
                                               Theater theater, boolean hasReview) {
        UserBookingResponse row = new UserBookingResponse();
        row.setId(booking.getId());

        row.setMovieId(movie == null ? null : movie.getId());
        row.setMovieTitle(movie == null ? "(unavailable)" : movie.getTitle());
        row.setMoviePosterUrl(movie == null ? null : movie.getPosterUrl());

        row.setShowId(booking.getShowId());
        row.setShowTime(show == null ? null : show.getShowTime());
        row.setTheaterName(theater == null ? null : theater.getName());
        row.setTheaterLocation(theater == null ? null : theater.getLocation());

        row.setSeats(booking.getSeats());
        row.setSeatsBooked(booking.getSeatsBooked());
        row.setSubtotal(booking.getSubtotal());
        row.setTaxAmount(booking.getTaxAmount());
        row.setTotalAmount(booking.getTotalAmount());
        row.setRefundAmount(booking.getRefundAmount());
        row.setStatus(booking.getStatus());
        row.setBookingDate(booking.getBookingDate());
        row.setCancelledAt(booking.getCancelledAt());
        row.setHasReview(hasReview);
        return row;
    }
}
