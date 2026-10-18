package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Sylvan Library (LEG #207) — "At the beginning of your draw step, you may draw two additional
 * cards. If you do, choose two cards in your hand drawn this turn. For each of those cards, pay 4
 * life or put the card on top of your library."
 *
 * The library is stacked Forest (the normal draw), Island, Swamp (the two additional draws), then
 * Mountains; the Plains in hand began the turn there, so it is never "drawn this turn".
 */
class SylvanLibraryScenarioTest : ScenarioTestBase() {

    init {
        fun setUp(life: Int = 20): TestGame {
            val builder = scenario()
                .withPlayers("Library Player", "Opponent")
                .withCardOnBattlefield(1, "Sylvan Library")
                .withCardInHand(1, "Plains")
                .withLifeTotal(1, life)
                .withActivePlayer(1)
                .withTurnNumber(3)
                .inPhase(Phase.BEGINNING, Step.UPKEEP)
            builder.withCardInLibrary(1, "Forest")
            builder.withCardInLibrary(1, "Island")
            builder.withCardInLibrary(1, "Swamp")
            repeat(10) {
                builder.withCardInLibrary(1, "Mountain")
                builder.withCardInLibrary(2, "Mountain")
            }
            return builder.build()
        }

        /** Pass priority until Sylvan Library's trigger asks whether to draw. */
        fun TestGame.passToTrigger() {
            var passes = 0
            while (state.pendingDecision == null && passes < 20) {
                passPriority()
                passes++
            }
            state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        }

        fun TestGame.drawn(name: String): EntityId = findCardsInHand(1, name).single()

        context("Sylvan Library") {

            test("only cards drawn this turn can be chosen; keep one for 4 life, put the other on top") {
                val game = setUp()
                game.passToTrigger()
                game.answerYesNo(true)

                val forest = game.drawn("Forest")
                val island = game.drawn("Island")
                val swamp = game.drawn("Swamp")
                val plains = game.drawn("Plains")

                val choose = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the three cards drawn this turn are the choices, not the Plains from before") {
                    choose.options shouldContainExactlyInAnyOrder listOf(forest, island, swamp)
                    choose.options shouldNotContain plains
                    choose.minSelections shouldBe 2
                    choose.maxSelections shouldBe 2
                }
                game.selectCards(listOf(island, swamp))

                val split = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                split.options shouldContainExactlyInAnyOrder listOf(island, swamp)
                split.maxSelections shouldBe 2
                game.selectCards(listOf(island))

                withClue("the kept card cost 4 life") { game.getLifeTotal(1) shouldBe 16 }
                withClue("the unpaid card went back on top of the library") {
                    game.state.getLibrary(game.player1Id).first() shouldBe swamp
                    game.state.getHand(game.player1Id) shouldContainExactlyInAnyOrder listOf(plains, forest, island)
                }
            }

            test("putting both back skips the payment and lets the player order them") {
                val game = setUp()
                game.passToTrigger()
                game.answerYesNo(true)
                val island = game.drawn("Island")
                val swamp = game.drawn("Swamp")
                game.selectCards(listOf(island, swamp))
                game.skipSelection()

                val order = game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
                order.cards shouldContainExactlyInAnyOrder listOf(island, swamp)
                game.keepLibraryOrder()

                game.getLifeTotal(1) shouldBe 20
                game.state.getLibrary(game.player1Id).take(2) shouldContainExactlyInAnyOrder listOf(island, swamp)
                game.handSize(1) shouldBe 2
            }

            test("at 7 life only one chosen card can be kept") {
                val game = setUp(life = 7)
                game.passToTrigger()
                game.answerYesNo(true)
                val island = game.drawn("Island")
                val swamp = game.drawn("Swamp")
                game.selectCards(listOf(island, swamp))

                val split = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("floor(7 / 4) = 1 — a second 4-life payment isn't payable") {
                    split.maxSelections shouldBe 1
                }
                game.selectCards(listOf(swamp))
                game.getLifeTotal(1) shouldBe 3
                game.state.getLibrary(game.player1Id).first() shouldBe island
            }

            test("below 4 life both chosen cards must go back") {
                val game = setUp(life = 3)
                game.passToTrigger()
                game.answerYesNo(true)
                val island = game.drawn("Island")
                val swamp = game.drawn("Swamp")
                game.selectCards(listOf(island, swamp))

                withClue("no keep prompt: nothing is payable, so only the order is asked") {
                    game.state.pendingDecision.shouldBeInstanceOf<ReorderLibraryDecision>()
                }
                game.keepLibraryOrder()
                game.getLifeTotal(1) shouldBe 3
                game.state.getHand(game.player1Id) shouldNotContain island
                game.state.getHand(game.player1Id) shouldNotContain swamp
            }

            test("cards drawn on the opponent's turn aren't offered, even when the untap step pauses") {
                val builder = scenario()
                    .withPlayers("Library Player", "Opponent")
                    .withCardOnBattlefield(1, "Sylvan Library")
                    // "You may choose not to untap" pauses the untap step for a decision.
                    .withCardOnBattlefield(1, "Old Man of the Sea", tapped = true)
                    .withCardInHand(1, "Ancestral Recall")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(2)
                    .withPriorityPlayer(1)
                    .withTurnNumber(4)
                    .inPhase(Phase.ENDING, Step.END)
                repeat(3) { builder.withCardInLibrary(1, "Plains") }
                builder.withCardInLibrary(1, "Forest")
                builder.withCardInLibrary(1, "Island")
                builder.withCardInLibrary(1, "Swamp")
                repeat(10) {
                    builder.withCardInLibrary(1, "Mountain")
                    builder.withCardInLibrary(2, "Mountain")
                }
                val game = builder.build()

                game.castSpellTargetingPlayer(1, "Ancestral Recall", 1).error shouldBe null
                game.resolveStack()
                val drawnLastTurn = game.findCardsInHand(1, "Plains")
                drawnLastTurn.size shouldBe 3

                var passes = 0
                while (game.state.pendingDecision !is YesNoDecision && passes < 30) {
                    if (game.state.pendingDecision is SelectCardsDecision) game.skipSelection() // untap Old Man
                    else game.passPriority()
                    passes++
                }
                game.answerYesNo(true)

                val choose = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
                choose.options shouldContainExactlyInAnyOrder listOf(game.drawn("Forest"), game.drawn("Island"), game.drawn("Swamp"))
            }

            test("declining the extra draws does nothing else") {
                val game = setUp()
                game.passToTrigger()
                game.answerYesNo(false)

                game.state.pendingDecision shouldBe null
                game.handSize(1) shouldBe 2
                game.state.getHand(game.player1Id) shouldContain game.drawn("Forest")
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
