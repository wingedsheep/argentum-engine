package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder

/**
 * Rhovanion Rampager (HOB #82) — {2}{B} Creature — Wolf 3/2.
 *
 * "Whenever this creature attacks, you may sacrifice another creature. If you do, put a number
 *  of +1/+1 counters on this creature equal to the sacrificed creature's power.
 *  When this creature dies, amass Goblins X, where X is this creature's power."
 *
 * Both halves read power off last-known information: the sacrificed creature is already in the
 * graveyard when the counters are placed, and the Rampager itself is gone when the dies trigger
 * resolves. The "you may" is a resolution-time choice, so declining must place no counters.
 */
class RhovanionRampagerScenarioTest : ScenarioTestBase() {

    init {
        context("Rhovanion Rampager") {

            fun ScenarioTestBase.TestGame.armyPower(): Int? =
                state.getBattlefield()
                    .firstOrNull { state.projectedState.isCreature(it) && state.projectedState.hasSubtype(it, "Army") }
                    ?.let { state.projectedState.getPower(it) }

            test("sacrificing another creature on attack adds counters equal to its power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rhovanion Rampager")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rampager = game.findPermanent("Rhovanion Rampager")!!
                val courser = game.findPermanent("Centaur Courser")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Rhovanion Rampager" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                withClue("the offer is 'another creature' — the Rampager itself is not on the list") {
                    (decision is SelectCardsDecision) shouldBe true
                    (decision as SelectCardsDecision).options shouldContainExactlyInAnyOrder listOf(courser)
                }
                game.selectCards(listOf(courser)).error shouldBe null
                game.resolveStack()

                withClue("the 3/3 Courser was sacrificed for three +1/+1 counters") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe true
                    game.state.projectedState.getPower(rampager) shouldBe 6
                    game.state.projectedState.getToughness(rampager) shouldBe 5
                }
            }

            test("declining the sacrifice places no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rhovanion Rampager")
                    .withCardOnBattlefield(1, "Centaur Courser")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rampager = game.findPermanent("Rhovanion Rampager")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Rhovanion Rampager" to 2)).error shouldBe null
                game.resolveStack()
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("'If you do' never fired — nothing was sacrificed and nothing grew") {
                    game.isInGraveyard(1, "Centaur Courser") shouldBe false
                    game.state.projectedState.getPower(rampager) shouldBe 3
                    game.state.projectedState.getToughness(rampager) shouldBe 2
                }
            }

            test("dying amasses Goblins equal to its last-known power") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Rhovanion Rampager")
                    .withCardInHand(2, "Lightning Bolt")
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val rampager = game.findPermanent("Rhovanion Rampager")!!
                game.castSpell(2, "Lightning Bolt", rampager)
                game.resolveStack()
                game.checkStateBasedActions()
                game.resolveStack()

                withClue("a 3-power Wolf died, so the Army carries three +1/+1 counters") {
                    game.isInGraveyard(1, "Rhovanion Rampager") shouldBe true
                    game.armyPower() shouldBe 3
                }
            }
        }
    }
}
