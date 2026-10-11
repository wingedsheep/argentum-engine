package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.c21.cards.PromiseOfLoyalty
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Promise of Loyalty (C21) — {4}{W} Sorcery.
 *
 * "Each player puts a vow counter on a creature they control and sacrifices the rest. Each of
 * those creatures can't attack you or planeswalkers you control for as long as it has a vow
 * counter on it."
 */
class PromiseOfLoyaltyScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + PromiseOfLoyalty)
        initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.vows(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.VOW) ?: 0

    test("each player keeps one creature with a vow counter; the vowed creature can't attack the caster") {
        val d = driver()
        val caster = d.activePlayer!!
        val opp = d.getOpponent(caster)

        val myKeeper = d.putCreatureOnBattlefield(caster, "Grizzly Bears")
        val myOther = d.putCreatureOnBattlefield(caster, "Hill Giant")
        val theirKeeper = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        val theirOther = d.putCreatureOnBattlefield(opp, "Hill Giant")
        val keep = mapOf(caster to myKeeper, opp to theirKeeper)

        val spell = d.putCardInHand(caster, "Promise of Loyalty")
        d.giveMana(caster, Color.WHITE, 5)
        d.castSpell(caster, spell)
        var guard = 0
        while ((d.pendingDecision != null || d.state.stack.isNotEmpty()) && guard++ < 50) {
            val decision = d.pendingDecision
            if (decision is SelectCardsDecision) d.submitCardSelection(decision.playerId, listOfNotNull(keep[decision.playerId]))
            else d.bothPass()
        }

        withClue("the rest are sacrificed") {
            d.getGraveyardCardNames(caster).sorted() shouldBe listOf("Hill Giant", "Promise of Loyalty")
            d.state.getBattlefield().contains(myOther) shouldBe false
            d.state.getBattlefield().contains(theirOther) shouldBe false
        }
        withClue("each keeper carries a vow counter") {
            d.vows(myKeeper) shouldBe 1
            d.vows(theirKeeper) shouldBe 1
        }

        // Move to the opponent's turn with a fresh, unvowed attacker beside the vowed one.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe opp
        d.removeSummoningSickness(theirKeeper)
        val fresh = d.putCreatureOnBattlefield(opp, "Hill Giant")
        d.removeSummoningSickness(fresh)
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        withClue("the vowed creature can't attack the caster") {
            d.declareAttackers(opp, listOf(theirKeeper), caster).error shouldNotBe null
        }
        withClue("a creature without a vow attacks freely") {
            d.declareAttackers(opp, listOf(fresh), caster).error shouldBe null
        }
    }

    test("removing the vow counter ends the restriction for good") {
        val d = driver()
        val caster = d.activePlayer!!
        val opp = d.getOpponent(caster)

        val theirKeeper = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.putCreatureOnBattlefield(caster, "Grizzly Bears")

        val spell = d.putCardInHand(caster, "Promise of Loyalty")
        d.giveMana(caster, Color.WHITE, 5)
        d.castSpell(caster, spell)
        var guard = 0
        while ((d.pendingDecision != null || d.state.stack.isNotEmpty()) && guard++ < 50) {
            val decision = d.pendingDecision
            if (decision is SelectCardsDecision) d.submitCardSelection(decision.playerId, decision.options.take(1))
            else d.bothPass()
        }
        d.vows(theirKeeper) shouldBe 1

        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.activePlayer shouldBe opp
        d.removeSummoningSickness(theirKeeper)
        val fresh = d.putCreatureOnBattlefield(opp, "Hill Giant")
        d.removeSummoningSickness(fresh)
        d.replaceState(d.state.updateEntity(theirKeeper) { c ->
            c.with(c.get<CountersComponent>()!!.withRemoved(CounterType.VOW, 1))
        })
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)

        d.declareAttackers(opp, listOf(theirKeeper), caster).error shouldBe null
    }
})
