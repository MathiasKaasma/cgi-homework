package com.example.homework.notification;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.example.homework.notification.sender.NotificationSender;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class LegacyNotificationEngine {

  private final Map<NotificationType, NotificationSender> sendersByType;

  private final LegacyDatabaseManager dbManager;

  public LegacyNotificationEngine(JdbcTemplate jdbcTemplate, List<NotificationSender> senders) {
    this.dbManager = new LegacyDatabaseManager(jdbcTemplate);
    this.sendersByType = senders.stream()
        .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
  }

  public synchronized LegacyNotificationResponse processNotification(NotificationType type, String target, String payload)
      throws Exception {

    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";
    NotificationSender sender = sendersByType.get(type);

    if (safeTarget == null || safeTarget.isEmpty() || !sender.isValidRecipient(safeTarget)) {
      throw new InvalidRecipientException(type, safeTarget);
    }

    if (dbManager.isUserBlacklistedOrRateLimited(safeTarget)) {
      throw new RuntimeException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }

    long id = dbManager.openDispatch(type, safeTarget, safePayload);

    if (dbManager.getFailureCountFromDatabase() >= 3) {
      dbManager.markAsSkipped(id, "Circuit breaker OPEN via database state.");
      throw new RuntimeException("Circuit breaker OPEN via database state.");
    }

    try {
      sender.send(safeTarget, safePayload);
      dbManager.resetDatabaseFailures(id);
      return dbManager.getDispatchById(id);
    } catch (Exception e) {
      dbManager.recordDatabaseFailure(id, safeTarget, e.getMessage());
      throw e;
    }
  }

  public List<LegacyNotificationResponse> getHistory() {
    return dbManager.getHistory();
  }

  public LegacyNotificationResponse getLatestState() {
    return dbManager.getLatestState();
  }

  public static class LegacyDatabaseManager {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<LegacyNotificationResponse> rowMapper = (rs, rowNum) -> new LegacyNotificationResponse(
        rs.getLong("id"),
        rs.getString("tp"),
        rs.getString("tgt"),
        rs.getString("pld"),
        rs.getString("st"),
        rs.getInt("fc"),
        rs.getInt("dc"),
        "Y".equals(rs.getString("bl")) ? "Y" : "N",
        rs.getString("lm"),
        rs.getTimestamp("crt"),
        rs.getTimestamp("upd"));

    public LegacyDatabaseManager(JdbcTemplate jdbcTemplate) {
      this.jdbcTemplate = jdbcTemplate;
    }

    public boolean isUserBlacklistedOrRateLimited(String target) {
      if (target != null && target.toLowerCase().contains("blocked")) {
        return true;
      }

      Integer recentCount = jdbcTemplate.queryForObject(
          "select count(*) "
              + "from ntf_tx "
              + "where tgt = ? "
              + "  and crt >= dateadd('minute', -1, current_timestamp)",
          Integer.class,
          target);

      return recentCount != null && recentCount.intValue() >= 3;
    }

    public long openDispatch(final NotificationType type, final String target, final String payload) {
      final long id = getNextId();
      final int nextDispatchCount = getTotalDispatches() + 1;
      final int currentFailureCount = getFailureCountFromDatabase();
      final String blocked = (target != null && target.toLowerCase().contains("blocked")) ? "Y" : "N";
      final Timestamp now = new Timestamp(System.currentTimeMillis());

      jdbcTemplate.update(
          "insert into ntf_tx(id, tp, tgt, pld, st, fc, dc, bl, lm, crt, upd) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          Long.valueOf(id),
          type.name(),
          target,
          payload,
          "N",
          Integer.valueOf(currentFailureCount),
          Integer.valueOf(nextDispatchCount),
          blocked,
          "START",
          now,
          now);

      return id;
    }

    private long getNextId() {
      Long maxId = jdbcTemplate.queryForObject(
          "select coalesce(max(id), 0) from ntf_tx",
          Long.class);
      return (maxId == null ? 0L : maxId.longValue()) + 1L;
    }

    public int getFailureCountFromDatabase() {
      List<Integer> results = jdbcTemplate.query(
          "select fc from ntf_tx order by id desc limit 1",
          new RowMapper<Integer>() {
            @Override
            public Integer mapRow(ResultSet resultSet, int rowNum) throws SQLException {
              return Integer.valueOf(resultSet.getInt(1));
            }
          });
      if (results.isEmpty()) {
        return 0;
      }
      return results.get(0).intValue();
    }

    public void recordDatabaseFailure(String target, String error) {
      recordDatabaseFailure(findLatestId(), target, error);
    }

    public void recordDatabaseFailure(long id, String target, String error) {
      int nextFailureCount = getFailureCountFromDatabase() + 1;
      jdbcTemplate.update(
          "update ntf_tx set st = ?, fc = ?, lm = ?, upd = ? where id = ?",
          "E",
          Integer.valueOf(nextFailureCount),
          shorten(error),
          new Timestamp(System.currentTimeMillis()),
          Long.valueOf(id));
      System.out.println("[DB AUDIT] Inserted failure record for " + target + ": " + error);
    }

    public void resetDatabaseFailures() {
      long id = findLatestId();
      if (id > 0L) {
        resetDatabaseFailures(id);
      }
    }

    public void resetDatabaseFailures(long id) {
      jdbcTemplate.update(
          "update ntf_tx set st = ?, fc = ?, lm = ?, upd = ? where id = ?",
          "S",
          Integer.valueOf(0),
          "SENT",
          new Timestamp(System.currentTimeMillis()),
          Long.valueOf(id));
    }

    public void markAsSkipped(long id, String message) {
      jdbcTemplate.update(
          "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
          "R",
          shorten(message),
          new Timestamp(System.currentTimeMillis()),
          Long.valueOf(id));
    }

    public int getTotalDispatches() {
      Integer latestDispatchCount = jdbcTemplate.queryForObject(
          "select coalesce(max(dc), 0) from ntf_tx",
          Integer.class);
      if (latestDispatchCount == null) {
        return 0;
      }
      return latestDispatchCount.intValue();
    }

    public LegacyNotificationResponse getLatestState() {
      List<LegacyNotificationResponse> items = jdbcTemplate.query(
          "select * from ntf_tx order by id desc limit 1",
          rowMapper);
      if (items.isEmpty()) {
        return null;
      }
      return items.get(0);
    }

    public LegacyNotificationResponse getDispatchById(long id) {
      return jdbcTemplate.queryForObject(
          "select * from ntf_tx where id = ?",
          rowMapper,
          Long.valueOf(id));
    }

    public List<LegacyNotificationResponse> getHistory() {
      return jdbcTemplate.query(
          "select * from ntf_tx order by id desc limit 20",
          rowMapper);
    }

    private long findLatestId() {
      List<Long> ids = jdbcTemplate.query(
          "select id from ntf_tx order by id desc limit 1",
          new RowMapper<Long>() {
            @Override
            public Long mapRow(ResultSet resultSet, int rowNum) throws SQLException {
              return Long.valueOf(resultSet.getLong(1));
            }
          });
      if (ids.isEmpty()) {
        return 0L;
      }
      return ids.get(0).longValue();
    }

    private String shorten(String value) {
      if (value == null) {
        return null;
      }
      if (value.length() <= 255) {
        return value;
      }
      return value.substring(0, 255);
    }
  }
}
