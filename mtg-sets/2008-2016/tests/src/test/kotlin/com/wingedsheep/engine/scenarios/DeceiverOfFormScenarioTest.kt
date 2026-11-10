package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Deceiver of Form — {6}{C} 8/8 Eldrazi. "At the beginning of combat on your turn, reveal the top
 * card of your library. If a creature card is revealed this way, you may have creatures you control
 * other than this creature become copies of that card until end of turn. You may put that card on
 * the bottom of your library."
 *
 * Covers the group copy off a card that stays in the library (only your other creatures, never
 * Deceiver or an opponent's), declining the copy, the bottom-of-library choice being offered
 * independently of the copy and of the card's type (ruling 2016-01-22), and the copy ending at
 * end of turn.
 */
class DeceiverOfFormScenarioTest : ScenarioTestBase() {
    init {
        fun board(top: String) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Deceiver of Form")
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, top)
            .withCardInLibrary(1, "Mountain")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** Move to beginning of combat and answer the trigger's "may" choices. */
        fun ScenarioTestBase.TestGame.runTrigger(copy: Boolean, toBottom: Boolean, expectCopyPrompt: Boolean) {
            passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
            resolveStack()
            var sawCopyPrompt = false
            var sawBottomPrompt = false
            while (hasPendingDecision()) {
                when (val decision = getPendingDecision()) {
                    is YesNoDecision -> {
                        sawCopyPrompt = true
                        answerYesNo(copy)
                    }
                    is SelectCardsDecision -> {
                        sawBottomPrompt = true
                        selectCards(if (toBottom) decision.options else emptyList())
                    }
                    else -> error("unexpected decision $decision")
                }
                resolveStack()
            }
            withClue("copy prompt offered only when a creature card is revealed") {
                sawCopyPrompt shouldBe expectCopyPrompt
            }
            withClue("bottom-of-library choice is always offered") { sawBottomPrompt shouldBe true }
        }

        fun ScenarioTestBase.TestGame.nameOf(id: com.wingedsheep.sdk.model.EntityId) =
            state.getEntity(id)?.get<CardComponent>()?.name

        test("creature revealed: your other creatures become copies, card goes to the bottom") {
            val game = board("Grizzly Bears")
            val deceiver = game.findPermanents("Deceiver of Form").single()
            val myGiant = game.state.getBattlefield().single {
                game.nameOf(it) == "Hill Giant" && game.state.projectedState.getController(it) == game.player1Id
            }
            val theirGiant = game.state.getBattlefield().single {
                game.nameOf(it) == "Hill Giant" && game.state.projectedState.getController(it) == game.player2Id
            }
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.runTrigger(copy = true, toBottom = true, expectCopyPrompt = true)

            withClue("your other creature became a copy of the revealed card") {
                game.nameOf(myGiant) shouldBe "Grizzly Bears"
                game.state.projectedState.getPower(myGiant) shouldBe 2
                game.state.projectedState.getToughness(myGiant) shouldBe 2
            }
            withClue("Deceiver and the opponent's creature are untouched") {
                game.nameOf(deceiver) shouldBe "Deceiver of Form"
                game.state.projectedState.getPower(deceiver) shouldBe 8
                game.nameOf(theirGiant) shouldBe "Hill Giant"
            }
            withClue("the revealed card went to the bottom of the library") {
                game.state.getLibrary(game.player1Id).last() shouldBe bears
            }

            game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
            withClue("the copy ends at end of turn") {
                game.nameOf(myGiant) shouldBe "Hill Giant"
                game.state.projectedState.getPower(myGiant) shouldBe 3
            }
        }

        test("declining the copy leaves creatures alone; the card may stay on top") {
            val game = board("Grizzly Bears")
            val myGiant = game.state.getBattlefield().single {
                game.nameOf(it) == "Hill Giant" && game.state.projectedState.getController(it) == game.player1Id
            }
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()

            game.runTrigger(copy = false, toBottom = false, expectCopyPrompt = true)

            game.nameOf(myGiant) shouldBe "Hill Giant"
            game.state.getLibrary(game.player1Id).first() shouldBe bears
        }

        test("noncreature revealed: no copy, but it may still go to the bottom") {
            val game = board("Island")
            val myGiant = game.state.getBattlefield().single {
                game.nameOf(it) == "Hill Giant" && game.state.projectedState.getController(it) == game.player1Id
            }
            val island = game.findCardsInLibrary(1, "Island").single()

            game.runTrigger(copy = true, toBottom = true, expectCopyPrompt = false)

            game.nameOf(myGiant) shouldBe "Hill Giant"
            game.state.getLibrary(game.player1Id).last() shouldBe island
        }
    }
}
