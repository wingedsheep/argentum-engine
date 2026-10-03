package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Planar Genesis (MH3 #198) — {G}{U} Instant.
 *
 * "Look at the top four cards of your library. You may put a land card from among them onto the
 *  battlefield tapped. If you don't, put a card from among them into your hand. Put the rest on
 *  the bottom of your library in a random order."
 *
 * The three branches: take the land (nothing goes to hand), decline the land (any one card goes
 * to hand, the land included), and no land among the four (straight to the hand choice).
 */
class PlanarGenesisScenarioTest : ScenarioTestBase() {

    private fun base(withLand: Boolean) = scenario()
        .withPlayers("Caster", "Opponent")
        .withCardInHand(1, "Planar Genesis")
        .withCardInLibrary(1, if (withLand) "Swamp" else "Centaur Courser")
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Lightning Bolt")
        .withCardInLibrary(1, "Giant Growth")
        .withCardInLibrary(1, "Hill Giant")
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

    init {
        test("putting a land onto the battlefield tapped means no card goes to hand") {
            val game = base(withLand = true).build()
            game.castSpell(1, "Planar Genesis").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(game.findCardsInLibrary(1, "Swamp").single()))
            game.resolveStack()

            val swamp = game.findPermanent("Swamp")
            withClue("the land entered tapped") {
                swamp shouldNotBe null
                game.state.getEntity(swamp!!)?.get<TappedComponent>() shouldNotBe null
            }
            withClue("no card was put into hand — the rest went to the bottom") {
                game.getPendingDecision() shouldBe null
                game.handSize(1) shouldBe 0
                game.librarySize(1) shouldBe 4
                game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Hill Giant").single()
            }
        }

        test("declining the land puts any one card into hand instead") {
            val game = base(withLand = true).build()
            game.castSpell(1, "Planar Genesis").error shouldBe null
            game.resolveStack()

            game.skipSelection()
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.selectCards(listOf(game.findCardsInLibrary(1, "Lightning Bolt").single()))
            game.resolveStack()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.isOnBattlefield("Swamp") shouldBe false
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 4
            withClue("the rest went under the untouched fifth card") {
                game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Hill Giant").single()
            }
        }

        test("with no land among the four, a card still goes to hand") {
            val game = base(withLand = false).build()
            game.castSpell(1, "Planar Genesis").error shouldBe null
            game.resolveStack()

            // The optional land choice has nothing eligible; if it is still shown, decline it.
            if ((game.getPendingDecision() as SelectCardsDecision).minSelections == 0) {
                game.skipSelection()
            }
            withClue("the hand choice is mandatory") {
                (game.getPendingDecision() as SelectCardsDecision).minSelections shouldBe 1
            }
            game.selectCards(listOf(game.findCardsInLibrary(1, "Centaur Courser").single()))
            game.resolveStack()

            game.isInHand(1, "Centaur Courser") shouldBe true
            game.handSize(1) shouldBe 1
            game.librarySize(1) shouldBe 4
            withClue("the rest went under the untouched fifth card") {
                game.state.getLibrary(game.player1Id).first() shouldBe game.findCardsInLibrary(1, "Hill Giant").single()
            }
        }
    }
}
