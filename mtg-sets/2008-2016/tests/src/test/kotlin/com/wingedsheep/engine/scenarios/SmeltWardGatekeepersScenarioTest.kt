package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Smelt-Ward Gatekeepers (DGM #39) — "When this creature enters, if you control two or more Gates,
 * gain control of target creature an opponent controls until end of turn. Untap that creature. It
 * gains haste until end of turn."
 *
 * Proves the Gate-count intervening-if gates the steal: with two Gates the trigger targets, steals,
 * untaps and hastes the creature, and control reverts at end of turn; with one Gate it never triggers.
 */
class SmeltWardGatekeepersScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()

    init {
        context("Smelt-Ward Gatekeepers") {

            test("with two Gates, steals, untaps and hastes an opponent's creature until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smelt-Ward Gatekeepers")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Boros Guildgate")
                    .withCardOnBattlefield(1, "Izzet Guildgate")
                    .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Smelt-Ward Gatekeepers")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val bear = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bear))
                game.resolveStack()

                val projected = projector.project(game.state)
                withClue("Control of the bear flips to the Gatekeepers' controller") {
                    projected.getController(bear) shouldBe game.player1Id
                }
                withClue("Stolen creature is untapped") {
                    game.state.getEntity(bear)?.has<TappedComponent>() shouldBe false
                }
                withClue("Stolen creature has haste") {
                    projected.hasKeyword(bear, Keyword.HASTE) shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.CLEANUP)
                withClue("Control reverts at end of turn") {
                    projector.project(game.state).getController(bear) shouldBe game.player2Id
                }
            }

            test("with only one Gate, the trigger does nothing") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Smelt-Ward Gatekeepers")
                    .withLandsOnBattlefield(1, "Mountain", 4)
                    .withCardOnBattlefield(1, "Boros Guildgate")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cast = game.castSpell(1, "Smelt-Ward Gatekeepers")
                withClue("Casting should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                withClue("No target prompt without two Gates") {
                    (game.getPendingDecision() is ChooseTargetsDecision) shouldBe false
                }
                val bear = game.findPermanent("Grizzly Bears")!!
                withClue("Opponent keeps the bear") {
                    projector.project(game.state).getController(bear) shouldBe game.player2Id
                }
            }
        }
    }
}
