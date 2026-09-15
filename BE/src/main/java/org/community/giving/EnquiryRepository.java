package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface EnquiryRepository extends JpaRepository<Enquiry,Long> { List<Enquiry> findAllByOrderByCreatedAtDesc(); }
