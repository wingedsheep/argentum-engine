package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
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
import io.kotest.matchers.shouldNotBe

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
 *  2. X clamps the number of targets, so an X=2 cast must offer at most two target slots and reject
 *     a third;
 *  3. the hexproof static is continuous over `tapped().youControl()`, not a snapshot — a creature
 *     that untaps loses it again.
 *
 * The targets belong to the *enters* trigger, not to the spell, so they are chosen from a
 * [ChooseTargetsDecision] after the enchantment resolves rather than at cast time. That is also why
 * the amount is [com.wingedsheep.sdk.scripting.values.DynamicAmount.CastX] and not `XValue` — by the
 * time the trigger asks, the spell's resolution context is gone.
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

    /**
     * Drain the stack, stopping the moment it empties or something needs answering.
     *
     * Deliberately *not* a fixed number of passes: passing on an empty stack walks the turn
     * forward, and the opponent's untap step spends the stun counter this test is asserting on
     * (CR 701.22 — a permanent with a stun counter loses one instead of untapping).
     */
    fun GameTestDriver.resolveStack(maxPasses: Int = 8) {
        var guard = 0
        while (state.stack.isNotEmpty() && pendingDecision == null && guard++ < maxPasses) bothPass()
    }

    /** Cast the Maze for [xValue] and resolve it up to the enters trigger's target decision. */
    fun GameTestDriver.castMaze(xValue: Int): ChooseTargetsDecision {
        val maze = putCardInHand(player1, "Lost in the Maze")
        giveMana(player1, Color.BLUE, xValue + 2)
        castXSpell(player1, maze, xValue = xValue).error shouldBe null
        resolveStack()
        return pendingDecision as? ChooseTargetsDecision
            ?: error("Expected a ChooseTargetsDecision for the enters trigger, got $pendingDecision")
    }

    test("X=2 taps both targets but stuns only the one you don't control") {
        val driver = newDriver()
        val mine = driver.putCreatureOnBattlefield(driver.player1, "Grizzly Bears")
        val theirs = driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        driver.castMaze(xValue = 2)
        driver.submitTargetSelection(driver.player1, listOf(mine, theirs)).error shouldBe null
        driver.resolveStack()

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

        driver.castMaze(xValue = 1)
        driver.submitTargetSelection(driver.player1, listOf(mine)).error shouldBe null
        driver.resolveStack()

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

        driver.castMaze(xValue = 1)
        driver.submitTargetSelection(driver.player1, listOf(theirs)).error shouldBe null
        driver.resolveStack()

        driver.isTapped(theirs) shouldBe true
        withClue("'you control' scopes the hexproof to the Maze's controller") {
            StateProjector().project(driver.state).hasKeyword(theirs, Keyword.HEXPROOF) shouldBe false
        }
    }

    test("X clamps the target count — an X=2 cast offers two slots and rejects a third") {
        val driver = newDriver()
        val a = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val b = driver.putCreatureOnBattlefield(driver.player2, "Grizzly Bears")
        val c = driver.putCreatureOnBattlefield(driver.player2, "Minotaur Warrior")

        val decision = driver.castMaze(xValue = 2)

        withClue("dynamicMaxCount = CastX caps the requirement at the X actually paid") {
            decision.targetRequirements.single().maxTargets shouldBe 2
        }
        withClue("'up to X' means a zero-target choice is legal too") {
            decision.targetRequirements.single().minTargets shouldBe 0
        }
        withClue("submitting a third target beyond the X paid must be rejected") {
            driver.submitTargetSelection(driver.player1, listOf(a, b, c)).error shouldNotBe null
        }
    }
})
