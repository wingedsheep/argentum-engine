package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Supreme Will (HOU #49) — Choose one —
 * • Counter target spell unless its controller pays {3}.
 * • Look at the top four cards of your library. Put one of them into your hand and the rest on
 *   the bottom of your library in any order.
 */
class SupremeWillScenarioTest : ScenarioTestBase() {

    init {
        context("Supreme Will") {

            test("mode 1 counters a spell whose controller can't pay {3}") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Supreme Will")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withLandsOnBattlefield(2, "Forest", 2)
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(2, "Grizzly Bears").error shouldBe null
                game.passPriority()

                val bears = game.state.stack.single()
                val will = game.findCardsInHand(1, "Supreme Will").single()
                val target = listOf(ChosenTarget.Spell(bears))
                game.execute(
                    CastSpell(game.player1Id, will, target, chosenModes = listOf(0), modeTargetsOrdered = listOf(target))
                ).error shouldBe null
                game.resolveStack()

                game.isOnBattlefield("Grizzly Bears") shouldBe false
                game.isInGraveyard(2, "Grizzly Bears") shouldBe true
            }

            test("mode 2 puts one of the top four into hand and the rest on the bottom") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardInHand(1, "Supreme Will")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Grizzly Bears")
                    .withCardInLibrary(1, "Hill Giant")
                    .withCardInLibrary(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Llanowar Elves")
                    .withCardInLibrary(1, "Island") // fifth card: stays put beneath the four looked at
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpellWithMode(1, "Supreme Will", 1).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision() as? SelectCardsDecision
                decision.shouldNotBeNull()
                val giant = decision.options.single {
                    game.state.getEntity(it)?.get<CardComponent>()?.name == "Hill Giant"
                }
                game.selectCards(listOf(giant))
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                withClue("the chosen card is in hand, the other three went back to the library") {
                    game.isInHand(1, "Hill Giant") shouldBe true
                    game.librarySize(1) shouldBe 4
                }
                withClue("the three unchosen cards went to the bottom, so the untouched fifth card is now on top") {
                    val top = game.state.getLibrary(game.player1Id).first()
                    game.state.getEntity(top)?.get<CardComponent>()?.name shouldBe "Island"
                }
            }
        }
    }
}
