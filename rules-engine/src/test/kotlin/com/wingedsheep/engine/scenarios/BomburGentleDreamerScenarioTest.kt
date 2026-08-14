package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bombur, Gentle Dreamer — "Bombur doesn't untap during your untap step unless you have an enduring
 * story."
 *
 * The inverted storied gate: every other storied card grants something *while* you have the enduring
 * story, Bombur takes a restriction away. That makes him the only card wiring
 * `Not(YouHaveEnduringStory)` into a `ConditionalStaticAbility`, and the reason this file exists — a
 * gate that silently evaluated the wrong way would leave the card either permanently stuck or never
 * stuck, and both look plausible on the battlefield.
 *
 * The threshold rules themselves are pinned in [StoriedEnduringStoryTest]; here Bombur is one of the
 * three qualifying permanents himself (he's legendary), so the "off" board is Bombur plus lands and
 * the "on" board is Bombur plus two more legendaries.
 */
class BomburGentleDreamerScenarioTest : ScenarioTestBase() {

    init {

        test("without an enduring story Bombur stays tapped through his controller's untap step") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bombur, Gentle Dreamer", tapped = true)
                // Lands are neither artifacts, legendaries, nor Sagas — Bombur is the only one of
                // the three, so the threshold stays unmet.
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardsInHand(1, "Mountain", 3)
                .withCardsInHand(2, "Mountain", 3)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bombur = game.findPermanent("Bombur, Gentle Dreamer")!!

            withClue("no enduring story, so the printed restriction is live") {
                EnduringStoryService.has(game.state, game.player1Id) shouldBe false
                game.state.projectedState.hasKeyword(bombur, AbilityFlag.DOESNT_UNTAP) shouldBe true
            }

            // Round the turn cycle back to Player 1's next upkeep — their untap step runs on the
            // boundary in between, and the untap step itself has no priority window to stop in.
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Player 2's upkeep
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN) // Player 2's main
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP) // Player 1's next upkeep
            game.state.activePlayerId shouldBe game.player1Id

            withClue("Bombur was skipped by the untap step") {
                game.state.getEntity(bombur)?.has<TappedComponent>() shouldBe true
            }
        }

        test("with an enduring story Bombur untaps normally") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bombur, Gentle Dreamer", tapped = true)
                // Two more legendaries put Bombur's controller over the storied threshold of three.
                .withCardOnBattlefield(1, "Ori, Keeper of Songs")
                .withCardOnBattlefield(1, "Thorin Oakenshield")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardsInHand(1, "Mountain", 3)
                .withCardsInHand(2, "Mountain", 3)
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withCardInLibrary(2, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bombur = game.findPermanent("Bombur, Gentle Dreamer")!!

            withClue("three qualifying permanents, so the restriction is switched off") {
                EnduringStoryService.has(game.state, game.player1Id) shouldBe true
                game.state.projectedState.hasKeyword(bombur, AbilityFlag.DOESNT_UNTAP) shouldBe false
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            game.state.activePlayerId shouldBe game.player1Id

            withClue("Bombur untapped with everything else") {
                game.state.getEntity(bombur)?.has<TappedComponent>() shouldBe false
            }
        }

        test("the restriction is the narrow untap-step one, not can't-become-untapped") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Bombur, Gentle Dreamer", tapped = true)
                .withLandsOnBattlefield(1, "Mountain", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bombur = game.findPermanent("Bombur, Gentle Dreamer")!!

            // The two flags are trivially easy to swap and resolve differently against any untap
            // *effect* — a Twiddle still untaps Bombur even while the storied gate is unmet.
            withClue("DOESNT_UNTAP only, so untap effects still work on him") {
                game.state.projectedState.hasKeyword(bombur, AbilityFlag.DOESNT_UNTAP) shouldBe true
                game.state.projectedState
                    .hasKeyword(bombur, AbilityFlag.CANT_BECOME_UNTAPPED) shouldBe false
            }
        }
    }
}
