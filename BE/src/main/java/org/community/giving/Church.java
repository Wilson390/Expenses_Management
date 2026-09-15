package org.community.giving;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
@Entity public class Church {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(nullable=false, length=150) public String name;
 @Column(length=255) public String address;
 @Column(length=20) public String phone;
 @Column(length=180) public String email;
 @Column(length=150) public String title;
 @Column(length=2000) public String description;
 public Integer foundedYear;
 @Column(length=150) public String foundedBy;
 @Column(length=255) public String foundationStoneImage;
 @ElementCollection(fetch=FetchType.EAGER)
 @CollectionTable(name="church_image", joinColumns=@JoinColumn(name="church_id", foreignKey=@ForeignKey(name="fk_church_image")))
 @Column(name="filename", length=255) @OrderColumn(name="position")
 public List<String> images=new ArrayList<>();
}
