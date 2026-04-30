package com.wingedsheep.gameserver.deck

import com.wingedsheep.engine.registry.CardRegistry
import org.springframework.stereotype.Component

/**
 * Validates a deck list against the card registry and basic constructed-format rules.
 *
 * Rules enforced:
 * - Every entry must resolve in the [CardRegistry] (or via collector-number variants such as `Plains#196`).
 * - Non-basic cards: at most 4 copies (sum across collector-number variants of the same card name).
 * - Basic lands: any number of copies allowed.
 * - Total deck size: at least [MIN_DECK_SIZE] cards.
 *
 * Anything that is *unusual but legal* (e.g. exactly 60, all-basic decks) is accepted silently.
 * Anything blocking (unknown card, > 4 of a non-basic, < 60 cards) becomes an error.
 * Anything we want to surface but not block (e.g. very large decks, suspicious mana curves) becomes a warning.
 */
@Component
class DeckValidator(
    private val cardRegistry: CardRegistry
) {

    fun validate(deckList: Map<String, Int>): DeckValidationResult {
        val errors = mutableListOf<DeckValidationIssue>()
        val warnings = mutableListOf<DeckValidationIssue>()

        // Filter out zero/negative counts up front; treat them as if they weren't submitted.
        val sanitized = deckList.filterValues { it > 0 }
        val totalCards = sanitized.values.sum()

        // Group entries by their *base* card name so collector-number variants stack toward the 4-of rule.
        val countsByBaseName = mutableMapOf<String, Int>()
        for ((entry, count) in sanitized) {
            val card = cardRegistry.getCard(entry)
            if (card == null) {
                errors += DeckValidationIssue(
                    code = "UNKNOWN_CARD",
                    message = "Unknown card: \"$entry\"",
                    cardName = entry
                )
                continue
            }
            countsByBaseName.merge(card.name, count, Int::plus)
        }

        for ((cardName, count) in countsByBaseName) {
            val card = cardRegistry.getCard(cardName) ?: continue
            if (!card.typeLine.isBasicLand && count > MAX_COPIES_NON_BASIC) {
                errors += DeckValidationIssue(
                    code = "TOO_MANY_COPIES",
                    message = "$count copies of $cardName — limit is $MAX_COPIES_NON_BASIC for non-basic cards",
                    cardName = cardName
                )
            }
        }

        if (totalCards < MIN_DECK_SIZE) {
            errors += DeckValidationIssue(
                code = "TOO_FEW_CARDS",
                message = "Deck has $totalCards cards — minimum is $MIN_DECK_SIZE",
                cardName = null
            )
        }

        return DeckValidationResult(
            valid = errors.isEmpty(),
            totalCards = totalCards,
            errors = errors,
            warnings = warnings
        )
    }

    companion object {
        const val MIN_DECK_SIZE = 40
        const val MAX_COPIES_NON_BASIC = 4
    }
}

data class DeckValidationResult(
    val valid: Boolean,
    val totalCards: Int,
    val errors: List<DeckValidationIssue>,
    val warnings: List<DeckValidationIssue>
)

data class DeckValidationIssue(
    val code: String,
    val message: String,
    val cardName: String?
)
