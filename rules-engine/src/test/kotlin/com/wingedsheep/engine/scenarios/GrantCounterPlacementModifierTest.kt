package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.DecisionHandler
import com.wingedsheep.engine.handlers.effects.ReplacementEffectUtils
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for [Effects.GrantCounterPlacementModifier] / `GrantCounterPlacementModifierEffect`.
 *
 * This is the temporary, duration-scoped, controller-scoped analogue of the static
 * `ModifyCounterPlacement` replacement (Hardened Scales). It backs Prairie Dog's
 * "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on a creature you
 * control, put that many plus one +1/+1 counters on it instead."
 *
 * The activated ability is driven through the real `ActionProcessor` (proving the executor
 * records the activating player as the modifier's controller), then counter placement is run
 * through the single chokepoint
 * (`ReplacementEffectUtils.applyCounterPlacementModifiers`, the same path every AddCounters-style
 * executor uses), and finally end-of-turn cleanup is run through the real
 * `CleanupPhaseManager.cleanupEndOfTurn`.
 */
class GrantCounterPlacementModifierTest : FunSpec({

    // Inline test stand-in for Prairie Dog's activated ability. We do not implement the real
    // card here (the parent agent owns that); we only need a permanent whose {4}{W} ability
    // installs the modifier, to prove the feature end to end.
    val testGranter = card("Test Counter-Placement Granter") {
        manaCost = "{1}{W}"
        typeLine = "Creature — Mouse"
        power = 1
        toughness = 1
        oracleText = "{4}{W}: Until end of turn, if you would put one or more +1/+1 counters on " +
            "a creature you control, put that many plus one +1/+1 counters on it instead."
        activatedAbility {
            cost = Costs.Mana("{4}{W}")
            effect = Effects.GrantCounterPlacementModifier()
        }
    }

    val grantAbilityId = testGranter.activatedAbilities.first().id

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(testGranter))
        return driver
    }

    /**
     * Activate the granter's {4}{W} ability as [player] and resolve it, returning the driver
     * positioned in the precombat main phase with the modifier active.
     */
    fun setupWithModifierActive(): Triple<GameTestDriver, com.wingedsheep.sdk.model.EntityId, com.wingedsheep.sdk.model.EntityId> {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Plains" to 40), startingLife = 20)
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val granter = driver.putPermanentOnBattlefield(activePlayer, "Test Counter-Placement Granter")
        driver.giveMana(activePlayer, Color.WHITE, 5)
        val result = driver.submit(
            ActivateAbility(playerId = activePlayer, sourceId = granter, abilityId = grantAbilityId)
        )
        result.isSuccess shouldBe true
        driver.bothPass() // resolve the ability → installs the modifier

        withClue("Modifier is installed and controller-scoped to the active player") {
            driver.state.activeCounterPlacementModifiers.size shouldBe 1
            driver.state.activeCounterPlacementModifiers.first().controllerId shouldBe activePlayer
        }
        return Triple(driver, activePlayer, opponent)
    }

    test("controller placing N +1/+1 counters on their own creature yields N+1") {
        val (driver, activePlayer, _) = setupWithModifierActive()
        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state = driver.state,
            targetId = myCreature,
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 3,
            placerId = activePlayer
        )
        modified shouldBe 4
    }

    test("does NOT affect counters placed on an opponent's creature") {
        val (driver, activePlayer, opponent) = setupWithModifierActive()
        val theirCreature = driver.putCreatureOnBattlefield(opponent, "Centaur Courser")

        // "a creature you control" is resolved relative to the modifier's controller (the active
        // player), so the opponent's creature is not a valid recipient even when the active
        // player is the one placing the counters.
        val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state = driver.state,
            targetId = theirCreature,
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 3,
            placerId = activePlayer
        )
        modified shouldBe 3
    }

    test("does NOT apply when the OPPONENT is the placer (controller-scoped 'you')") {
        val (driver, activePlayer, opponent) = setupWithModifierActive()
        // A creature the active player controls, but counters placed by the opponent.
        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state = driver.state,
            targetId = myCreature,
            counterType = CounterType.PLUS_ONE_PLUS_ONE,
            count = 3,
            placerId = opponent
        )
        modified shouldBe 3
    }

    test("only applies to +1/+1 counters, not other counter kinds") {
        val (driver, activePlayer, _) = setupWithModifierActive()
        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        val modified = ReplacementEffectUtils.applyCounterPlacementModifiers(
            state = driver.state,
            targetId = myCreature,
            counterType = CounterType.MINUS_ONE_MINUS_ONE,
            count = 3,
            placerId = activePlayer
        )
        modified shouldBe 3
    }

    test("after end-of-turn cleanup the modifier is gone and N yields N") {
        val (driver, activePlayer, _) = setupWithModifierActive()
        val myCreature = driver.putCreatureOnBattlefield(activePlayer, "Centaur Courser")

        // Sanity: active while present.
        ReplacementEffectUtils.applyCounterPlacementModifiers(
            driver.state, myCreature, CounterType.PLUS_ONE_PLUS_ONE, 3, placerId = activePlayer
        ) shouldBe 4

        // Run the real end-of-turn cleanup.
        val cleanup = com.wingedsheep.engine.core.CleanupPhaseManager(
            driver.cardRegistry, DecisionHandler()
        )
        val cleaned = cleanup.cleanupEndOfTurn(driver.state)
        driver.replaceState(cleaned)

        withClue("EndOfTurn modifier removed by cleanup") {
            driver.state.activeCounterPlacementModifiers.isEmpty() shouldBe true
        }
        ReplacementEffectUtils.applyCounterPlacementModifiers(
            driver.state, myCreature, CounterType.PLUS_ONE_PLUS_ONE, 3, placerId = activePlayer
        ) shouldBe 3
    }
})
