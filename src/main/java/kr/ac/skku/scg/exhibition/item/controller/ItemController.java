package kr.ac.skku.scg.exhibition.item.controller;

import jakarta.validation.Valid;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUserArgumentResolver;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import static org.springframework.data.domain.Sort.Direction.DESC;

@Validated
@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> get(
            @PathVariable UUID id,
            @RequestAttribute(name = CurrentUserArgumentResolver.REQUEST_ATTR_USER_ID, required = false) UUID currentUserId) {
        return ResponseEntity.ok(itemService.get(id, currentUserId));
    }

    @GetMapping
    public ResponseEntity<ListResponse<ItemResponse>> list(
            @Valid @ModelAttribute ItemListRequest request,
            @PageableDefault(sort = "createdAt", direction = DESC)
            Pageable pageable,
            @RequestAttribute(name = CurrentUserArgumentResolver.REQUEST_ATTR_USER_ID, required = false) UUID currentUserId) {
        return ResponseEntity.ok(itemService.list(request, pageable, currentUserId));
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
