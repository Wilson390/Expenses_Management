package org.community.giving;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(uniqueConstraints=@UniqueConstraint(columnNames={"closing_date"})) public class DayEndClosing {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="closing_date") public java.time.LocalDate closingDate;
 public String status="PENDING";
 public String approvedBy; public Instant approvedAt;
 public String verifiedBy; public Instant verifiedAt;
 public String depositedBy; public Instant depositedAt; @Column(length=150) public String bankReference; public Long bankAccountId;
}
