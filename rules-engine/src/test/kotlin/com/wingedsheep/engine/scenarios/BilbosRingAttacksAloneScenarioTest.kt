package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.BilbosRing
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Bilbo's Ring (LTR) — "Whenever equipped creature attacks alone, you draw a card and you lose 1
 * life." Pins the `AttackPredicate.Alone` requirement on an **ATTACHED**-binding attack trigger:
 * ATTACHED triggers are detected by `AttachmentTriggerDetector`, which previously fired the
 * trigger on bare membership in the attacker set without applying the trigger's `requires`
 * predicates — so the draw happened even when the equipped creature attacked alongside others.
 */
class BilbosRingAttacksAloneScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(
            TestCards.all +
                com.wingedsheep.mtg.sets.tokens.PredefinedTokens.allTokens +
                listOf(BilbosRing)
        )
        return d
    }

    test("equipped creature attacking alone draws a card and loses 1 life") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bear = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        val ring = d.putPermanentOnBattlefield(active, "Bilbo's Ring")
        d.replaceState(d.state.updateEntity(ring) { it.with(AttachedToComponent(bear)) })

        val handBefore = d.getHandSize(active)
        val lifeBefore = d.getLifeTotal(active)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(bear), opp)
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        d.getHandSize(active) shouldBe handBefore + 1
        d.getLifeTotal(active) shouldBe lifeBefore - 1
    }

    test("equipped creature attacking alongside another creature does NOT draw") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true)
        val active = d.activePlayer!!
        val opp = d.getOpponent(active)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val equipped = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        val other = d.putCreatureOnBattlefield(active, "Grizzly Bears")
        d.removeSummoningSickness(equipped)
        d.removeSummoningSickness(other)
        val ring = d.putPermanentOnBattlefield(active, "Bilbo's Ring")
        d.replaceState(d.state.updateEntity(ring) { it.with(AttachedToComponent(equipped)) })

        val handBefore = d.getHandSize(active)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(active, listOf(equipped, other), opp) // not attacking alone
        repeat(6) { if (d.pendingDecision != null) d.autoResolveDecision() else d.bothPass() }

        d.getHandSize(active) shouldBe handBefore // no "attacks alone" draw
    }
})
