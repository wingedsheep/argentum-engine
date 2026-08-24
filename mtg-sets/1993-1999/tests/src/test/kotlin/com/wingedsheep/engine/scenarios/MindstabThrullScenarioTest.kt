package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Mindstab Thrull (Fallen Empires).
 *
 * Oracle: "Whenever this creature attacks and isn't blocked, you may sacrifice it. If you do,
 * defending player discards three cards."
 *
 * The trap the tests pin: the sacrifice is the first half of the effect, so by the time the
 * discard runs the source is off the battlefield and "defending player" can no longer be read
 * off its `AttackingComponent`. It must still be the *defending* player who discards.
 */
class MindstabThrullScenarioTest : ScenarioTestBase() {

    init {
        context("Mindstab Thrull — attacks unblocked, sacrifice for three discards") {

            test("the defending player discards three, not the attacker") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mindstab Thrull", summoningSickness = false)
                    .withCardsInHand(1, "Swamp", 4)
                    .withCardsInHand(2, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mindstab Thrull" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // The trigger has no targets, so its "you may" question is asked at resolution.
                game.resolveStack()
                game.answerYesNo(true)

                withClue("the defending player chooses which of their own cards to discard") {
                    game.state.pendingDecision?.playerId shouldBe game.player2Id
                }
                game.selectCards(game.findCardsInHand(2, "Plains").take(3))
                game.resolveStack()

                withClue("the Thrull paid for the discard with its own life") {
                    game.isOnBattlefield("Mindstab Thrull") shouldBe false
                }
                withClue("defending player discards three: 5 -> 2") {
                    game.handSize(2) shouldBe 2
                }
                withClue("the attacking player discards nothing") {
                    game.handSize(1) shouldBe 4
                }
            }

            test("declining keeps the Thrull and leaves both hands alone") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Mindstab Thrull", summoningSickness = false)
                    .withCardsInHand(1, "Swamp", 4)
                    .withCardsInHand(2, "Plains", 5)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Mindstab Thrull" to 2))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                game.resolveStack()
                game.answerYesNo(false)

                withClue("a declined sacrifice leaves the attacker on the battlefield") {
                    game.isOnBattlefield("Mindstab Thrull") shouldBe true
                }
                game.handSize(2) shouldBe 5
                game.handSize(1) shouldBe 4
            }
        }
    }
}
