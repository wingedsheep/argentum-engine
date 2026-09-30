package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fin.cards.MinwuWhiteMage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Minwu, White Mage — "Whenever you gain life, put a +1/+1 counter on each Cleric you control."
 *
 * "Each Cleric" is a bare tribal noun, so it names every permanent with the subtype, not only
 * creatures: a kindred Cleric enchantment gets the counter too. Minwu herself is a Cleric; a
 * non-Cleric creature is left alone.
 */
class MinwuWhiteMageScenarioTest : FunSpec({

    val clericShrine = card("Cleric Shrine") {
        manaCost = "{1}{W}"; colorIdentity = "W"; typeLine = "Kindred Enchantment — Cleric"
    }
    val salve = card("Test Salve") {
        manaCost = "{W}"; colorIdentity = "W"; typeLine = "Instant"; spell { effect = Effects.GainLife(3) }
    }
    val ox = card("Plain Ox") {
        manaCost = "{2}"; typeLine = "Creature — Ox"; power = 2; toughness = 2
    }

    fun GameTestDriver.plusOnes(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("gaining life puts a counter on each Cleric permanent, kindred enchantment included") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MinwuWhiteMage, clericShrine, salve, ox))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!

        val minwu = driver.putPermanentOnBattlefield(me, "Minwu, White Mage")
        val shrine = driver.putPermanentOnBattlefield(me, "Cleric Shrine")
        val plainOx = driver.putPermanentOnBattlefield(me, "Plain Ox")

        val spell = driver.putCardInHand(me, "Test Salve")
        driver.giveMana(me, Color.WHITE, 1)
        driver.castSpell(me, spell)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 20) driver.bothPass()

        driver.plusOnes(minwu) shouldBe 1
        driver.plusOnes(shrine) shouldBe 1
        driver.plusOnes(plainOx) shouldBe 0
    }
})
