package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Marsh Viper — "Whenever this creature deals damage to a player, that player
 * gets two poison counters."
 *
 * The failure mode worth pinning is *which* player is poisoned: the effect targets
 * `Player.TriggeringPlayer`, so a wrong player reference would quietly poison the Viper's own
 * controller instead of the one taking the damage, and the game would still look plausible.
 *
 * Not covered: the non-combat half of "deals damage". The card triggers on any damage, not just
 * combat damage (hence `DamageType.Any` in the script), but arranging a non-combat damage source
 * for the Viper itself needs a second card, so this exercises the combat path only.
 */
class MarshViperScenarioTest : ScenarioTestBase() {

    private fun poison(game: TestGame, playerId: EntityId): Int =
        game.state.getEntity(playerId)?.get<CountersComponent>()?.getCount(CounterType.POISON) ?: 0

    init {
        context("Marsh Viper — damage to a player means two poison counters") {

            test("an unblocked attack poisons the defending player, not its controller") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Marsh Viper", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marsh Viper" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(emptyMap()).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("the player who took the damage gets the counters") {
                    poison(game, game.player2Id) shouldBe 2
                }
                withClue("the Viper's controller must not be poisoned by their own snake") {
                    poison(game, game.player1Id) shouldBe 0
                }
            }

            test("a blocked Viper poisons nobody") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Marsh Viper", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                    .withActivePlayer(1)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Marsh Viper" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Marsh Viper"))).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.resolveStack()

                withClue("damage went to the blocker, so no player was dealt damage") {
                    poison(game, game.player2Id) shouldBe 0
                }
            }
        }
    }
}
