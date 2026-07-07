package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GoldfuryStrider
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goldfury Strider (LCI #152).
 *
 * Goldfury Strider {4}{R}
 * Artifact Creature — Golem 3/5
 * Trample
 * Tap two untapped artifacts and/or creatures you control: Target creature gets +2/+0 until end
 * of turn. Activate only as a sorcery.
 *
 * Covers:
 *  - Static keyword: Goldfury Strider has trample.
 *  - Happy path: two untapped creatures available → activation taps both and target gets +2/+0.
 *  - Sorcery-speed gate: activation is rejected outside a main phase with an empty stack.
 *  - Cost gate: activation is rejected when fewer than two matching untapped permanents are available.
 */
class GoldfuryStriderScenarioTest : FunSpec({

    val abilityId = GoldfuryStrider.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(GoldfuryStrider)
        return driver
    }

    fun GameTestDriver.power(id: EntityId): Int = state.projectedState.getPower(id) ?: 0
    fun GameTestDriver.toughness(id: EntityId): Int = state.projectedState.getToughness(id) ?: 0

    // ─────────────────────────────────────────────────────────────────────────
    // Test 1: Goldfury Strider has trample
    // ─────────────────────────────────────────────────────────────────────────
    test("Goldfury Strider has trample") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        val strider = driver.putCreatureOnBattlefield(me, "Goldfury Strider")

        driver.state.projectedState.hasKeyword(strider, Keyword.TRAMPLE) shouldBe true
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 2: Tapping two creatures pumps target creature +2/+0 until end of turn
    // ─────────────────────────────────────────────────────────────────────────
    test("tapping two untapped creatures gives target creature +2/+0 until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val strider = driver.putCreatureOnBattlefield(me, "Goldfury Strider")
        driver.removeSummoningSickness(strider)
        val bear1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bear1)
        val bear2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bear2)

        // Target is the strider itself (any creature is legal)
        driver.power(strider) shouldBe 3
        driver.toughness(strider) shouldBe 5

        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = strider,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(bear1, bear2)),
                targets = listOf(ChosenTarget.Permanent(strider))
            )
        )
        driver.bothPass() // resolve the ability

        // Both permanents tapped to pay the cost.
        driver.isTapped(bear1) shouldBe true
        driver.isTapped(bear2) shouldBe true

        // Target received +2/+0 until end of turn.
        driver.power(strider) shouldBe 5
        driver.toughness(strider) shouldBe 5
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 3: Cannot be activated at instant speed (sorcery-speed gate)
    // ─────────────────────────────────────────────────────────────────────────
    test("cannot be activated at instant speed") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        val strider = driver.putCreatureOnBattlefield(me, "Goldfury Strider")
        driver.removeSummoningSickness(strider)
        val bear1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bear1)
        val bear2 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bear2)
        val target = driver.putCreatureOnBattlefield(me, "Grizzly Bears")

        // Advance into the declare-attackers step — not a main phase with an empty stack.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = strider,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(bear1, bear2)),
                targets = listOf(ChosenTarget.Permanent(target))
            )
        )
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test 4: Cannot be activated without two matching untapped permanents
    // ─────────────────────────────────────────────────────────────────────────
    test("cannot be activated without two matching untapped permanents") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val me = driver.activePlayer!!

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val strider = driver.putCreatureOnBattlefield(me, "Goldfury Strider")
        driver.removeSummoningSickness(strider)
        // Only one other creature — cost requires two
        val bear1 = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        driver.removeSummoningSickness(bear1)

        driver.submitExpectFailure(
            ActivateAbility(
                playerId = me,
                sourceId = strider,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(tappedPermanents = listOf(bear1)),
                targets = listOf(ChosenTarget.Permanent(strider))
            )
        )
    }
})
