package org.community.giving;

import java.util.List;
import java.util.Map;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class MasterDefaults implements ApplicationRunner {
    private final MasterRepository masters;

    public MasterDefaults(MasterRepository masters) {
        this.masters = masters;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        var values = Map.of(
            "gender", List.of("MALE", "FEMALE"),
            "amount_type", List.of("Birthday", "Anniversary", "Thanksgiving", "Donation By Amount", "Physical Donation(Text Box to enter details )", "Monthly Pledge", "Sunday School", "Sunday Worship Offerings", "Election Form Fee", "Common", "graveyard"),
            "events_type", List.of("Harvest festival", "Church day", "Christmas", "New Year", "Good Friday", "General", "Sunday Worship Offerings"),
            "denomination_type", List.of("1", "2", "5", "10", "20", "50", "100", "500", "1000", "2000"),
            "payment_type", List.of("CASH", "Online", "Cheque"),
            "election_category", List.of("Pastorate Committee", "Diocese Council"),
            "election_sub_category", List.of("Youth", "General", "Womens"));
        values.forEach((type, entries) -> entries.forEach(code -> {
            if (!masters.existsByTypeAndCode(type, code) && !masters.existsByGroupnameAndValue(type, code)) {
                Master entry = new Master();
                entry.type = type;
                entry.code = code;
                entry.groupname = type;
                entry.value = code;
                masters.save(entry);
            }
        }));
        // Migrate the original misspelled amount group if it exists.
        masters.findByGroupnameOrderByIdAsc("amonutype").forEach(entry -> {
            entry.groupname = "amount_type";
            entry.type = "amount_type";
            if (entry.code == null) entry.code = entry.value;
            masters.save(entry);
        });
        masters.findByGroupnameOrderByIdAsc("amountype").forEach(entry -> {
            entry.groupname = "amount_type";
            entry.type = "amount_type";
            if (entry.code == null) entry.code = entry.value;
            masters.save(entry);
        });
    }
}
