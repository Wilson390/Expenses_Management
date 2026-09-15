package org.community.giving;
import jakarta.persistence.*;
import java.time.LocalDate;
@Entity public class Member {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public String name, gender, address, profession, baptismBy, confirmationBy, marriedBy;
 @Column(name="baptism_by_pastor_id") public Long baptismByPastorId;
 @Column(name="dhrudikaran_by_pastor_id") public Long dhrudikaranByPastorId;
 @Column(name="married_by_pastor_id") public Long marriedByPastorId;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="baptism_by_pastor_id", insertable=false, updatable=false, foreignKey=@ForeignKey(name="fk_member_baptism_pastor")) public Pastor baptismPastor;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="dhrudikaran_by_pastor_id", insertable=false, updatable=false, foreignKey=@ForeignKey(name="fk_member_dhrudikaran_pastor")) public Pastor dhrudikaranPastor;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="married_by_pastor_id", insertable=false, updatable=false, foreignKey=@ForeignKey(name="fk_member_married_pastor")) public Pastor marriedPastor;
 public LocalDate dob, baptismDate, confirmationDate, marriageDate, deathDate;
 public boolean married=false, active=true;
 @Column(length=255) public String profilePicImage;
 @Column(unique=true, nullable=false) public String mobileNo;
 public java.time.Instant createdAt, updatedAt;
 public String createdBy, updatedBy;
 @PrePersist void created(){createdAt=java.time.Instant.now();updatedAt=createdAt;}
 @PreUpdate void updated(){updatedAt=java.time.Instant.now();}
}
