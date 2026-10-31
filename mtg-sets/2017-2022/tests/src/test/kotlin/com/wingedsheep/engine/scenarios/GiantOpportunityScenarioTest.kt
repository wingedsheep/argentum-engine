package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Giant Opportunity (ELD #159, {2}{G} sorcery) — "You may sacrifice two Foods. If you do, create a
 * 7/7 green Giant creature token. Otherwise, create three Food tokens."
 *
 * An Effects.MayPay gate over a two-Food sacrifice: "yes" is only offered while you control two
 * Foods, paying it makes the Giant, and declining (or controlling fewer than two) makes three Foods.
 */
class GiantOpportunityScenarioTest : ScenarioTestBase() {

    private fun setup(foods: Int) = scenario()
        .withPlayers("P1", "P2")
        .withCardInHand(1, "Giant Opportunity")
        .withLandsOnBattlefield(1, "Forest", 3)
        .apply { repeat(foods) { withCardOnBattlefield(1, "Food") } }
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("sacrificing two Foods creates a 7/7 green Giant and no Foods") {
            val game = setup(foods = 2)
            val foods = game.findAllPermanents("Food")

            game.castSpell(1, "Giant Opportunity").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true)
            if (game.state.pendingDecision is SelectCardsDecision) game.selectCards(foods)
            game.resolveStack()

            game.findAllPermanents("Food") shouldHaveSize 0
            val giant = game.findPermanent("Giant Token").shouldNotBeNull()
            game.state.projectedState.getPower(giant) shouldBe 7
            game.state.projectedState.getToughness(giant) shouldBe 7
        }

        test("declining the sacrifice keeps both Foods and creates three more") {
            val game = setup(foods = 2)

            game.castSpell(1, "Giant Opportunity").error shouldBe null
            game.resolveStack()

            game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.findAllPermanents("Food") shouldHaveSize 5
            game.findPermanent("Giant Token") shouldBe null
        }

        test("with only one Food the sacrifice can't be paid: no prompt, three Foods are created") {
            val game = setup(foods = 1)

            game.castSpell(1, "Giant Opportunity").error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe false
            game.findAllPermanents("Food") shouldHaveSize 4
            game.findPermanent("Giant Token") shouldBe null
        }
    }
}
