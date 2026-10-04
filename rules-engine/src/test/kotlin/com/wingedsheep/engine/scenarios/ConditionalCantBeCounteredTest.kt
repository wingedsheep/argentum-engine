package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Tests for the card-level `cantBeCounteredIf` ("If …, this spell can't be countered").
 *
 * The condition is read off the spell on the stack when a counter resolves, with the spell's
 * caster as `You` — so the counterer's board never satisfies it, and the board at counter time
 * (not at cast time) is what counts.
 */
class ConditionalCantBeCounteredTest : FunSpec({

    val guardedBear = card("Guarded Bear") {
        manaCost = "{1}{G}"
        colorIdentity = "G"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
        oracleText = "If you control an enchantment, this spell can't be countered."
        cantBeCounteredIf = Conditions.ControlEnchantment
    }

    val plainEnchantment = card("Plain Enchantment") {
        manaCost = "{G}"
        colorIdentity = "G"
        typeLine = "Enchantment"
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(guardedBear, plainEnchantment))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    /** Cast the bear, then have the opponent Counterspell it; returns the opponent's id. */
    fun castBearIntoCounterspell(driver: GameTestDriver, active: EntityId): EntityId {
        val opponent = driver.getOpponent(active)
        val bear = driver.putCardInHand(active, "Guarded Bear")
        val counterspell = driver.putCardInHand(opponent, "Counterspell")
        driver.giveMana(active, Color.GREEN, 2)
        driver.castSpell(active, bear)
        driver.passPriority(active)
        driver.giveMana(opponent, Color.BLUE, 2)
        driver.submit(
            CastSpell(
                playerId = opponent,
                cardId = counterspell,
                targets = listOf(ChosenTarget.Spell(driver.getTopOfStack()!!)),
                paymentStrategy = PaymentStrategy.FromPool
            )
        )
        driver.stackSize shouldBe 2
        return opponent
    }

    test("caster controls an enchantment: the counter does nothing") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(active, "Plain Enchantment")

        castBearIntoCounterspell(driver, active)
        driver.bothPass() // Counterspell resolves, bear stays
        driver.getTopOfStackName() shouldBe "Guarded Bear"
        driver.bothPass()
        driver.findPermanent(active, "Guarded Bear") shouldNotBe null
    }

    test("only the counterer controls an enchantment: the spell is countered") {
        val driver = createDriver()
        val active = driver.activePlayer!!
        driver.putPermanentOnBattlefield(driver.getOpponent(active), "Plain Enchantment")

        castBearIntoCounterspell(driver, active)
        driver.bothPass()
        driver.stackSize shouldBe 0
        driver.findPermanent(active, "Guarded Bear") shouldBe null
    }
})
