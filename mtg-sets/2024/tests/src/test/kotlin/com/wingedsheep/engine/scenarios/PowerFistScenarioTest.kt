package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Power Fist (PIP #81) — {1}{G} Artifact — Equipment
 *
 *   Equipped creature has trample and "Whenever this creature deals combat damage to a player,
 *   put that many +1/+1 counters on it."
 *   Equip {2}
 *
 * The granted trigger's "it" is the equipped creature, so the counters must land on the creature
 * (not on the Equipment), and the count is the damage dealt to the player.
 */
class PowerFistScenarioTest : ScenarioTestBase() {

    private fun plusCounters(game: TestGame, name: String): Int {
        val id = game.findPermanent(name)!!
        return game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
    }

    private fun advanceThroughCombatDamage(game: TestGame) {
        game.passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var iterations = 0
        while (game.state.pendingDecision == null && game.state.stack.isNotEmpty() && iterations++ < 20) {
            game.passPriority()
        }
    }

    init {
        context("Power Fist") {

            test("equipped creature has trample") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Power Fist", "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.TRAMPLE) shouldBe true
            }

            test("combat damage to a player puts that many +1/+1 counters on the creature, not the Equipment") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Power Fist", "Grizzly Bears")
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                advanceThroughCombatDamage(game)

                withClue("the 2/2 connects for 2") {
                    game.getLifeTotal(2) shouldBe 18
                }
                withClue("two +1/+1 counters on the equipped creature") {
                    plusCounters(game, "Grizzly Bears") shouldBe 2
                }
                withClue("none on the Equipment") {
                    plusCounters(game, "Power Fist") shouldBe 0
                }
                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.getPower(bears) shouldBe 4
            }

            test("combat damage only to a blocking creature adds no counters") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Power Fist", "Grizzly Bears")
                    .withCardOnBattlefield(2, "Wall of Wood", summoningSickness = false)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Wall of Wood" to listOf("Grizzly Bears"))).error shouldBe null
                advanceThroughCombatDamage(game)

                game.getLifeTotal(2) shouldBe 20
                plusCounters(game, "Grizzly Bears") shouldBe 0
            }
        }
    }
}
