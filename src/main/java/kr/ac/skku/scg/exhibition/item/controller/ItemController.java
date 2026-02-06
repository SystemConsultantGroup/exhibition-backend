package kr.ac.skku.scg.exhibition.item.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.item.dto.request.ItemListRequest;
import kr.ac.skku.scg.exhibition.item.dto.response.ItemResponse;
import kr.ac.skku.scg.exhibition.item.service.ItemService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/items")
public class ItemController {

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(itemService.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ItemResponse>> list(@Valid @ModelAttribute ItemListRequest request) {
        return ResponseEntity.ok(itemService.list(request));
    }
}
