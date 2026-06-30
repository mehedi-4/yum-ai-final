package com.yumai.repository;

import com.yumai.entity.WasteLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface WasteLogRepository extends JpaRepository<WasteLog, Long> {

    List<WasteLog> findByLoggedAtBetween(LocalDateTime from, LocalDateTime to);

    List<WasteLog> findAllByOrderByLoggedAtDesc();
}
