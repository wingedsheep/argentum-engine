package com.wingedsheep.engine.legalactions.support

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.Matcher
import io.kotest.matchers.MatcherResult
import io.kotest.matchers.should
import io.kotest.matchers.shouldNot

/**
 * Custom matchers for [LegalActionsView].
 *
 * These exist so tests read like statements about player intent
 * ("the player can cast Lightning Bolt") rather than collection arithmetic
 * ("the list contains a LegalAction whose action.cardId resolves to ...").
 */

// =============================================================================
// Cast spell matchers
// =============================================================================

fun containCastOf(cardName: String): Matcher<LegalActionsView> = Matcher { view ->
    val matches = view.castActionsFor(cardName)
    MatcherResult(
        matches.isNotEmpty(),
        { "expected legal actions to contain a Cast of '$cardName' but had: ${describeCasts(view)}" },
        { "expected legal actions NOT to contain a Cast of '$cardName' but did" }
    )
}

fun containAffordableCastOf(cardName: String): Matcher<LegalActionsView> = Matcher { view ->
    val matches = view.castActionsFor(cardName)
    val affordable = matches.filter { it.affordable }
    MatcherResult(
        affordable.isNotEmpty(),
        {
            "expected legal actions to contain an AFFORDABLE Cast of '$cardName' " +
                "but had ${matches.size} matching cast(s), of which 0 were affordable. " +
                "All casts: ${describeCasts(view)}"
        },
        { "expected legal actions NOT to contain an affordable Cast of '$cardName' but did" }
    )
}

infix fun LegalActionsView.shouldContainCastOf(cardName: String): LegalActionsView =
    apply { this should containCastOf(cardName) }

infix fun LegalActionsView.shouldNotContainCastOf(cardName: String): LegalActionsView =
    apply { this shouldNot containCastOf(cardName) }

infix fun LegalActionsView.shouldContainAffordableCastOf(cardName: String): LegalActionsView =
    apply { this should containAffordableCastOf(cardName) }

// =============================================================================
// Play land matchers
// =============================================================================

fun containPlayLandOf(cardName: String): Matcher<LegalActionsView> = Matcher { view ->
    val matches = view.playLandActionsFor(cardName)
    MatcherResult(
        matches.isNotEmpty(),
        { "expected legal actions to contain a PlayLand of '$cardName' but had: ${describePlayLands(view)}" },
        { "expected legal actions NOT to contain a PlayLand of '$cardName' but did" }
    )
}

infix fun LegalActionsView.shouldContainPlayLandOf(cardName: String): LegalActionsView =
    apply { this should containPlayLandOf(cardName) }

infix fun LegalActionsView.shouldNotContainPlayLandOf(cardName: String): LegalActionsView =
    apply { this shouldNot containPlayLandOf(cardName) }

// =============================================================================
// Activated ability matchers
// =============================================================================

fun containActivatedAbilityOn(sourceId: EntityId): Matcher<LegalActionsView> = Matcher { view ->
    val matches = view.activatedAbilityActionsFor(sourceId)
    MatcherResult(
        matches.isNotEmpty(),
        { "expected legal actions to contain an ActivateAbility on $sourceId but had none" },
        { "expected legal actions NOT to contain an ActivateAbility on $sourceId but did" }
    )
}

infix fun LegalActionsView.shouldContainActivatedAbilityOn(sourceId: EntityId): LegalActionsView =
    apply { this should containActivatedAbilityOn(sourceId) }

infix fun LegalActionsView.shouldNotContainActivatedAbilityOn(sourceId: EntityId): LegalActionsView =
    apply { this shouldNot containActivatedAbilityOn(sourceId) }

// =============================================================================
// Generic shape matchers
// =============================================================================

fun containOnlyPassPriority(): Matcher<LegalActionsView> = Matcher { view ->
    val onlyPass = view.size == 1 && view.single().action is PassPriority
    MatcherResult(
        onlyPass,
        {
            "expected legal actions to contain only PassPriority but had ${view.size} action(s): " +
                view.joinToString { it.actionType }
        },
        { "expected legal actions NOT to contain only PassPriority but it did" }
    )
}

fun LegalActionsView.shouldContainOnlyPassPriority(): LegalActionsView =
    apply { this should containOnlyPassPriority() }

// =============================================================================
// Internals
// =============================================================================

private fun describeCasts(view: LegalActionsView): String {
    val casts = view.castActions()
    if (casts.isEmpty()) return "[no cast actions]"
    return casts.joinToString { "${view.cardNameOf(it.action) ?: "?"}(affordable=${it.affordable})" }
}

private fun describePlayLands(view: LegalActionsView): String {
    val lands = view.playLandActions()
    if (lands.isEmpty()) return "[no play-land actions]"
    return lands.joinToString { view.cardNameOf(it.action) ?: "?" }
}

