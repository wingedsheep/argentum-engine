package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Petals of Insight — "Look at the top three cards of your library. You may put those cards on
 * the bottom of your library in any order. If you do, return Petals of Insight to its owner's
 * hand. Otherwise, draw three cards."
 *
 * The scenario builder appends library cards top-down, so the first `withCardInLibrary` call is
 * the top card.
 */
class PetalsOfInsightScenarioTest : ScenarioTestBase() {
    init {
        fun base() = scenario().withPlayers("P1", "P2")
            .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardInHand(1, "Petals of Insight")
            .withCardInLibrary(1, "Grizzly Bears")
            .withCardInLibrary(1, "Hill Giant")
            .withCardInLibrary(1, "Forest")
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(1, "Mountain")
            .withCardInLibrary(2, "Island")

        test("putting the three cards on the bottom returns Petals of Insight to hand") {
            val game = base().build()
            game.castSpell(1, "Petals of Insight").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            // "In any order" prompt for the bottom of the library.
            game.keepLibraryOrder().error shouldBe null
            game.resolveStack()

            // Returned straight from the stack — never touches the graveyard (2013-06-07 ruling).
            game.isInHand(1, "Petals of Insight") shouldBe true
            game.isInGraveyard(1, "Petals of Insight") shouldBe false
            game.handSize(1) shouldBe 1

            val library = game.state.getZone(game.player1Id, Zone.LIBRARY)
                .map { game.state.getEntity(it)?.get<CardComponent>()?.name }
            library.size shouldBe 5
            library.take(2) shouldBe listOf("Swamp", "Mountain")
            library.drop(2).toSet() shouldBe setOf("Grizzly Bears", "Hill Giant", "Forest")
        }

        test("declining draws the three cards and Petals of Insight goes to the graveyard") {
            val game = base().build()
            game.castSpell(1, "Petals of Insight").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Petals of Insight") shouldBe true
            game.handSize(1) shouldBe 3
            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe true
            game.isInHand(1, "Forest") shouldBe true
            game.librarySize(1) shouldBe 2
        }
    }
}
