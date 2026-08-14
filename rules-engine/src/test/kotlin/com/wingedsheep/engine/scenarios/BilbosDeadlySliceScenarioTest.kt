package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bilbo's Deadly Slice (HOB #62) — {1}{B}{B} Instant. "Destroy target creature."
 *
 * Covers that the kill is a *destruction* (indestructible survives it), that the creature lands
 * in its owner's graveyard rather than the caster's, and that the spell itself is put away.
 */
class BilbosDeadlySliceScenarioTest : ScenarioTestBase() {

    init {
        cardRegistry.register(
            CardDefinition.creature(
                "Test Unkillable", ManaCost.parse("{3}"), emptySet(),
                power = 3, toughness = 3, keywords = setOf(Keyword.INDESTRUCTIBLE)
            )
        )

        context("Bilbo's Deadly Slice") {

            test("destroys the targeted creature into its owner's graveyard") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bilbo's Deadly Slice")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val courser = game.findPermanent("Centaur Courser")!!
                game.castSpell(1, "Bilbo's Deadly Slice", targetId = courser).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("the creature left the battlefield") {
                    game.findPermanent("Centaur Courser") shouldBe null
                }
                withClue("it went to its *owner's* graveyard, not the caster's") {
                    game.isInGraveyard(2, "Centaur Courser") shouldBe true
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                }
                withClue("the instant itself is in the caster's graveyard") {
                    game.isInGraveyard(1, "Bilbo's Deadly Slice") shouldBe true
                }
            }

            test("an indestructible creature survives — the effect destroys rather than exiles") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Bilbo's Deadly Slice")
                    .withLandsOnBattlefield(1, "Swamp", 3)
                    .withCardOnBattlefield(2, "Test Unkillable")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val unkillable = game.findPermanent("Test Unkillable")!!
                game.castSpell(1, "Bilbo's Deadly Slice", targetId = unkillable).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("indestructible ignores 'destroy'") {
                    game.findPermanent("Test Unkillable") shouldBe unkillable
                    game.isInGraveyard(2, "Test Unkillable") shouldBe false
                }
            }
        }
    }
}
