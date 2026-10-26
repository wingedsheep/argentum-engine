package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Cosmos Charger (KHM #51) — "Foretelling cards from your hand costs {1} less and can be done on
 * any player's turn." Foretell {2}{U}.
 */
class CosmosChargerScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("foretelling costs {1} and can be done on the opponent's turn") {
        val d = driver()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        d.putCreatureOnBattlefield(p, "Cosmos Charger")
        val cup = d.putCardInHand(p, "Poison the Cup")
        d.legalActions(p).single { it.action == ForetellCard(p, cup) }.manaCostString shouldBe "{1}"

        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriority(opp)
        d.giveColorlessMana(p, 1)
        d.submitSuccess(ForetellCard(p, cup))
        d.getExile(p).contains(cup) shouldBe true
    }

    test("it doesn't discount foretelling itself from hand") {
        val d = driver()
        val p = d.activePlayer!!
        val charger = d.putCardInHand(p, "Cosmos Charger")
        d.legalActions(p).single { it.action == ForetellCard(p, charger) }.manaCostString shouldBe "{2}"
    }
})
