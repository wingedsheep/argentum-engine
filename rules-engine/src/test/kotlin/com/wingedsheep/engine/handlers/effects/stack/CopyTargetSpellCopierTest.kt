package com.wingedsheep.engine.handlers.effects.stack

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * [CopyTargetSpellEffect.copier]: "the controller of target spell copies it" (Meletis Charlatan).
 * The copier controls the copy (CR 707.10), makes its retarget decision, and is the "you" of
 * "whenever you copy a spell" — not the player whose effect made the copy.
 */
class CopyTargetSpellCopierTest : FunSpec({

    val copyWatcher = card("Copy Watcher") {
        manaCost = "{0}"
        typeLine = "Artifact"
        triggeredAbility {
            trigger = Triggers.you.copies(GameObjectFilter.InstantOrSorcery)
            effect = Effects.GainLife(1)
        }
    }

    val draw = card("Quiet Draw") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell { effect = Effects.DrawCards(1) }
    }

    val theirCopier = card("Their Copier") {
        manaCost = "{0}"
        typeLine = "Instant"
        spell {
            val original = target(TargetFilter.InstantOrSorcerySpellOnStack)
            effect = Effects.CopyTargetSpell(original, copier = Player.ControllerOf("target spell"))
        }
    }

    fun driver(): GameTestDriver = GameTestDriver().also {
        it.registerCards(TestCards.all + listOf(copyWatcher, draw, theirCopier))
        it.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        it.passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the description names the spell's controller as the copier") {
        CopyTargetSpellEffect(copier = Player.ControllerOf("target spell")).description shouldBe
            "The controller of target spell copies it"
    }

    test("the targeted spell's controller controls the copy and fires their copy triggers") {
        val d = driver()
        val me = d.player1
        val opp = d.player2
        d.putPermanentOnBattlefield(me, copyWatcher.name)
        d.putPermanentOnBattlefield(opp, copyWatcher.name)

        val spell = d.putCardInHand(me, draw.name)
        d.castSpell(me, spell).outcome shouldBe Outcome.Done
        d.passPriority(me)
        val copierSpell = d.putCardInHand(opp, theirCopier.name)
        d.castSpellWithTargets(opp, copierSpell, listOf(ChosenTarget.Spell(spell))).outcome shouldBe Outcome.Done
        d.bothPass() // the copier resolves: the copy goes on the stack for me

        (d.pendingDecision is ChooseTargetsDecision) shouldBe false
        val copy = d.state.stack.single { d.state.getEntity(it)?.has<CopyOfComponent>() == true }
        d.state.getEntity(copy)!!.get<SpellOnStackComponent>()!!.casterId shouldBe me
        var guard = 0
        while (d.stackSize > 0 && guard++ < 20) d.bothPass()
        d.getLifeTotal(me) shouldBe 21
        d.getLifeTotal(opp) shouldBe 20
    }
})
