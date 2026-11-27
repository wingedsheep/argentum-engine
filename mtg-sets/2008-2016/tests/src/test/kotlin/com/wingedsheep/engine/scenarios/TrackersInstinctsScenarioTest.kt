package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class TrackersInstinctsScenarioTest : ScenarioTestBase() {
    init {
        test("puts a creature from the top four into hand and the rest into the graveyard, then flashes back") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Tracker's Instincts")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withLandsOnBattlefield(1, "Island", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Savannah Lions")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Giant Growth")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val librarySizeBefore = game.librarySize(1)

            val result = game.castSpell(1, "Tracker's Instincts")
            withClue("${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(game.findCardsInLibrary(1, "Grizzly Bears"))
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInGraveyard(1, "Savannah Lions") shouldBe true
            game.librarySize(1) shouldBe librarySizeBefore - 4
            game.isInGraveyard(1, "Tracker's Instincts") shouldBe true
            game.graveyardSize(1) shouldBe 4

            val flashback = game.castSpellFromGraveyard(1, "Tracker's Instincts")
            withClue("${flashback.error}") { flashback.error shouldBe null }
            game.resolveStack()
            if (game.getPendingDecision() is SelectCardsDecision) game.selectCards(emptyList())
            game.resolveStack()
            game.isInExile(1, "Tracker's Instincts") shouldBe true
            // Only Giant Growth was left to reveal: no creature, so it goes to the graveyard too.
            game.isInGraveyard(1, "Giant Growth") shouldBe true
            game.isInHand(1, "Giant Growth") shouldBe false
            game.librarySize(1) shouldBe librarySizeBefore - 5
        }
    }
}
