package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseNumberDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AbilityCost
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Urza Assembles the Titans (DMU #37).
 *
 *   Read ahead
 *   I — Scry 4, then you may reveal the top card of your library. If a planeswalker card is
 *       revealed this way, put it into your hand.
 *   II — You may put a planeswalker card with mana value 6 or less from your hand onto the
 *        battlefield.
 *   III — You may activate the loyalty abilities of planeswalkers you control twice this turn
 *         rather than only once.
 *
 * Chapter III's grant is held by the player: it outlives the Saga (sacrificed right after III) and
 * never stacks past two (2022-09-09 ruling).
 */
class UrzaAssemblesTheTitansScenarioTest : ScenarioTestBase() {

    init {
        fun TestGame.castAndChoose(chapter: Int) {
            castSpell(1, "Urza Assembles the Titans").error shouldBe null
            resolveStack()
            getPendingDecision().shouldBeInstanceOf<ChooseNumberDecision>()
            chooseNumber(chapter)
            resolveStack()
        }

        fun TestGame.activateJacePlusTwo() = run {
            val jace = findPermanent("Jace Beleren")!!
            val plusTwo = cardRegistry.getCard("Jace Beleren")!!.script.activatedAbilities
                .first { (it.cost as? AbilityCost.Loyalty)?.change == 2 }
            execute(ActivateAbility(player1Id, jace, plusTwo.id))
        }

        fun builder() = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Urza Assembles the Titans")
            .withLandsOnBattlefield(1, "Plains", 5)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

        test("chapter III: each planeswalker you control may activate twice this turn, not three times") {
            val game = builder()
                .withCardOnBattlefield(1, "Jace Beleren")
                .apply { repeat(6) { withCardInLibrary(1, "Island"); withCardInLibrary(2, "Island") } }
                .build()

            game.castAndChoose(3)
            withClue("the Saga is sacrificed after chapter III") {
                game.findPermanent("Urza Assembles the Titans") shouldBe null
            }

            game.activateJacePlusTwo().error shouldBe null
            game.resolveStack()
            withClue("the grant outlives the sacrificed Saga: a second activation is legal") {
                game.activateJacePlusTwo().error shouldBe null
            }
            game.resolveStack()
            withClue("twice, not three times") {
                game.activateJacePlusTwo().error shouldNotBe null
            }
        }

        test("chapter II: puts a planeswalker card with mana value 6 or less from hand onto the battlefield") {
            val game = builder()
                .withCardInHand(1, "Jace Beleren")
                .withCardInHand(1, "Karn Liberated") // mana value 7 — not eligible
                .build()

            game.castAndChoose(2)
            val select = game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            withClue("only the mana value 6-or-less planeswalker is offered") {
                select.options shouldContainExactly game.findCardsInHand(1, "Jace Beleren")
            }
            game.selectCards(select.options).error shouldBe null
            game.resolveStack()
            game.findPermanent("Jace Beleren") shouldNotBe null
            game.findPermanent("Karn Liberated") shouldBe null
        }

        test("chapter I: scry 4, then a revealed planeswalker card goes to hand") {
            val game = builder()
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Jace Beleren")
                .withCardInLibrary(1, "Island")
                .withCardInLibrary(1, "Island")
                .build()
            val jace = game.state.getLibrary(game.player1Id).first {
                game.state.getEntity(it)?.get<com.wingedsheep.engine.state.components.identity.CardComponent>()?.name == "Jace Beleren"
            }

            game.castAndChoose(1)
            game.getPendingDecision().shouldBeInstanceOf<SelectCardsDecision>()
            game.skipSelection().error shouldBe null
            val reorder = game.getPendingDecision().shouldBeInstanceOf<ReorderLibraryDecision>()
            withClue("scry looks at four cards") { reorder.cards.size shouldBe 4 }
            game.submitDecision(OrderedResponse(reorder.id, listOf(jace) + (reorder.cards - jace))).error shouldBe null

            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            withClue("the revealed planeswalker card was put into hand") {
                game.findCardsInHand(1, "Jace Beleren") shouldContainExactly listOf(jace)
            }
        }
    }
}
