package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.ShuYunTheSilentTempest
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Shu Yun, the Silent Tempest (Fate Reforged #52) — {2}{U} Legendary Creature — Human Monk 3/2.
 *
 * "Prowess. Whenever you cast a noncreature spell, you may pay {R/W}{R/W}. If you do, target
 *  creature gains double strike until end of turn."
 *
 * Covers paying the hybrid cost (with Plains) and declining it: double strike only when paid,
 * the lands untouched when declined, and prowess either way.
 */
class ShuYunTheSilentTempestScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + ShuYunTheSilentTempest)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast Giant Growth on [bears], point Shu Yun's trigger at [bears], and answer the payment with [pay]. */
    fun GameTestDriver.growAndResolve(me: EntityId, bears: EntityId, pay: Boolean): Boolean {
        val growth = putCardInHand(me, "Giant Growth")
        giveMana(me, Color.GREEN, 1)
        castSpell(me, growth, listOf(bears)).error shouldBe null
        var asked = false
        var guard = 0
        while (guard++ < 30) {
            when (val decision = pendingDecision) {
                is OrderTriggersDecision ->
                    submitDecision(me, TriggerOrderResponse(decision.id, decision.triggers.indices.toList()))
                is ChooseTargetsDecision -> {
                    decision.legalTargets.getValue(0) shouldContain bears
                    submitTargetSelection(me, listOf(bears)).error shouldBe null
                }
                is YesNoDecision -> {
                    asked = true
                    submitYesNo(me, pay).error shouldBe null
                }
                is SelectManaSourcesDecision -> {
                    asked = true
                    submitManaAutoPayOrDecline(me, pay).error shouldBe null
                }
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> error("unexpected decision $decision")
            }
        }
        return asked
    }

    test("paying {R/W}{R/W} gives the target creature double strike") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val shuYun = driver.putCreatureOnBattlefield(me, "Shu Yun, the Silent Tempest")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val plains = List(2) { driver.putLandOnBattlefield(me, "Plains") }

        driver.growAndResolve(me, bears, pay = true) shouldBe true

        val projected = driver.state.projectedState
        projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe true
        projected.getPower(bears) shouldBe 5
        withClue("the hybrid cost was paid with both Plains") { plains.all { driver.isTapped(it) } shouldBe true }
        withClue("prowess") { projected.getPower(shuYun) shouldBe 4 }
    }

    test("declining the payment grants nothing") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val shuYun = driver.putCreatureOnBattlefield(me, "Shu Yun, the Silent Tempest")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val plains = List(2) { driver.putLandOnBattlefield(me, "Plains") }

        driver.growAndResolve(me, bears, pay = false) shouldBe true

        val projected = driver.state.projectedState
        projected.hasKeyword(bears, Keyword.DOUBLE_STRIKE) shouldBe false
        plains.none { driver.isTapped(it) } shouldBe true
        withClue("prowess still triggers") { projected.getPower(shuYun) shouldBe 4 }
    }
})
