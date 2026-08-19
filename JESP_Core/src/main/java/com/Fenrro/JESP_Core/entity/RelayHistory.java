package com.Fenrro.JESP_Core.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "relay_history")
@Getter
@Setter
@NoArgsConstructor
public class RelayHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "timestamp", nullable = false, updatable = false)
    private LocalDateTime timestamp;

    @Column(name = "relay_index")
    private Integer relayIndex;

    @Column(name = "new_state")
    private Boolean newState;

    @Column(name = "source")
    private String source;

    public RelayHistory(int relayIndex, boolean state, String source) {
        this.relayIndex = relayIndex;
        this.newState = state;
        this.source = source;
    }

    @PrePersist
    void prePersist() {
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}