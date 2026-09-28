package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ptk.cards.*
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class HuntingCheetahScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(HuntingCheetah)
    }

    fun settle(driver: GameTestDriver, yes: Boolean = true) {
        var guard = 0
        while (guard++ < 20 && (driver.state.stack.isNotEmpty() || driver.pendingDecision != null)) {
            when (val d = driver.pendingDecision) {
                null -> driver.bothPass()
                is YesNoDecision -> driver.submitYesNo(d.playerId, yes)
                is SelectCardsDecision -> driver.submitCardSelection(d.playerId, d.options.take(d.minSelections.coerceAtLeast(1)))
                else -> error("unexpected decision $d")
            }
        }
    }


    fun hitOpponent(driver: GameTestDriver, me: com.wingedsheep.sdk.model.EntityId, opponent: com.wingedsheep.sdk.model.EntityId, attacker: com.wingedsheep.sdk.model.EntityId) {
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(attacker), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareNoBlockers(opponent)
        driver.passPriorityUntil(Step.COMBAT_DAMAGE)
    }

    fun run(yes: Boolean): Pair<Int, Int> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val cheetah = driver.putCreatureOnBattlefield(me, "Hunting Cheetah")
        driver.removeSummoningSickness(cheetah)
        val before = driver.getHandSize(me)
        hitOpponent(driver, me, opponent, cheetah)
        settle(driver, yes)
        driver.getLifeTotal(opponent) shouldBe 18
        val forestsInHand = driver.getHand(me).count { driver.getCardName(it) == "Forest" }
        return (driver.getHandSize(me) - before) to forestsInHand
    }

    test("dealing damage to an opponent fetches a Forest into hand") {
        val (delta, _) = run(true)
        delta shouldBe 1
    }

    test("declining the may leaves the library alone") {
        val (delta, _) = run(false)
        delta shouldBe 0
    }
})
