package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.otj.cards.DoubleDown
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Double Down — {3}{U} Enchantment.
 *
 * Whenever you cast an outlaw spell, copy that spell.
 *
 * Verifies the cast-an-outlaw-spell trigger copies the spell (a creature copy resolves into a
 * token, so the outlaw creature ends up on the battlefield twice) and that a non-outlaw spell is
 * not copied.
 */
class OtjDoubleDownScenarioTest : FunSpec({

    // Minimal outlaw creature spell (Rogue = an outlaw type).
    val testOutlaw = card("Test Bandit") {
        manaCost = "{1}"
        typeLine = "Creature — Human Rogue"
        power = 2
        toughness = 2
    }

    // Minimal non-outlaw creature spell (Bear is not an outlaw type).
    val testNonOutlaw = card("Test Bear") {
        manaCost = "{1}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DoubleDown)
        driver.registerCard(testOutlaw)
        driver.registerCard(testNonOutlaw)
        driver.initMirrorMatch(Deck.of("Island" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.countNamed(playerId: EntityId, name: String): Int =
        getPermanents(playerId).count {
            state.getEntity(it)?.get<CardComponent>()?.name == name
        }

    // Pass priority back and forth until the stack is empty (resolves the spell, the Double Down
    // trigger, and the copy it creates).
    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < 12) {
            bothPass()
        }
    }

    test("casting an outlaw creature spell copies it (resolves as a token)") {
        val driver = newDriver()
        val caster = driver.player1
        driver.putPermanentOnBattlefield(caster, "Double Down")

        val bandit = driver.putCardInHand(caster, "Test Bandit")
        driver.giveColorlessMana(caster, 1)
        driver.castSpell(caster, bandit).isSuccess shouldBe true

        // Resolve the Double Down trigger (the copy → token) and the original spell.
        driver.resolveStack()

        // Original Bandit + its token copy = 2 on the battlefield.
        driver.countNamed(caster, "Test Bandit") shouldBe 2
    }

    test("casting a non-outlaw creature spell does not copy it") {
        val driver = newDriver()
        val caster = driver.player1
        driver.putPermanentOnBattlefield(caster, "Double Down")

        val bear = driver.putCardInHand(caster, "Test Bear")
        driver.giveColorlessMana(caster, 1)
        driver.castSpell(caster, bear).isSuccess shouldBe true
        driver.resolveStack()

        driver.countNamed(caster, "Test Bear") shouldBe 1
    }
})
