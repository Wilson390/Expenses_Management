package org.community.giving;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:gender-test;DB_CLOSE_DELAY=-1", "app.admin-password=test-admin-password"})
@Transactional
class GenderMasterTest {
    @Autowired Api api;
    @Autowired MasterRepository masters;
    @Autowired MasterDefaults defaults;

    @Test void genderOptionsComeFromMasterAndDefaultsAreIdempotent() {
        defaults.run(null);
        assertEquals(List.of("MALE", "FEMALE"), api.masters().get("genders"));
        Master female = masters.findByGroupnameOrderByIdAsc("gender").get(1);
        female.value = "UPDATED";
        female.code = "UPDATED";
        masters.saveAndFlush(female);
        assertEquals(List.of("MALE", "UPDATED"), api.masters().get("genders"));
    }

    @Test void validatesAgainstMasterOnCreateAndUpdate() {
        Member member = new Member();
        member.name = "Gender Test";
        member.mobileNo = "9876543210";
        member.gender = " male ";
        Member saved = api.create(new Api.MemberInput(member, "gender-test", "test-member-password"));
        assertEquals("MALE", saved.gender);
        saved.gender = "INVALID";
        assertThrows(ResponseStatusException.class, () -> api.update(saved.id, saved));
        saved.gender = "FEMALE";
        assertEquals("FEMALE", api.update(saved.id, saved).gender);
    }
}
