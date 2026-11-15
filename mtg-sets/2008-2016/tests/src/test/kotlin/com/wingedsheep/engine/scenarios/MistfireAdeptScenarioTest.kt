package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OrderTriggersDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.core.TriggerOrderResponse
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.frf.cards.MistfireAdept
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
 * Mistfire Adept (Fate Reforged #42) — {3}{U} Creature — Human Monk 3/3.
 *
 * "Prowess. Whenever you cast a noncreature spell, target creature gains flying until end of turn."
 *
 * A noncreature spell fires both prowess and the flying grant; a creature spell fires neither.
 */
class MistfireAdeptScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + MistfireAdept)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Resolve everything on the stack, pointing Mistfire Adept's trigger at [flyer]. */
    fun GameTestDriver.resolveAll(me: EntityId, flyer: EntityId?): Boolean {
        var targeted = false
        var guard = 0
        while (guard++ < 20) {
            when (val decision = pendingDecision) {
                is OrderTriggersDecision ->
                    submitDecision(me, TriggerOrderResponse(decision.id, decision.triggers.indices.toList()))
                is ChooseTargetsDecision -> {
                    val chosen = checkNotNull(flyer) { "unexpected target prompt" }
                    decision.legalTargets.getValue(0) shouldContain chosen
                    targeted = true
                    submitTargetSelection(me, listOf(chosen)).error shouldBe null
                }
                null -> if (state.stack.isNotEmpty()) bothPass() else break
                else -> error("unexpected decision $decision")
            }
        }
        return targeted
    }

    test("a noncreature spell gives target creature flying and triggers prowess") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val adept = driver.putCreatureOnBattlefield(me, "Mistfire Adept")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        val growth = driver.putCardInHand(me, "Giant Growth")
        driver.giveMana(me, Color.GREEN, 1)
        driver.castSpell(me, growth, listOf(bears)).error shouldBe null
        driver.resolveAll(me, flyer = bears) shouldBe true

        val projected = driver.state.projectedState
        projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
        projected.hasKeyword(adept, Keyword.FLYING) shouldBe false
        withClue("prowess: +1/+1 until end of turn") {
            projected.getPower(adept) shouldBe 4
            projected.getToughness(adept) shouldBe 4
        }
    }

    test("a creature spell triggers neither ability") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val adept = driver.putCreatureOnBattlefield(me, "Mistfire Adept")

        val bearsCard = driver.putCardInHand(me, "Grizzly Bears")
        driver.giveMana(me, Color.GREEN, 2)
        driver.castSpell(me, bearsCard).outcome shouldBe Outcome.Done
        driver.resolveAll(me, flyer = null) shouldBe false

        val bears = driver.findPermanent(me, "Grizzly Bears")!!
        val projected = driver.state.projectedState
        projected.hasKeyword(bears, Keyword.FLYING) shouldBe false
        projected.getPower(adept) shouldBe 3
    }
})
