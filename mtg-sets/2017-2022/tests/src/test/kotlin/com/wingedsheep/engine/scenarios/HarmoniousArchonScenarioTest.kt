package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.HarmoniousArchon
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Harmonious Archon — "Non-Archon creatures have base power and toughness 3/3." is a layer-7b
 * base-setting effect over every non-Archon creature (both controllers), so +1/+1 counters (7c)
 * still apply on top, and the Archon itself — and other Archons — keep their printed stats.
 */
class HarmoniousArchonScenarioTest : FunSpec({
    val bear = card("Archon Test Bear") { typeLine = "Creature — Bear"; power = 2; toughness = 2 }
    val giant = card("Archon Test Giant") { typeLine = "Creature — Giant"; power = 7; toughness = 7 }
    val otherArchon = card("Archon Test Archon") { typeLine = "Creature — Archon"; power = 2; toughness = 6 }

    fun setup() = GameTestDriver().apply {
        registerCards(TestCards.all)
        registerCards(listOf(HarmoniousArchon, bear, giant, otherArchon))
        initMirrorMatch(deck = Deck.of("Plains" to 30), skipMulligans = true)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("non-Archon creatures of both players become base 3/3; Archons are unaffected") {
        val d = setup()
        val p = d.activePlayer!!
        val opp = d.getOpponent(p)
        val archon = d.putCreatureOnBattlefield(p, HarmoniousArchon.name)
        val myBear = d.putCreatureOnBattlefield(p, bear.name)
        val theirGiant = d.putCreatureOnBattlefield(opp, giant.name)
        val theirArchon = d.putCreatureOnBattlefield(opp, otherArchon.name)
        val projected = d.state.projectedState

        projected.getPower(myBear) shouldBe 3
        projected.getToughness(myBear) shouldBe 3
        projected.getPower(theirGiant) shouldBe 3
        projected.getToughness(theirGiant) shouldBe 3
        projected.getPower(archon) shouldBe 4
        projected.getToughness(archon) shouldBe 5
        projected.getPower(theirArchon) shouldBe 2
        projected.getToughness(theirArchon) shouldBe 6
    }

    test("+1/+1 counters apply on top of the new base") {
        val d = setup()
        val p = d.activePlayer!!
        d.putCreatureOnBattlefield(p, HarmoniousArchon.name)
        val myBear = d.putCreatureOnBattlefield(p, bear.name)
        d.replaceState(d.state.updateEntity(myBear) {
            it.with(CountersComponent(mapOf(CounterType.PLUS_ONE_PLUS_ONE to 2)))
        })

        d.state.projectedState.getPower(myBear) shouldBe 5
        d.state.projectedState.getToughness(myBear) shouldBe 5
    }

    test("enters: creates two Human tokens that are 3/3 while the Archon remains") {
        val d = setup()
        val p = d.activePlayer!!
        val spell = d.putCardInHand(p, HarmoniousArchon.name)
        d.giveMana(p, Color.WHITE, 6)

        d.castSpell(p, spell).outcome shouldBe Outcome.Done
        d.bothPass() // resolve the creature spell
        d.bothPass() // resolve the enters trigger

        val tokens = d.getCreatures(p).filter { it != spell }
        tokens shouldHaveSize 2
        tokens.forEach {
            d.state.projectedState.getPower(it) shouldBe 3
            d.state.projectedState.getToughness(it) shouldBe 3
        }
    }
})
