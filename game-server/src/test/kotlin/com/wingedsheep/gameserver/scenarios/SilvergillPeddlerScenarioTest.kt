package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Silvergill Peddler.
 *
 * Card reference:
 * - Silvergill Peddler ({2}{U}): Creature — Merfolk Citizen, 2/3
 *   "Whenever this creature becomes tapped, draw a card, then discard a card."
 *
 * Also covers the engine fix that made `AttackPhaseManager` emit `TappedEvent`
 * when declaring attackers (so self-tap "becomes tapped" triggers fire on attack).
 */
class SilvergillPeddlerScenarioTest : ScenarioTestBase() {

    init {
        context("Silvergill Peddler becomes-tapped trigger") {

            test("attacking taps the Peddler and triggers draw-then-discard") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Silvergill Peddler")
                    .withCardInHand(1, "Island")           // a pre-existing hand card
                    .withCardInLibrary(1, "Mountain")      // what will be drawn
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val initialHandSize = game.handSize(1)
                withClue("Starts with exactly one card in hand") { initialHandSize shouldBe 1 }

                // Declare Peddler as attacker — this should tap it and fire the trigger.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Silvergill Peddler" to 2))

                // After attack declaration, a trigger should be on the stack.
                // Pass priority until the discard selection decision appears.
                // (The draw happens automatically during trigger resolution; the
                // discard is a pending SelectCardsDecision.)
                var iterations = 0
                while (!game.hasPendingDecision() && iterations < 50) {
                    val p = game.state.priorityPlayerId ?: break
                    game.execute(PassPriority(p))
                    iterations++
                }

                withClue("Trigger should pause for discard choice") {
                    game.hasPendingDecision() shouldBe true
                }
                val decision = game.getPendingDecision()
                withClue("Pending decision is a card-selection (discard)") {
                    (decision is SelectCardsDecision) shouldBe true
                }

                // At this point we've drawn — hand size is initial + 1 (2 cards total)
                // before the discard reduces it back.
                withClue("Drew a card before the discard decision") {
                    game.handSize(1) shouldBe initialHandSize + 1
                }

                // Discard the Island we started with; keep the freshly drawn Mountain.
                val islandInHand = game.findCardsInHand(1, "Island")
                withClue("Island still in hand when discard decision is presented") {
                    islandInHand.size shouldBe 1
                }
                game.selectCards(islandInHand)

                // After the discard, hand should be back to 1 card, which is the Mountain.
                withClue("Hand returns to size 1 after discard") {
                    game.handSize(1) shouldBe initialHandSize
                }
                withClue("The Island was discarded (Mountain remains)") {
                    game.isInHand(1, "Mountain") shouldBe true
                    game.isInHand(1, "Island") shouldBe false
                }

                // Finish combat and verify the Peddler hit for 2.
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Defender took 2 combat damage") {
                    game.getLifeTotal(2) shouldBe 18
                }
            }
        }
    }
}
