package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.AncestralRecall
import com.wingedsheep.mtg.sets.definitions.lea.cards.SerraAngel
import com.wingedsheep.mtg.sets.definitions.lea.cards.ShivanDragon
import com.wingedsheep.mtg.sets.definitions.woe.cards.WillScionOfPeace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Will, Scion of Peace (WOE #218) — {1}{W}{U} 2/4 Legendary Creature — Human Wizard.
 *
 *   Vigilance
 *   {T}: Spells you cast this turn that are white and/or blue cost {X} less to cast, where X is
 *   the amount of life you gained this turn. Activate only as a sorcery.
 *
 * Covers the new [com.wingedsheep.sdk.scripting.effects.ReduceSpellCostsThisTurnEffect]:
 * the discount lands on matching spells only, is fixed at resolution (life gained afterwards
 * doesn't raise it), survives Will leaving the battlefield, and expires with the turn.
 */
class WillScionOfPeaceScenarioTest : FunSpec({

    val abilityId = WillScionOfPeace.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(WillScionOfPeace, SerraAngel, ShivanDragon, AncestralRecall))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Pass until the turn rolls over — the UNTAP step never holds priority. */
    fun endTurn(driver: GameTestDriver) {
        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard++ < 300) driver.bothPass()
    }

    /** Effective cost of [cardName] for [driver]'s player 1, in total mana. */
    fun costOf(driver: GameTestDriver, cardName: String): Int {
        val calculator = CostCalculator(driver.cardRegistry)
        val cardDef = driver.cardRegistry.requireCard(cardName)
        return calculator.calculateEffectiveCost(driver.state, cardDef, driver.player1).cmc
    }

    /** Gain [amount] life for player 1 through the shared life-gain accumulator. */
    fun gainLife(driver: GameTestDriver, amount: Int) {
        driver.setLifeTotal(driver.player1, driver.getLifeTotal(driver.player1) + amount)
        driver.replaceState(
            com.wingedsheep.engine.handlers.effects.DamageUtils
                .markLifeGainedThisTurn(driver.state, driver.player1, amount)
        )
    }

    test("a white spell costs X less, where X is the life gained this turn") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        val before = costOf(driver, "Serra Angel") // {3}{W}{W} = 5
        before shouldBe 5

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        costOf(driver, "Serra Angel") shouldBe 2 // {3} generic wiped out, {W}{W} untouched
    }

    test("only generic mana is reduced — a spell that is all colored keeps its cost") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 5)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        // Ancestral Recall is {U} — no generic component for the discount to eat (CR 601.2f).
        costOf(driver, "Ancestral Recall") shouldBe 1
    }

    test("spells that are neither white nor blue are not discounted") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        // Shivan Dragon is {4}{R}{R} — red, so the discount must not apply.
        costOf(driver, "Shivan Dragon") shouldBe 6
    }

    test("X is locked in at resolution — life gained afterwards does not deepen the discount") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 2)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()
        costOf(driver, "Serra Angel") shouldBe 3

        gainLife(driver, 4) // total gained this turn is now 6...
        costOf(driver, "Serra Angel") shouldBe 3 // ...but the discount stays at the resolved 2
    }

    test("activating with no life gained installs nothing") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        driver.state.turnSpellCostReductions.isEmpty() shouldBe true
        costOf(driver, "Serra Angel") shouldBe 5
    }

    test("the discount survives Will leaving the battlefield") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        driver.moveToGraveyard(will)

        // The ability already resolved; its effect lasts the turn regardless of its source.
        costOf(driver, "Serra Angel") shouldBe 2
    }

    test("the discount does not carry into the next turn") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()
        costOf(driver, "Serra Angel") shouldBe 2

        endTurn(driver)

        driver.state.turnSpellCostReductions.isEmpty() shouldBe true
        costOf(driver, "Serra Angel") shouldBe 5
    }

    test("the opponent's white spells are not discounted") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        driver.submitSuccess(ActivateAbility(driver.player1, will, abilityId))
        driver.bothPass()

        val calculator = CostCalculator(driver.cardRegistry)
        val serra = driver.cardRegistry.requireCard("Serra Angel")
        calculator.calculateEffectiveCost(driver.state, serra, driver.player2).cmc shouldBe 5
    }

    test("Will has vigilance") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")

        driver.state.projectedState
            .hasKeyword(will, com.wingedsheep.sdk.core.Keyword.VIGILANCE) shouldBe true
        driver.state.projectedState.getPower(will) shouldBe 2
        driver.state.projectedState.getToughness(will) shouldBe 4
    }

    test("the ability is sorcery-speed only") {
        val driver = newDriver()
        val will = driver.putCreatureOnBattlefield(driver.player1, "Will, Scion of Peace")
        driver.removeSummoningSickness(will)
        gainLife(driver, 3)

        // Move to a step where sorcery-speed activation is illegal.
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.submitExpectFailure(ActivateAbility(driver.player1, will, abilityId))

        // Unused, but keeps the mana-availability shape identical to the other cases.
        driver.giveMana(driver.player1, Color.WHITE, 1)
    }
})
