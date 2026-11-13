package com.example.homework.blacklist;

import java.time.Clock;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class BlacklistService {

  private final BlacklistRepository repository;

  private final Clock clock;

  public BlacklistService(BlacklistRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  public boolean isBlacklisted(String recipient) {
    return repository.existsByRecipient(normalize(recipient));
  }

  public BlacklistResponse add(String rawRecipient, String reason) {
    String recipient = normalize(rawRecipient);
    if (repository.existsByRecipient(recipient)) {
      throw new AlreadyBlacklistedException(recipient);
    }
    BlacklistedRecipient entry = new BlacklistedRecipient(recipient, reason, clock.instant());
    return BlacklistResponse.from(repository.save(entry));
  }

  public List<BlacklistResponse> getAll() {
    return repository.findAllByOrderByRecipientAsc().stream()
        .map(BlacklistResponse::from)
        .toList();
  }

  public void remove(long id) {
    if (!repository.existsById(id)) {
      throw new BlacklistEntryNotFoundException(id);
    }
    repository.deleteById(id);
  }

  private static String normalize(String recipient) {
    return recipient.trim().toLowerCase(Locale.ROOT);
  }
}
