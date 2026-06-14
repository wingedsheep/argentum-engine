package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.ShiftingGrift
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shifting Grift — {U}{U} Sorcery, Spree.
 *
 * + {2} — Exchange control of two target creatures.
 * + {1} — Exchange control of two target artifacts.
 * + {1} — Exchange control of two target enchantments.
 *
 * Both permanents in each mode are targets; the swap lasts indefinitely (Effects.ExchangeControl).
 */
class OtjShiftingGriftScenarioTest : FunSpec({

    // Simple permanents to swap control of.
    val testArtifact = card("Test Trinket") {
        manaCost = "{1}"
        typeLine = "Artifact"
        oracleText = ""
    }
    val testEnchantment = card("Test Charm") {
        manaCost = "{1}"
        typeLine = "Enchantment"
        oracleText = ""
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(ShiftingGrift, testArtifact, testEnchantment))
        return driver
    }

    test("mode + {2}: exchange control of two target creatures") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirs = driver.putCreatureOnBattlefield(opp, "Savannah Lions")

        val spell = driver.putCardInHand(me, "Shifting Grift")
        driver.giveMana(me, Color.BLUE, 2) // {U}{U} base
        driver.giveColorlessMana(me, 2)     // {2} for the mode
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                chosenModes = listOf(0),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        // Control is exchanged: I now control their Lions, they control my Courser.
        driver.state.projectedState.getController(theirs) shouldBe me
        driver.state.projectedState.getController(mine) shouldBe opp
    }

    test("mode + {1}: exchange control of two target artifacts") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val mine = driver.putPermanentOnBattlefield(me, "Test Trinket")
        val theirs = driver.putPermanentOnBattlefield(opp, "Test Trinket")

        val spell = driver.putCardInHand(me, "Shifting Grift")
        driver.giveMana(me, Color.BLUE, 2)
        driver.giveColorlessMana(me, 1)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs)),
                chosenModes = listOf(1),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(mine), ChosenTarget.Permanent(theirs))
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.projectedState.getController(theirs) shouldBe me
        driver.state.projectedState.getController(mine) shouldBe opp
    }

    test("two modes: exchange creatures and enchantments in one cast") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val myCreature = driver.putCreatureOnBattlefield(me, "Centaur Courser")
        val theirCreature = driver.putCreatureOnBattlefield(opp, "Savannah Lions")
        val myEnchant = driver.putPermanentOnBattlefield(me, "Test Charm")
        val theirEnchant = driver.putPermanentOnBattlefield(opp, "Test Charm")

        val spell = driver.putCardInHand(me, "Shifting Grift")
        driver.giveMana(me, Color.BLUE, 2) // {U}{U} base
        driver.giveColorlessMana(me, 3)     // {2} (creatures) + {1} (enchantments)
        driver.submit(
            CastSpell(
                playerId = me,
                cardId = spell,
                targets = listOf(
                    ChosenTarget.Permanent(myCreature), ChosenTarget.Permanent(theirCreature),
                    ChosenTarget.Permanent(myEnchant), ChosenTarget.Permanent(theirEnchant)
                ),
                chosenModes = listOf(0, 2),
                modeTargetsOrdered = listOf(
                    listOf(ChosenTarget.Permanent(myCreature), ChosenTarget.Permanent(theirCreature)),
                    listOf(ChosenTarget.Permanent(myEnchant), ChosenTarget.Permanent(theirEnchant))
                ),
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).isSuccess shouldBe true
        driver.bothPass()

        driver.state.projectedState.getController(theirCreature) shouldBe me
        driver.state.projectedState.getController(myCreature) shouldBe opp
        driver.state.projectedState.getController(theirEnchant) shouldBe me
        driver.state.projectedState.getController(myEnchant) shouldBe opp
    }
})
