package com.kyungyu.ideaDrop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Request {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String slackUserId;

    @Column(columnDefinition = "TEXT")
    private String prompt;

    @Enumerated(EnumType.STRING)
    private Status status;

    private LocalDateTime createdAt;

    public void markInvalid() {
        this.status = Status.INVALID;
    }

    public void markSuccess() {
        this.status = Status.SUCCESS;
    }
}
