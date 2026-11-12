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

  private final Map<String, NotificationSender> sendersByType;

  private final LegacyDatabaseManager dbManager;

  public LegacyNotificationEngine(JdbcTemplate jdbcTemplate, List<NotificationSender> senders) {
    this.dbManager = new LegacyDatabaseManager(jdbcTemplate);
    this.sendersByType = senders.stream()
        .collect(Collectors.toMap(NotificationSender::type, Function.identity()));
  }

  public synchronized LegacyNotificationResponse processNotification(String type, String target, String payload)
      throws Exception {

    String safeType = type != null ? type.trim().toUpperCase() : "";
    String safeTarget = target != null ? target.trim() : null;
    String safePayload = payload != null ? payload : "";

    if (safeTarget == null || safeTarget.length() == 0) {
      throw new IllegalArgumentException("Target is mandatory.");
    }

    if (dbManager.isUserBlacklistedOrRateLimited(safeTarget)) {
      throw new RuntimeException("Business Rule Violation: User is blacklisted or rate-limited by DB policy.");
    }

    long id = dbManager.openDispatch(safeType, safeTarget, safePayload);

    if (dbManager.getFailureCountFromDatabase() >= 3) {
      dbManager.markAsSkipped(id, "Circuit breaker OPEN via database state.");
      throw new RuntimeException("Circuit breaker OPEN via database state.");
    }

    NotificationSender sender = sendersByType.get(safeType);
    if (sender == null) {
      System.out.println("LOGGING TO DB: Unknown type attempted -> " + type);
      dbManager.recordUnsupportedType(id, safeType);
      throw new Exception("Unsupported type: " + type);
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
    private final RowMapper<LegacyNotificationResponse> rowMapper = new RowMapper<LegacyNotificationResponse>() {
      @Override
      public LegacyNotificationResponse mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        LegacyNotificationResponse response = new LegacyNotificationResponse();
        response.setId(rs.getLong("id"));
        response.setType(rs.getString("tp"));
        response.setTarget(rs.getString("tgt"));
        response.setPayload(rs.getString("pld"));
        response.setStatus(rs.getString("st"));
        response.setFailureCount(rs.getInt("fc"));
        response.setDispatchCount(rs.getInt("dc"));
        response.setBlocked("Y".equals(rs.getString("bl")) ? "Y" : "N");
        response.setMessage(rs.getString("lm"));

        response.setCreatedAt(rs.getTimestamp("crt"));
        response.setUpdatedAt(rs.getTimestamp("upd"));
        return response;
      }
    };

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

    public long openDispatch(final String type, final String target, final String payload) {
      final long id = getNextId();
      final int nextDispatchCount = getTotalDispatches() + 1;
      final int currentFailureCount = getFailureCountFromDatabase();
      final String blocked = (target != null && target.toLowerCase().contains("blocked")) ? "Y" : "N";
      final Timestamp now = new Timestamp(System.currentTimeMillis());

      jdbcTemplate.update(
          "insert into ntf_tx(id, tp, tgt, pld, st, fc, dc, bl, lm, crt, upd) "
              + "values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
          Long.valueOf(id),
          type,
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

    public void recordUnsupportedType(long id, String type) {
      jdbcTemplate.update(
          "update ntf_tx set st = ?, lm = ?, upd = ? where id = ?",
          "U",
          shorten("Unsupported type: " + type),
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