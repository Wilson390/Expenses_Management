package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.Optional;
public interface DayEndClosingRepository extends JpaRepository<DayEndClosing,Long> { Optional<DayEndClosing> findByClosingDate(LocalDate date); }
