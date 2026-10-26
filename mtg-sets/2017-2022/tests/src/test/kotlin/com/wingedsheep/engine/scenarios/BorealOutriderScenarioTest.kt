package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Boreal Outrider (KHM #163).
 *
 *   Whenever you cast a creature spell, if {S} of any of that spell's colors was spent to cast it,
 *   that creature enters with an additional +1/+1 counter on it.
 *
 * Snow mana is mana from a snow source (CR 107.4h), and it counts on any part of the cost — a snow
 * {G} paying Grizzly Bears' generic pip qualifies. Snow mana of a color the spell doesn't have does
 * not, nor does plain mana of the spell's color.
 */
class BorealOutriderScenarioTest : ScenarioTestBase() {

    private fun TestGame.plusCounters(name: String): Int =
        state.getEntity(findPermanent(name)!!)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun outriderBoard(vararg lands: String): TestGame {
        val builder = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Boreal Outrider")
            .withCardInHand(1, "Grizzly Bears")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        lands.forEach { builder.withLandsOnBattlefield(1, it, 1) }
        return builder.build()
    }

    private fun TestGame.castBears() {
        castSpell(1, "Grizzly Bears").error shouldBe null
        resolveStack()
    }

    init {
        context("Boreal Outrider") {

            test("snow mana of the spell's color puts a +1/+1 counter on the creature") {
                val game = outriderBoard("Snow-Covered Forest", "Forest")
                game.castBears()
                withClue("a snow {G} was spent on Grizzly Bears") { game.plusCounters("Grizzly Bears") shouldBe 1 }
            }

            test("snow mana floated before casting counts") {
                val game = outriderBoard("Snow-Covered Forest", "Forest")
                val snowForest = game.findPermanent("Snow-Covered Forest")!!
                val manaAbility = cardRegistry.getCard("Snow-Covered Forest")!!.activatedAbilities.first().id
                game.execute(ActivateAbility(playerId = game.player1Id, sourceId = snowForest, abilityId = manaAbility))
                    .error shouldBe null

                game.castBears()
                withClue("the floated snow {G} paid part of the cost") { game.plusCounters("Grizzly Bears") shouldBe 1 }
            }

            test("snow mana of a color the spell doesn't have does not count") {
                val game = outriderBoard("Snow-Covered Island", "Forest")
                game.castBears()
                withClue("only snow {U} was spent, and Grizzly Bears is green") {
                    game.plusCounters("Grizzly Bears") shouldBe 0
                }
            }

            test("no snow mana, no counter") {
                val game = outriderBoard("Forest", "Forest")
                game.castBears()
                game.plusCounters("Grizzly Bears") shouldBe 0
            }
        }
    }
}
