package org.community.giving;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "pastor")
public class Pastor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;
    @Column(nullable = false, length = 150)
    public String name;
    @Column(nullable = false, length = 20)
    public String mobileNo;
    @Column(length = 180)
    public String email;
    public LocalDate dob, doj, dor;
    @Column(length = 20)
    public String gender;
    public boolean active = true;
    public boolean isBishop = false;
    @Column(length = 255)
    public String profilePicImage;
    public Instant createdAt, updatedAt;
    public String createdBy, updatedBy;
    @PrePersist void created() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
}
