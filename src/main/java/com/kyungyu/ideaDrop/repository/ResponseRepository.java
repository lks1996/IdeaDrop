package com.kyungyu.ideaDrop.repository;

import com.kyungyu.ideaDrop.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    // 복잡한 쿼리 대신, 비교할 최근 아이디어 100개만 가져오는 전략을 사용.
    List<Response> findTop15ByOrderByCreatedAtDesc();

    // DB단에서 직접 카운트를 1 올리는 원자적 연산
    @Modifying
    @Query("UPDATE Response r SET r.likeCount = r.likeCount + 1 WHERE r.id = :id")
    void incrementLikeCount(@Param("id") Long id);
}
