package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.LifeChangeReason
import com.wingedsheep.engine.handlers.effects.DamageUtils
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.state.components.player.LifeLostAmountThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.SerraAngel
import com.wingedsheep.mtg.sets.definitions.lea.cards.ShivanDragon
import com.wingedsheep.mtg.sets.definitions.woe.cards.RowanScionOfWar
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Rowan, Scion of War (WOE #211) — {1}{B}{R} 4/2 Legendary Creature — Human Wizard.
 *
 *   Menace
 *   {T}: Spells you cast this turn that are black and/or red cost {X} less to cast, where X is
 *   the amount of life you lost this turn. Activate only as a sorcery.
 *
 * The black/red twin of [WillScionOfPeaceScenarioTest]. Its own coverage focus is the new
 * life-lost *amount* accumulator (`TurnTracker.LIFE_LOST_AMOUNT`): that damage, life-loss effects
 * and life paid as a cost all feed it, and that life gained never nets against it.
 */
class RowanScionOfWarScenarioTest : FunSpec({

    val abilityId = RowanScionOfWar.activatedAbilities.first().id

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(RowanScionOfWar, SerraAngel, ShivanDragon))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Pass until the turn rolls over — the UNTAP step never holds priority. */
    fun endTurn(driver: GameTestDriver) {
        val startTurn = driver.state.turnNumber
        var guard = 0
        while (driver.state.turnNumber == startTurn && guard++ < 300) driver.bothPass()
    }

    fun costOf(driver: GameTestDriver, cardName: String): Int {
        val calculator = CostCalculator(driver.cardRegistry)
        val cardDef = driver.cardRegistry.requireCard(cardName)
        return calculator.calculateEffectiveCost(driver.state, cardDef, driver.player1).cmc
    }

    fun lifeLostAmount(driver: GameTestDriver): Int =
        driver.state.getEntity(driver.player1)?.get<LifeLostAmountThisTurnComponent>()?.amount ?: 0

    fun loseLife(driver: GameTestDriver, amount: Int) {
        val (newState, _) = DamageUtils.loseLife(
            driver.state, driver.player1, amount, LifeChangeReason.LIFE_LOSS
        )
        driver.replaceState(newState)
    }

    test("a red spell costs X less, where X is the life lost this turn") {
        val driver = newDriver()
        val rowan = driver.putCreatureOnBattlefield(driver.player1, "Rowan, Scion of War")
        driver.removeSummoningSickness(rowan)
        loseLife(driver, 4)

        costOf(driver, "Shivan Dragon") shouldBe 6 // {4}{R}{R}

        driver.submitSuccess(ActivateAbility(driver.player1, rowan, abilityId))
        driver.bothPass()

        costOf(driver, "Shivan Dragon") shouldBe 2 // {4} gone, {R}{R} untouched
    }

    test("white spells are not discounted") {
        val driver = newDriver()
        val rowan = driver.putCreatureOnBattlefield(driver.player1, "Rowan, Scion of War")
        driver.removeSummoningSickness(rowan)
        loseLife(driver, 4)

        driver.submitSuccess(ActivateAbility(driver.player1, rowan, abilityId))
        driver.bothPass()

        costOf(driver, "Serra Angel") shouldBe 5
    }

    test("damage dealt to you counts toward the life lost this turn") {
        val driver = newDriver()
        driver.replaceState(
            DamageUtils.trackDamageReceivedByPlayer(driver.state, driver.player1, 3)
        )

        lifeLostAmount(driver) shouldBe 3
    }

    test("life paid, life lost and damage all accumulate") {
        val driver = newDriver()
        loseLife(driver, 2)
        driver.replaceState(
            DamageUtils.trackDamageReceivedByPlayer(driver.state, driver.player1, 3)
        )
        val (afterPayment, _) = DamageUtils.loseLife(
            driver.state, driver.player1, 1, LifeChangeReason.PAYMENT
        )
        driver.replaceState(afterPayment)

        lifeLostAmount(driver) shouldBe 6
    }

    test("life gained does not net against the life lost total") {
        val driver = newDriver()
        loseLife(driver, 3)
        driver.replaceState(
            DamageUtils.markLifeGainedThisTurn(driver.state, driver.player1, 3)
        )

        // Rowan's ruling: gain 3 and lose 3 in a turn and the discount is still {3}.
        lifeLostAmount(driver) shouldBe 3

        val rowan = driver.putCreatureOnBattlefield(driver.player1, "Rowan, Scion of War")
        driver.removeSummoningSickness(rowan)
        driver.submitSuccess(ActivateAbility(driver.player1, rowan, abilityId))
        driver.bothPass()

        costOf(driver, "Shivan Dragon") shouldBe 3
    }

    test("the life-lost total resets at end of turn") {
        val driver = newDriver()
        loseLife(driver, 3)
        lifeLostAmount(driver) shouldBe 3

        endTurn(driver)

        lifeLostAmount(driver) shouldBe 0
    }

    test("Rowan has menace") {
        val driver = newDriver()
        val rowan = driver.putCreatureOnBattlefield(driver.player1, "Rowan, Scion of War")

        driver.state.projectedState.hasKeyword(rowan, Keyword.MENACE) shouldBe true
        driver.state.projectedState.getPower(rowan) shouldBe 4
        driver.state.projectedState.getToughness(rowan) shouldBe 2
    }
})
