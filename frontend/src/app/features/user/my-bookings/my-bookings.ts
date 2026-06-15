import { CurrencyPipe, DatePipe, LowerCasePipe } from "@angular/common";
import { Component, OnInit, computed, inject, signal } from "@angular/core";
import { RouterLink } from "@angular/router";
import {
  LucideCalendar,
  LucideMapPin,
  LucideStar,
  LucideTicket,
  LucideX
} from "@lucide/angular";
import { UserBooking } from "../../../core/models/catalog.model";
import { ReviewService } from "../../../core/services/review.service";
import { UserBookingService } from "../../../core/services/user-booking.service";

/** Status-tab matching the admin "All Bookings" ledger pattern. */
type StatusTab = "ALL" | "CONFIRMED" | "CANCELLED";

/**
 * User-facing "My Bookings" ledger. Lists the caller's own reservations
 * (newest first) with status badges, refund info, and contextual actions:
 *   - Cancel: visible for CONFIRMED / PARTIALLY_CANCELLED bookings.
 *   - Rate: visible only after the show has played and the user hasn't yet
 *     reviewed the booking.
 *   - Tab strip: ALL / CONFIRMED / CANCELLED, mirroring the admin ledger.
 */
@Component({
  selector: "app-my-bookings",
  standalone: true,
  imports: [
    CurrencyPipe,
    DatePipe,
    LowerCasePipe,
    RouterLink,
    LucideCalendar,
    LucideMapPin,
    LucideStar,
    LucideTicket,
    LucideX
  ],
  templateUrl: "./my-bookings.html",
  styleUrl: "./my-bookings.css"
})
export class MyBookingsComponent implements OnInit {
  private readonly bookingService = inject(UserBookingService);
  private readonly reviewService = inject(ReviewService);

  readonly bookings = this.bookingService.bookings;

  /** Booking the user is currently rating; null when the modal is closed. */
  readonly rateTarget = signal<UserBooking | null>(null);
  readonly ratingValue = signal(0);
  readonly submittingRating = signal(false);
  readonly cancellingId = signal<number | null>(null);
  readonly error = signal<string | null>(null);

  /** Active status-tab. Mirrors the admin "All Bookings" tab strip. */
  readonly statusTab = signal<StatusTab>("ALL");

  /** Bookings narrowed by the active tab — drives the rendered list. */
  readonly visibleBookings = computed(() => {
    const tab = this.statusTab();
    const rows = this.bookings();
    if (tab === "ALL") return rows;
    if (tab === "CONFIRMED") return rows.filter((b) => b.status === "CONFIRMED");
    // "CANCELLED" tab also surfaces partial cancellations — same as admin pattern.
    return rows.filter(
      (b) => b.status === "CANCELLED" || b.status === "PARTIALLY_CANCELLED"
    );
  });

  readonly allCount = computed(() => this.bookings().length);
  readonly confirmedCount = computed(
    () => this.bookings().filter((b) => b.status === "CONFIRMED").length
  );
  readonly cancelledCount = computed(
    () =>
      this.bookings().filter(
        (b) => b.status === "CANCELLED" || b.status === "PARTIALLY_CANCELLED"
      ).length
  );

  readonly stars = [1, 2, 3, 4, 5];

  ngOnInit(): void {
    this.bookingService.loadMine().subscribe({
      error: (err) => {
        console.error("Failed to load bookings", err);
        this.error.set("Could not load your bookings. Try again later.");
      }
    });
  }

  cancel(booking: UserBooking): void {
    if (booking.status === "CANCELLED") return;
    if (!confirm("Cancel this booking? Refund will be processed for cancelled seats.")) return;
    this.cancellingId.set(booking.id);
    this.bookingService.cancel(booking.id).subscribe({
      next: () => this.cancellingId.set(null),
      error: (err) => {
        this.cancellingId.set(null);
        this.error.set(err?.error?.message ?? "Cancellation failed.");
      }
    });
  }

  openRate(booking: UserBooking): void {
    this.rateTarget.set(booking);
    this.ratingValue.set(0);
    this.error.set(null);
  }

  closeRate(): void {
    this.rateTarget.set(null);
    this.ratingValue.set(0);
    this.submittingRating.set(false);
  }

  pickStar(value: number): void {
    this.ratingValue.set(value);
  }

  submitRating(): void {
    const target = this.rateTarget();
    const rating = this.ratingValue();
    if (!target || rating < 1) {
      this.error.set("Pick a rating between 1 and 5.");
      return;
    }
    this.submittingRating.set(true);
    this.reviewService.submit({ bookingId: target.id, rating }).subscribe({
      next: () => {
        this.submittingRating.set(false);
        // Flag the booking locally so the Rate button disappears immediately.
        this.bookingService.bookings.update((list) =>
          list.map((b) => (b.id === target.id ? { ...b, hasReview: true } : b))
        );
        this.closeRate();
      },
      error: (err) => {
        this.submittingRating.set(false);
        this.error.set(err?.error?.message ?? "Could not submit rating.");
      }
    });
  }

  statusClass(status: UserBooking["status"]): string {
    switch (status) {
      case "CONFIRMED":
        return "status-badge status-confirmed";
      case "PARTIALLY_CANCELLED":
        return "status-badge status-partial";
      case "CANCELLED":
        return "status-badge status-cancelled";
    }
  }

  setStatusTab(tab: StatusTab): void {
    this.statusTab.set(tab);
  }

  /**
   * Rating button only appears once the show has actually been watched:
   * <ul>
   *   <li>booking not yet reviewed,</li>
   *   <li>booking not fully cancelled,</li>
   *   <li>and the show's start time is in the past.</li>
   * </ul>
   * Mirrors the backend guard added to ReviewService.createReview.
   */
  canRate(b: UserBooking): boolean {
    if (b.hasReview || b.status === "CANCELLED") return false;
    if (!b.showTime) return false;
    return new Date(b.showTime).getTime() <= Date.now();
  }
}
