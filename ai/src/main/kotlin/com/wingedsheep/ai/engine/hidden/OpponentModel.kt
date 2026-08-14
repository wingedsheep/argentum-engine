package com.wingedsheep.ai.engine.hidden

/**
 * Prior knowledge available when sampling an opponent's hidden cards.
 *
 * A known list is appropriate for open-decklist formats and the arena. When no list is available,
 * identity permutation removes knowledge of which hidden card occupies which slot without
 * inventing cards outside the game. The latter is intentionally "cheating-lite": it knows the
 * hidden multiset, but not hand contents or library order.
 */
sealed interface OpponentModel {
    data class KnownDecklist(val cards: Map<String, Int>) : OpponentModel {
        init {
            require(cards.values.all { it >= 0 }) { "Decklist counts must be non-negative" }
        }
    }

    data object IdentityPermutation : OpponentModel
}
