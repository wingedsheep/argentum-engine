package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Howling Moon (VOW #204).
 *
 *   At the beginning of combat on your turn, target Wolf or Werewolf you control gets +2/+2 until
 *   end of turn.
 *   Whenever an opponent casts their second spell each turn, create a 2/2 green Wolf creature token.
 *
 * Exercises the begin-combat targeted +2/+2 (restricted to a Wolf or Werewolf you control) and the
 * per-opponent second-spell Wolf-token trigger.
 */
class HowlingMoonScenarioTest : ScenarioTestBase() {

    init {
        context("Howling Moon") {

            test("begin combat pumps a target Wolf you control +2/+2") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Howling Moon")
                    .withCardOnBattlefield(1, "Runebound Wolf", summoningSickness = false) // a 2/2 Wolf
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val wolf = game.findPermanent("Runebound Wolf")!!

                // passUntilPhase (not advanceToPhase) so entering begin combat fires the trigger.
                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                if (game.getPendingDecision() is ChooseTargetsDecision) game.selectTargets(listOf(wolf))
                game.resolveStack()

                withClue("the targeted Wolf gets +2/+2 (2/2 -> 4/4)") {
                    game.state.projectedState.getPower(wolf) shouldBe 4
                    game.state.projectedState.getToughness(wolf) shouldBe 4
                }
            }

            test("an opponent's second spell each turn creates a 2/2 green Wolf token") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Howling Moon")
                    .withLandsOnBattlefield(2, "Mountain", 4)
                    .withCardsInHand(2, "Lightning Bolt", 2)
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // First opponent spell — no token.
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                game.resolveStack()
                withClue("no token after the opponent's first spell") {
                    game.findPermanents("Wolf Token").size shouldBe 0
                }

                // Second opponent spell — the trigger fires, making a Wolf under Howling Moon's controller.
                game.castSpellTargetingPlayer(2, "Lightning Bolt", 1)
                game.resolveStack()
                withClue("the opponent's second spell creates one Wolf token") {
                    val wolves = game.findPermanents("Wolf Token")
                    wolves.size shouldBe 1
                    game.state.getEntity(wolves.first())!!
                        .get<com.wingedsheep.engine.state.components.identity.ControllerComponent>()!!
                        .playerId shouldBe game.player1Id
                }
            }
        }
    }
}
