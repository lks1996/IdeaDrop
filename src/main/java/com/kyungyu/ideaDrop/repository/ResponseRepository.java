package com.kyungyu.ideaDrop.repository;

import com.kyungyu.ideaDrop.entity.Response;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ResponseRepository extends JpaRepository<Response, Long> {

    // 복잡한 쿼리 대신, 비교할 최근 아이디어 100개만 가져오는 전략을 사용.
    List<Response> findTop15ByOrderByCreatedAtDesc();
}
