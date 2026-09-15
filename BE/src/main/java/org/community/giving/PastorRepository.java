package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
public interface PastorRepository extends JpaRepository<Pastor, Long> {
    boolean existsByMobileNo(String mobileNo);
    boolean existsByMobileNoAndIdNot(String mobileNo, Long id);
}
