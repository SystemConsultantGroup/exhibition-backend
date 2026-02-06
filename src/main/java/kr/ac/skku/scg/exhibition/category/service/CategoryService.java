package kr.ac.skku.scg.exhibition.category.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.ac.skku.scg.exhibition.category.domain.CategoryEntity;
import kr.ac.skku.scg.exhibition.category.dto.CategoryDtos.CategoryTreeNode;
import kr.ac.skku.scg.exhibition.category.repository.CategoryRepository;
import kr.ac.skku.scg.exhibition.exhibition.service.ExhibitionService;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ExhibitionService exhibitionService;

    public List<CategoryEntity> getTree(UUID exhibitionId) {
        exhibitionService.get(exhibitionId);
        return categoryRepository.findByExhibitionIdOrderByDepthAscOrderIndexAscNameAsc(exhibitionId);
    }

    public List<CategoryTreeNode> buildTreeResponse(UUID exhibitionId) {
        List<CategoryEntity> categories = getTree(exhibitionId);
        Map<UUID, CategoryTreeNode> nodes = categories.stream()
            .collect(Collectors.toMap(
                CategoryEntity::getId,
                c -> new CategoryTreeNode(c.getId(), c.getName(), c.getOrderIndex(), c.getPath(), c.getDepth(), new ArrayList<>())
            ));

        List<CategoryTreeNode> roots = new ArrayList<>();
        Function<UUID, CategoryTreeNode> getNode = nodes::get;

        for (CategoryEntity category : categories) {
            var node = getNode.apply(category.getId());
            if (category.getParent() == null) {
                roots.add(node);
                continue;
            }
            var parentNode = getNode.apply(category.getParent().getId());
            if (parentNode != null) {
                parentNode.children().add(node);
            }
        }

        sortTree(roots);
        return roots;
    }

    private void sortTree(List<CategoryTreeNode> nodes) {
        nodes.sort(Comparator.comparingInt(CategoryTreeNode::orderIndex));
        for (var node : nodes) {
            sortTree(node.children());
        }
    }
}
