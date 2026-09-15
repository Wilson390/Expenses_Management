package org.community.giving;

import jakarta.persistence.*;

@Entity
@Table(name = "master", uniqueConstraints = @UniqueConstraint(columnNames = {"groupname", "\"value\""}))
public class Master {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, length = 80)
    public String groupname;

    /** Master group type. The old groupname column is retained for compatibility. */
    @Column(length = 80)
    public String type;

    @Column(length = 150)
    public String code;

    @Column(name = "\"value\"", nullable = false, length = 150)
    public String value;
}
