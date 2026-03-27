package com.kyungyu.ideaDrop.repository;

import com.kyungyu.ideaDrop.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    // 생성일자(createdAt) 기준 내림차순 정렬 후 상위1개 조회.
    Optional<Request> findTopByOrderByCreatedAtDesc();

}
