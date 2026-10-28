package com.example.techfeed.feature.settings.model

import org.junit.Assert.*
import org.junit.Test

class DisplayOrderTest {

    @Test fun savedOrderDropsObsoleteValuesAndAppendsNewValues() {
        assertEquals(listOf("AI", "セキュリティ", "Android"),
            normalizeOrder(listOf("old", "AI", "AI", "セキュリティ"),
                listOf("セキュリティ", "AI", "Android")))
    }

    @Test fun moveSwapsNeighborsAndPreservesAllItems() {
        assertEquals(listOf("AI", "Android", "セキュリティ"),
            moveInOrder(listOf("AI", "セキュリティ", "Android"), "Android", -1))
        assertEquals(listOf("セキュリティ", "AI", "Android"),
            moveInOrder(listOf("AI", "セキュリティ", "Android"), "AI", 1))
    }

    @Test fun edgesAndMissingItemsDoNotChangeOrder() {
        val order = listOf("AI", "Android")
        assertEquals(order, moveInOrder(order, "AI", -1))
        assertEquals(order, moveInOrder(order, "Android", 1))
        assertEquals(order, moveInOrder(order, "missing", 1))
    }
}
