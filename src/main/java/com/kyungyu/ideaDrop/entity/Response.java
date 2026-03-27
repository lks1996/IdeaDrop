package com.kyungyu.ideaDrop.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Response {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Request request;

    @Column(columnDefinition = "TEXT")
    private String output;

    // 벡터 데이터를 저장할 컬럼 추가
    // DB 설정에 따라 JSON 스트링으로 저장하거나, pgvector의 vector 타입으로 매핑해
    @Column(columnDefinition = "TEXT")
    private String embeddingVector;

    private int likeCount;
    private LocalDateTime createdAt;

    public void increaseLike() {
        this.likeCount++;
    }
}