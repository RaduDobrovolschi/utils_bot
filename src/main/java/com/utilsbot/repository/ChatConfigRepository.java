package com.utilsbot.repository;

import com.utilsbot.domain.ChatConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatConfigRepository extends JpaRepository<ChatConfig, Long> {
}
