package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Munda, Ambush Leader (BFZ #215) — {2}{R}{W} Legendary Creature — Kor Ally 3/4.
 *
 *   Haste
 *   Rally — Whenever Munda or another Ally you control enters, you may look at the top four cards
 *   of your library. If you do, reveal any number of Ally cards from among them, then put those
 *   cards on top of your library in any order and the rest on the bottom in any order.
 */
class MundaAmbushLeaderScenarioTest : ScenarioTestBase() {

    private fun mundaInHandWithStackedLibrary() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardInHand(1, "Munda, Ambush Leader")
        .withLandsOnBattlefield(1, "Mountain", 2)
        .withLandsOnBattlefield(1, "Plains", 2)
        .withCardInLibrary(1, "Makindi Patrol") // Ally
        .withCardInLibrary(1, "Grizzly Bears")
        .withCardInLibrary(1, "Halimar Tidecaller") // Ally
        .withCardInLibrary(1, "Shock")
        .withCardInLibrary(1, "Craw Wurm") // fifth card — out of reach
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("Munda's own entry triggers rally: revealed Allies go on top, the rest on the bottom") {
            val game = mundaInHandWithStackedLibrary()

            game.castSpell(1, "Munda, Ambush Leader").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null

            val pick = game.getPendingDecision()
            pick.shouldBeInstanceOf<SelectCardsDecision>()
            val patrol = game.findCardsInLibrary(1, "Makindi Patrol").single()
            val tidecaller = game.findCardsInLibrary(1, "Halimar Tidecaller").single()
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val shock = game.findCardsInLibrary(1, "Shock").single()
            val wurm = game.findCardsInLibrary(1, "Craw Wurm").single()
            withClue("all four looked-at cards are shown, only the Allies are selectable") {
                pick.options shouldContainExactlyInAnyOrder listOf(patrol, tidecaller)
                pick.nonSelectableOptions shouldContainExactlyInAnyOrder listOf(bears, shock)
            }

            game.selectCards(listOf(patrol, tidecaller)).error shouldBe null
            while (game.getPendingDecision() is ReorderLibraryDecision) {
                game.keepLibraryOrder().error shouldBe null
            }
            game.resolveStack()

            val library = game.state.getLibrary(game.player1Id)
            library.size shouldBe 5
            withClue("the revealed Allies sit on top") {
                library.take(2) shouldContainExactlyInAnyOrder listOf(patrol, tidecaller)
            }
            withClue("the untouched fifth card is next") {
                library[2] shouldBe wurm
            }
            withClue("the non-Allies went to the bottom") {
                library.takeLast(2) shouldContainExactlyInAnyOrder listOf(bears, shock)
            }
            game.isOnBattlefield("Munda, Ambush Leader") shouldBe true
        }

        test("declining to look leaves the library untouched") {
            val game = mundaInHandWithStackedLibrary()
            val before = game.state.getLibrary(game.player1Id)

            game.castSpell(1, "Munda, Ambush Leader").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            game.state.getLibrary(game.player1Id) shouldBe before
        }
    }
}
