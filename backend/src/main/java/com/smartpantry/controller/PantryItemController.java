package com.smartpantry.controller;

import com.smartpantry.domain.PantryStatus;
import com.smartpantry.dto.ConsumeRequest;
import com.smartpantry.dto.PantryItemRequest;
import com.smartpantry.dto.PantryItemResponse;
import com.smartpantry.dto.PantrySummaryResponse;
import com.smartpantry.security.PantryUserDetails;
import com.smartpantry.service.PantryItemService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/pantry-items")
public class PantryItemController {

    private final PantryItemService service;

    public PantryItemController(PantryItemService service) {
        this.service = service;
    }

    @GetMapping
    public List<PantryItemResponse> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) PantryStatus status,
            @RequestParam(required = false) Integer expiringWithinDays,
            @RequestParam(required = false, name = "name") String search,
            @AuthenticationPrincipal PantryUserDetails user) {
        return service.list(category, status, expiringWithinDays, search, user.getId());
    }

    @GetMapping("/summary")
    public PantrySummaryResponse summary(@AuthenticationPrincipal PantryUserDetails user) {
        return service.summary(user.getId());
    }

    @GetMapping("/{id}")
    public PantryItemResponse get(@PathVariable Long id,
                                  @AuthenticationPrincipal PantryUserDetails user) {
        return service.get(id, user.getId());
    }

    @PostMapping
    public ResponseEntity<PantryItemResponse> create(@Valid @RequestBody PantryItemRequest request,
                                                     @AuthenticationPrincipal PantryUserDetails user) {
        PantryItemResponse created = service.create(request, user.getId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public PantryItemResponse update(@PathVariable Long id,
                                     @Valid @RequestBody PantryItemRequest request,
                                     @AuthenticationPrincipal PantryUserDetails user) {
        return service.update(id, request, user.getId());
    }

    @PatchMapping("/{id}/consume")
    public PantryItemResponse consume(@PathVariable Long id,
                                      @Valid @RequestBody(required = false) ConsumeRequest request,
                                      @AuthenticationPrincipal PantryUserDetails user) {
        return service.consume(id, request, user.getId());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @AuthenticationPrincipal PantryUserDetails user) {
        service.delete(id, user.getId());
        return ResponseEntity.noContent().build();
    }
}
