package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * `CantCastSpellsEffect.moreAllowed` — "you can cast only N more spells this turn". The allowance
 * is counted from resolution: spells cast earlier in the turn (including the restricting spell
 * itself) don't use it up, the next N casts go through, and the ban binds after that until it
 * expires at end of turn.
 */
class CastSpellsAllowanceTest : FunSpec({
    val twoMore = card("Allowance Two More") {
        manaCost = "{0}"; typeLine = "Sorcery"
        spell { effect = Effects.CantCastSpells(EffectTarget.Controller, moreAllowed = 2) }
    }
    val filler = card("Allowance Filler") {
        manaCost = "{0}"; typeLine = "Instant"
        spell { effect = Effects.GainLife(1) }
    }

    fun setup(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(twoMore, filler))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard++ < 20) bothPass()
    }

    fun GameTestDriver.castFiller(player: EntityId): Boolean {
        val id = putCardInHand(player, filler.name)
        val offered = legalActions(player).any { (it.action as? CastSpell)?.cardId == id }
        if (!offered) {
            submitExpectFailure(CastSpell(player, id))
            return false
        }
        submitSuccess(CastSpell(player, id))
        resolveStack()
        return true
    }

    test("earlier spells don't use the allowance; exactly N more casts go through, then the ban binds") {
        val d = setup(); val me = d.activePlayer!!
        d.castFiller(me) shouldBe true
        d.submitSuccess(CastSpell(me, d.putCardInHand(me, twoMore.name)))
        d.resolveStack()
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe false
    }

    test("the restriction only binds the affected player and ends with the turn") {
        val d = setup(); val me = d.activePlayer!!; val opp = d.getOpponent(me)
        d.submitSuccess(CastSpell(me, d.putCardInHand(me, twoMore.name)))
        d.resolveStack()
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe false
        // The opponent, holding priority after this player passes, is unaffected.
        d.passPriority(me)
        d.castFiller(opp) shouldBe true
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, opp)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN, me)
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe true
        d.castFiller(me) shouldBe true
    }
})
