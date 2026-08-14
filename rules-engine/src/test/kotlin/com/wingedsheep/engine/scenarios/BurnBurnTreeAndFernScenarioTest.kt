package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.BurnBurnTreeAndFern
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Chapter-by-chapter test for Burn, Burn, Tree and Fern (HOB #90) — {3}{R} Enchantment — Saga.
 *
 * I — This Saga deals 6 damage to target creature an opponent controls.
 * II — Destroy target artifact an opponent controls.
 * III, IV — Add {R}.
 *
 * The interesting part is the shared "III, IV" line — declared as two chapters — and the fact that
 * both targeting chapters are opponent-scoped, so nothing on the controller's own side is at risk.
 */
class BurnBurnTreeAndFernScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.registerCard(BurnBurnTreeAndFern)
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

    /** Drain, answering every target request with [targets]. */
    fun GameTestDriver.drainTargeting(chooser: EntityId, targets: List<EntityId>) {
        var guard = 0
        while ((state.stack.isNotEmpty() || state.pendingDecision != null) && guard < 60) {
            val decision = state.pendingDecision
            when {
                decision is ChooseTargetsDecision -> submitTargetSelection(chooser, targets)
                decision != null -> autoResolveDecision()
                else -> bothPass()
            }
            guard++
        }
    }

    /** Advance to the precombat main of the starting player's [nth] turn (a duel: turn 2n − 1). */
    fun GameTestDriver.advanceToMain(nth: Int) {
        val targetTurn = nth * 2 - 1
        var guard = 0
        while (!(state.turnNumber == targetTurn && state.step == Step.PRECOMBAT_MAIN) && guard < 500) {
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    test("I burns an opposing creature, II destroys an opposing artifact, III and IV each add {R}") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        // The opponent's board: a creature for chapter I and an artifact for chapter II.
        val giant = driver.putCreatureOnBattlefield(opponent, "Hill Giant")
        val ornithopter = driver.putPermanentOnBattlefield(opponent, "Ornithopter")
        // Our own creature and artifact must survive — both chapters are opponent-scoped.
        val ourBears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        driver.giveMana(controller, Color.RED, 4)
        val saga = driver.putCardInHand(controller, "Burn, Burn, Tree and Fern")
        driver.castSpell(controller, saga)
        driver.drainTargeting(controller, listOf(giant))

        withClue("chapter I deals 6 damage to the 3/3, killing it") {
            driver.findPermanent(opponent, "Hill Giant") shouldBe null
            driver.getGraveyardCardNames(opponent).contains("Hill Giant") shouldBe true
        }
        withClue("our own creature is untouched") {
            driver.findPermanent(controller, "Grizzly Bears") shouldBe ourBears
        }

        driver.advanceToMain(2) // lore 2 → chapter II
        driver.drainTargeting(controller, listOf(ornithopter))

        withClue("chapter II destroys the opponent's artifact") {
            driver.findPermanent(opponent, "Ornithopter") shouldBe null
            driver.getGraveyardCardNames(opponent).contains("Ornithopter") shouldBe true
        }

        driver.advanceToMain(3) // lore 3 → chapter III
        driver.drain()

        withClue("chapter III adds {R} to the controller's pool") {
            driver.state.getEntity(controller)?.get<ManaPoolComponent>()?.red shouldBe 1
        }

        driver.advanceToMain(4) // lore 4 → chapter IV
        driver.drain()

        withClue("chapter IV adds {R} as well") {
            driver.state.getEntity(controller)?.get<ManaPoolComponent>()?.red shouldBe 1
        }
        withClue("a four-chapter Saga is sacrificed after its last chapter resolves") {
            driver.findPermanent(controller, "Burn, Burn, Tree and Fern") shouldBe null
        }
    }
})
