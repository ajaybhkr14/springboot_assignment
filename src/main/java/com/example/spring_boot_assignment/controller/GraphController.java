package com.example.spring_boot_assignment.controller;


import com.example.spring_boot_assignment.model.ChildrenTransactionsResponse;
import com.example.spring_boot_assignment.model.LevelAggregatesResponse;
import com.example.spring_boot_assignment.model.NodeResponse;
import com.example.spring_boot_assignment.service.GraphService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/graph/nodes")
public class GraphController {

    private final GraphService graphService;

    public GraphController(GraphService graphService) {
        this.graphService = graphService;
    }

    @GetMapping("/{id}")
    public NodeResponse getNode(@PathVariable("id") String id,
                                @RequestParam(name = "maxDepth", defaultValue = "1") int maxDepth) {
        return graphService.getNode(id, maxDepth);
    }

    @GetMapping("/{id}/children-transactions")
    public ChildrenTransactionsResponse getChildrenTransactions(
        @PathVariable("id") String id,
        @RequestParam(name = "minAmount", required = false) Double minAmount,
        @RequestParam(name = "maxAmount", required = false) Double maxAmount,
        @RequestParam(name = "txnType", required = false) String txnType) {
        return graphService.getChildrenTransactions(id, minAmount, maxAmount, txnType);
    }

    @GetMapping("/{id}/level-aggregates")
    public LevelAggregatesResponse getLevelAggregates(@PathVariable("id") String id) {
        return graphService.getLevelAggregates(id);
    }
}
