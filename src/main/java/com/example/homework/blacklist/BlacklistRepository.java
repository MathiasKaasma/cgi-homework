package com.example.homework.blacklist;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface BlacklistRepository extends JpaRepository<BlacklistedRecipient, Long> {

  boolean existsByRecipient(String recipient);

  List<BlacklistedRecipient> findAllByOrderByRecipientAsc();
}
