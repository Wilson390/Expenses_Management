ALTER TABLE member ADD COLUMN IF NOT EXISTS baptism_by_pastor_id BIGINT;
ALTER TABLE member ADD COLUMN IF NOT EXISTS dhrudikaran_by_pastor_id BIGINT;
ALTER TABLE member ADD COLUMN IF NOT EXISTS married_by_pastor_id BIGINT;
ALTER TABLE member ADD CONSTRAINT fk_member_baptism_pastor FOREIGN KEY (baptism_by_pastor_id) REFERENCES pastor(id);
ALTER TABLE member ADD CONSTRAINT fk_member_dhrudikaran_pastor FOREIGN KEY (dhrudikaran_by_pastor_id) REFERENCES pastor(id);
ALTER TABLE member ADD CONSTRAINT fk_member_married_pastor FOREIGN KEY (married_by_pastor_id) REFERENCES pastor(id);
