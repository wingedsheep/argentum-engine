package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Kozilek, the Broken Reality (MH3 #10) — {9} Legendary Creature — Eldrazi 9/9
 *
 *   When you cast this spell, up to two target players each manifest two cards from their hands.
 *   For each card manifested this way, you draw a card.
 *   Other colorless creatures you control get +3/+2.
 */
class KozilekTheBrokenRealityScenarioTest : ScenarioTestBase() {

    private fun TestGame.faceDownOf(playerId: EntityId): List<EntityId> =
        state.getBattlefield(playerId).filter { state.getEntity(it)?.get<FaceDownComponent>() != null }

    private fun TestGame.handIdOf(playerNumber: Int, name: String): EntityId {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getHand(playerId).first { state.getEntity(it)?.get<CardComponent>()?.name == name }
    }

    init {
        context("Kozilek, the Broken Reality") {

            test("both targeted players manifest from hand, you draw per card manifested, your manifests get +3/+2") {
                var builder = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Kozilek, the Broken Reality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInHand(1, "Hill Giant")
                    .withCardInHand(1, "Craw Wurm")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(1, "Island", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                repeat(5) { builder = builder.withCardInLibrary(1, "Island") }
                repeat(5) { builder = builder.withCardInLibrary(2, "Island") }
                val game = builder.build()

                game.castSpell(1, "Kozilek, the Broken Reality").error shouldBe null
                game.selectTargets(listOf(game.player1Id, game.player2Id)).error shouldBe null
                game.resolveStack()

                val bears = game.handIdOf(1, "Grizzly Bears")
                val giant = game.handIdOf(1, "Hill Giant")
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("Player1 chooses two of their own three cards") {
                    decision.playerId shouldBe game.player1Id
                }
                game.selectCards(listOf(bears, giant)).error shouldBe null
                // The opponent holds a single card: they manifest that one (Kozilek's ruling).
                while (game.hasPendingDecision()) {
                    val d = game.getPendingDecision() as SelectCardsDecision
                    d.playerId shouldBe game.player2Id
                    game.selectCards(d.options.take(1)).error shouldBe null
                }
                game.resolveStack()

                withClue("Player1 manifested two, the opponent its only card") {
                    game.faceDownOf(game.player1Id).toSet() shouldBe setOf(bears, giant)
                    game.faceDownOf(game.player2Id).size shouldBe 1
                    game.handSize(2) shouldBe 0
                }
                withClue("three cards manifested: Player1 keeps Craw Wurm and draws three") {
                    game.handSize(1) shouldBe 4
                }
                game.isOnBattlefield("Kozilek, the Broken Reality") shouldBe true

                withClue("Player1's manifests are colorless creatures, so they get +3/+2") {
                    game.state.projectedState.getPower(bears) shouldBe 5
                    game.state.projectedState.getToughness(bears) shouldBe 4
                }
                val oppManifest = game.faceDownOf(game.player2Id).single()
                withClue("the opponent's manifest isn't yours; it stays 2/2") {
                    game.state.projectedState.getPower(oppManifest) shouldBe 2
                    game.state.projectedState.getToughness(oppManifest) shouldBe 2
                    game.state.projectedState.getController(oppManifest) shouldBe game.player2Id
                }
                val kozilek = game.findPermanent("Kozilek, the Broken Reality")!!
                withClue("Kozilek doesn't pump itself") {
                    game.state.projectedState.getPower(kozilek) shouldBe 9
                }
            }

            test("with no targets chosen, nothing is manifested and nothing is drawn") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Kozilek, the Broken Reality")
                    .withCardInHand(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Island")
                    .withLandsOnBattlefield(1, "Island", 9)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Kozilek, the Broken Reality").error shouldBe null
                if (game.hasPendingDecision()) game.selectTargets(emptyList()).error shouldBe null
                game.resolveStack()

                game.hasPendingDecision() shouldBe false
                game.faceDownOf(game.player1Id).size shouldBe 0
                game.handSize(1) shouldBe 1
                game.isOnBattlefield("Kozilek, the Broken Reality") shouldBe true
            }
        }
    }
}
