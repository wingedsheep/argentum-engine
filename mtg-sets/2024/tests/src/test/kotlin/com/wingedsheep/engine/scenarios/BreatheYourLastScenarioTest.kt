package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.BreatheYourLast
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Breathe Your Last destroys its target and gains 1 life per color it had. The count must be
 * frozen before the destroy, since the target is gone by the time the life gain resolves.
 */
class BreatheYourLastScenarioTest : FunSpec({
    val tricolor = card("Breathe Test Tricolor") {
        manaCost = "{W}{U}{B}"
        typeLine = "Creature — Bear"
        power = 3
        toughness = 3
        oracleText = ""
    }
    val colorless = card("Breathe Test Golem") {
        manaCost = "{3}"
        typeLine = "Artifact Creature — Golem"
        power = 2
        toughness = 2
        oracleText = ""
    }

    fun driver() = GameTestDriver().apply {
        registerCards(TestCards.all + listOf(BreatheYourLast, tricolor, colorless))
        initMirrorMatch(Deck.of("Swamp" to 40), startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    for ((name, expectedGain) in listOf("Breathe Test Tricolor" to 3, "Breathe Test Golem" to 0)) {
        test("destroying $name gains $expectedGain life") {
            val d = driver()
            val victim = d.putCreatureOnBattlefield(d.player2, name)
            val spell = d.putCardInHand(d.player1, "Breathe Your Last")
            d.giveMana(d.player1, Color.BLACK, 3)
            val startingLife = d.getLifeTotal(d.player1)

            d.castSpell(d.player1, spell, listOf(victim)).error shouldBe null
            d.bothPass().error shouldBe null

            d.state.getZone(ZoneKey(d.player2, Zone.GRAVEYARD)).contains(victim) shouldBe true
            d.getLifeTotal(d.player1) shouldBe startingLife + expectedGain
        }
    }
})
