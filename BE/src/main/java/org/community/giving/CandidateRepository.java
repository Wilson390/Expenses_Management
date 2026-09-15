package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CandidateRepository extends JpaRepository<Candidate,Long>{java.util.List<Candidate> findByElectionId(Long id); java.util.List<Candidate> findByWinnerTrue();}