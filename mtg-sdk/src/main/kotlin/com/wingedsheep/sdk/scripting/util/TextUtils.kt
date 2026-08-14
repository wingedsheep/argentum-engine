package com.wingedsheep.sdk.scripting.util

/**
 * "a Goblin" / "three Goblins" — the article-or-count phrase shared by the selection atoms. Small
 * counts are spelled out (oracle convention); the article respects a vowel-leading filter description.
 */
fun quantify(count: Int, filterDescription: String): String =
    if (count == 1) {
        val article = if (filterDescription.firstOrNull()?.lowercaseChar() in listOf('a', 'e', 'i', 'o', 'u')) "an" else "a"
        "$article $filterDescription"
    } else {
        "${numberToWord(count)} ${filterDescription}s"
    }

fun numberToWord(n: Int): String = when (n) {
    1 -> "one"
    2 -> "two"
    3 -> "three"
    4 -> "four"
    5 -> "five"
    6 -> "six"
    7 -> "seven"
    8 -> "eight"
    9 -> "nine"
    10 -> "ten"
    else -> n.toString()
}
