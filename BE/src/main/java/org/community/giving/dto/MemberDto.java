package org.community.giving.dto;

import java.time.LocalDate;

/** API representation of a member, including relationship IDs but no password/account data. */
public record MemberDto(Long id, String name, String gender, String address, String mobileNo,
        String profession, LocalDate dob, LocalDate baptismDate, Long baptismByPastorId,
        LocalDate confirmationDate, Long dhrudikaranByPastorId, boolean married,
        LocalDate marriageDate, Long marriedByPastorId, LocalDate deathDate, boolean active, String profilePic) {
}
