package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Prophet of the Peak (ELD #227) — {6} Artifact Creature — Cat 5/5.
 *
 * "When this creature enters, scry 2."
 */
class ProphetOfThePeakScenarioTest : ScenarioTestBase() {

    init {
        context("Prophet of the Peak") {

            test("entering scries 2 — both looked-at cards can go to the bottom") {
                // Library top→bottom: Mountain, Forest, Island, Island.
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Prophet of the Peak")
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Prophet of the Peak").error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
                withClue("scry 2 looks at exactly the top two cards") {
                    decision.options.size shouldBe 2
                }
                game.selectCards(decision.options).error shouldBe null
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                val library = game.state.getLibrary(game.player1Id)
                val names = library.map { game.state.getEntity(it)!!.get<CardComponent>()!!.name }
                withClue("Mountain and Forest went to the bottom; an Island is now on top") {
                    names.size shouldBe 4
                    names.first() shouldBe "Island"
                    names.takeLast(2).toSet() shouldBe setOf("Mountain", "Forest")
                }
                game.isOnBattlefield("Prophet of the Peak") shouldBe true
            }
        }
    }
}
