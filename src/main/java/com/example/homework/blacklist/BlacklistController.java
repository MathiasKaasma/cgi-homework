package com.example.homework.blacklist;

import java.net.URI;
import java.util.List;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blacklist")
public class BlacklistController {

  private final BlacklistService service;

  public BlacklistController(BlacklistService service) {
    this.service = service;
  }

  @GetMapping
  public List<BlacklistResponse> getAll() {
    return service.getAll();
  }

  @PostMapping
  public ResponseEntity<BlacklistResponse> add(@Valid @RequestBody BlacklistRequest request) {
    BlacklistResponse response = service.add(request.recipient(), request.reason());
    return ResponseEntity
        .created(URI.create("/api/blacklist/" + response.id()))
        .body(response);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> remove(@PathVariable long id) {
    service.remove(id);
    return ResponseEntity.noContent().build();
  }
}
