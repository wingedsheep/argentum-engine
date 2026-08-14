package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tdm.cards.RediscoverTheWay
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Rediscover the Way (TDM #215) chapter III — the *targeting* event-based delayed
 * trigger.
 *
 *   III — Whenever you cast a noncreature spell this turn, target creature you control gains
 *         double strike until end of turn.
 *
 * Chapter III installs `CreateDelayedTriggerEffect(trigger = YouCastNoncreature, fireOnce = false,
 * expiry = EndOfTurn, targetRequirement = Targets.CreatureYouControl, effect = GrantKeyword(DOUBLE_STRIKE))`.
 * Each noncreature spell cast that turn must prompt for a target creature you control and grant it
 * double strike until end of turn — exercising the new
 * [com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect.targetRequirement].
 */
class RediscoverTheWayScenarioTest : FunSpec({

    val bear = card("Test Bear") {
        manaCost = "{1}{G}"
        typeLine = "Creature — Bear"
        power = 2
        toughness = 2
    }
    val bolt = card("Test Bolt") {
        manaCost = "{R}"
        typeLine = "Instant"
        spell { effect = com.wingedsheep.sdk.dsl.Effects.DealDamage(3, com.wingedsheep.sdk.scripting.targets.EffectTarget.PlayerRef(com.wingedsheep.sdk.scripting.references.Player.EachOpponent)) }
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(RediscoverTheWay, bear, bolt))
        return driver
    }

    fun GameTestDriver.resolveStack() {
        var guard = 0
        while (state.stack.isNotEmpty() && guard < 50) {
            if (state.pendingDecision != null) autoResolveDecision() else bothPass()
            guard++
        }
    }

    /**
     * Advance to the precombat main phase of the starting player's [nth] turn — the clock a Saga's
     * lore counters run on. `GameState.turnNumber` counts player turns, and these are duels where
     * the two seats alternate, so the starting player's nth turn is turn `2n - 1`.
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

    fun GameTestDriver.castSaga(controller: EntityId) {
        passPriorityUntil(Step.PRECOMBAT_MAIN)
        giveMana(controller, Color.BLUE, 1)
        giveMana(controller, Color.RED, 1)
        giveMana(controller, Color.WHITE, 1)
        // Library cards so the chapter I/II "look at top three" has something to look at.
        repeat(4) { putCardOnTopOfLibrary(controller, "Mountain") }
        val saga = putCardInHand(controller, "Rediscover the Way")
        castSpell(controller, saga)
        resolveStack() // saga enters (lore 1 → chapter I) and chapter I resolves
    }

    test("Chapter III grants double strike to a chosen creature when you cast a noncreature spell") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Mountain" to 40), startingLife = 20)
        val controller = driver.activePlayer!!

        driver.castSaga(controller)
        driver.advanceToMain(2) // chapter II
        driver.resolveStack()
        driver.advanceToMain(3) // chapter III installs the delayed trigger
        driver.resolveStack()

        // The delayed double-strike trigger should now be resident.
        driver.state.delayedTriggers.size shouldBe 1

        // Put a creature on the battlefield to receive double strike, then cast a noncreature spell.
        val target = driver.putCreatureOnBattlefield(controller, "Test Bear")
        driver.giveMana(controller, Color.RED, 1)
        val boltCard = driver.putCardInHand(controller, "Test Bolt")
        driver.submit(CastSpell(playerId = controller, cardId = boltCard))

        // Resolve the cast trigger: it should prompt to target our creature, then resolve the bolt.
        var guard = 0
        while ((driver.state.stack.isNotEmpty() || driver.state.pendingDecision != null) && guard < 50) {
            val decision = driver.state.pendingDecision
            if (decision is com.wingedsheep.engine.core.ChooseTargetsDecision) {
                driver.submitTargetSelection(controller, listOf(target))
            } else if (decision != null) {
                driver.autoResolveDecision()
            } else {
                driver.bothPass()
            }
            guard++
        }

        driver.state.projectedState.hasKeyword(target, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
