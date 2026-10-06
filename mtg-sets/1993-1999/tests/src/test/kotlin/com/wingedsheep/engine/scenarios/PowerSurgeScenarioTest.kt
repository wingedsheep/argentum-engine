package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.PowerSurge
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Power Surge (LEA #167) — "At the beginning of each player's upkeep, this enchantment deals X
 * damage to that player, where X is the number of untapped lands they controlled at the beginning
 * of this turn."
 *
 * Setup runs on player 2's turn, so player 1's untap step — where the engine takes the snapshot —
 * is one transition away. Each test makes the turn-start count disagree with the live count at
 * resolution, which is the only thing that tells the printed card apart from "untapped lands you
 * control".
 */
class PowerSurgeScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + PowerSurge)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingPlayer = 1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.putPermanentOnBattlefield(d.player2, "Power Surge")
        return d
    }

    fun resolveStack(d: GameTestDriver) {
        var guard = 0
        while (guard++ < 30 && d.state.stack.isNotEmpty() && !d.isPaused) d.bothPass()
    }

    test("lands tapped when the turn began don't count, even though they untapped since") {
        val d = driver()
        val lands = List(3) { d.putLandOnBattlefield(d.player1, "Mountain") }
        d.tapPermanent(lands[0])
        repeat(4) { d.putLandOnBattlefield(d.player2, "Mountain") }

        d.passPriorityUntil(Step.UPKEEP)
        d.activePlayer shouldBe d.player1
        withClue("all three lands untapped in the untap step") {
            lands.none { d.isTapped(it) } shouldBe true
        }
        resolveStack(d)

        withClue("two were untapped at the turn's start; the opponent's lands are not counted") {
            d.getLifeTotal(d.player1) shouldBe 18
            d.getLifeTotal(d.player2) shouldBe 20
        }
    }

    test("tapping lands after the turn began doesn't shrink the damage") {
        val d = driver()
        val lands = List(3) { d.putLandOnBattlefield(d.player1, "Mountain") }

        d.passPriorityUntil(Step.UPKEEP)
        withClue("the trigger is waiting on the stack") {
            d.state.stack.isNotEmpty() shouldBe true
        }
        lands.forEach { d.tapPermanent(it) }
        resolveStack(d)

        d.getLifeTotal(d.player1) shouldBe 17
    }
})
