package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Sage's Reverie (FRF #23) — {3}{W} Enchantment — Aura.
 *
 * "Enchant creature
 *  When this Aura enters, draw a card for each Aura you control that's attached to a creature.
 *  Enchanted creature gets +1/+1 for each Aura you control that's attached to a creature."
 *
 * Counts only Auras *you control* whose host is a *creature* — including one you control on an
 * opponent's creature, and the Reverie itself — but not an opponent's Aura or your Aura on a land.
 * The opponent's Aura has a different name from yours because `withCardAttachedTo` looks the Aura
 * up by name.
 */
class SagesReverieScenarioTest : ScenarioTestBase() {

    init {
        context("Sage's Reverie") {

            test("the enters trigger draws a card for each Aura you control attached to a creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Sage's Reverie")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Holy Strength", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Hill Giant")
                    // Your Pacifism on the opponent's creature counts…
                    .withCardAttachedTo(1, "Pacifism", "Hill Giant")
                    // …the opponent's Aura on their own creature does not…
                    .withCardOnBattlefield(2, "Savannah Lions")
                    .withCardAttachedTo(2, "Spectral Flight", "Savannah Lions")
                    // …and neither does your Aura on a land.
                    .withCardOnBattlefield(1, "Forest")
                    .withCardAttachedTo(1, "Evil Presence", "Forest")
                    .withLandsOnBattlefield(1, "Plains", 4)
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                val handBefore = game.handSize(1)

                game.castSpell(1, "Sage's Reverie", bears).error shouldBe null
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                val reverie = game.findPermanent("Sage's Reverie")!!
                withClue("Sage's Reverie is attached to the Bears") {
                    game.state.getEntity(reverie)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("Reverie + Holy Strength + Pacifism = 3 cards drawn (Reverie left the hand)") {
                    game.handSize(1) shouldBe handBefore - 1 + 3
                }
                withClue("Bears 2/2, Holy Strength +1/+2, Reverie +3/+3 → 6/7") {
                    game.state.projectedState.getPower(bears) shouldBe 6
                    game.state.projectedState.getToughness(bears) shouldBe 7
                }
            }

            test("the static bonus counts your Auras on other creatures but not an opponent's") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardAttachedTo(1, "Sage's Reverie", "Grizzly Bears")
                    .withCardOnBattlefield(1, "Savannah Lions")
                    .withCardAttachedTo(1, "Holy Strength", "Savannah Lions")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardAttachedTo(2, "Spectral Flight", "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("Reverie + your Holy Strength on the Lions = +2/+2; the opponent's Aura doesn't count") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
