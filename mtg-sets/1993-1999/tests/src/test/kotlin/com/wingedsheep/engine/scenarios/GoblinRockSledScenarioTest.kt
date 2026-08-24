package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Goblin Rock Sled — and, through it, the "attacked during your last turn"
 * record the untap clause needs.
 *
 * The behaviour is a cycle, so the test walks it: attack, and the Sled is still tapped on the next
 * untap step; let a turn pass without attacking, and it untaps normally. A card that used the bare
 * DOESNT_UNTAP flag would pass the first half and fail the second — it would never untap again.
 */
class GoblinRockSledScenarioTest : ScenarioTestBase() {

    init {
        context("Goblin Rock Sled — the conditional untap") {

            test("stays tapped on the untap step after a turn it attacked") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Rock Sled", summoningSickness = false)
                    // The defender needs a Mountain or it can't attack at all.
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sled = game.findPermanent("Goblin Rock Sled")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Rock Sled" to 2)).error shouldBe null
                withClue("attacking taps it") { game.state.getEntity(sled)?.has<TappedComponent>() shouldBe true }

                // Through the opponent's turn and back past my own untap step. Both hops are
                // asserted: if navigation stalled, "still tapped" would pass for the wrong reason.
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                // Step off the upkeep before asking for it again — passUntilPhase is a no-op when
                // the game is already at the requested phase/step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id

                withClue("it attacked during my last turn, so my untap step skipped it") {
                    game.state.getEntity(sled)?.has<TappedComponent>() shouldBe true
                }
            }

            test("untaps normally after a turn it did not attack") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Rock Sled", summoningSickness = false, tapped = true)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val sled = game.findPermanent("Goblin Rock Sled")!!

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                // Step off the upkeep before asking for it again — passUntilPhase is a no-op when
                // the game is already at the requested phase/step.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player1Id

                withClue("no attack last turn, so the clause doesn't hold and it untaps") {
                    game.state.getEntity(sled)?.has<TappedComponent>() shouldBe false
                }
            }
        }

        context("Goblin Rock Sled — the attack restriction") {

            test("can't attack a defender with no Mountain") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Goblin Rock Sled", summoningSickness = false)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Goblin Rock Sled" to 2)).error shouldNotBe null
            }
        }
    }
}
