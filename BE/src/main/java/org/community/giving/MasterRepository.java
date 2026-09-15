package org.community.giving;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MasterRepository extends JpaRepository<Master, Long> {
    List<Master> findByGroupnameOrderByIdAsc(String groupname);
    boolean existsByGroupnameAndValue(String groupname, String value);
    boolean existsByTypeAndCode(String type, String code);
    List<Master> findByTypeOrderByIdAsc(String type);
}
