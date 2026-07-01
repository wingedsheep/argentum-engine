package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tla.cards.TheLastAgniKai
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * The Last Agni Kai — {1}{R} instant
 *  "Target creature you control fights target creature an opponent controls. If the creature the
 *   opponent controls is dealt excess damage this way, add that much {R}.
 *   Until end of turn, you don't lose unspent red mana as steps and phases end."
 *
 * Two new pieces, composed onto the existing symmetric fight:
 *  - the fight captures the excess damage (CR 120.4a) it deals to the opponent's creature into a
 *    pipeline number, which "add that much {R}" reads — verified for a true excess hit and the
 *    no-excess (exactly-lethal) case;
 *  - a red-filtered, turn-scoped mana retention keeps the controller's red mana through the
 *    end-of-turn pool emptying while every other colour empties as normal.
 */
class TheLastAgniKaiScenarioTest : FunSpec({

    // 5/5 fighter — deals 3 excess to a 2/2.
    val bigFighter = card("Test Agni Big") {
        manaCost = "{4}{R}"
        typeLine = "Creature — Elemental"
        power = 5
        toughness = 5
    }
    // 2/2 fighter — exactly lethal to another 2/2 (no excess).
    val smallFighter = card("Test Agni Small") {
        manaCost = "{1}{R}"
        typeLine = "Creature — Elemental"
        power = 2
        toughness = 2
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheLastAgniKai, bigFighter, smallFighter))
        return driver
    }

    fun GameTestDriver.resolveAll() {
        var guard = 0
        while ((pendingDecision != null || stackSize > 0) && guard < 50) {
            bothPass()
            guard++
        }
    }

    fun GameTestDriver.red(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.red ?: 0

    fun GameTestDriver.green(playerId: EntityId): Int =
        state.getEntity(playerId)?.get<ManaPoolComponent>()?.green ?: 0

    test("excess fight damage to the opponent's creature is added as that much red mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Agni Big")     // 5/5
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Agni Small") // 2/2

        val agniKai = driver.putCardInHand(active, "The Last Agni Kai")
        driver.giveMana(active, Color.RED, 2)  // pays {1}{R}; spent before resolution
        driver.castSpell(active, agniKai, targets = listOf(mine, theirs)).isSuccess shouldBe true
        driver.resolveAll()

        // 5 damage to a 2/2 → lethal 2 → excess 3 → add {R}{R}{R}.
        driver.red(active) shouldBe 3
    }

    test("a fight that deals exactly lethal damage produces no excess and adds no red mana") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Agni Small")     // 2/2
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Agni Small") // 2/2

        val agniKai = driver.putCardInHand(active, "The Last Agni Kai")
        driver.giveMana(active, Color.RED, 2)
        driver.castSpell(active, agniKai, targets = listOf(mine, theirs)).isSuccess shouldBe true
        driver.resolveAll()

        // 2 damage to a 2/2 = exactly lethal, 0 excess → no red added.
        driver.red(active) shouldBe 0
    }

    test("red mana is retained through the end-of-turn pool emptying while other colours empty") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val mine = driver.putCreatureOnBattlefield(active, "Test Agni Big")       // 5/5
        val theirs = driver.putCreatureOnBattlefield(opponent, "Test Agni Small") // 2/2

        val agniKai = driver.putCardInHand(active, "The Last Agni Kai")
        driver.giveMana(active, Color.RED, 2)
        driver.castSpell(active, agniKai, targets = listOf(mine, theirs)).isSuccess shouldBe true
        driver.resolveAll()

        // 3 red from the excess, plus some unprotected green floating in the pool.
        driver.red(active) shouldBe 3
        driver.giveMana(active, Color.GREEN, 2)
        driver.green(active) shouldBe 2

        // Cross this turn's cleanup into the opponent's turn (the engine's only mana-empty point).
        driver.passPriorityUntil(Step.UPKEEP)
        driver.activePlayer shouldBe opponent

        // Red survived the end-of-turn emptying; green did not.
        driver.red(active) shouldBe 3
        driver.green(active) shouldBe 0
    }
})
