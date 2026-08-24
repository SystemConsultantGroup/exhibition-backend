package kr.ac.skku.scg.exhibition.board.controller;

import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import kr.ac.skku.scg.exhibition.board.dto.request.AdminBoardRequest;
import kr.ac.skku.scg.exhibition.board.dto.response.BoardResponse;
import kr.ac.skku.scg.exhibition.board.service.BoardService;
import kr.ac.skku.scg.exhibition.exhibition.domain.ExhibitionEntity;
import kr.ac.skku.scg.exhibition.global.auth.resolver.AuthenticatedUser;
import kr.ac.skku.scg.exhibition.global.auth.resolver.CurrentUser;
import kr.ac.skku.scg.exhibition.global.tenant.CurrentExhibition;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/boards")
@RequiredArgsConstructor
public class AdminBoardController {

    private final BoardService boardService;

    /** 현재 전시회에 공지사항을 작성한다. */
    @PostMapping
    public ResponseEntity<BoardResponse> create(
            @Valid @RequestBody AdminBoardRequest request,
            @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user
    ) {
        BoardResponse response = boardService.create(exhibition, request, user);
        return ResponseEntity.created(URI.create("/admin/boards/" + response.id())).body(response);
    }

    /** 현재 전시회의 공지사항을 수정한다. */
    @PutMapping("/{id}")
    public ResponseEntity<BoardResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody AdminBoardRequest request,
            @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user
    ) {
        return ResponseEntity.ok(boardService.update(id, exhibition.getId(), request, user));
    }

    /** 현재 전시회의 공지사항을 삭제한다. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable UUID id,
            @CurrentExhibition ExhibitionEntity exhibition,
            @CurrentUser AuthenticatedUser user
    ) {
        boardService.delete(id, exhibition.getId(), user);
        return ResponseEntity.noContent().build();
    }
}
