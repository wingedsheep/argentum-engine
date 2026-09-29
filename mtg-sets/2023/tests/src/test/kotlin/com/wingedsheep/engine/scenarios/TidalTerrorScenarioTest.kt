package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tidal Terror (March of the Machine #82) — "Whenever this creature attacks, you may tap two other
 * untapped creatures you control. If you do, this creature can't be blocked this turn."
 */
class TidalTerrorScenarioTest : ScenarioTestBase() {

    private fun attackWithTerror(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Tidal Terror")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Wall of Granite")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Tidal Terror" to 2)).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Tidal Terror") {

            test("tapping two other creatures makes it unblockable this turn") {
                val game = attackWithTerror()
                game.answerYesNo(true)
                val bears = game.findPermanents("Grizzly Bears")
                game.selectCards(bears).error shouldBe null
                game.resolveStack()

                withClue("both Grizzly Bears were tapped to pay") {
                    bears.forEach { game.state.getEntity(it)?.has<TappedComponent>() shouldBe true }
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                withClue("Wall of Granite can't block the unblockable Terror") {
                    game.declareBlockers(mapOf("Wall of Granite" to listOf("Tidal Terror"))).error shouldNotBe null
                }
            }

            test("declining leaves the other creatures untapped and the Terror blockable") {
                val game = attackWithTerror()
                game.answerYesNo(false)
                game.resolveStack()

                withClue("no Grizzly Bears were tapped") {
                    game.findPermanents("Grizzly Bears").forEach {
                        game.state.getEntity(it)?.has<TappedComponent>() shouldBe false
                    }
                }

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Granite" to listOf("Tidal Terror"))).error shouldBe null
            }
        }
    }
}
