package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/** Scenario tests for Lord Skitter's Butcher. */
class LordSkittersButcherScenarioTest : ScenarioTestBase() {

    init {
        context("Lord Skitter's Butcher — three-mode ETB") {
            fun butcherGame() = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Lord Skitter's Butcher")
                .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            /**
             * Cast the Butcher, answer the ETB's mode question, then let the ability resolve.
             *
             * The mode is picked as the trigger goes on the stack (CR 603.3c), so answering it only
             * gets the ability *onto* the stack — the trailing `resolveStack` is what runs the chosen
             * mode, and it stops early if that mode needs a decision of its own.
             */
            fun ScenarioTestBase.TestGame.castAndChooseMode(index: Int) {
                castSpell(1, "Lord Skitter's Butcher").error shouldBe null
                if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
                resolveStack()
                val modeDecision = getPendingDecision() as? ChooseOptionDecision
                    ?: error("expected a ChooseOptionDecision for the ETB; got ${getPendingDecision()}")
                submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = index))
                resolveStack()
            }

            test("mode 0 creates a Rat token") {
                val game = butcherGame()
                game.castAndChooseMode(0)

                game.findAllPermanents("Rat Token").size shouldBe 1
            }

            test("mode 1 — sacrificing another creature scries 2 and draws") {
                val game = butcherGame()
                val handBefore = game.handSize(1)
                game.castAndChooseMode(1)

                val bears = game.findPermanent("Grizzly Bears")!!
                val sacChoice = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the sacrifice selection; got ${game.getPendingDecision()}")
                withClue("the Butcher itself is not offered — 'another creature'") {
                    sacChoice.options.contains(game.findPermanent("Lord Skitter's Butcher")) shouldBe false
                }
                game.selectCards(listOf(bears)).error shouldBe null

                withClue("the chosen creature was sacrificed") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                val scry = game.getPendingDecision() as? SelectCardsDecision
                    ?: error("expected the scry 2 selection; got ${game.getPendingDecision()}")
                withClue("scry 2 looks at the top two cards") { scry.options.size shouldBe 2 }
                game.skipSelection().error shouldBe null // keep both on top
                if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()
                game.resolveStack()

                withClue("the Butcher (cast from hand) left, and the draw added one card") {
                    game.handSize(1) shouldBe handBefore
                }
                game.librarySize(1) shouldBe 2
            }

            test("mode 1 — declining the sacrifice costs nothing and draws nothing") {
                val game = butcherGame()
                val handBefore = game.handSize(1)
                game.castAndChooseMode(1)

                game.getPendingDecision() shouldNotBe null
                game.skipSelection().error shouldBe null
                game.resolveStack()

                withClue("no sacrifice happened") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
                withClue("no draw happened — the Butcher just left hand for the battlefield") {
                    game.handSize(1) shouldBe handBefore - 1
                }
                game.librarySize(1) shouldBe 3
            }

            test("mode 2 gives your creatures menace until end of turn") {
                val game = butcherGame()
                game.castAndChooseMode(2)

                val bears = game.findPermanent("Grizzly Bears")!!
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
                val butcher = game.findPermanent("Lord Skitter's Butcher")!!
                withClue("the Butcher is a creature you control too") {
                    game.state.projectedState.hasKeyword(butcher, Keyword.MENACE) shouldBe true
                }
            }
        }
    }
}
