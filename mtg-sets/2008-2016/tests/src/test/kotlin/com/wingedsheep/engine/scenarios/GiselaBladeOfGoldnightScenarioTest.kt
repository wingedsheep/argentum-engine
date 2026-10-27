package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.DamageComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.avr.cards.GiselaBladeOfGoldnight
import com.wingedsheep.mtg.sets.definitions.ons.cards.PinpointAvalanche
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Gisela, Blade of Goldnight (AVR #209): damage to an opponent or their permanents is doubled;
 * damage to you or your permanents has half of it prevented, rounded up.
 */
class GiselaBladeOfGoldnightScenarioTest : FunSpec({
    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(GiselaBladeOfGoldnight, PinpointAvalanche))
        initMirrorMatch(Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.resolve() {
        var passes = 0
        while (stackSize > 0 && passes++ < 20) bothPass()
        stackSize shouldBe 0
    }

    fun GameTestDriver.bolt(target: EntityId) {
        giveMana(player1, Color.RED, 1)
        castSpell(player1, putCardInHand(player1, "Lightning Bolt"), listOf(target)).error shouldBe null
        resolve()
    }

    fun GameTestDriver.damage(id: EntityId) = state.getEntity(id)?.get<DamageComponent>()?.amount ?: 0

    test("damage to Gisela's controller has half prevented, rounded up") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Gisela, Blade of Goldnight")
        d.bolt(d.player2)
        // 3 damage: 2 prevented (half of 3, rounded up), 1 dealt.
        d.getLifeTotal(d.player2) shouldBe 19
    }

    test("damage to a permanent Gisela's controller controls is halved too") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Gisela, Blade of Goldnight")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.bolt(courser)
        d.damage(courser) shouldBe 1
    }

    test("her own controller's sources are halved against them as well") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Gisela, Blade of Goldnight")
        d.bolt(d.player1)
        d.getLifeTotal(d.player1) shouldBe 19
    }

    test("damage to an opponent and their permanents is doubled") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player1, "Gisela, Blade of Goldnight")
        val angler = d.putCreatureOnBattlefield(d.player2, "Gurmag Angler") // 5/5
        d.bolt(d.player2)
        d.getLifeTotal(d.player2) shouldBe 14
        d.bolt(angler)
        d.getGraveyard(d.player2).contains(angler) shouldBe true
    }

    test("damage that can't be prevented is dealt in full") {
        val d = driver()
        d.putCreatureOnBattlefield(d.player2, "Gisela, Blade of Goldnight")
        val courser = d.putCreatureOnBattlefield(d.player2, "Centaur Courser")
        d.giveMana(d.player1, Color.RED, 5)
        d.castSpell(d.player1, d.putCardInHand(d.player1, "Pinpoint Avalanche"), listOf(courser)).error shouldBe null
        d.resolve()
        d.getGraveyard(d.player2).contains(courser) shouldBe true
    }
})
