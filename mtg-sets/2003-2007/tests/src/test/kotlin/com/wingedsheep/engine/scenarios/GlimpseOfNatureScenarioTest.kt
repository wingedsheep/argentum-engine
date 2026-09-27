package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Glimpse of Nature — Champions of Kamigawa #210, {G} Sorcery
 *
 * "Whenever you cast a creature spell this turn, draw a card."
 *
 * Pins that the delayed trigger fires for *every* creature spell (not just the next one), ignores
 * noncreature spells, and expires at end of turn.
 */
class GlimpseOfNatureScenarioTest : ScenarioTestBase() {

    init {
        context("Glimpse of Nature") {

            test("draws a card for each creature spell cast this turn, not for noncreature spells") {
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Glimpse of Nature")
                    .withCardInHand(1, "Glimpse of Nature")
                    .withCardInHand(1, "Llanowar Elves")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Glimpse of Nature").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 3 // Glimpse + 2 Elves

                // First creature spell: one draw.
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 3 // -1 Elves, +1 draw

                // A noncreature spell doesn't trigger it (the second Glimpse sets up its own trigger).
                game.castSpell(1, "Glimpse of Nature").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 2

                // Second creature spell: both Glimpses trigger — two draws.
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe 3 // -1 Elves, +2 draws
                game.isOnBattlefield("Llanowar Elves") shouldBe true
            }

            test("the trigger expires at end of turn") {
                var builder = scenario()
                    .withPlayers("Caster", "Opponent")
                    .withCardInHand(1, "Glimpse of Nature")
                    .withCardInHand(1, "Llanowar Elves")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Forest") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Forest") }
                val game = builder.build()

                game.castSpell(1, "Glimpse of Nature").error shouldBe null
                game.resolveStack()

                // Advance through the opponent's turn to the caster's next main phase.
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                game.state.activePlayerId shouldBe game.player1Id
                val casterHand = game.handSize(1)

                // Last turn's Glimpse has expired: a creature spell now draws nothing.
                game.castSpell(1, "Llanowar Elves").error shouldBe null
                game.resolveStack()
                game.handSize(1) shouldBe casterHand - 1
            }
        }
    }
}
