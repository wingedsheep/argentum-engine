package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test for Crawling Sensation (SOI #199) — {2}{G} Enchantment.
 *
 *   At the beginning of your upkeep, you may mill two cards.
 *   Whenever one or more land cards are put into your graveyard from anywhere for the first time
 *   each turn, create a 1/1 green Insect creature token.
 *
 * "For the first time each turn" is turn history: a land that reached the graveyard before
 * Crawling Sensation was on the battlefield still closes the turn's window. A non-land card
 * doesn't, and the window opens on every turn, not just yours.
 */
class CrawlingSensationScenarioTest : ScenarioTestBase() {

    init {
        context("Crawling Sensation") {

            test("milling two lands at once makes one Insect") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crawling Sensation")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.BEGINNING, Step.UNTAP)
                    .build()

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.resolveStack()
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)
                game.resolveStack()

                withClue("two land cards milled together are one batch — one Insect") {
                    game.findPermanents("Insect Token").size shouldBe 1
                }
            }

            test("a second land later in the turn makes no second Insect; it works on an opponent's turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crawling Sensation")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Island")
                    .withCardInHand(2, "Stone Rain")
                    .withCardInHand(2, "Stone Rain")
                    .withLandsOnBattlefield(2, "Mountain", 6)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Stone Rain", game.findPermanent("Plains")!!).error shouldBe null
                game.resolveStack()
                withClue("the opponent destroying our land on their turn is the turn's first land") {
                    game.isInGraveyard(1, "Plains") shouldBe true
                    game.findPermanents("Insect Token").size shouldBe 1
                }
                // Priority can land on the trigger's controller after it resolves; hand it back.
                if (game.state.priorityPlayerId == game.player1Id) game.passPriority()

                game.castSpell(2, "Stone Rain", game.findPermanent("Island")!!).error shouldBe null
                game.resolveStack()
                withClue("the second land this turn isn't the first time — still one Insect") {
                    game.isInGraveyard(1, "Island") shouldBe true
                    game.findPermanents("Insect Token").size shouldBe 1
                }
            }

            test("a land that hit the graveyard before Crawling Sensation entered closes the window") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Crawling Sensation")
                    .withCardInHand(1, "Stone Rain")
                    .withCardInHand(1, "Stone Rain")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Island")
                    .withLandsOnBattlefield(1, "Mountain", 6)
                    .withLandsOnBattlefield(1, "Forest", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Stone Rain", game.findPermanent("Plains")!!).error shouldBe null
                game.resolveStack()
                game.castSpell(1, "Crawling Sensation").error shouldBe null
                game.resolveStack()
                withClue("Crawling Sensation is on the battlefield before the second land goes") {
                    (game.findPermanent("Crawling Sensation") != null) shouldBe true
                }

                game.castSpell(1, "Stone Rain", game.findPermanent("Island")!!).error shouldBe null
                game.resolveStack()
                withClue("the Plains already went to the graveyard this turn — no Insect") {
                    game.isInGraveyard(1, "Island") shouldBe true
                    game.findPermanents("Insect Token").size shouldBe 0
                }
            }

            test("a non-land card earlier in the turn doesn't close the window") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Crawling Sensation")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardInHand(2, "Doom Blade")
                    .withCardInHand(2, "Stone Rain")
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withLandsOnBattlefield(2, "Mountain", 3)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Doom Blade", game.findPermanent("Grizzly Bears")!!).error shouldBe null
                game.resolveStack()
                withClue("a creature card isn't a land card") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.findPermanents("Insect Token").size shouldBe 0
                }

                game.castSpell(2, "Stone Rain", game.findPermanent("Plains")!!).error shouldBe null
                game.resolveStack()
                withClue("the Plains is the turn's first land card") {
                    game.findPermanents("Insect Token").size shouldBe 1
                }
            }
        }
    }
}
