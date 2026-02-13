package kr.ac.skku.scg.exhibition.item.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.get(id));
    }

    @GetMapping
    public ResponseEntity<ListResponse<ItemResponse>> list(@Valid @ModelAttribute ItemListRequest request) {
        List<ItemResponse> items = itemService.list(request);
        return ResponseEntity.ok(ListResponse.of(items));
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> like(@PathVariable UUID id, @CurrentUser AuthenticatedUser currentUser) {
        itemService.like(id, currentUser);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlike(@PathVariable UUID id, @CurrentUser AuthenticatedUser currentUser) {
        itemService.unlike(id, currentUser);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
