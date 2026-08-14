package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.woe.cards.DiscerningFinancier
import com.wingedsheep.mtg.sets.tokens.PredefinedTokens
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Discerning Financier (WOE) — {2}{W} Creature — Human Noble, 2/3.
 *
 * - "At the beginning of your upkeep, if an opponent controls more lands than you, create a
 *   Treasure token."
 * - "{2}{W}: Choose another player. That player gains control of target Treasure you control.
 *   You draw a card."
 *
 * The upkeep half is an intervening-if trigger, so it's pinned both ways — behind on lands and
 * not. The activated half is the donate: the Treasure has to end up under the *other* player's
 * control while the card goes to us.
 */
class DiscerningFinancierScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + PredefinedTokens.allTokens + listOf(DiscerningFinancier))
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        return driver
    }

    /** Drain the stack, auto-answering anything that pauses. */
    fun GameTestDriver.drain() {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    fun GameTestDriver.treasures(playerId: EntityId): List<EntityId> =
        getPermanents(playerId).filter { getCardName(it) == "Treasure" }

    /** Advance to the upkeep of the starting player's [nth] turn (turn `2n - 1` in a duel). */
    fun GameTestDriver.advanceToUpkeep(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.UPKEEP) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn upkeep")
    }

    test("upkeep makes a Treasure only while an opponent is ahead on lands") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        driver.putPermanentOnBattlefield(me, "Discerning Financier")
        // Both players on one land: nobody is ahead, so the intervening-if fails.
        driver.putPermanentOnBattlefield(me, "Plains")
        driver.putPermanentOnBattlefield(opponent, "Plains")

        driver.advanceToUpkeep(2)
        driver.drain()

        withClue("equal land counts — 'more lands than you' is strict, so no Treasure") {
            driver.treasures(me).size shouldBe 0
        }

        // Put the opponent ahead and come back round to our next upkeep.
        driver.putPermanentOnBattlefield(opponent, "Plains")
        driver.advanceToUpkeep(3)
        driver.drain()

        withClue("the opponent now controls more lands, so the trigger creates a Treasure") {
            driver.treasures(me).size shouldBe 1
        }
    }

    test("the activated ability donates the Treasure and draws us a card") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opponent = driver.getOpponent(me)

        val financier = driver.putPermanentOnBattlefield(me, "Discerning Financier")
        val treasure = driver.putPermanentOnBattlefield(me, "Treasure")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val handBefore = driver.getHandSize(me)
        driver.giveMana(me, Color.WHITE, 3)

        val donate = DiscerningFinancier.activatedAbilities[0].id
        driver.submitSuccess(
            ActivateAbility(
                playerId = me,
                sourceId = financier,
                abilityId = donate,
                targets = listOf(ChosenTarget.Permanent(treasure))
            )
        )
        driver.drain()

        withClue("the chosen other player ends up controlling the Treasure") {
            // Control change is a Layer 2 continuous effect, so read it off the projection.
            driver.state.projectedState.getController(treasure) shouldBe opponent
        }
        withClue("and we draw the card") {
            driver.getHandSize(me) shouldBe handBefore + 1
        }
    }
})
