package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.blc.cards.InsatiableFrugivore
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Insatiable Frugivore (BLC #18).
 *
 * "When this creature enters, create a Food token, then you may exile three cards from your
 * graveyard. If you do, repeat this process." — a do-while loop that stops on a decline, and is
 * not offered with fewer than three cards in the graveyard (2024-07-26 ruling).
 *
 * "{3}{B}, Sacrifice X Foods: Creatures you control get +X/+0 and gain menace until end of turn."
 */
class InsatiableFrugivoreScenarioTest : ScenarioTestBase() {

    init {
        val pumpId = InsatiableFrugivore.activatedAbilities.single().id

        fun castBoard(graveyardCards: Int) = scenario()
            .withPlayers("P1", "P2")
            .withCardInHand(1, "Insatiable Frugivore")
            .withLandsOnBattlefield(1, "Swamp", 4)
            .apply { repeat(graveyardCards) { withCardInGraveyard(1, "Grizzly Bears") } }
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        /** Accept the "may" and exile the first three offered cards. */
        fun TestGame.acceptAndExileThree() {
            getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            answerYesNo(true)
            val select = getPendingDecision()
            select.shouldBeInstanceOf<SelectCardsDecision>()
            selectCards(select.options.take(3))
        }

        test("repeats while you exile three, and stops offering once fewer than three remain") {
            val game = castBoard(graveyardCards = 7)
            game.castSpell(1, "Insatiable Frugivore").error shouldBe null
            game.resolveStack()

            game.acceptAndExileThree() // pass 1: Food #1, 7 -> 4
            game.acceptAndExileThree() // pass 2: Food #2, 4 -> 1
            // Pass 3 creates Food #3; with one card left the "may" is never offered.
            withClue("no prompt with fewer than three cards in the graveyard") {
                game.hasPendingDecision() shouldBe false
            }
            game.resolveStack()

            game.findPermanents("Food").size shouldBe 3
            game.graveyardSize(1) shouldBe 1
        }

        test("declining the first exile stops after one Food") {
            val game = castBoard(graveyardCards = 5)
            game.castSpell(1, "Insatiable Frugivore").error shouldBe null
            game.resolveStack()

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findPermanents("Food").size shouldBe 1
            game.graveyardSize(1) shouldBe 5
        }

        test("sacrificing two Foods gives creatures you control +2/+0 and menace") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardOnBattlefield(1, "Insatiable Frugivore")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withCardOnBattlefield(1, "Food", isToken = true)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Swamp", 4)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val frugivore = game.findPermanent("Insatiable Frugivore")!!
            val result = game.execute(
                ActivateAbility(
                    game.player1Id, frugivore, pumpId,
                    costPayment = AdditionalCostPayment(variableCostPermanents = game.findPermanents("Food"))
                )
            )
            withClue("${result.error}") { result.error shouldBe null }
            game.findPermanents("Food").size shouldBe 0
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(frugivore) shouldBe 4
            projected.getToughness(frugivore) shouldBe 4
            projected.hasKeyword(frugivore, Keyword.MENACE) shouldBe true

            val bears = game.findPermanents("Grizzly Bears")
            val mine = bears.single { projected.getController(it) == game.player1Id }
            val theirs = bears.single { it != mine }
            projected.getPower(mine) shouldBe 4
            projected.hasKeyword(mine, Keyword.MENACE) shouldBe true
            projected.getPower(theirs) shouldBe 2
            projected.hasKeyword(theirs, Keyword.MENACE) shouldBe false
        }
    }
}
