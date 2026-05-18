package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.UntapFilteredDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Regression test for the `else -> true` fall-through in
 * [com.wingedsheep.engine.core.BeginningPhaseManager.matchesFilterForUntap].
 *
 * A `UntapFilteredDuringOtherUntapSteps` ability whose filter uses a predicate the
 * untap matcher doesn't yet know how to evaluate (e.g. `NameEquals`, `HasSubtype`,
 * `IsLegendary`) used to fall through to `true`, causing every controlled permanent
 * to untap during other players' untap steps. The fix is fail-closed: an unhandled
 * predicate means "we can't tell — don't untap".
 */
class FilteredUntapFailClosedTest : FunSpec({

    // Granter: "Untap each permanent named \"Special\" you control during each other
    // player's untap step." The filter's `NameEquals` predicate is not handled by the
    // untap matcher, so under the bug every permanent would untap.
    val NameFilterGranter = CardDefinition.creature(
        name = "Name Filter Granter",
        manaCost = ManaCost.parse("{3}{W}{B}{G}"),
        subtypes = setOf(Subtype("Elephant")),
        power = 5,
        toughness = 7,
        oracleText = "Untap each permanent named \"Special\" you control during each other player's untap step.",
        script = CardScript(
            staticAbilities = listOf(
                UntapFilteredDuringOtherUntapSteps(
                    filter = GameObjectFilter(
                        cardPredicates = listOf(CardPredicate.NameEquals("Special"))
                    )
                )
            )
        )
    )

    val NotSpecialCreature = CardDefinition.creature(
        name = "Not Special Creature",
        manaCost = ManaCost.parse("{2}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 3,
        toughness = 3
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(
            TestCards.all + listOf(NameFilterGranter, NotSpecialCreature)
        )
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 20, "Forest" to 20),
            skipMulligans = true
        )
        return driver
    }

    test("filtered untap with unhandled predicate does not untap non-matching permanents") {
        val driver = createDriver()
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent puts the granter and a non-matching creature on the battlefield.
        driver.putCreatureOnBattlefield(opponent, "Name Filter Granter")
        val notSpecial = driver.putCreatureOnBattlefield(opponent, "Not Special Creature")

        // End active player's turn → opponent becomes active. Advance to opponent's main
        // phase. Tapping must happen *after* opponent's own UNTAP step (which would
        // otherwise untap the creature as part of normal turn flow, unrelated to the
        // filtered-untap ability under test). UNTAP itself has no priority so we target
        // UPKEEP — by then UNTAP has already fired.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 400)
        driver.activePlayer shouldBe opponent
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN, maxPasses = 400)
        driver.tapPermanent(notSpecial)

        // Advance past opponent's turn back to the original active player's UPKEEP. The
        // active player's UNTAP step has just fired, which is where the opponent's
        // UntapFilteredDuringOtherUntapSteps would attempt to untap matching permanents.
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 400)
        driver.activePlayer shouldBe activePlayer

        // The non-matching creature must still be tapped. Under the previous
        // `else -> true` fall-through, the unhandled `NameEquals` predicate would match
        // every permanent and the creature would have untapped — which is the regression
        // we're guarding against.
        val isTapped = driver.state.getEntity(notSpecial)?.has<TappedComponent>() ?: false
        isTapped shouldBe true
    }
})
