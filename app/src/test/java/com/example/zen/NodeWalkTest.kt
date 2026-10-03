package com.example.zen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every node a walk obtains is recycled once, on every return path. The caller's root is not
 * recycled by the walk. Nothing here needs a device or an [android.accessibilityservice.AccessibilityService].
 */
class NodeWalkTest {

    @Test
    fun walk_exhaustsTheTreeAndRecyclesEveryChildOnce() {
        val a0 = node("a0")
        val a1 = node("a1")
        val a = node("a", a0, null, a1)
        val b = node("b")
        val rootSpec = node("root", a, null, b)
        val root = rootSpec.obtain()

        val order = mutableListOf<String>()
        val stopped = NodeWalk.walk(root, maxNodes = 2000, maxDepth = 30) { copy ->
            order += copy.fake.markVisit()
            false
        }

        assertFalse(stopped)
        assertEquals(listOf("root", "b", "a", "a1", "a0"), order)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyMatch_onRootDoesNotObtainChildren() {
        val hidden = node("hidden")
        val rootSpec = node("root", hidden)
        val root = rootSpec.obtain()

        val found = NodeWalk.anyMatch(root, maxNodes = 2000, maxDepth = 30) { it.fake.name == "root" }

        assertTrue(found)
        assertEquals(0, hidden.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyMatch_stopsBeforeTheMatchsChildrenAndRecyclesUnvisitedSiblings() {
        val hidden = node("hidden")
        val match = node("match", hidden)
        val later = node("later", node("later-child"))
        // Last child is visited first. `later` is obtained with the match, then left on the stack.
        val rootSpec = node("root", later, match)
        val root = rootSpec.obtain()

        val visited = mutableListOf<String>()
        val found = NodeWalk.anyMatch(root, maxNodes = 2000, maxDepth = 30) { copy ->
            visited += copy.fake.markVisit()
            copy.fake.name == "match"
        }

        assertTrue(found)
        assertEquals(listOf("root", "match"), visited)
        assertEquals(0, hidden.copies.size)
        assertEquals(0, later.copies.single().visits)
        assertEquals(0, later.children.filterNotNull().single().copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyMatch_nodePastTheCapIsNotAMatchAndIsStillRecycled() {
        val a = node("a")
        val b = node("b")
        val rootSpec = node("root", a, b)
        val root = rootSpec.obtain()

        // `visited++ > maxNodes` reads maxNodes + 1 nodes. With 1, root and b are read;
        // a is popped after that, recycled, and is not a match.
        val found = NodeWalk.anyMatch(root, maxNodes = 1, maxDepth = 30) {
            it.fake.markVisit()
            it.fake.name == "a"
        }

        assertFalse(found)
        assertEquals(1, root.visits)
        assertEquals(1, b.copies.single().visits)
        assertEquals(0, a.copies.single().visits)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun walk_doesNotObtainPastMaxDepth() {
        val grand = node("grand")
        val child = node("child", grand)
        val rootSpec = node("root", child)
        val root = rootSpec.obtain()

        NodeWalk.walk(root, maxNodes = 2000, maxDepth = 1) { copy ->
            copy.fake.markVisit()
            false
        }

        assertEquals(1, child.copies.single().visits)
        assertEquals(0, grand.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun walk_stopBeforeVisitRecyclesObtainedNodesItNeverReads() {
        val child = node("child", node("grand"))
        val rootSpec = node("root", child)
        val root = rootSpec.obtain()
        var visits = 0

        val stopped = NodeWalk.walk(
            root,
            maxNodes = 2000,
            maxDepth = 30,
            stopBeforeVisit = { visits >= 1 },
        ) { copy ->
            visits++
            copy.fake.markVisit()
            false
        }

        assertTrue(stopped)
        assertEquals(1, root.visits)
        assertEquals(0, child.copies.single().visits)
        assertEquals(0, child.children.single()!!.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun walk_recyclesWhenTheVisitorThrows() {
        val hidden = node("hidden")
        val high = node("high", hidden)
        val low = node("low")
        val rootSpec = node("root", low, high)
        val root = rootSpec.obtain()

        try {
            NodeWalk.walk(root, maxNodes = 2000, maxDepth = 30) { copy ->
                copy.fake.markVisit()
                if (copy.fake.name == "high") error("boom")
                false
            }
            error("expected the visitor to throw")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertEquals(0, hidden.copies.size)
        assertEquals(0, low.copies.single().visits)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyNode_nullAndDepthCutoffRecycleTheObtainedNodeWithoutReadingIt() {
        assertFalse(NodeWalk.anyNode(null, depth = 0, maxDepth = 8) { true })

        val tooDeep = node("too-deep")
        val edge = node("edge", tooDeep)
        val rootSpec = node("root", edge)
        val root = rootSpec.obtain()
        val read = mutableListOf<String>()

        val found = NodeWalk.anyNode(root, depth = 0, maxDepth = 0) { copy ->
            read += copy.fake.markVisit()
            false
        }

        assertFalse(found)
        assertEquals(listOf("root"), read)
        assertEquals(0, edge.copies.single().visits)
        assertEquals(0, tooDeep.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyNode_directMessageDepthReadsThroughEightAndRecyclesTheNinth() {
        val specs = arrayOfNulls<FakeTree>(11)
        for (depth in 10 downTo 0) {
            val deeper = if (depth == 10) null else specs[depth + 1]
            specs[depth] = if (deeper == null) node("n$depth") else node("n$depth", deeper)
        }
        val rootSpec = specs[0]!!
        val root = rootSpec.obtain()
        val read = mutableListOf<String>()

        val found = NodeWalk.anyNode(root, depth = 0, maxDepth = 8) { copy ->
            read += copy.fake.name
            false
        }

        assertFalse(found)
        assertEquals((0..8).map { "n$it" }, read)
        assertEquals(1, specs[9]!!.copies.single().recycleCount)
        assertEquals(0, specs[10]!!.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyNode_matchOnRootDoesNotObtainChildren() {
        val hidden = node("hidden")
        val rootSpec = node("root", hidden)
        val root = rootSpec.obtain()

        val found = NodeWalk.anyNode(root, depth = 0, maxDepth = 8) { it.fake.name == "root" }

        assertTrue(found)
        assertEquals(0, hidden.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyNode_matchRecyclesThatChildAndSkipsLaterSiblings() {
        val hidden = node("hidden")
        val match = node("match", hidden)
        val earlier = node("earlier")
        val later = node("later")
        val rootSpec = node("root", earlier, match, later)
        val root = rootSpec.obtain()

        val found = NodeWalk.anyNode(root, depth = 0, maxDepth = 8) { it.fake.name == "match" }

        assertTrue(found)
        assertEquals(1, earlier.copies.size)
        assertEquals(0, hidden.copies.size)
        assertEquals(0, later.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun anyNode_recyclesTheChildWhenThePredicateThrows() {
        val later = node("later")
        val child = node("child")
        val rootSpec = node("root", child, later)
        val root = rootSpec.obtain()

        try {
            NodeWalk.anyNode(root, depth = 0, maxDepth = 8) { copy ->
                if (copy.fake.name == "child") error("boom")
                false
            }
            error("expected the predicate to throw")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertEquals(0, later.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 0)
    }

    @Test
    fun useObtained_recyclesRootAfterAMatchAndOnANonLocalReturn() {
        val child = node("child")
        val rootSpec = node("root", child)
        val root = rootSpec.obtain()

        val found = useObtained(root, FakeCopy::recycle) { window ->
            NodeWalk.anyMatch(window, maxNodes = 2000, maxDepth = 30) { it.fake.name == "child" }
        }

        assertTrue(found)
        assertOwned(rootSpec, root, rootRecycles = 1)

        val earlySpec = node("early", node("unused"))
        val early = earlySpec.obtain()
        assertEquals("left", leaveEarly(early))
        assertEquals(0, earlySpec.children.single()!!.copies.size)
        assertOwned(earlySpec, early, rootRecycles = 1)
    }

    @Test
    fun useObtained_recyclesRootAndChildrenWhenTheWalkThrows() {
        val hidden = node("hidden")
        val high = node("high", hidden)
        val low = node("low")
        val rootSpec = node("root", low, high)
        val root = rootSpec.obtain()

        try {
            useObtained(root, FakeCopy::recycle) { window ->
                NodeWalk.anyMatch(window, maxNodes = 2000, maxDepth = 30) { copy ->
                    if (copy.fake.name == "high") error("boom")
                    false
                }
            }
            error("expected the walk to throw")
        } catch (e: IllegalStateException) {
            assertEquals("boom", e.message)
        }

        assertEquals(0, hidden.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 1)
    }

    @Test
    fun useObtainedOrNull_nullRootDoesNotThrow() {
        var ran = false
        val value = useObtainedOrNull(null as FakeCopy?, FakeCopy::recycle) {
            ran = true
            4
        }
        assertTrue(ran)
        assertEquals(4, value)
    }

    @Test
    fun repeatedPasses_recycleEveryCopyFromEachPass() {
        val reel = node("reel")
        val message = node("message")
        val chat = node("chat", message)
        val rootSpec = node("root", reel, chat)
        val root = rootSpec.obtain()

        val decision = useObtained(root, FakeCopy::recycle) { window ->
            NodeWalk.anyMatch(window, maxNodes = 2000, maxDepth = 30) { it.fake.name == "reel" }
            NodeWalk.anyNode(window, depth = 0, maxDepth = 8) { it.fake.name == "message" }
            val stillThere = NodeWalk.anyMatch(window, maxNodes = 2000, maxDepth = 30) {
                it.fake.name == "absent"
            }
            if (!stillThere) return@useObtained "stay"
            "block"
        }

        assertEquals("stay", decision)
        // Short-form, direct-message, short-form again: each pass obtains its own children.
        assertEquals(3, reel.copies.size)
        assertEquals(3, chat.copies.size)
        assertEquals(3, message.copies.size)
        assertOwned(rootSpec, root, rootRecycles = 1)
    }

    @Test
    fun serviceDoesNotObtainNodesItself() {
        val source = sourceFile("ZenAccessibilityService.kt")
        assertFalse(source.contains("rootInActiveWindow"))
        assertFalse(source.contains("getChild("))
        assertFalse(source.contains(".recycle("))
        assertTrue(source.contains("obtainActiveWindow()"))
        assertTrue(source.contains("useObtained"))
        assertTrue(source.contains("const val MAX_NODES = 2000"))
        assertTrue(source.contains("maxDepth = 8"))

        val walk = sourceFile("NodeWalk.kt")
        assertTrue(walk.contains("rootInActiveWindow"))
        assertTrue(walk.contains("getChild("))
        assertTrue(walk.contains("node.recycle()"))
    }

    private fun leaveEarly(root: FakeCopy): String {
        useObtainedOrNull(root, FakeCopy::recycle) {
            return "left"
        }
    }

    private fun sourceFile(name: String): String {
        val relative = "src/main/java/com/example/zen/$name"
        val start = File(System.getProperty("user.dir") ?: error("user.dir is unset"))
        val matches = generateSequence(start) { it.parentFile }
            .take(6)
            .flatMap { dir ->
                sequenceOf(File(dir, relative), File(dir, "app/$relative"))
            }
            .firstOrNull { it.isFile }
        check(matches != null) { "$name not found from $start" }
        return matches.readText()
    }

    private fun assertOwned(spec: FakeTree, root: FakeCopy, rootRecycles: Int) {
        fun visit(tree: FakeTree) {
            for (copy in tree.copies) {
                val expected = if (copy === root) rootRecycles else 1
                assertTrue(
                    "${copy.name} recycled ${copy.recycleCount} times, expected $expected",
                    copy.recycleCount == expected,
                )
            }
            tree.children.filterNotNull().forEach { visit(it) }
        }
        visit(spec)
    }

    private fun node(name: String, vararg children: FakeTree?): FakeTree =
        FakeTree(name, children.toList())

    private class FakeTree(
        val name: String,
        val children: List<FakeTree?>,
    ) {
        val copies = mutableListOf<FakeCopy>()

        fun obtain(): FakeCopy = FakeCopy(this).also { copies += it }
    }

    private class FakeCopy(private val spec: FakeTree) : WalkNode {
        val name: String get() = spec.name
        var recycleCount = 0
            private set
        var visits = 0
            private set

        override val childCount: Int
            get() = spec.children.size

        override fun obtainChild(index: Int): WalkNode? {
            check(recycleCount == 0) { "$name used after recycle" }
            val child = spec.children[index] ?: return null
            return child.obtain()
        }

        override fun recycle() {
            check(recycleCount == 0) { "$name recycled twice" }
            recycleCount++
        }

        override val viewIdResourceName: String? = null
        override val text: CharSequence? = null
        override val contentDescription: CharSequence? = null
        override val packageName: CharSequence? = null

        fun markVisit(): String {
            check(recycleCount == 0) { "$name visited after recycle" }
            visits++
            return name
        }
    }

    private val WalkNode.fake: FakeCopy
        get() = this as FakeCopy
}
