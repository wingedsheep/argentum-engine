package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.khans.cards.ButcherOfTheHorde
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Butcher of the Horde:
 * {1}{R}{W}{B} Creature — Demon 5/4
 * Flying
 * Sacrifice another creature: Butcher of the Horde gains your choice of vigilance, lifelink,
 * or haste until end of turn.
 */
class ButcherOfTheHordeTest : FunSpec({

    val projector = StateProjector()

    val TestCreature = CardDefinition.creature(
        name = "Test Soldier",
        manaCost = ManaCost.parse("{1}"),
        subtypes = emptySet(),
        power = 1,
        toughness = 1
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ButcherOfTheHorde, TestCreature))
        return driver
    }

    test("has flying") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")

        projector.hasProjectedKeyword(driver.state, butcher, Keyword.FLYING) shouldBe true
    }

    test("sacrificing a creature grants vigilance until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")

        val abilityId = ButcherOfTheHorde.activatedAbilities[0].id

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(soldier))
            )
        )
        result.isSuccess shouldBe true

        // Resolve ability → modal choice
        driver.bothPass()

        // Choose vigilance (mode 0)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 0))

        driver.findPermanent(activePlayer, "Test Soldier") shouldBe null
        projector.hasProjectedKeyword(driver.state, butcher, Keyword.VIGILANCE) shouldBe true
    }

    test("sacrificing a creature grants lifelink until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")

        val abilityId = ButcherOfTheHorde.activatedAbilities[0].id

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(soldier))
            )
        )
        result.isSuccess shouldBe true

        // Resolve ability → modal choice
        driver.bothPass()

        // Choose lifelink (mode 1)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 1))

        driver.findPermanent(activePlayer, "Test Soldier") shouldBe null
        projector.hasProjectedKeyword(driver.state, butcher, Keyword.LIFELINK) shouldBe true
    }

    test("sacrificing a creature grants haste until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")
        val soldier = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")

        val abilityId = ButcherOfTheHorde.activatedAbilities[0].id

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(soldier))
            )
        )
        result.isSuccess shouldBe true

        // Resolve ability → modal choice
        driver.bothPass()

        // Choose haste (mode 2)
        val modeDecision = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision.id, 2))

        driver.findPermanent(activePlayer, "Test Soldier") shouldBe null
        projector.hasProjectedKeyword(driver.state, butcher, Keyword.HASTE) shouldBe true
    }

    test("cannot sacrifice self") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")
        // No other creatures on battlefield

        val abilityId = ButcherOfTheHorde.activatedAbilities[0].id

        val result = driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(butcher))
            )
        )
        result.isSuccess shouldBe false
    }

    test("can sacrifice multiple creatures for different keywords") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val butcher = driver.putCreatureOnBattlefield(activePlayer, "Butcher of the Horde")
        val soldier1 = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")
        val soldier2 = driver.putCreatureOnBattlefield(activePlayer, "Test Soldier")

        val abilityId = ButcherOfTheHorde.activatedAbilities[0].id

        // Sacrifice first for vigilance
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(soldier1))
            )
        )
        driver.bothPass()

        // Choose vigilance (mode 0)
        val modeDecision1 = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision1.id, 0))

        // Sacrifice second for lifelink
        driver.submit(
            ActivateAbility(
                playerId = activePlayer,
                sourceId = butcher,
                abilityId = abilityId,
                costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(soldier2))
            )
        )
        driver.bothPass()

        // Choose lifelink (mode 1)
        val modeDecision2 = driver.pendingDecision as ChooseOptionDecision
        driver.submitDecision(activePlayer, OptionChosenResponse(modeDecision2.id, 1))

        projector.hasProjectedKeyword(driver.state, butcher, Keyword.VIGILANCE) shouldBe true
        projector.hasProjectedKeyword(driver.state, butcher, Keyword.LIFELINK) shouldBe true
    }
})
