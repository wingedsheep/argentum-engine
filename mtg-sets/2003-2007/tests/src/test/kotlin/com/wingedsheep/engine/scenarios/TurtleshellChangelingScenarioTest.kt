package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lrw.cards.TurtleshellChangeling
import com.wingedsheep.mtg.sets.definitions.tsp.cards.MomentaryBlink
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TurtleshellChangelingScenarioTest : FunSpec({
    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(TurtleshellChangeling, MomentaryBlink))
        initMirrorMatch(Deck.of("Plains" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun GameTestDriver.castTurtleshell(): EntityId {
        val creature = putCardInHand(player1, "Turtleshell Changeling")
        giveMana(player1, Color.BLUE, 4)
        castSpell(player1, creature).error shouldBe null
        bothPass().error shouldBe null
        return creature
    }

    fun GameTestDriver.switch(creature: EntityId, resolve: Boolean = true) {
        giveMana(player1, Color.BLUE, 2)
        submit(ActivateAbility(player1, creature, TurtleshellChangeling.activatedAbilities.single().id)).error shouldBe null
        if (resolve) bothPass().error shouldBe null
    }

    fun GameTestDriver.stats(creature: EntityId, power: Int, toughness: Int) {
        state.projectedState.getPower(creature) shouldBe power
        state.projectedState.getToughness(creature) shouldBe toughness
    }

    test("switches without tapping and expires at end of turn") {
        val d = driver()
        val creature = d.castTurtleshell()
        d.switch(creature)
        d.stats(creature, 4, 1)
        d.passPriorityUntil(Step.UPKEEP)
        d.stats(creature, 1, 4)
    }

    test("two switches restore the original stats") {
        val d = driver()
        val creature = d.castTurtleshell()
        d.switch(creature)
        d.switch(creature)
        d.stats(creature, 1, 4)
    }

    for (pumpFirst in listOf(true, false)) {
        test("switch applies after stat boosts with pumpFirst=$pumpFirst") {
            val d = driver()
            val creature = d.castTurtleshell()
            if (!pumpFirst) d.switch(creature)
            val growth = d.putCardInHand(d.player1, "Giant Growth")
            d.giveMana(d.player1, Color.GREEN, 1)
            d.castSpell(d.player1, growth, listOf(creature)).error shouldBe null
            d.bothPass().error shouldBe null
            if (pumpFirst) d.switch(creature)
            d.stats(creature, 7, 4)
        }
    }

    test("switch makes previously nonlethal marked damage lethal") {
        val d = driver()
        val creature = d.castTurtleshell()
        val bolt = d.putCardInHand(d.player1, "Lightning Bolt")
        d.giveMana(d.player1, Color.RED, 1)
        d.castSpell(d.player1, bolt, listOf(creature)).error shouldBe null
        d.bothPass().error shouldBe null
        (creature in d.state.getBattlefield()) shouldBe true
        d.switch(creature)
        (creature in d.state.getBattlefield()) shouldBe false
    }

    for (resolveBeforeBlink in listOf(true, false)) {
        test("blink discards ${if (resolveBeforeBlink) "resolved switch" else "pending self reference"}") {
            val d = driver()
            val creature = d.castTurtleshell()
            d.switch(creature, resolveBeforeBlink)
            val blink = d.putCardInHand(d.player1, "Momentary Blink")
            d.giveMana(d.player1, Color.WHITE, 2)
            d.castSpell(d.player1, blink, listOf(creature)).error shouldBe null
            d.bothPass().error shouldBe null
            if (!resolveBeforeBlink) d.bothPass().error shouldBe null
            d.stats(creature, 1, 4)
        }
    }
})
