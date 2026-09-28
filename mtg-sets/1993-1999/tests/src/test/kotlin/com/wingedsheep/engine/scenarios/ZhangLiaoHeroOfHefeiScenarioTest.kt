package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.*
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class ZhangLiaoHeroOfHefeiScenarioTest : FunSpec({
    test("damage to an opponent makes them discard a card") {
        val driver = GameTestDriver().apply {
            registerCards(TestCards.all)
            registerCard(ZhangLiaoHeroOfHefei)
        }
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val zhang = driver.putCreatureOnBattlefield(me, "Zhang Liao, Hero of Hefei")
        driver.removeSummoningSickness(zhang)
        val before = driver.getHandSize(opponent)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(zhang), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val d = driver.pendingDecision) {
                null -> driver.bothPass()
                is SelectCardsDecision -> driver.submitCardSelection(d.playerId, d.options.take(d.minSelections.coerceAtLeast(1)))
                else -> error("unexpected decision $d")
            }
        }

        driver.getLifeTotal(opponent) shouldBe 17
        driver.getHandSize(opponent) shouldBe before - 1
    }
})
