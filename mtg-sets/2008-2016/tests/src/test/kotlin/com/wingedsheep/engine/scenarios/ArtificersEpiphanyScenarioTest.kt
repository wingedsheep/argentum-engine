package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Artificer's Epiphany — {2}{U} Instant.
 * "Draw two cards. If you control no artifacts, discard a card."
 */
class ArtificersEpiphanyScenarioTest : ScenarioTestBase() {
    init {
        context("Artificer's Epiphany") {
            test("with no artifacts, draws two then discards a chosen card") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Artificer's Epiphany")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lib = game.librarySize(1)
                game.castSpell(1, "Artificer's Epiphany").error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe lib - 2
                game.handSize(1) shouldBe 2
                game.hasPendingDecision() shouldBe true

                val toDiscard = game.findCardsInHand(1, "Hill Giant").ifEmpty {
                    game.findCardsInHand(1, "Grizzly Bears")
                }.first()
                game.selectCards(listOf(toDiscard)).error shouldBe null

                game.handSize(1) shouldBe 1
                // the spell itself + the discarded card
                game.graveyardSize(1) shouldBe 2
                game.hasPendingDecision() shouldBe false
            }

            test("controlling an artifact skips the discard") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Artificer's Epiphany")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(1, "Ornithopter")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val lib = game.librarySize(1)
                game.castSpell(1, "Artificer's Epiphany").error shouldBe null
                game.resolveStack()

                game.librarySize(1) shouldBe lib - 2
                game.handSize(1) shouldBe 2
                game.graveyardSize(1) shouldBe 1
                game.hasPendingDecision() shouldBe false
            }

            test("an opponent's artifact does not count") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardInHand(1, "Artificer's Epiphany")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardOnBattlefield(2, "Ornithopter")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Artificer's Epiphany").error shouldBe null
                game.resolveStack()

                game.handSize(1) shouldBe 2
                game.hasPendingDecision() shouldBe true
            }
        }
    }
}
