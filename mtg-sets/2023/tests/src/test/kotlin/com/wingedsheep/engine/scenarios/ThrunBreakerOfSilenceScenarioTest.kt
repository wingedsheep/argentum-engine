package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.ThrunBreakerOfSilence
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Thrun, Breaker of Silence (ONE #186) — {3}{G}{G} 5/5 Legendary Creature — Troll Shaman.
 *
 * "This spell can't be countered. / Trample / Thrun can't be the target of nongreen spells your
 *  opponents control or abilities from nongreen sources your opponents control. / During your
 *  turn, Thrun has indestructible."
 *
 * The full hexproof-from-nongreen matrix (colorless sources, green-white spells, enumeration,
 * resolution re-check) lives in the engine's `HexproofFromNonColorTest`; this pins the card.
 */
class ThrunBreakerOfSilenceScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(ThrunBreakerOfSilence)
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    test("an opponent's red spell can't target Thrun, their green spell can") {
        val driver = newDriver()
        val caster = driver.player1
        val thrun = driver.putCreatureOnBattlefield(driver.player2, "Thrun, Breaker of Silence")

        val bolt = driver.putCardInHand(caster, "Lightning Bolt")
        driver.giveMana(caster, Color.RED, 1)
        driver.castSpell(caster, bolt, listOf(thrun)).outcome shouldNotBe Outcome.Done

        val growth = driver.putCardInHand(caster, "Giant Growth")
        driver.giveMana(caster, Color.GREEN, 1)
        driver.castSpell(caster, growth, listOf(thrun)).outcome shouldBe Outcome.Done
    }

    test("Thrun's controller can target it with a nongreen spell") {
        val driver = newDriver()
        val me = driver.player1
        val thrun = driver.putCreatureOnBattlefield(me, "Thrun, Breaker of Silence")

        val bolt = driver.putCardInHand(me, "Lightning Bolt")
        driver.giveMana(me, Color.RED, 1)
        driver.castSpell(me, bolt, listOf(thrun)).outcome shouldBe Outcome.Done
    }

    test("Thrun has trample, and indestructible only during its controller's turn") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Thrun, Breaker of Silence")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Thrun, Breaker of Silence")
        val projected = driver.state.projectedState

        projected.hasKeyword(mine, Keyword.TRAMPLE) shouldBe true
        projected.hasKeyword(mine, Keyword.INDESTRUCTIBLE) shouldBe true
        projected.hasKeyword(theirs, Keyword.INDESTRUCTIBLE) shouldBe false
    }

    test("the spell can't be countered") {
        ThrunBreakerOfSilence.script.cantBeCountered shouldBe true
    }
})
