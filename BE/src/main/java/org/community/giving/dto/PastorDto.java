package org.community.giving.dto;

import java.time.LocalDate;

/** API representation of a pastor; persistence/audit fields are intentionally omitted. */
public record PastorDto(Long id, String name, String mobileNo, String email, LocalDate dob,
        LocalDate doj, LocalDate dor, String gender, boolean active, boolean isBishop, String profilePicImage) {
}
