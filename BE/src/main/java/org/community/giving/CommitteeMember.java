package org.community.giving;
import jakarta.persistence.*;
import java.time.Instant;
@Entity public class CommitteeMember {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 public Long memberId; public String memberName, category, subcategory, profilePicImage, post;
 public boolean active=true;
 public String appointedBy; public Instant appointedAt;
}
