package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.LostInTheMaze
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Lost in the Maze — "Flash. When this enchantment enters, tap X target creatures. Put a stun counter
 * on each of those creatures you don't control. Tapped creatures you control have hexproof."
 *
 * The card is asymmetric in a way that is easy to implement symmetrically by accident, so the tests
 * are built around the split:
 *
 *  1. every chosen target gets tapped, but only the ones you *don't* control get a stun counter —
 *     an implementation that stunned all targets, or none, passes a single-target test either way,
 *     so the discriminating case targets one creature on each side at once;
 *  2. X clamps the number of targets, so X=2 must not permit a third;
 *  3. the hexproof static is continuous over `tapped().youControl()`, not a snapshot — a creature
 *     that untaps loses it again.
 */
class LostInTheMazeScenarioTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(LostInTheMaze)
        driver.initMirrorMatch(Deck.of("Island" to 40), skipMulligans = true, startingPlayer = 0)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun stunCount(driver: GameTestDriver, entity: EntityId): Int =
        driver.state.getEntity(entity)?.get<CountersComponent>()
            ?.getCount(CounterType.STUN) ?: 0

    test("X=2 taps both targets but stuns only the one you don't control") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        val maze = driver.putCardInHand(driver.player1, "Lost in the Maze")
        driver.giveMana(driver.player1, Color.BLUE, 4)
        driver.castXSpell(driver.player1, maze, xValue = 2, targets = listOf(mine, theirs))
            .error shouldBe null
        driver.bothPass() // resolve the enchantment; its enters trigger goes on the stack
        driver.bothPass() // resolve the trigger

        withClue("'tap X target creatures' is not scoped by controller") {
            driver.isTapped(mine) shouldBe true
            driver.isTapped(theirs) shouldBe true
        }
        withClue("only creatures you don't control are stunned") {
            stunCount(driver, theirs) shouldBe 1
            stunCount(driver, mine) shouldBe 0
        }
    }

    test("tapped creatures you control have hexproof, and lose it when they untap") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")

        val maze = driver.putCardInHand(driver.player1, "Lost in the Maze")
        driver.giveMana(driver.player1, Color.BLUE, 3)
        driver.castXSpell(driver.player1, maze, xValue = 1, targets = listOf(mine)).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.isTapped(mine) shouldBe true
        withClue("the static grants hexproof to your tapped creatures") {
            StateProjector().project(driver.state).hasKeyword(mine, Keyword.HEXPROOF) shouldBe true
        }

        driver.untapPermanent(mine)
        withClue("the grant is continuous — untapping takes it away again") {
            StateProjector().project(driver.state).hasKeyword(mine, Keyword.HEXPROOF) shouldBe false
        }
    }

    test("an opponent's tapped creature gets no hexproof from your Maze") {
        val driver = newDriver()
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        val maze = driver.putCardInHand(driver.player1, "Lost in the Maze")
        driver.giveMana(driver.player1, Color.BLUE, 3)
        driver.castXSpell(driver.player1, maze, xValue = 1, targets = listOf(theirs)).error shouldBe null
        driver.bothPass()
        driver.bothPass()

        driver.isTapped(theirs) shouldBe true
        withClue("'you control' scopes the hexproof to the Maze's controller") {
            StateProjector().project(driver.state).hasKeyword(theirs, Keyword.HEXPROOF) shouldBe false
        }
    }

    test("X clamps the target count — three targets on an X=2 cast is illegal") {
        val driver = newDriver()
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val c = driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        val maze = driver.putCardInHand(driver.player1, "Lost in the Maze")
        driver.giveMana(driver.player1, Color.BLUE, 4)
        val result = driver.castXSpell(driver.player1, maze, xValue = 2, targets = listOf(a, b, c))

        withClue("dynamicMaxCount = XValue must reject a target beyond the X paid") {
            result.isSuccess shouldBe false
        }
    }
})
