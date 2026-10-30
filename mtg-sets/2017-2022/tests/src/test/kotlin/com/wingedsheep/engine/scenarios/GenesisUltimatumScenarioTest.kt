package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Genesis Ultimatum (IKO #189) — {G}{G}{U}{U}{U}{R}{R} Sorcery.
 *
 *   "Look at the top five cards of your library. Put any number of permanent cards from among them
 *    onto the battlefield and the rest into your hand. Exile Genesis Ultimatum."
 *
 * Only permanent cards may be put onto the battlefield; every card not chosen — the instant and any
 * permanent card left behind — goes to hand. The resolved sorcery is exiled, not put into the
 * graveyard.
 */
class GenesisUltimatumScenarioTest : ScenarioTestBase() {

    private fun game() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInHand(1, "Genesis Ultimatum")
        .withLandsOnBattlefield(1, "Forest", 2)
        .withLandsOnBattlefield(1, "Island", 3)
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Centaur Courser")
        .withCardInLibrary(1, "Sol Ring")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("chosen permanent cards enter the battlefield, the rest go to hand, and the spell is exiled") {
            val game = game()

            game.castSpell(1, "Genesis Ultimatum").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val plains = game.findCardsInLibrary(1, "Plains").single()
            val courser = game.findCardsInLibrary(1, "Centaur Courser").single()
            val solRing = game.findCardsInLibrary(1, "Sol Ring").single()
            withClue("Every permanent card is offered; the instant is not") {
                decision.options shouldContainExactlyInAnyOrder listOf(bears, plains, courser, solRing)
            }

            game.selectCards(listOf(bears, plains, solRing))
            game.resolveStack()

            withClue("The three chosen permanent cards are on the battlefield") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Sol Ring") shouldBe true
                game.findAllPermanents("Plains").size shouldBe 1
            }
            withClue("The unchosen permanent card and the instant go to hand") {
                game.isInHand(1, "Centaur Courser") shouldBe true
                game.isInHand(1, "Lightning Bolt") shouldBe true
                game.librarySize(1) shouldBe 0
            }
            withClue("Genesis Ultimatum is exiled instead of going to the graveyard") {
                game.isInExile(1, "Genesis Ultimatum") shouldBe true
                game.isInGraveyard(1, "Genesis Ultimatum") shouldBe false
            }
        }

        test("choosing nothing puts all five cards into your hand") {
            val game = game()

            game.castSpell(1, "Genesis Ultimatum").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection()
            game.resolveStack()

            game.handSize(1) shouldBe 5
            game.librarySize(1) shouldBe 0
            game.isInExile(1, "Genesis Ultimatum") shouldBe true
        }
    }
}
