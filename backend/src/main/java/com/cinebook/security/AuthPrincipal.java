package com.cinebook.security;

import com.cinebook.entity.Role;

/**
 * The authenticated caller's identity, derived from a verified JWT and held as the
 * Spring Security principal. Controllers can read it with
 * {@code @AuthenticationPrincipal AuthPrincipal principal}.
 */
public record AuthPrincipal(Long userId, String username, Role role, Long theaterId) {
}
