package org.community.giving;
import jakarta.persistence.*;
import java.time.Instant;
@Entity public class Enquiry {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false, length=150) public String name;
 @Column(nullable=false, length=180) public String contact;
 @Column(nullable=false, length=1000) public String message;
 public boolean read=false;
 public Instant createdAt;
 @PrePersist void created(){createdAt=Instant.now();}
}
