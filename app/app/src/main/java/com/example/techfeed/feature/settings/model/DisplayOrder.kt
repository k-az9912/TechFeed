package com.example.techfeed.feature.settings.model

fun normalizeOrder(order: List<String>, available: List<String>): List<String> =
    (order.filter { it in available } + available).distinct()

fun moveInOrder(order: List<String>, item: String, offset: Int): List<String> {
    val from = order.indexOf(item)
    val to = from + offset
    if (from < 0 || to !in order.indices || offset !in listOf(-1, 1)) return order
    return order.toMutableList().apply { add(to, removeAt(from)) }
}
