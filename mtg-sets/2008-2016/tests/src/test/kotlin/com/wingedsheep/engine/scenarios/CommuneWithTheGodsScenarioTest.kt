package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Commune with the Gods (THS #155) — {1}{G} Sorcery.
 *
 *   Reveal the top five cards of your library. You may put a creature or enchantment card from
 *   among them into your hand. Put the rest into your graveyard.
 */
class CommuneWithTheGodsScenarioTest : ScenarioTestBase() {
    init {
        test("takes one creature or enchantment from the top five; the rest go to the graveyard") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Commune with the Gods")
                .withLandsOnBattlefield(1, "Forest", 2)
                // Top five (first added is the top of the library).
                .withCardInLibrary(1, "Pacifism")
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(1, "Lightning Bolt")
                .withCardInLibrary(1, "Hill Giant")
                // Below the five — never revealed.
                .withCardInLibrary(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val pacifism = game.findCardsInLibrary(1, "Pacifism").single()

            game.castSpell(1, "Commune with the Gods").error shouldBe null
            game.resolveStack()

            game.selectCards(listOf(pacifism)).error shouldBe null
            game.resolveStack()

            withClue("the chosen enchantment is in hand") { game.isInHand(1, "Pacifism") shouldBe true }
            withClue("the other four revealed cards went to the graveyard") {
                game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Forest") shouldBe true
                game.isInGraveyard(1, "Lightning Bolt") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
            }
            withClue("the card below the top five is untouched") {
                game.librarySize(1) shouldBe 1
                game.isInGraveyard(1, "Mountain") shouldBe false
            }
        }
    }
}
