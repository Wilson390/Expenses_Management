package org.community.giving;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
@Entity public class Donation {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="member_id") public Long memberId;
 @ManyToOne(fetch=FetchType.LAZY)
 @JoinColumn(name="member_id", referencedColumnName="id", insertable=false, updatable=false, foreignKey=@ForeignKey(name="fk_donation_member"))
 public Member member;
 public String memberName, category, event, paymentType, reference, description, contributionMonth, createdBy;
 @Column(length=255) public String attachmentImage;
 public String updatedBy;
 public Instant updatedAt;
 public LocalDate receivedDate;
 public Instant createdAt;
 @PrePersist void auditCreated(){createdAt=Instant.now();updatedAt=createdAt;}
 @PreUpdate void auditUpdated(){updatedAt=Instant.now();}
 @Column(precision=14,scale=2) public BigDecimal amount;
 @ElementCollection(fetch=FetchType.EAGER) @CollectionTable(name="donation_denomination", joinColumns=@JoinColumn(name="donation_id", foreignKey=@ForeignKey(name="fk_denomination_donation"))) @MapKeyColumn(name="face_value") @Column(name="quantity") public Map<Integer,Integer> denominations=new TreeMap<>();
}
