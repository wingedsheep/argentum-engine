package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lci.cards.GoblinTombRaider
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Goblin Tomb Raider (LCI #151).
 *
 * Goblin Tomb Raider {R}
 * Creature — Goblin Pirate 1/2
 * As long as you control an artifact, this creature gets +1/+0 and has haste.
 *
 * Covers:
 *  1. Base case: without any artifact, the creature is a vanilla 1/2 without haste.
 *  2. Artifact present: while you control an artifact, it becomes a 2/2 with haste.
 */
class GoblinTombRaiderScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GoblinTombRaider))
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        return driver
    }

    test("base stats are 1/2 and no haste when no artifact is controlled") {
        val driver = createDriver()
        val me = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val raider = driver.putCreatureOnBattlefield(me, "Goblin Tomb Raider")

        val projected = driver.state.projectedState
        projected.getPower(raider) shouldBe 1
        projected.getToughness(raider) shouldBe 2
        projected.hasKeyword(raider, Keyword.HASTE) shouldBe false
    }

    test("gets +1/+0 and haste while controller owns an artifact") {
        val driver = createDriver()
        val me = driver.player1

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val raider = driver.putCreatureOnBattlefield(me, "Goblin Tomb Raider")
        // "Artifact Creature" from TestCards is an artifact creature and satisfies the artifact condition.
        driver.putCreatureOnBattlefield(me, "Artifact Creature")

        val projected = driver.state.projectedState
        projected.getPower(raider) shouldBe 2
        projected.getToughness(raider) shouldBe 2
        projected.hasKeyword(raider, Keyword.HASTE) shouldBe true
    }
})
