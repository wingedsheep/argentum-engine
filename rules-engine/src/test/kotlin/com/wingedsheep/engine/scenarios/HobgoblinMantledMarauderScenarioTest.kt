package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Hobgoblin, Mantled Marauder (SPM #80).
 *   {1}{R} Legendary Creature — Goblin Human Villain, 1/2
 *   Flying, haste
 *   Whenever you discard a card, Hobgoblin gets +2/+0 until end of turn.
 *
 * Discards are driven by casting Careful Study ({B}, "Draw a card, then discard a card"),
 * whose discard emits the DiscardEvent that fires Hobgoblin's trigger. Each discard is a
 * separate event, so two casts stack two +2/+0 pumps.
 */
class HobgoblinMantledMarauderScenarioTest : ScenarioTestBase() {

    private val projector = StateProjector()
    private val p1 = EntityId.of("player-1")

    private fun discardCardIn(state: com.wingedsheep.engine.state.GameState): EntityId =
        state.getHand(p1).first { id ->
            state.getEntity(id)?.get<CardComponent>()?.name != "Careful Study"
        }

    init {
        context("Hobgoblin, Mantled Marauder") {
            test("has flying and haste") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hobgoblin, Mantled Marauder")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hob = game.findPermanent("Hobgoblin, Mantled Marauder")!!
                val p = projector.project(game.state)
                withClue("base 1/2") {
                    p.getPower(hob) shouldBe 1
                    p.getToughness(hob) shouldBe 2
                }
                p.hasKeyword(hob, Keyword.FLYING) shouldBe true
                p.hasKeyword(hob, Keyword.HASTE) shouldBe true
            }

            test("discarding a card pumps Hobgoblin +2/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hobgoblin, Mantled Marauder")
                    .withCardInHand(1, "Careful Study")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hob = game.findPermanent("Hobgoblin, Mantled Marauder")!!
                withClue("base power 1") { projector.project(game.state).getPower(hob) shouldBe 1 }

                val cast = game.castSpell(1, "Careful Study")
                withClue("casting Careful Study should succeed: ${cast.error}") { cast.error shouldBe null }
                game.resolveStack()

                // Careful Study draws, then pauses to choose a discard.
                withClue("paused to choose a card to discard") { game.hasPendingDecision() shouldBe true }
                game.selectCards(listOf(discardCardIn(game.state)))
                game.resolveStack()

                withClue("discard trigger pumps Hobgoblin to 3/2") {
                    val p = projector.project(game.state)
                    p.getPower(hob) shouldBe 3
                    p.getToughness(hob) shouldBe 2
                }
            }

            test("multiple discards stack the +2/+0 bonus") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Hobgoblin, Mantled Marauder")
                    .withCardInHand(1, "Careful Study")
                    .withCardInHand(1, "Careful Study")
                    .withCardInHand(1, "Mountain")
                    .withCardInHand(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Swamp", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val hob = game.findPermanent("Hobgoblin, Mantled Marauder")!!

                repeat(2) {
                    val cast = game.castSpell(1, "Careful Study")
                    withClue("casting Careful Study #$it should succeed: ${cast.error}") { cast.error shouldBe null }
                    game.resolveStack()
                    withClue("paused to choose a card to discard") { game.hasPendingDecision() shouldBe true }
                    game.selectCards(listOf(discardCardIn(game.state)))
                    game.resolveStack()
                }

                withClue("two discards stack: 1 + 2 + 2 = 5 power") {
                    val p = projector.project(game.state)
                    p.getPower(hob) shouldBe 5
                    p.getToughness(hob) shouldBe 2
                }
            }
        }
    }
}
