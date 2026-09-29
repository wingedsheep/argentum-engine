package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/** Invasion of Ergamon // Truga Cliffcharger (MOM #233). */
class InvasionOfErgamonScenarioTest : ScenarioTestBase() {

    private fun frontBoard() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Invasion of Ergamon")
        .withCardInHand(1, "Grizzly Bears")
        .withLandsOnBattlefield(1, "Mountain", 1)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(1, "Island")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Invasion of Ergamon") {
            test("front: creates a Treasure, then discarding a card draws one") {
                val game = frontBoard()
                game.castSpell(1, "Invasion of Ergamon").error shouldBe null
                game.resolveStack()
                game.answerYesNo(true).error shouldBe null
                (game.getPendingDecision() as? SelectCardsDecision)?.let {
                    game.selectCards(it.options.take(1)).error shouldBe null
                }
                game.resolveStack()

                game.findAllPermanents("Treasure") shouldHaveSize 1
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.handSize(1) shouldBe 1
                game.isInHand(1, "Island") shouldBe true
            }

            test("front: declining the discard still leaves the Treasure and draws nothing") {
                val game = frontBoard()
                game.castSpell(1, "Invasion of Ergamon").error shouldBe null
                game.resolveStack()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()

                game.findAllPermanents("Treasure") shouldHaveSize 1
                game.handSize(1) shouldBe 1
                game.isInHand(1, "Grizzly Bears") shouldBe true
            }

            test("back: defeated, Truga Cliffcharger enters and discarding tutors a land or battle") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Invasion of Ergamon")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Lightning Bolt")
                    .withCardInHand(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 2)
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Invasion of Mercadia")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.checkStateBasedActions()
                repeat(2) {
                    game.castSpell(1, "Lightning Bolt", game.findPermanent("Invasion of Ergamon")!!).error shouldBe null
                    game.resolveStack()
                }
                // Siege defeated: cast it transformed.
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()

                val truga = game.findPermanent("Truga Cliffcharger")!!
                game.state.projectedState.hasKeyword(truga, Keyword.TRAMPLE) shouldBe true

                // Truga's ETB: may discard → search. Grizzly Bears is the only card left in hand,
                // so the discard is forced onto it and the next decision is the library search.
                game.answerYesNo(true).error shouldBe null
                val search = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the library search, got ${game.getPendingDecision()}")
                // Only the land and the battle are findable — never Hill Giant.
                val names = search.options.map { game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name }.toSet()
                names shouldBe setOf("Invasion of Mercadia", "Forest")
                val battle = game.findCardsInLibrary(1, "Invasion of Mercadia").single()
                game.selectCards(listOf(battle)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInHand(1, "Invasion of Mercadia") shouldBe true
            }
        }
    }
}
