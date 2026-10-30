package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.eld.cards.BelleOfTheBrawl
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Belle of the Brawl (ELD #78) — {2}{B} Creature — Human Knight 3/2.
 *
 * "Menace. Whenever this creature attacks, other Knights you control get +1/+0 until end of turn."
 */
class BelleOfTheBrawlScenarioTest : FunSpec({

    val testKnight = card("Test Knight") {
        manaCost = "{1}{B}"
        typeLine = "Creature — Human Knight"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BelleOfTheBrawl, testKnight))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), startingLife = 20)
        return driver
    }

    test("attacking pumps other Knights you control by +1/+0, not Belle herself or non-Knights") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)
        val belle = driver.putCreatureOnBattlefield(me, "Belle of the Brawl")
        val knight = driver.putCreatureOnBattlefield(me, "Test Knight")
        val bears = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val enemyKnight = driver.putCreatureOnBattlefield(opponent, "Test Knight")
        driver.removeSummoningSickness(belle)

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(me, listOf(belle), opponent)
        var guard = 0
        while (driver.state.stack.isNotEmpty() && guard++ < 10) driver.bothPass()

        val projected = driver.state.projectedState
        projected.getPower(knight) shouldBe 3
        projected.getToughness(knight) shouldBe 2
        projected.getPower(belle) shouldBe 3
        projected.getPower(bears) shouldBe 2
        projected.getPower(enemyKnight) shouldBe 2
    }
})
