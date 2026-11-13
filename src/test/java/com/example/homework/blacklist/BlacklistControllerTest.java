package com.example.homework.blacklist;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(BlacklistController.class)
class BlacklistControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private BlacklistService service;

  @Test
  void addReturnsCreatedWithLocation() throws Exception {
    when(service.add("a@b.com", "spam"))
        .thenReturn(new BlacklistResponse(7, "a@b.com", "spam", Instant.now()));

    postBlacklist("""
        {"recipient": "a@b.com", "reason": "spam"}
        """)
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/blacklist/7"));
  }

  @Test
  void blankRecipientIsBadRequest() throws Exception {
    postBlacklist("""
        {"recipient": " "}
        """)
        .andExpect(status().isBadRequest());
  }

  @Test
  void duplicateIsConflict() throws Exception {
    when(service.add(any(), any())).thenThrow(new AlreadyBlacklistedException("a@b.com"));

    postBlacklist("""
        {"recipient": "a@b.com"}
        """)
        .andExpect(status().isConflict());
  }

  @Test
  void removeReturnsNoContent() throws Exception {
    mockMvc.perform(delete("/api/blacklist/7"))
        .andExpect(status().isNoContent());
  }

  @Test
  void removeUnknownIsNotFound() throws Exception {
    doThrow(new BlacklistEntryNotFoundException(999)).when(service).remove(999);

    mockMvc.perform(delete("/api/blacklist/999"))
        .andExpect(status().isNotFound());
  }

  private ResultActions postBlacklist(String body) throws Exception {
    return mockMvc.perform(post("/api/blacklist")
        .contentType(MediaType.APPLICATION_JSON)
        .content(body));
  }
}
