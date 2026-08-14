package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.TheMountainKingsReturn
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * The Mountain-king's Return (HOB #22) — {2}{W} Enchantment — Saga.
 *
 * I — Recruit.
 * II — Return target creature card with mana value 3 or less from your graveyard to the battlefield.
 * III — Put a +1/+1 counter on up to one target creature.
 *
 * Walks all three chapters in one game: the chapter I recruit must pause for its discard, chapter II
 * must reanimate the mana-value-2 Bears (untapped — this Saga has no "tapped" rider), and chapter III
 * must put a +1/+1 counter on the creature it targets. The Saga is sacrificed after III.
 */
class TheMountainKingsReturnScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(TheMountainKingsReturn))
        return driver
    }

    /** Drain the stack, answering the decisions each chapter raises via [onDecision]. */
    fun GameTestDriver.drain(onDecision: (GameTestDriver) -> Unit) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 50) {
            if (state.pendingDecision != null) onDecision(this) else bothPass()
            guard++
        }
    }

    /**
     * Advance to the precombat main phase of the starting player's [nth] turn — the clock a Saga's
     * lore counters run on. `turnNumber` counts player turns and the two seats alternate, so the
     * starting player's nth turn is turn `2n - 1`.
     */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
    }

    fun GameTestDriver.lore(saga: EntityId): Int =
        state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun GameTestDriver.plusOneCounters(entity: EntityId): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    test("walks chapters I through III: recruit, reanimate, then a +1/+1 counter") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val me = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Chapter II's reanimation target: Grizzly Bears is {1}{G}, mana value 2.
        val bearsCard = driver.putCardInGraveyard(me, "Grizzly Bears")
        // The nonland card chapter I's recruit will discard.
        val lions = driver.putCardInHand(me, "Savannah Lions")

        driver.giveMana(me, Color.WHITE, 3)
        val saga = driver.putCardInHand(me, "The Mountain-king's Return")
        driver.castSpell(me, saga).error shouldBe null

        // --- Chapter I: recruit ---
        driver.drain { d ->
            val decision = d.state.pendingDecision
            if (decision is SelectCardsDecision && lions in decision.options) {
                d.submitCardSelection(me, listOf(lions))
            } else {
                d.autoResolveDecision()
            }
        }

        val sagaPermanent = driver.findPermanent(me, "The Mountain-king's Return")
        sagaPermanent shouldNotBe null
        driver.lore(sagaPermanent!!) shouldBe 1
        driver.state.getZone(me, com.wingedsheep.sdk.core.Zone.GRAVEYARD).contains(lions) shouldBe true
        driver.getPermanents(me).count { driver.getCardName(it) == "Human Soldier Token" } shouldBe 1

        // --- Chapter II: return the Bears from the graveyard to the battlefield ---
        driver.advanceToMain(2)
        driver.drain { d ->
            if (d.state.pendingDecision is ChooseTargetsDecision) {
                d.submitTargetSelection(me, listOf(bearsCard))
            } else {
                d.autoResolveDecision()
            }
        }

        driver.lore(sagaPermanent) shouldBe 2
        val bears = driver.findPermanent(me, "Grizzly Bears")
        bears shouldNotBe null
        driver.isTapped(bears!!) shouldBe false

        // --- Chapter III: +1/+1 counter on the targeted creature ---
        driver.advanceToMain(3)
        driver.drain { d ->
            val decision = d.state.pendingDecision
            if (decision is ChooseTargetsDecision) {
                d.submitTargetSelection(me, listOf(bears))
            } else {
                d.autoResolveDecision()
            }
        }

        driver.plusOneCounters(bears) shouldBe 1
        // Sacrificed after III.
        driver.findPermanent(me, "The Mountain-king's Return") shouldBe null
    }
})
