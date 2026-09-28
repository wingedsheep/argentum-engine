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

class WeiNightRaidersScenarioTest : FunSpec({
    fun createDriver() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCard(WeiNightRaiders)
        registerCard(ShuDefender)
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

    test("unblocked, it makes the opponent discard a card") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val raiders = driver.putCreatureOnBattlefield(me, "Wei Night Raiders")
        driver.removeSummoningSickness(raiders)
        val before = driver.getHandSize(opponent)
        val gy = driver.getGraveyard(opponent).size

        hitOpponent(driver, me, opponent, raiders)
        settle(driver)

        driver.getLifeTotal(opponent) shouldBe 18
        driver.getHandSize(opponent) shouldBe before - 1
        driver.getGraveyard(opponent).size shouldBe gy + 1
    }

    test("a non-horsemanship creature cannot block it, so no damage is dodged") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val raiders = driver.putCreatureOnBattlefield(me, "Wei Night Raiders")
        driver.removeSummoningSickness(raiders)
        val blocker = driver.putCreatureOnBattlefield(opponent, "Shu Defender")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(raiders), opponent).outcome shouldBe Outcome.Done
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        withClue("horsemanship") {
            (driver.declareBlockers(opponent, mapOf(blocker to listOf(raiders))).outcome == Outcome.Done) shouldBe false
        }
    }
})
