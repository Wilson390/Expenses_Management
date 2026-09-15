package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface CommitteeMemberRepository extends JpaRepository<CommitteeMember,Long> { List<CommitteeMember> findByActiveTrue(); }
