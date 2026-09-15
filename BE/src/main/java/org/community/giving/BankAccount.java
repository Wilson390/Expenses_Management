package org.community.giving;
import jakarta.persistence.*;
@Entity public class BankAccount {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long churchId;
 @Column(nullable=false, length=150) public String bankName;
 @Column(nullable=false, length=50) public String accountNumber;
 @Column(length=20) public String ifscCode;
 @Column(length=150) public String accountHolderName;
 public boolean active=true;
}
