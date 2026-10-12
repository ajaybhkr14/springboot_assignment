package com.example.spring_boot_assignment.service;


import com.example.spring_boot_assignment.exception.CycleDetectedException;
import com.example.spring_boot_assignment.exception.InvalidRequestException;
import com.example.spring_boot_assignment.exception.NodeNotFoundException;
import com.example.spring_boot_assignment.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


class GraphServiceTest {

    private GraphService service;

    @BeforeEach
    void setUp() {
        service = new GraphService();
        service.loadData();
    }

    // ---------- tests using the real data file ----------

    @Test
    void rootNodeHasLevelZeroAndItsChildren() {
        NodeResponse r = service.getNode("N1");

        assertEquals(0, r.level());
        assertTrue(r.isRoot());
        assertFalse(r.isLeaf());
        assertTrue(r.parentChain().isEmpty());
        assertEquals(List.of("N2", "N3"), ids(r.children()));
    }

    @Test
    void middleNodeReturnsOwnAndNextLevelTransactions() {
        NodeResponse r = service.getNode("N2");

        assertEquals(1, r.level());
        assertEquals(List.of("N1"), ids(r.parentChain()));
        assertEquals(List.of("N4", "N5"), ids(r.children()));
        assertEquals(5, r.transactions().size());
        assertEquals("T6", r.transactions().get(0).txnId());
        assertEquals(Set.of("N4", "N5"), r.nextLevelTransactions().keySet());
        assertEquals(5, r.nextLevelTransactions().get("N4").size());
    }

    @Test
    void leafNodeHasParentChainFromRootDown() {
        NodeResponse r = service.getNode("N13");

        assertEquals(3, r.level());
        assertEquals(List.of("N1", "N3", "N7"), ids(r.parentChain()));
        assertFalse(r.isRoot());
        assertTrue(r.isLeaf());
        assertTrue(r.children().isEmpty());
        assertTrue(r.nextLevelTransactions().isEmpty());
    }

    @Test
    void nodeWithMissingParentIsTreatedAsRoot() {
        NodeResponse r = service.getNode("N19");

        assertEquals(0, r.level());
        assertTrue(r.isRoot());
        assertTrue(r.isLeaf());
        assertTrue(r.parentChain().isEmpty());
    }

    @Test
    void unknownIdThrowsNodeNotFound() {
        NodeNotFoundException e = assertThrows(NodeNotFoundException.class,
            () -> service.getNode("N999"));

        assertEquals("Graph node N999 does not exist", e.getMessage());
    }

    @Test
    void blankIdThrowsInvalidRequest() {
        assertThrows(InvalidRequestException.class, () -> service.getNode("   "));
    }

    // ---------- tests using hand-made data ----------

    @Test
    void twoNodesPointingAtEachOtherIsACycle() {
        GraphService cyclic = new GraphService();
        cyclic.index(List.of(node("A", "B"), node("B", "A")));

        assertThrows(CycleDetectedException.class, () -> cyclic.getNode("A"));
    }

    @Test
    void nodeThatIsItsOwnParentIsACycle() {
        GraphService cyclic = new GraphService();
        cyclic.index(List.of(node("A", "A")));

        assertThrows(CycleDetectedException.class, () -> cyclic.getNode("A"));
    }

    // ---------- helpers ----------

    private static GraphNode node(String id, String parentId) {
        return new GraphNode(id, parentId, "Node " + id, "ACC-" + id, List.of());
    }

    private static List<String> ids(List<NodeSummary> nodes) {
        return nodes.stream().map(NodeSummary::id).toList();
    }

    // ---------- children-transactions filters ----------

    @Test
    void noFiltersReturnsAllChildTransactions() {
        ChildrenTransactionsResponse r = service.getChildrenTransactions("N2", null, null, null);

        assertEquals(1, r.level());
        assertEquals(Set.of("N4", "N5"), r.childrenTransactions().keySet());
        assertEquals(5, r.childrenTransactions().get("N4").size());
        assertEquals(5, r.childrenTransactions().get("N5").size());
    }

    @Test
    void txnTypeFilterKeepsOnlyThatType() {
        ChildrenTransactionsResponse r = service.getChildrenTransactions("N2", null, null, "POS");

        assertEquals(List.of("T18"), txnIds(r.childrenTransactions().get("N4")));
        assertEquals(List.of("T22"), txnIds(r.childrenTransactions().get("N5")));
    }

    @Test
    void txnTypeFilterIgnoresCase() {
        ChildrenTransactionsResponse r = service.getChildrenTransactions("N2", null, null, "pos");

        assertEquals(List.of("T18"), txnIds(r.childrenTransactions().get("N4")));
    }

    @Test
    void amountRangeIncludesBothBounds() {
        ChildrenTransactionsResponse r = service.getChildrenTransactions("N2", 2000.0, 2200.0, null);

        assertEquals(List.of("T20"), txnIds(r.childrenTransactions().get("N4")));
        assertEquals(List.of("T21", "T22", "T23", "T24"), txnIds(r.childrenTransactions().get("N5")));
    }

    @Test
    void leafNodeHasNoChildTransactions() {
        ChildrenTransactionsResponse r = service.getChildrenTransactions("N13", null, null, null);

        assertEquals(3, r.level());
        assertTrue(r.childrenTransactions().isEmpty());
    }

    @Test
    void minGreaterThanMaxIsInvalid() {
        assertThrows(InvalidRequestException.class,
            () -> service.getChildrenTransactions("N2", 500.0, 100.0, null));
    }

    @Test
    void unknownTxnTypeIsInvalid() {
        assertThrows(InvalidRequestException.class,
            () -> service.getChildrenTransactions("N2", null, null, "CASH"));
    }

    @Test
    void filtersOnUnknownNodeThrowsNotFound() {
        assertThrows(NodeNotFoundException.class,
            () -> service.getChildrenTransactions("N999", null, null, null));
    }

    private static List<String> txnIds(List<NodeTransaction> txns) {
        return txns.stream().map(NodeTransaction::txnId).toList();
    }
}
