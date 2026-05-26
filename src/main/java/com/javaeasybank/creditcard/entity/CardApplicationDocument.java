package com.javaeasybank.creditcard.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "CARD_APPLICATION_DOCUMENT")
public class CardApplicationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "document_id")
    private Integer documentId;

    @Column(name = "file_url", length = 500, nullable = false)
    private String fileUrl;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @ManyToOne
    @JoinColumn(name = "application_id", nullable = false)
    private CardApplication application;

    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}
