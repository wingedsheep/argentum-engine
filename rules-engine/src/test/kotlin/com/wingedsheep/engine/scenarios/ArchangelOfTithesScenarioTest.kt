package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ori.cards.ArchangelOfTithes
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Archangel of Tithes (ORI #4, reprinted OTJ #2).
 *
 * {1}{W}{W}{W} 3/5 Angel, Flying.
 * - As long as it is untapped, creatures can't attack you or planeswalkers you control unless
 *   their controller pays {1} for each of those creatures (gated [AttackTax]).
 * - As long as it is attacking, creatures can't block unless their controller pays {1} for each
 *   of those creatures ([BlockTax]).
 *
 * Exercises the two new combat-tax gates: the untapped condition on the attack tax and the
 * attacking condition on the block tax.
 */
class ArchangelOfTithesScenarioTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    val FlyingBear = CardDefinition.creature(
        name = "Test Flying Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        keywords = setOf(Keyword.FLYING),
        oracleText = "Flying"
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ArchangelOfTithes, Bear, FlyingBear))
        return driver
    }

    test("untapped Archangel taxes attacks against its controller") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        driver.putCreatureOnBattlefield(defender, "Archangel of Tithes")
        val bear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        driver.removeSummoningSickness(bear)
        // Give the attacker a single untapped Plains so they *can* pay the tax.
        driver.putPermanentOnBattlefield(attacker, "Plains")

        // Advance to the attacker's (active player's) declare-attackers step.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        val result = driver.declareAttackers(attacker, listOf(bear), defender)
        // The untapped Archangel forces a {1} attack tax → pause for mana-source confirmation.
        result.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }

    test("tapped Archangel does not tax attacks") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        val archangel = driver.putCreatureOnBattlefield(defender, "Archangel of Tithes")
        driver.tapPermanent(archangel)
        val bear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        driver.removeSummoningSickness(bear)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = driver.declareAttackers(attacker, listOf(bear), defender)

        // No tax while Archangel is tapped → attack resolves with no mana decision.
        result.isSuccess shouldBe true
        (result.newState.pendingDecision is SelectManaSourcesDecision) shouldBe false
    }

    test("untapped Archangel still pauses when the tax cannot be paid") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), startingLife = 20)

        val attacker = driver.activePlayer!!
        val defender = driver.getOpponent(attacker)

        driver.putCreatureOnBattlefield(defender, "Archangel of Tithes")
        val bear = driver.putCreatureOnBattlefield(attacker, "Test Bear")
        driver.removeSummoningSickness(bear)
        // Attacker has no mana sources, so the {1} tax cannot be paid — but a tax is still owed,
        // so the engine raises the confirmation pause (the player must decline, a clean no-op).

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val result = driver.declareAttackers(attacker, listOf(bear), defender)

        result.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }

    test("attacking Archangel taxes the opponent's blockers") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 30, "Forest" to 30), startingLife = 20)

        val attacker = driver.activePlayer!!
        val blockerPlayer = driver.getOpponent(attacker)

        val archangel = driver.putCreatureOnBattlefield(attacker, "Archangel of Tithes")
        driver.removeSummoningSickness(archangel)
        // The blocker must be able to block a flier (Archangel has flying).
        val blocker = driver.putCreatureOnBattlefield(blockerPlayer, "Test Flying Bear")
        // The blocking player needs untapped mana so the block can pause for payment.
        driver.putPermanentOnBattlefield(blockerPlayer, "Plains")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        // Archangel attacks → it becomes "attacking", enabling the block tax. No attack tax
        // against the opponent (they have no AttackTax permanent).
        val attackResult = driver.declareAttackers(attacker, listOf(archangel), blockerPlayer)
        attackResult.isSuccess shouldBe true

        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        val blockResult = driver.declareBlockers(blockerPlayer, mapOf(blocker to listOf(archangel)))

        // The attacking Archangel forces a {1} block tax → pause for mana-source confirmation.
        blockResult.newState.pendingDecision.shouldBeInstanceOf<SelectManaSourcesDecision>()
    }
})
