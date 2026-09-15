package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
public interface MemberRepository extends JpaRepository<Member,Long> {
 boolean existsByMobileNo(String mobileNo);
 boolean existsByMobileNoAndIdNot(String mobileNo, Long id);
}
