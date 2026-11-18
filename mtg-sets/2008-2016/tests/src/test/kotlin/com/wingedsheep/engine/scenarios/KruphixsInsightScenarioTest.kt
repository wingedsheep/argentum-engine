package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Kruphix's Insight (JOU #129) — {2}{G} Sorcery.
 *
 *   Reveal the top six cards of your library. Put up to three enchantment cards from among them
 *   into your hand and the rest of the revealed cards into your graveyard.
 */
class KruphixsInsightScenarioTest : ScenarioTestBase() {
    init {
        test("takes up to three enchantments from the top six; the rest go to the graveyard") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Kruphix's Insight")
                .withLandsOnBattlefield(1, "Forest", 3)
                // Top six (first added is the top of the library).
                .withCardInLibrary(1, "Glorious Anthem")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Glorious Anthem")
                .withCardInLibrary(1, "Hill Giant")
                // Below the six — never revealed.
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val anthems = game.findCardsInLibrary(1, "Glorious Anthem")
            val topPacifism = game.findCardsInLibrary(1, "Pacifism").first()
            val bottomPacifism = game.findCardsInLibrary(1, "Pacifism").last()

            game.castSpell(1, "Kruphix's Insight").error shouldBe null
            game.resolveStack()

            game.selectCards(anthems + topPacifism).error shouldBe null
            game.resolveStack()

            withClue("the three enchantments are in hand") {
                game.findCardsInHand(1, "Glorious Anthem").size shouldBe 2
                game.findCardsInHand(1, "Pacifism") shouldBe listOf(topPacifism)
            }
            withClue("the three other revealed cards went to the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
            withClue("cards below the top six are untouched") {
                game.librarySize(1) shouldBe 2
                game.findCardsInLibrary(1, "Pacifism") shouldBe listOf(bottomPacifism)
            }
        }
    }
}
