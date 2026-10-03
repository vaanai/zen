package com.example.zen

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Nodes this service obtains — the active window, and every [AccessibilityNodeInfo.getChild] —
 * have to be recycled by the service. That includes a match, a node cap, and a depth cutoff.
 * The node a walk is given stays owned by the caller; the walk recycles only what it obtains.
 */
internal interface WalkNode {
    val childCount: Int
    fun obtainChild(index: Int): WalkNode?
    fun recycle()
    val viewIdResourceName: String?
    val text: CharSequence?
    val contentDescription: CharSequence?
    val packageName: CharSequence?
}

internal class FrameworkNode(
    private val node: AccessibilityNodeInfo,
) : WalkNode {
    override val childCount: Int
        get() = node.childCount

    override fun obtainChild(index: Int): WalkNode? =
        node.getChild(index)?.let { FrameworkNode(it) }

    @Suppress("DEPRECATION")
    override fun recycle() {
        node.recycle()
    }

    override val viewIdResourceName: String?
        get() = node.viewIdResourceName

    override val text: CharSequence?
        get() = node.text

    override val contentDescription: CharSequence?
        get() = node.contentDescription

    override val packageName: CharSequence?
        get() = node.packageName
}

internal fun AccessibilityService.obtainActiveWindow(): FrameworkNode? =
    rootInActiveWindow?.let { FrameworkNode(it) }

/**
 * Recycles [node] on every exit from [block], including a `return` from the enclosing function.
 * [block] is inlined so that return still runs the recycle.
 */
internal inline fun <T, R> useObtained(node: T, recycle: (T) -> Unit, block: (T) -> R): R {
    try {
        return block(node)
    } finally {
        recycle(node)
    }
}

internal inline fun <T, R> useObtainedOrNull(node: T?, recycle: (T) -> Unit, block: (T?) -> R): R {
    try {
        return block(node)
    } finally {
        node?.let(recycle)
    }
}

internal object NodeWalk {
    /**
     * Depth-first, last child first — the same order as the previous stack walk, so a node cap
     * still stops on the same node.
     *
     * [onNode] returns true to stop without obtaining that node's children. [stopBeforeVisit]
     * stops before the next pop; nodes already obtained are recycled even if they were not visited.
     * The node that trips [maxNodes] (`visited++ > maxNodes`) is recycled and not passed to [onNode].
     * Does not recycle [root].
     */
    fun walk(
        root: WalkNode,
        maxNodes: Int,
        maxDepth: Int,
        stopBeforeVisit: () -> Boolean = { false },
        onNode: (WalkNode) -> Boolean,
    ): Boolean {
        var visited = 0
        val stack = ArrayDeque<Pair<WalkNode, Int>>()
        val owned = ArrayDeque<WalkNode>()
        stack.addLast(root to 0)
        try {
            while (stack.isNotEmpty()) {
                if (stopBeforeVisit()) return true
                val (node, depth) = stack.removeLast()
                if (visited++ > maxNodes) return true
                if (onNode(node)) return true
                if (depth < maxDepth) {
                    for (i in 0 until node.childCount) {
                        val child = node.obtainChild(i) ?: continue
                        owned.addLast(child)
                        stack.addLast(child to depth + 1)
                    }
                }
            }
            return false
        } finally {
            // Children are still valid until here. Recycle every node this walk obtained, once.
            while (owned.isNotEmpty()) {
                owned.removeLast().recycle()
            }
        }
    }

    fun anyMatch(
        root: WalkNode,
        maxNodes: Int,
        maxDepth: Int,
        predicate: (WalkNode) -> Boolean,
    ): Boolean {
        var found = false
        walk(root, maxNodes, maxDepth) { node ->
            found = predicate(node)
            found
        }
        return found
    }

    /**
     * Preorder walk for the direct-message check. A node deeper than [maxDepth] is not read.
     * Its parent still obtains it, and this function recycles it; that node's children are not obtained.
     * Does not recycle [node] when it is the caller's node.
     */
    fun anyNode(
        node: WalkNode?,
        depth: Int,
        maxDepth: Int,
        predicate: (WalkNode) -> Boolean,
    ): Boolean {
        if (node == null || depth > maxDepth) return false
        if (predicate(node)) return true
        for (i in 0 until node.childCount) {
            val child = node.obtainChild(i) ?: continue
            try {
                if (anyNode(child, depth + 1, maxDepth, predicate)) return true
            } finally {
                child.recycle()
            }
        }
        return false
    }
}
