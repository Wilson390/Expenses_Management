package org.community.giving;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface BankAccountRepository extends JpaRepository<BankAccount,Long> { List<BankAccount> findByActiveTrue(); }
