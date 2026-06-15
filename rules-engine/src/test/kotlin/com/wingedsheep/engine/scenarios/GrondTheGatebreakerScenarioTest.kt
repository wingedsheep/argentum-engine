package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.ltr.cards.GrondTheGatebreaker
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Grond, the Gatebreaker — Legendary Artifact — Vehicle, 5/5.
 *   Trample
 *   As long as it's your turn and you control an Army, Grond is an artifact creature.
 *   Crew 3
 *
 * Verifies three things:
 *  - A Vehicle is not a creature by default (printed P/T but only an artifact).
 *  - Crew 3 (tap creatures totalling power >= 3) animates it to an artifact creature
 *    until end of turn, with its printed P/T and Trample.
 *  - The conditional static makes it an artifact creature while it's your turn AND you
 *    control an Army, and not when no Army is present.
 */
class GrondTheGatebreakerScenarioTest : FunSpec({

    // Minimal Army creature for the static-condition tests (a 2/2 Zombie Army).
    val zombieArmy = CardDefinition.creature(
        name = "Test Zombie Army",
        manaCost = ManaCost.parse("{1}{B}"),
        subtypes = setOf(Subtype("Zombie"), Subtype("Army")),
        power = 2,
        toughness = 2,
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCards(listOf(GrondTheGatebreaker, zombieArmy))
        return driver
    }

    test("Grond is not a creature by default — only an artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val grond = driver.putPermanentOnBattlefield(you, "Grond, the Gatebreaker")

        // No Army present: not a creature even though it's your turn.
        driver.state.projectedState.isCreature(grond) shouldBe false
    }

    test("Crew 3 makes Grond an artifact creature with Trample until end of turn") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val grond = driver.putPermanentOnBattlefield(you, "Grond, the Gatebreaker")
        // Force of Nature is 5/5 — power 5 >= Crew 3.
        val crewer = driver.putCreatureOnBattlefield(you, "Force of Nature")

        driver.state.projectedState.isCreature(grond) shouldBe false

        driver.submit(CrewVehicle(you, grond, listOf(crewer))).isSuccess shouldBe true
        driver.bothPass() // resolve the Crew ability

        // Now an artifact creature with printed 5/5 and Trample.
        val projected = driver.state.projectedState
        projected.isCreature(grond) shouldBe true
        projected.getPower(grond) shouldBe 5
        projected.getToughness(grond) shouldBe 5
        projected.hasKeyword(grond, Keyword.TRAMPLE) shouldBe true

        // It can attack (was on the battlefield since the turn began).
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        val opponent = driver.getOpponent(you)
        driver.declareAttackers(you, listOf(grond), opponent).isSuccess shouldBe true
    }

    test("static makes Grond an artifact creature on your turn while you control an Army") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val grond = driver.putPermanentOnBattlefield(you, "Grond, the Gatebreaker")

        // No Army yet — not a creature.
        driver.state.projectedState.isCreature(grond) shouldBe false

        // Control an Army on your turn — now a creature.
        driver.putCreatureOnBattlefield(you, "Test Zombie Army")
        driver.state.projectedState.isCreature(grond) shouldBe true
    }

    test("static does not apply on your turn without an Army") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val you = driver.activePlayer!!

        val grond = driver.putPermanentOnBattlefield(you, "Grond, the Gatebreaker")
        // A non-Army creature does not satisfy the condition.
        driver.putCreatureOnBattlefield(you, "Centaur Courser")

        driver.state.projectedState.isCreature(grond) shouldBe false
    }
})
