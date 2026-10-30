package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.InspiringVeteran
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Inspiring Veteran (ELD #194) — "Other Knights you control get +1/+1."
 */
class InspiringVeteranScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    test("pumps your other Knights only — not itself, non-Knights, or the opponent's Knights") {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(InspiringVeteran, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val foe = driver.getOpponent(me)
        val veteran = driver.putCreatureOnBattlefield(me, "Inspiring Veteran")
        val knight = driver.putCreatureOnBattlefield(me, "Test Knight")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val theirKnight = driver.putCreatureOnBattlefield(foe, "Test Knight")

        val projected = driver.state.projectedState
        projected.getPower(veteran) shouldBe 2
        projected.getPower(knight) shouldBe 3
        projected.getToughness(knight) shouldBe 3
        projected.getPower(bears) shouldBe 2
        projected.getPower(theirKnight) shouldBe 2
    }
})
