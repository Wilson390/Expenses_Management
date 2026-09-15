package org.community.giving;
import jakarta.persistence.*;
@Entity @Table(name="app_account") public class Account {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(unique=true,nullable=false) public String username;
 public String passwordHash, role;
 public Long memberId;
}
