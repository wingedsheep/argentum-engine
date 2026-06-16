package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.AnnieJoinsUp
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Annie Joins Up (OTJ #191).
 *
 * 1. ETB: "it deals 5 damage to target creature or planeswalker an opponent controls."
 * 2. Static: "If a triggered ability of a legendary creature you control triggers, that
 *    ability triggers an additional time." — the generic AdditionalSourceTriggers doubler
 *    scoped to legendary creatures you control (CR 603.2d).
 */
class AnnieJoinsUpTest : FunSpec({

    // A legendary creature with an ETB draw trigger. Its trigger is what Annie's static doubles.
    val legendaryDrawer = card("Test Legendary Drawer") {
        manaCost = "{2}"
        typeLine = "Legendary Creature — Human"
        power = 2
        toughness = 2
        oracleText = "When Test Legendary Drawer enters, draw a card."
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.DrawCards(1)
        }
    }

    // A non-legendary creature with the same ETB draw trigger, used to prove the doubler is
    // scoped to legendary creatures only.
    val plainDrawer = card("Test Plain Drawer") {
        manaCost = "{2}"
        typeLine = "Creature — Human"
        power = 2
        toughness = 2
        oracleText = "When Test Plain Drawer enters, draw a card."
        triggeredAbility {
            trigger = Triggers.EntersBattlefield
            effect = Effects.DrawCards(1)
        }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(AnnieJoinsUp, legendaryDrawer, plainDrawer))
        return driver
    }

    test("ETB deals 5 damage to a target creature an opponent controls") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        val opponent = driver.getOpponent(you)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // A 3/3 opposing creature so we can observe lethal damage.
        val target = driver.putCreatureOnBattlefield(opponent, "Hill Giant")

        // Cast Annie so her ETB trigger fires, then submit the target.
        val annie = driver.putCardInHand(you, "Annie Joins Up")
        driver.giveMana(you, Color.RED, 1)
        driver.giveMana(you, Color.GREEN, 1)
        driver.giveMana(you, Color.WHITE, 1)
        driver.giveColorlessMana(you, 1)
        driver.castSpell(you, annie).isSuccess shouldBe true
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision is ChooseTargetsDecision) && guard++ < 20) {
            if (driver.state.pendingDecision is ChooseTargetsDecision) {
                driver.submitTargetSelection(you, listOf(target))
            } else {
                driver.bothPass()
            }
        }

        // Hill Giant is 3/3 — 5 damage is lethal, so it should be gone from the battlefield.
        driver.getCreatures(opponent).contains(target) shouldBe false
    }

    test("doubles a legendary creature's ETB trigger — controller draws twice") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Annie is already in play, providing her static doubler.
        driver.putPermanentOnBattlefield(you, "Annie Joins Up")

        // A legendary creature is cast: its ETB draw trigger should fire an additional time.
        val drawer = driver.putCardInHand(you, "Test Legendary Drawer")
        driver.giveColorlessMana(you, 2)
        val before = driver.getHandSize(you)
        driver.castSpell(you, drawer).isSuccess shouldBe true
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) {
            driver.bothPass()
        }

        // Casting moved the card from hand, then two ETB draws (original + additional firing):
        // net delta = -1 (cast) + 2 (draws) = +1.
        (driver.getHandSize(you) - before) shouldBe 1
    }

    test("does not double a non-legendary creature's ETB trigger") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val you = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putPermanentOnBattlefield(you, "Annie Joins Up")

        val drawer = driver.putCardInHand(you, "Test Plain Drawer")
        driver.giveColorlessMana(you, 2)
        val before = driver.getHandSize(you)
        driver.castSpell(you, drawer).isSuccess shouldBe true
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) {
            driver.bothPass()
        }

        // -1 (cast) + 1 (single ETB draw, not doubled) = 0. The doubler is legendary-only.
        (driver.getHandSize(you) - before) shouldBe 0
    }
})
