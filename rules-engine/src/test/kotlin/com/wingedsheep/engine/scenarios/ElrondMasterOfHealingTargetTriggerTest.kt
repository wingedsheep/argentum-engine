package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.ElrondMasterOfHealing
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Elrond, Master of Healing (LTR) — second ability: "Whenever a creature you control **with a
 * +1/+1 counter on it** becomes the target of a spell or ability an opponent controls, you may
 * draw a card."
 *
 * The `withCounter(+1/+1)` filter on the becomes-target trigger must be honored — the trigger
 * fires only for a counter-bearing creature, not any creature you control.
 */
class ElrondMasterOfHealingTargetTriggerTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(ElrondMasterOfHealing)
        )
        return d
    }

    test("opponent targeting a creature WITH a +1/+1 counter offers the may-draw") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(opp, "Elrond, Master of Healing")
        val counterBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears")
        d.addComponent(counterBear, CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 1)))

        val giantGrowth = d.putCardInHand(active, "Giant Growth")
        d.giveMana(active, Color.GREEN, 1)
        d.castSpellWithTargets(active, giantGrowth, listOf(ChosenTarget.Permanent(counterBear))).error shouldBe null
        d.bothPass() // resolve targeting; Elrond's trigger goes on the stack

        // Elrond's controller (opp) should be offered the may-draw.
        d.pendingDecision shouldNotBe null
    }

    test("opponent targeting a creature WITHOUT a +1/+1 counter does NOT trigger") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(opp, "Elrond, Master of Healing")
        val plainBear = d.putCreatureOnBattlefield(opp, "Grizzly Bears") // no counter

        val oppHandBefore = d.getHandSize(opp)
        val giantGrowth = d.putCardInHand(active, "Giant Growth")
        d.giveMana(active, Color.GREEN, 1)
        d.castSpellWithTargets(active, giantGrowth, listOf(ChosenTarget.Permanent(plainBear))).error shouldBe null
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        d.getHandSize(opp) shouldBe oppHandBefore // no Elrond draw
    }
})
