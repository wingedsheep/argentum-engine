package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.DeclareAttackers
import com.wingedsheep.engine.core.DeclareBlockers
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.one.cards.GraazUnstoppableJuggernaut
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain

private val projector = StateProjector()

/**
 * Graaz, Unstoppable Juggernaut (ONE #229) — {8} Legendary Artifact Creature — Juggernaut 7/5.
 *
 *   Juggernauts you control attack each combat if able.
 *   Juggernauts you control can't be blocked by Walls.
 *   Other creatures you control have base power and toughness 5/3 and are Juggernauts in addition
 *   to their other creature types.
 */
class GraazUnstoppableJuggernautScenarioTest : FunSpec({

    val testBear = CardDefinition.creature(
        name = "Test Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2
    )

    val testWall = CardDefinition.creature(
        name = "Test Wall",
        manaCost = ManaCost.parse("{1}"),
        subtypes = setOf(Subtype.WALL),
        power = 0,
        toughness = 6,
        keywords = setOf(Keyword.DEFENDER)
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(GraazUnstoppableJuggernaut, testBear, testWall))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, skipMulligans = true)
        return driver
    }

    fun GameTestDriver.advanceToPlayer1DeclareAttackers() {
        passPriorityUntil(Step.DECLARE_ATTACKERS)
        var safety = 0
        while (activePlayer != player1 && safety < 50) {
            bothPass()
            passPriorityUntil(Step.DECLARE_ATTACKERS)
            safety++
        }
    }

    test("other creatures you control are base 5/3 Juggernauts; Graaz and opponents' creatures are not changed") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val graaz = driver.putCreatureOnBattlefield(me, "Graaz, Unstoppable Juggernaut")
        val myBear = driver.putCreatureOnBattlefield(me, "Test Bear")
        val enemyBear = driver.putCreatureOnBattlefield(opponent, "Test Bear")

        val projected = projector.project(driver.state)

        projected.getPower(graaz) shouldBe 7
        projected.getToughness(graaz) shouldBe 5

        projected.getPower(myBear) shouldBe 5
        projected.getToughness(myBear) shouldBe 3
        projected.hasSubtype(myBear, "Juggernaut") shouldBe true
        projected.hasSubtype(myBear, "Bear") shouldBe true

        projected.getPower(enemyBear) shouldBe 2
        projected.getToughness(enemyBear) shouldBe 2
        projected.hasSubtype(enemyBear, "Juggernaut") shouldBe false
    }

    test("Juggernauts you control — including ones Graaz made — must attack each combat") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val graaz = driver.putCreatureOnBattlefield(me, "Graaz, Unstoppable Juggernaut")
        driver.removeSummoningSickness(graaz)
        val bear = driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.removeSummoningSickness(bear)

        driver.advanceToPlayer1DeclareAttackers()
        driver.activePlayer shouldBe me

        val none = driver.submit(DeclareAttackers(playerId = me, attackers = emptyMap()))
        none.outcome shouldNotBe Outcome.Done
        none.error shouldContain "must attack"

        // Graaz alone isn't enough — the Bear is a Juggernaut too.
        val onlyGraaz = driver.submit(DeclareAttackers(playerId = me, attackers = mapOf(graaz to opponent)))
        onlyGraaz.outcome shouldNotBe Outcome.Done

        driver.submit(
            DeclareAttackers(playerId = me, attackers = mapOf(graaz to opponent, bear to opponent))
        ).outcome shouldBe Outcome.Done
    }

    test("Juggernauts you control can't be blocked by Walls, but other creatures may block them") {
        val driver = createDriver()
        val me = driver.player1
        val opponent = driver.player2

        val graaz = driver.putCreatureOnBattlefield(me, "Graaz, Unstoppable Juggernaut")
        driver.removeSummoningSickness(graaz)
        val bear = driver.putCreatureOnBattlefield(me, "Test Bear")
        driver.removeSummoningSickness(bear)
        val wall = driver.putCreatureOnBattlefield(opponent, "Test Wall")
        driver.removeSummoningSickness(wall)
        val enemyBear = driver.putCreatureOnBattlefield(opponent, "Test Bear")
        driver.removeSummoningSickness(enemyBear)

        driver.advanceToPlayer1DeclareAttackers()
        driver.declareAttackers(me, listOf(graaz, bear), opponent).outcome shouldBe Outcome.Done
        driver.bothPass()
        driver.currentStep shouldBe Step.DECLARE_BLOCKERS

        driver.submitExpectFailure(DeclareBlockers(opponent, mapOf(wall to listOf(graaz))))
            .outcome shouldNotBe Outcome.Done
        driver.submitExpectFailure(DeclareBlockers(opponent, mapOf(wall to listOf(bear))))
            .outcome shouldNotBe Outcome.Done

        driver.declareBlockers(opponent, mapOf(enemyBear to listOf(bear))).outcome shouldBe Outcome.Done
    }
})
