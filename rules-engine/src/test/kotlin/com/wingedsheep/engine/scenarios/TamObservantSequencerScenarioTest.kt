package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.battlefield.PreparedComponent
import com.wingedsheep.engine.state.components.battlefield.PreparedSpellCopyComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Tam, Observant Sequencer // Deep Sight (Secrets of Strixhaven #237).
 *
 * Tam, Observant Sequencer ({2}{G}{U}, 4/3, Legendary Gorgon Wizard):
 *   Landfall — Whenever a land you control enters, Tam becomes prepared. (While it's prepared,
 *     you may cast a copy of its spell. Doing so unprepares it.)
 *   //
 *   Deep Sight — {G}{U}, Sorcery: You draw a card and gain 1 life.
 *
 * Tam does NOT enter prepared (no PREPARED keyword) — it only becomes prepared via its landfall
 * trigger (Effects.BecomePrepared). Casting the Deep Sight prepare-spell copy draws a card, gains
 * 1 life, and unprepares Tam.
 */
class TamObservantSequencerScenarioTest : ScenarioTestBase() {

    private fun TestGame.findExileCopy(playerNumber: Int, name: String): EntityId? {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).firstOrNull { id ->
            val e = state.getEntity(id)
            e?.get<CardComponent>()?.name == name && e.get<PreparedSpellCopyComponent>() != null
        }
    }

    init {
        context("Tam, Observant Sequencer — landfall makes it prepared") {

            test("playing a land makes Tam prepared and exposes a Deep Sight copy in exile") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tam, Observant Sequencer", summoningSickness = false)
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val tam = game.findPermanent("Tam, Observant Sequencer")!!
                withClue("Tam must NOT be prepared before a land enters") {
                    game.state.getEntity(tam)?.get<PreparedComponent>() shouldBe null
                }

                val forest = game.findCardsInHand(1, "Forest").first()
                game.execute(PlayLand(game.player1Id, forest))
                game.resolveStack() // landfall -> BecomePrepared

                withClue("Landfall should make Tam prepared") {
                    game.state.getEntity(tam)?.get<PreparedComponent>() shouldNotBe null
                }
                withClue("A Deep Sight prepare-spell copy should now exist in exile") {
                    game.findExileCopy(1, "Tam, Observant Sequencer") shouldNotBe null
                }
            }

            test("casting the Deep Sight copy draws a card, gains 1 life, and unprepares Tam") {
                var builder = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Tam, Observant Sequencer", summoningSickness = false)
                    .withCardInHand(1, "Forest")
                    .withLandsOnBattlefield(1, "Forest", 1)
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Plains") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Plains") }
                val game = builder.build()

                val tam = game.findPermanent("Tam, Observant Sequencer")!!

                val forest = game.findCardsInHand(1, "Forest").first()
                game.execute(PlayLand(game.player1Id, forest))
                game.resolveStack()

                withClue("Tam should be prepared after the landfall trigger") {
                    game.state.getEntity(tam)?.get<PreparedComponent>() shouldNotBe null
                }

                val copyId = game.findExileCopy(1, "Tam, Observant Sequencer")!!
                val handBefore = game.handSize(1)
                val lifeBefore = game.getLifeTotal(1)

                // Deep Sight is a sorcery — we're in our own precombat main with an empty stack.
                game.execute(CastSpell(game.player1Id, copyId, faceIndex = 0))
                game.resolveStack()

                withClue("Deep Sight draws one card") {
                    game.handSize(1) shouldBe handBefore + 1
                }
                withClue("Deep Sight gains 1 life") {
                    game.getLifeTotal(1) shouldBe lifeBefore + 1
                }
                withClue("Tam is no longer prepared after casting the copy") {
                    game.state.getEntity(tam)?.get<PreparedComponent>() shouldBe null
                }
                withClue("The Deep Sight copy should be gone from exile") {
                    game.findExileCopy(1, "Tam, Observant Sequencer") shouldBe null
                }
            }
        }
    }
}
