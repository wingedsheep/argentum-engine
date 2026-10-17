package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.TheMindskinner
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.DamageCantBePrevented
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import com.wingedsheep.engine.core.Outcome

/**
 * Scenario tests for The Mindskinner (DSK 66).
 *
 * 1. "The Mindskinner can't be blocked." — combat damage path: an unblocked 10/1 deals 10 combat
 *    damage to an opponent, which is replaced — the opponent loses no life and mills 10 instead.
 * 2. "If a source you control would deal damage to an opponent, prevent that damage and each
 *    opponent mills that many cards." — noncombat damage from a source you control is likewise
 *    replaced with milling.
 * 3. Damage that can't be prevented is dealt in full, and each opponent still mills that many.
 */
class TheMindskinnerTest : FunSpec({

    // A 0/1 with an ETB "deal 3 damage to target opponent" — a noncombat source you control.
    val zapper = card("Test Zapper") {
        manaCost = "{1}"
        typeLine = "Creature — Wizard"
        power = 0
        toughness = 1
        oracleText = "When Test Zapper enters, it deals 3 damage to target opponent."
        triggeredAbility {
            trigger = Triggers.self.enters()
            val foe = target(Targets.Opponent)
            effect = Effects.DealDamage(3, foe)
        }
    }

    val leyline = card("Test Leyline") {
        manaCost = "{0}"
        typeLine = "Enchantment"
        oracleText = "Damage can't be prevented."
        replacementEffect(DamageCantBePrevented())
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(TheMindskinner, zapper, leyline))
        d.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
            startingLife = 20,
        )
        return d
    }

    test("unblocked combat damage to an opponent is replaced — no life lost, opponent mills that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val skinner = d.putPermanentOnBattlefield(p1, "The Mindskinner")
        d.removeSummoningSickness(skinner)

        val libBefore = d.state.getLibrary(p2).size
        val gyBefore = d.getGraveyard(p2).size

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(skinner), p2)
        d.bothPass()
        // The Mindskinner can't be blocked — no blocker can be declared.
        d.declareNoBlockers(p2)
        d.bothPass()
        d.bothPass()

        // Combat damage (10) was replaced: the opponent loses no life and mills 10.
        d.assertLifeTotal(p2, 20)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 10
        (d.getGraveyard(p2).size - gyBefore) shouldBe 10
    }

    test("noncombat damage from a source you control is replaced with milling") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(p1, "The Mindskinner")

        val libBefore = d.state.getLibrary(p2).size
        val gyBefore = d.getGraveyard(p2).size

        // Cast the zapper; its ETB deals 3 to the opponent — a source p1 controls.
        val zap = d.putCardInHand(p1, "Test Zapper")
        d.giveColorlessMana(p1, 1)
        d.castSpell(p1, zap).outcome shouldBe Outcome.Done
        var guard = 0
        while ((d.state.stack.isNotEmpty() || d.state.pendingDecision is ChooseTargetsDecision) && guard++ < 20) {
            if (d.state.pendingDecision is ChooseTargetsDecision) {
                d.submitTargetSelection(p1, listOf(p2))
            } else {
                d.bothPass()
            }
        }

        // The 3 damage was replaced: opponent loses no life and mills 3.
        d.assertLifeTotal(p2, 20)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 3
        (d.getGraveyard(p2).size - gyBefore) shouldBe 3
    }

    test("unpreventable combat damage is dealt, and the opponent still mills that many") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val skinner = d.putPermanentOnBattlefield(p1, "The Mindskinner")
        d.removeSummoningSickness(skinner)
        d.putPermanentOnBattlefield(p1, "Test Leyline")
        val libBefore = d.state.getLibrary(p2).size

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(skinner), p2)
        d.bothPass()
        d.declareNoBlockers(p2)
        d.bothPass()
        d.bothPass()

        d.assertLifeTotal(p2, 10)
        (libBefore - d.state.getLibrary(p2).size) shouldBe 10
    }
})
