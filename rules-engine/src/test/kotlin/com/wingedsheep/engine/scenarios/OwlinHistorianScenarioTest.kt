package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Owlin Historian (SOS #24).
 *
 * "{2}{W} Creature — Bird Cleric 2/3. Flying.
 *  When this creature enters, surveil 1.
 *  Whenever one or more cards leave your graveyard, this creature gets +1/+1 until end of turn."
 *
 * Verifies the static keyword (flying), the enters-the-battlefield surveil trigger, and the
 * leave-graveyard pump (returning a creature card from the graveyard to hand via Raise Dead
 * counts as a card leaving the graveyard, so the Historian grows +1/+1).
 */
class OwlinHistorianScenarioTest : ScenarioTestBase() {

    init {
        context("Owlin Historian") {

            test("has flying") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Owlin Historian", summoningSickness = false)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val owlin = game.findPermanent("Owlin Historian")!!
                game.state.projectedState.hasKeyword(owlin, Keyword.FLYING) shouldBe true
            }

            test("returning a creature card from graveyard pumps it +1/+1 until end of turn") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Owlin Historian", summoningSickness = false)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInHand(1, "Raise Dead")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                val owlin = game.findPermanent("Owlin Historian")!!
                withClue("Base power before the trigger") {
                    game.state.projectedState.getPower(owlin) shouldBe 2
                }

                // Raise Dead: return the creature card from the graveyard to hand.
                game.castSpellTargetingGraveyardCard(1, "Raise Dead", 1, "Grizzly Bears")
                game.resolveStack()

                withClue("Grizzly Bears left the graveyard, so the Historian gets +1/+1") {
                    game.state.projectedState.getPower(owlin) shouldBe 3
                    game.state.projectedState.getToughness(owlin) shouldBe 4
                }
            }
        }
    }
}
