package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.LifeTotalAtMost
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Convalescent Care:
 * {1}{W}{W} Enchantment
 * At the beginning of your upkeep, if you have 5 or less life, you gain 3 life and draw a card.
 */
class ConvalescentCareTest : FunSpec({

    val ConvalescentCare = CardDefinition.enchantment(
        name = "Convalescent Care",
        manaCost = ManaCost.parse("{1}{W}{W}"),
        oracleText = "At the beginning of your upkeep, if you have 5 or less life, you gain 3 life and draw a card.",
        script = CardScript.permanent(
            triggeredAbilities = listOf(
                TriggeredAbility.create(
                    trigger = OnUpkeep(controllerOnly = true),
                    triggerCondition = LifeTotalAtMost(5),
                    effect = ConditionalEffect(
                        condition = LifeTotalAtMost(5),
                        effect = CompositeEffect(
                            listOf(
                                GainLifeEffect(3, EffectTarget.Controller),
                                DrawCardsEffect(1, EffectTarget.Controller)
                            )
                        )
                    )
                )
            )
        )
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ConvalescentCare))
        return driver
    }

    fun advanceToPlayerUpkeep(driver: GameTestDriver, targetPlayer: EntityId) {
        driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        if (driver.activePlayer == targetPlayer) {
            driver.passPriorityUntil(Step.DRAW, maxPasses = 200)
        }
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe targetPlayer
    }

    test("at 5 life - trigger fires, gains 3 life and draws a card") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 5
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        val handSizeBefore = driver.getHand(activePlayer).size

        // Advance to controller's upkeep
        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger should fire automatically (no targets, no optional)
        // Resolve the trigger on the stack
        driver.bothPass()

        // Should have gained 3 life (5 -> 8) and drawn a card
        driver.getLifeTotal(activePlayer) shouldBe 8
        driver.getHand(activePlayer).size shouldBe handSizeBefore + 1
    }

    test("at 1 life - trigger fires") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 1
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        advanceToPlayerUpkeep(driver, activePlayer)
        driver.bothPass()

        // Should have gained 3 life (1 -> 4)
        driver.getLifeTotal(activePlayer) shouldBe 4
    }

    test("at 6 life - trigger does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 6
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        val handSizeBefore = driver.getHand(activePlayer).size
        val lifeBefore = driver.getLifeTotal(activePlayer)

        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger should NOT fire (life > 5)
        driver.stackSize shouldBe 0
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore
        driver.getHand(activePlayer).size shouldBe handSizeBefore
    }

    test("at 20 life - trigger does not fire") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        val lifeBefore = driver.getLifeTotal(activePlayer)

        advanceToPlayerUpkeep(driver, activePlayer)

        // Trigger should NOT fire
        driver.stackSize shouldBe 0
        driver.getLifeTotal(activePlayer) shouldBe lifeBefore
    }

    test("does not trigger on opponent's upkeep") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 5
        )

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Put Convalescent Care under activePlayer's control
        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        // Advance to opponent's upkeep
        driver.passPriorityUntil(Step.UPKEEP, maxPasses = 200)
        driver.currentStep shouldBe Step.UPKEEP
        driver.activePlayer shouldBe opponent

        // Even though both players are at 5 life, the trigger should NOT fire on opponent's upkeep
        driver.stackSize shouldBe 0
    }

    test("at exactly 5 life - trigger fires (boundary check)") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 5
        )

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(activePlayer, "Convalescent Care")

        advanceToPlayerUpkeep(driver, activePlayer)
        driver.bothPass()

        // At exactly 5, trigger should fire: 5 + 3 = 8
        driver.getLifeTotal(activePlayer) shouldBe 8
    }
})
