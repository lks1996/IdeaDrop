package com.kyungyu.ideaDrop.repository;

import com.kyungyu.ideaDrop.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    // 코사인 거리(<=>) 연산자를 사용하여 유사도를 측정해.
    // 임계값(threshold)보다 거리가 가까운(작은) 데이터 중 가장 유사한 1건을 가져오는 쿼리야.
    @Query(value = """
            SELECT * FROM response 
            WHERE CAST(embedding_vector AS vector) <=> CAST(:vector AS vector) < :threshold 
            ORDER BY CAST(embedding_vector AS vector) <=> CAST(:vector AS vector) 
            LIMIT 1
            """, nativeQuery = true)
    Optional<Response> findMostSimilarIdea(@Param("vector") String vector, @Param("threshold") double threshold);
}
