package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.combat.GoadedComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.spm.cards.MaximumCarnage
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Maximum Carnage — {4}{R} Enchantment — Saga (SPM #83).
 *
 *   I   — Until your next turn, each creature attacks each combat if able and attacks a player
 *         other than you if able.
 *   II  — Add {R}{R}{R}.
 *   III — This Saga deals 5 damage to each opponent.
 *
 * Chapter I is a mass goad by the Saga's controller (CR 701.15): applied to every creature on the
 * battlefield (both players'), with the controller as the goader of record — so each creature
 * gains a [GoadedComponent] listing that controller. Chapter II adds three red mana to the
 * controller's pool. Chapter III deals 5 to each opponent.
 */
class MaximumCarnageScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(MaximumCarnage))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20, startingPlayer = 0)
        return driver
    }

    fun GameTestDriver.pool(player: EntityId): ManaPoolComponent =
        state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()

    fun GameTestDriver.lore(saga: EntityId): Int =
        state.getEntity(saga)?.get<CountersComponent>()?.getCount(CounterType.LORE) ?: 0

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 60) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /** Cast Maximum Carnage from hand and resolve its entry chapter (chapter I). */
    fun GameTestDriver.castCarnage(controller: EntityId): EntityId {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.RED, 1)
        giveColorlessMana(controller, 4)
        val saga = putCardInHand(controller, "Maximum Carnage")
        castSpell(controller, saga)
        resolveStack() // saga enters (lore 1 → chapter I) and its chapter resolves
        return saga
    }

    /** Advance (auto-passing priority / resolving decisions) until the Saga holds [target] lore. */
    fun GameTestDriver.advanceUntilLore(saga: EntityId, target: Int) {
        var guard = 0
        while (lore(saga) < target && guard < 800) {
            if (state.gameOver) throw AssertionError("Game ended before lore $target (step=${state.step})")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> {
                    autoSubmitCombatDeclarationIfNeeded()
                    passPriority(state.priorityPlayerId!!)
                }
            }
            guard++
        }
        if (lore(saga) < target) throw AssertionError("Saga never reached lore $target (step=${state.step})")
    }

    test("chapter I — goads every creature (both players') with you as the goader") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        // Creatures on both sides — "each creature" includes your own.
        val myBear = driver.putCreatureOnBattlefield(me, "Grizzly Bears")
        val oppBear = driver.putCreatureOnBattlefield(opp, "Grizzly Bears")

        val saga = driver.castCarnage(me)

        driver.lore(saga) shouldBe 1
        // Both creatures are goaded, and the goader of record is the Saga's controller ("you").
        driver.state.getEntity(myBear)?.get<GoadedComponent>()?.goaderIds shouldBe setOf(me)
        driver.state.getEntity(oppBear)?.get<GoadedComponent>()?.goaderIds shouldBe setOf(me)
    }

    test("chapter II — adds {R}{R}{R} to your mana pool") {
        val driver = createDriver()
        val me = driver.activePlayer!!

        // No creatures, so chapter I goads nothing and the turn cycle advances cleanly.
        val saga = driver.castCarnage(me)

        // Reach the second lore counter, then resolve chapter II while still in the main phase so
        // the freshly-added mana is observed before it empties at end of step.
        driver.advanceUntilLore(saga, 2)
        driver.resolveStack()

        driver.lore(saga) shouldBe 2
        val pool = driver.pool(me)
        pool.red shouldBe 3
        pool.green shouldBe 0
        pool.colorless shouldBe 0
    }

    test("chapter III — this Saga deals 5 damage to each opponent") {
        val driver = createDriver()
        val me = driver.activePlayer!!
        val opp = driver.getOpponent(me)

        val saga = driver.castCarnage(me)
        val lifeBeforeChapterIII = driver.getLifeTotal(opp)

        driver.advanceUntilLore(saga, 3)
        driver.resolveStack()

        driver.getLifeTotal(opp) shouldBe lifeBeforeChapterIII - 5
    }
})
