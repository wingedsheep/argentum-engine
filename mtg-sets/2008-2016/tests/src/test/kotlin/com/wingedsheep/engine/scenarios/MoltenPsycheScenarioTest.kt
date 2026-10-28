package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Molten Psyche — "Each player shuffles the cards from their hand into their library, then draws
 * that many cards. Metalcraft — If you control three or more artifacts, Molten Psyche deals damage
 * to each opponent equal to the number of cards that player has drawn this turn."
 *
 * The metalcraft count is read after the wheel, so it includes the cards the opponent just drew
 * plus every earlier draw that turn (ruling); without metalcraft there is no damage.
 */
class MoltenPsycheScenarioTest : ScenarioTestBase() {

    private val trinket = card("Molten Psyche Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
    }

    private fun TestGame.castAndResolve() {
        castSpell(1, "Molten Psyche").error shouldBe null
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        resolveStack()
    }

    init {
        cardRegistry.register(trinket)

        context("Molten Psyche") {

            test("with metalcraft, each opponent takes damage equal to all cards they drew this turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Molten Psyche")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Molten Psyche Test Trinket")
                    .withCardOnBattlefield(1, "Molten Psyche Test Trinket")
                    .withCardOnBattlefield(1, "Molten Psyche Test Trinket")
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withCardsDrawnThisTurn(2, 1)
                    .withLifeTotal(1, 20)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAndResolve()

                withClue("each player redrew as many cards as they shuffled away") {
                    game.handSize(1) shouldBe 1
                    game.handSize(2) shouldBe 2
                }
                withClue("opponent drew 1 earlier this turn + 2 from the wheel = 3 damage") {
                    game.state.lifeTotal(game.player2Id) shouldBe 17
                }
                withClue("the caster is not an opponent and takes no damage") {
                    game.state.lifeTotal(game.player1Id) shouldBe 20
                }
            }

            test("without metalcraft only the wheel happens") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Molten Psyche")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardOnBattlefield(1, "Molten Psyche Test Trinket")
                    .withCardOnBattlefield(1, "Molten Psyche Test Trinket")
                    .withCardsInHand(2, "Grizzly Bears", 2)
                    .withLifeTotal(2, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castAndResolve()

                game.handSize(2) shouldBe 2
                withClue("two artifacts is not metalcraft — no damage") {
                    game.state.lifeTotal(game.player2Id) shouldBe 20
                }
            }
        }
    }
}
