package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
public interface VoteRepository extends JpaRepository<Vote,Long>{boolean existsByElectionIdAndVoterIdAndCategoryAndSubcategory(Long e,Long v,String c,String s); java.util.List<Vote> findByElectionId(Long e);}