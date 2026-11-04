package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CombatResolutionDecision

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Chant of Vitu-Ghazi (RAV #7) — "Prevent all damage that would be dealt by creatures this turn.
 * You gain life equal to the damage prevented this way."
 *
 * Covers the source-side group shield with `gainLifeFromPrevented`: combat damage in both
 * directions is prevented and credited as one gain, a creature's noncombat damage is prevented
 * and credited too, and a noncreature source (Shock) is untouched.
 */
class ChantOfVituGhaziScenarioTest : FunSpec({

    // A creature whose ETB deals 3 damage to target player — noncombat damage *by a creature*.
    val zapper = card("Chant Test Zapper") {
        manaCost = "{1}"
        typeLine = "Creature — Wizard"
        power = 0
        toughness = 1
        oracleText = "When this creature enters, it deals 3 damage to target player."
        triggeredAbility {
            trigger = Triggers.self.enters()
            val victim = target(Targets.Player)
            effect = Effects.DealDamage(3, victim)
        }
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(zapper))
        d.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingLife = 20)
        return d
    }

    fun GameTestDriver.castChant(caster: EntityId) {
        val chant = putCardInHand(caster, "Chant of Vitu-Ghazi")
        giveMana(caster, Color.WHITE, 2)
        giveColorlessMana(caster, 6)
        castSpell(caster, chant).outcome shouldBe Outcome.Done
        bothPass()
    }

    test("combat damage by creatures in both directions is prevented and gained as life") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        val giant = d.putCreatureOnBattlefield(p1, "Hill Giant")
        d.removeSummoningSickness(bears)
        d.removeSummoningSickness(giant)
        val blocker = d.putCreatureOnBattlefield(p2, "Centaur Courser")

        d.castChant(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(bears, giant), p2)
        d.bothPass()
        d.declareBlockers(p2, mapOf(blocker to listOf(bears)))
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Hill Giant's 3 to the player, Bears' 2 to the Courser and the Courser's 3 to the Bears
        // are all prevented: nobody is damaged, and the caster gains 3 + 2 + 3 = 8.
        d.assertLifeTotal(p2, 20)
        d.assertLifeTotal(p1, 28)
        d.findPermanent(p1, "Grizzly Bears") shouldBe bears
        d.findPermanent(p2, "Centaur Courser") shouldBe blocker
    }

    test("a creature's noncombat damage is prevented and gained; a noncreature source is not") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.castChant(p1)

        // A creature's ETB deals 3 to p2 — prevented, and p1 (Chant's controller) gains 3.
        val zap = d.putCardInHand(p1, "Chant Test Zapper")
        d.giveColorlessMana(p1, 1)
        d.castSpell(p1, zap).outcome shouldBe Outcome.Done
        var guard = 0
        while ((d.state.stack.isNotEmpty() || d.state.pendingDecision is ChooseTargetsDecision) && guard++ < 20) {
            if (d.state.pendingDecision is ChooseTargetsDecision) d.submitTargetSelection(p1, listOf(p2))
            else d.bothPass()
        }
        d.assertLifeTotal(p2, 20)
        d.assertLifeTotal(p1, 23)

        // Shock is not a creature: its damage is dealt and gains nothing.
        val shock = d.putCardInHand(p1, "Shock")
        d.giveMana(p1, Color.RED, 1)
        d.castSpell(p1, shock, listOf(p2)).outcome shouldBe Outcome.Done
        d.bothPass()
        d.assertLifeTotal(p2, 18)
        d.assertLifeTotal(p1, 23)
    }

    test("with a damage doubler, the life gained is the doubled damage actually prevented") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putPermanentOnBattlefield(p1, "Gratuitous Violence")
        val giant = d.putCreatureOnBattlefield(p1, "Hill Giant")
        d.removeSummoningSickness(giant)

        d.castChant(p1)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(giant), p2)
        d.bothPass()
        d.declareNoBlockers(p2)
        d.passPriorityUntil(Step.COMBAT_DAMAGE)
        if (d.pendingDecision is CombatResolutionDecision) d.confirmCombatDamage().error shouldBe null
        val order = d.pendingDecision.shouldBeInstanceOf<ChooseOptionDecision>()
        order.playerId shouldBe p2
        val doubleFirst = order.options.indexOfFirst { it.contains("Gratuitous Violence") }
        (doubleFirst >= 0) shouldBe true
        d.submitDecision(p2, OptionChosenResponse(order.id, doubleFirst)).error shouldBe null
        d.pendingDecision shouldBe null
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        // Hill Giant would deal 3 doubled to 6; all 6 is prevented and gained.
        d.assertLifeTotal(p2, 20)
        d.assertLifeTotal(p1, 26)
    }

    test("without the Chant, creature damage is dealt normally") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val giant = d.putCreatureOnBattlefield(p1, "Hill Giant")
        d.removeSummoningSickness(giant)

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(giant), p2)
        d.bothPass()
        d.declareNoBlockers(p2)
        d.passPriorityUntil(Step.POSTCOMBAT_MAIN)

        d.assertLifeTotal(p2, 17)
        d.assertLifeTotal(p1, 20)
    }
})
