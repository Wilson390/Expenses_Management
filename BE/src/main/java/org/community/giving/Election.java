package org.community.giving;
import jakarta.persistence.*;
@Entity  public class Election { @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id; public String name, description; public java.time.LocalDate startDate,endDate; }