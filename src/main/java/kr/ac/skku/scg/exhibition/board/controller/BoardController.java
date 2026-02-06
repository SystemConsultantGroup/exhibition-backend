package kr.ac.skku.scg.exhibition.board.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.dto.request.BoardListRequest;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.service.BoardService;
import kr.ac.skku.scg.exhibition.global.dto.ListResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/boards")
@RequiredArgsConstructor
public class BoardController {

    private final BoardService boardService;

    @GetMapping("/{id}")
    public ResponseEntity<BoardResponse> get(@PathVariable UUID id) {
        return ResponseEntity.ok(boardService.get(id));
    }

    @GetMapping
    public ResponseEntity<ListResponse<BoardResponse>> list(@Valid @ModelAttribute BoardListRequest request) {
        List<BoardResponse> boards = boardService.list(request);
        return ResponseEntity.ok(ListResponse.of(boards));
    }
}
