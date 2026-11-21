package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dgm.cards.KrasisIncubation
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Krasis Incubation (Dragon's Maze).
 *
 * Oracle: "Enchant creature / Enchanted creature can't attack or block, and its activated
 * abilities can't be activated. / {1}{G}{U}, Return this Aura to its owner's hand: Put two +1/+1
 * counters on enchanted creature."
 *
 * The interesting half is the self-bounce cost: the Aura is in its owner's hand before the
 * ability resolves, so "enchanted creature" is read as last-known information (CR 608.2h).
 */
class KrasisIncubationScenarioTest : ScenarioTestBase() {

    private val abilityId = KrasisIncubation.activatedAbilities.first().id

    init {
        context("Krasis Incubation — bounce the Aura to grow the creature it enchanted") {

            test("returning the Aura puts two +1/+1 counters on the creature it was enchanting") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Krasis Incubation", "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val incubation = game.findPermanent("Krasis Incubation")!!
                game.execute(
                    ActivateAbility(playerId = game.player1Id, sourceId = incubation, abilityId = abilityId)
                ).error shouldBe null

                withClue("the Aura returned to hand as part of the cost") {
                    game.isOnBattlefield("Krasis Incubation") shouldBe false
                    game.isInHand(1, "Krasis Incubation") shouldBe true
                }

                game.resolveStack()

                val bears = game.findPermanent("Grizzly Bears")!!
                withClue("the former host got two +1/+1 counters") {
                    game.state.projectedState.getPower(bears) shouldBe 4
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
            }
        }
    }
}
