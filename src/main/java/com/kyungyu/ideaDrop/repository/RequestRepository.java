package com.kyungyu.ideaDrop.repository;

import com.kyungyu.ideaDrop.entity.Request;
import com.kyungyu.ideaDrop.entity.Status;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RequestRepository extends JpaRepository<Request, Long> {

    // 가장 최근에 성공한 요청 1건만 가져오는 쿼리 메서드
    Optional<Request> findFirstByStatusOrderByCreatedAtDesc(Status status);

}
