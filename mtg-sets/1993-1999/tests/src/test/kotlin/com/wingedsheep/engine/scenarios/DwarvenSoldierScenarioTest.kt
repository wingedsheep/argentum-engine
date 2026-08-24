package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.fem.cards.DwarvenSoldier
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Dwarven Soldier (Fallen Empires).
 *
 * "Whenever this creature blocks or becomes blocked by **one or more** Orcs, this creature gets
 * +0/+2 until end of turn." The load-bearing words are "one or more": however many Orcs pile in,
 * the ability triggers once, so the Soldier is a 2/3 against one Orc and a 2/3 against three. The
 * engine's default for this event is one trigger *per* matching partner — the right reading for the
 * singular "blocked by a creature" wording — which is why the card asks for `oncePerCombat`.
 */
class DwarvenSoldierScenarioTest : FunSpec({

    val projector = StateProjector()

    // Locally defined so the P/T arithmetic is pinned here rather than inherited from a shared
    // catalog stub. Two Orcs, because the bug only shows up from the second one onward.
    val orcRaider = card("Test Orc Raider") {
        manaCost = "{2}{R}"
        typeLine = "Creature — Orc Warrior"
        power = 1
        toughness = 1
    }
    // 0 power on purpose: with no pump the Soldier is a 2/1, and any blocker that deals damage
    // would kill it before the projection could be read.
    val plainBear = card("Test Plain Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 0
        toughness = 1
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(DwarvenSoldier)
        driver.registerCard(orcRaider)
        driver.registerCard(plainBear)
        return driver
    }

    test("blocked by one Orc, the Soldier gets +0/+2 once") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(alice, "Dwarven Soldier")
        driver.removeSummoningSickness(soldier)
        val orc = driver.putCreatureOnBattlefield(bob, "Test Orc Raider")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(soldier), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(orc to listOf(soldier)))
        driver.bothPass()

        val projected = projector.project(driver.state)
        withClue("2/1 base plus a single +0/+2") {
            projected.getPower(soldier) shouldBe 2
            projected.getToughness(soldier) shouldBe 3
        }
    }

    test("blocked by two Orcs, the Soldier still gets only +0/+2") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(alice, "Dwarven Soldier")
        driver.removeSummoningSickness(soldier)
        val orcOne = driver.putCreatureOnBattlefield(bob, "Test Orc Raider")
        val orcTwo = driver.putCreatureOnBattlefield(bob, "Test Orc Raider")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(soldier), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(orcOne to listOf(soldier), orcTwo to listOf(soldier)))

        // The load-bearing assertion. Without `oncePerCombat` the detector fans the event out per
        // matching partner and puts *two* copies of the ability on the stack. The projected
        // toughness alone would not catch that: the two identical +0/+2 continuous effects collapse
        // into one, so the P/T reads correctly for the wrong reason.
        withClue("\"one or more Orcs\" is a single trigger, however many Orcs block") {
            driver.state.stack.size shouldBe 1
        }
        driver.bothPass()
    }

    test("blocked by a non-Orc, the Soldier is not pumped at all") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)

        val alice = driver.activePlayer!!
        val bob = driver.getOpponent(alice)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val soldier = driver.putCreatureOnBattlefield(alice, "Dwarven Soldier")
        driver.removeSummoningSickness(soldier)
        val bear = driver.putCreatureOnBattlefield(bob, "Test Plain Bear")

        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        driver.declareAttackers(alice, listOf(soldier), bob)
        driver.passPriorityUntil(Step.DECLARE_BLOCKERS)
        driver.declareBlockers(bob, mapOf(bear to listOf(soldier)))
        driver.bothPass()

        val projected = projector.project(driver.state)
        withClue("the partner filter is Orcs only") {
            projected.getPower(soldier) shouldBe 2
            projected.getToughness(soldier) shouldBe 1
        }
    }
})
