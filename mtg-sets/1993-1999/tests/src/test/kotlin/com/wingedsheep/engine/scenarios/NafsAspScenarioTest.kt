package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.support.answerForcedPayments
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Nafs Asp's deferred-bite delayed trigger.
 *
 * Oracle: "Whenever this creature deals damage to a player, that player loses 1 life
 * at the beginning of their next draw step unless they pay {1} before that draw step."
 *
 * Covers the new [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.fireOnPlayer]
 * axis end-to-end: the damaged player is captured at schedule time, the delayed trigger
 * is gated to fire only on *that* player's draw step (not Nafs Asp's controller's), and
 * the pay-or-suffer decision is presented to the damaged player.
 */
class NafsAspScenarioTest : ScenarioTestBase() {

    init {
        context("Nafs Asp deferred bite") {

            test("damaged player auto-loses 1 life on their next draw step when they can't pay {1}") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    .withActivePlayer(1)
                // Give both players a few library cards so the turn-based draw at their draw
                // step doesn't bottom them out into a state-based loss before the bite lands.
                repeat(5) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val startLife = game.getLifeTotal(2)

                // Combat damage: Nafs Asp swings unblocked into Player2.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Nafs Asp" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                withClue("Combat damage itself drops life by 1 (and only 1)") {
                    game.getLifeTotal(2) shouldBe startLife - 1
                }

                withClue("Damage trigger should have scheduled a delayed bite on P2's next draw step") {
                    game.state.delayedTriggers.size shouldBe 1
                    game.state.delayedTriggers[0].fireAtStep shouldBe Step.DRAW
                    game.state.delayedTriggers[0].fireOnPlayerId shouldBe game.player2Id
                }

                val lifeAfterCombat = game.getLifeTotal(2)
                val p1LifeBefore = game.getLifeTotal(1)

                // End Player1's turn, advance through cleanup to Player2's draw step.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()
                game.answerForcedPayments()
                game.resolveStack()

                withClue("Player1's life must not move — target is the damaged player, not the controller") {
                    game.getLifeTotal(1) shouldBe p1LifeBefore
                }
                withClue("Player2 has no mana sources, so the delayed trigger's pay-or-suffer can only suffer") {
                    game.getLifeTotal(2) shouldBe lifeAfterCombat - 1
                }
                withClue("The delayed trigger fires only once") {
                    game.state.delayedTriggers.size shouldBe 0
                }
            }

            test("delayed bite waits for the damaged player's draw step, not their upkeep") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    .withActivePlayer(1)
                repeat(5) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Nafs Asp" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                val lifeAfterCombat = game.getLifeTotal(2)

                // Stop at P2's UPKEEP — strictly one step before DRAW. The bite must not
                // land yet (fireAtStep is DRAW, not UPKEEP).
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()

                withClue("Trigger is scheduled for P2's DRAW, not their UPKEEP") {
                    game.getLifeTotal(2) shouldBe lifeAfterCombat
                    game.state.delayedTriggers.size shouldBe 1
                }

                // Advance one more step to DRAW — trigger fires and the forced refusal suffers.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()
                game.answerForcedPayments()
                game.resolveStack()

                withClue("Bite lands on P2's DRAW step") {
                    game.getLifeTotal(2) shouldBe lifeAfterCombat - 1
                    game.state.delayedTriggers.size shouldBe 0
                }
            }

            test("damaged player loses 1 life when they CAN pay but decline (regression: trigger context survives the YesNo continuation)") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    // Untapped Forest on P2 so ManaSolver.canPay({1}) returns true and the
                    // YesNoDecision is actually presented (rather than the auto-suffer shortcut).
                    .withCardOnBattlefield(2, "Forest")
                    .withActivePlayer(1)
                repeat(5) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Nafs Asp" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                val lifeAfterCombat = game.getLifeTotal(2)

                // Advance into P2's draw step. The trigger resolves and presents a YesNo
                // decision to P2 ("Pay {1} or accept consequence?"). resolveStack stops on
                // the pending decision rather than auto-answering.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()

                withClue("Trigger resolved → P2 sees the pay-or-suffer decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // P2 declines to pay {1}. The suffer (LoseLife on the triggering player)
                // must fire — regression for the bug where the continuation rebuilt
                // EffectContext without triggeringPlayerId, fizzling the LoseLife.
                game.answerYesNo(false)
                game.resolveStack()

                withClue("P2 declined to pay {1}, so the suffer's LoseLife on the triggering player resolves and drops life by 1") {
                    game.getLifeTotal(2) shouldBe lifeAfterCombat - 1
                }
                withClue("Trigger fully consumed; no further bite scheduled") {
                    game.state.delayedTriggers.size shouldBe 0
                }
            }

            test("damaging a player twice schedules two bites that both resolve on their next draw step (ruling: pay or suffer twice)") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    .withActivePlayer(1)
                repeat(5) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val startLife = game.getLifeTotal(2)

                // Two Nafs Asps swing unblocked into Player2: two independent combat-damage
                // events, each triggering its own deferred bite. declareAttackers() keys by
                // card name and so can't declare two same-named attackers — build the action
                // directly from both entity ids.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                val asps = game.findAllPermanents("Nafs Asp")
                asps.size shouldBe 2
                game.execute(
                    DeclareAttackers(game.player1Id, asps.associateWith { game.player2Id })
                ).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                game.resolveStack()

                val lifeAfterCombat = game.getLifeTotal(2)

                withClue("Both attackers connect: combat itself drops P2 by 2") {
                    game.getLifeTotal(2) shouldBe startLife - 2
                }
                withClue("Each damage event schedules its own deferred bite on P2's draw step") {
                    game.state.delayedTriggers.size shouldBe 2
                    game.state.delayedTriggers.all {
                        it.fireAtStep == Step.DRAW && it.fireOnPlayerId == game.player2Id
                    } shouldBe true
                }

                // Advance to P2's draw step: both bites fire and, with no mana to pay {1},
                // both can only suffer — P2 loses 1 life per bite.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()
                game.answerForcedPayments()
                game.resolveStack()
                game.answerForcedPayments()
                game.resolveStack()

                withClue("Both deferred bites resolve at the draw step → P2 loses 2 more life") {
                    game.getLifeTotal(2) shouldBe lifeAfterCombat - 2
                }
                withClue("Both delayed triggers are consumed") {
                    game.state.delayedTriggers.size shouldBe 0
                }
            }

            test("no delayed trigger when Nafs Asp deals no damage to a player this turn") {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nafs Asp")
                    .withActivePlayer(1)
                // Give both players a few library cards so the turn-based draw at their draw
                // step doesn't bottom them out into a state-based loss before the bite lands.
                repeat(5) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Forest")
                }
                val game = builder.build()

                val startLifeP2 = game.getLifeTotal(2)

                // Don't attack — pass straight through to P2's draw step.
                game.passUntilPhase(Phase.BEGINNING, Step.DRAW)
                game.resolveStack()

                withClue("Nafs Asp dealt no damage to a player, so no delayed bite is scheduled") {
                    game.getLifeTotal(2) shouldBe startLifeP2
                }
            }
        }
    }
}
