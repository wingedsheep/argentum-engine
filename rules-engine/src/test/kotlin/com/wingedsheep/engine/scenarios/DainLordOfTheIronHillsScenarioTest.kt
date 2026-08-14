package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.mechanics.enduringstory.EnduringStoryService
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Dáin, Lord of the Iron Hills — "As long as you have an enduring story, creatures can't attack you
 * unless their controller pays {1} for each of those creatures."
 *
 * A Ghostly Prison behind the storied gate. The gate rides `AttackTax.condition` rather than a
 * `ConditionalStaticAbility` wrapper, and that is exactly what these tests pin:
 * `AttackPhaseManager.calculateTotalAttackTax` scans `cardDef.staticAbilities` **raw**, matching only
 * a bare `AttackTax`, so a wrapped ability would tax nothing at all and the "off" case below would
 * pass for the wrong reason. Asserting both directions is what separates "gate works" from "gate
 * never fires".
 */
class DainLordOfTheIronHillsScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        return d
    }

    test("without an enduring story Dáin taxes nothing") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), skipMulligans = true)
        val attacker = d.activePlayer!!
        val defender = d.getOpponent(attacker)

        // Dáin is legendary and so counts toward his own threshold, but he is the only one of the
        // three — the tax stays dormant.
        d.putCreatureOnBattlefield(defender, "Dáin, Lord of the Iron Hills")
        val bear = d.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        d.removeSummoningSickness(bear)

        EnduringStoryService.has(d.state, defender) shouldBe false

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = d.declareAttackers(attacker, listOf(bear), defender)

        withClue("no enduring story → no tax, so the attack goes through unpaused") {
            result.isSuccess shouldBe true
            (result.newState.pendingDecision is SelectManaSourcesDecision) shouldBe false
        }
    }

    test("with an enduring story Dáin taxes each attacker {1}") {
        val d = driver()
        d.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), skipMulligans = true)
        val attacker = d.activePlayer!!
        val defender = d.getOpponent(attacker)

        d.putCreatureOnBattlefield(defender, "Dáin, Lord of the Iron Hills")
        d.putCreatureOnBattlefield(defender, "Ori, Keeper of Songs")
        d.putCreatureOnBattlefield(defender, "Thorin Oakenshield")
        val bear = d.putCreatureOnBattlefield(attacker, "Grizzly Bears")
        d.removeSummoningSickness(bear)
        // A single untapped land so the attacker can actually pay the {1} they now owe.
        d.putLandOnBattlefield(attacker, "Forest")

        EnduringStoryService.has(d.state, defender) shouldBe true

        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = d.declareAttackers(attacker, listOf(bear), defender)

        withClue("the tax is owed, so declaring attackers pauses for mana") {
            result.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
        }
    }
})
