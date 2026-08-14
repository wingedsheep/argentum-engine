package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.hob.cards.OldFatSpiderCantSeeMe
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Old Fat Spider Can't See Me (HOB #50) — {2}{U} Enchantment — Saga.
 *
 * I — Target creature you control gains hexproof for as long as this Saga remains on the battlefield.
 * II — Prevent all damage that would be dealt by up to one target creature for as long as this Saga
 *      remains on the battlefield.
 * III, IV — Draw a card.
 *
 * Both open-ended chapters hang on [com.wingedsheep.sdk.scripting.Duration.WhileSourceOnBattlefield],
 * so the interesting claims are about *when they stop*: the hexproof has to outlive the turn it was
 * granted (an `EndOfTurn` duration would silently pass a same-turn assertion) and has to end when
 * the Saga is sacrificed after chapter IV. The chapter II shield is source-side — the named creature
 * deals no damage to anything.
 */
class OldFatSpiderCantSeeMeScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + OldFatSpiderCantSeeMe)
        return driver
    }

    /** Drain the stack, answering every target request with [targets] (empty = decline). */
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
            if (state.gameOver) throw AssertionError("Game ended while advancing to turn $targetTurn")
            when {
                state.pendingDecision != null -> autoResolveDecision()
                state.priorityPlayerId != null -> bothPass()
                else -> break
            }
            guard++
        }
        if (guard >= 500) error("Failed to reach turn $targetTurn precombat main")
    }

    fun GameTestDriver.castSaga(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.BLUE, 3)
        castSpell(controller, putCardInHand(controller, "Old Fat Spider Can't See Me"))
    }

    test("chapter I's hexproof outlives the turn and ends when the Saga is sacrificed after IV") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        val bears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.castSaga(controller)
        driver.drainTargeting(controller, listOf(bears)) // chapter I targets the Bears

        withClue("chapter I granted hexproof") {
            driver.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        }

        driver.advanceToMain(2) // lore 2 → chapter II
        driver.drainTargeting(controller, emptyList()) // decline the "up to one target"

        withClue("'for as long as this Saga remains' is not 'until end of turn'") {
            driver.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe true
        }

        driver.advanceToMain(3) // lore 3 → chapter III draws
        driver.drainTargeting(controller, emptyList())
        driver.advanceToMain(4) // lore 4 → chapter IV draws, then the Saga is sacrificed
        driver.drainTargeting(controller, emptyList())

        withClue("a four-chapter Saga is sacrificed after its last chapter resolves") {
            driver.getGraveyardCardNames(controller).contains("Old Fat Spider Can't See Me") shouldBe true
        }
        withClue("the Saga leaving the battlefield ends the hexproof") {
            driver.state.projectedState.hasKeyword(bears, Keyword.HEXPROOF) shouldBe false
        }
    }

    test("chapter II stops the named creature from dealing combat damage") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        val controller = driver.activePlayer!!
        val opponent = driver.getOpponent(controller)

        val bears = driver.putCreatureOnBattlefield(controller, "Grizzly Bears")

        driver.castSaga(controller)
        driver.drainTargeting(controller, listOf(bears)) // chapter I

        driver.advanceToMain(2) // lore 2 → chapter II
        driver.drainTargeting(controller, listOf(bears)) // shield the Bears

        val turn = driver.state.turnNumber
        driver.passPriorityUntil(Step.DECLARE_ATTACKERS)
        withClue("combat must happen on the same turn chapter II resolved") {
            driver.state.turnNumber shouldBe turn
        }
        driver.declareAttackers(controller, listOf(bears), opponent)
        driver.declareNoBlockers(opponent)
        driver.drainTargeting(controller, emptyList())

        withClue("an unblocked 2/2 whose damage is all prevented deals nothing") {
            driver.getLifeTotal(opponent) shouldBe 20
        }
    }
})
