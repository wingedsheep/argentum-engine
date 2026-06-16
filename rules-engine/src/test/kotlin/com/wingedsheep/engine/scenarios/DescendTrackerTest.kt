package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.ZoneTransitionService
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.state.components.player.PlayerDescendedThisTurnComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for the descend tracker (CR 700.11).
 *
 * "Some cards refer to whether a player has 'descended this turn.' This means that a
 * permanent card has been put into that player's graveyard from anywhere this turn.
 * 'The number of times [a player] descended this turn' means 'the number of permanent
 * cards put into [that player's] graveyard from anywhere this turn.' In both cases, no
 * permanent cards put into the player's graveyard that turn are required to still be
 * in that graveyard." — CR 700.11
 *
 * Backed by `PlayerDescendedThisTurnComponent`, incremented in `ZoneTransitionService`
 * whenever a permanent (nontoken) card lands in a player's graveyard, and cleared at
 * cleanup by `CleanupPhaseManager`. Surfaced via `TurnTracker.DESCENDED` so it composes
 * with the existing `Conditions.YouDescendedThisTurn(atLeast)` / `DynamicAmount` chain.
 */
class DescendTrackerTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of(
                "Forest" to 10,
                "Plains" to 10,
                "Mountain" to 5,
                "Grizzly Bears" to 5,
                "Lightning Bolt" to 5,
                "Centaur Courser" to 5
            ),
            skipMulligans = true
        )
        return driver
    }

    fun GameTestDriver.descendCount(playerId: EntityId): Int =
        state.getEntity(playerId)
            ?.get<PlayerDescendedThisTurnComponent>()?.count ?: 0

    fun GameTestDriver.evalDescended(atLeast: Int = 1): Boolean {
        val controller = activePlayer!!
        val context = EffectContext(
            sourceId = null,
            controllerId = controller,
            targets = emptyList(),
            xValue = 0
        )
        return ConditionEvaluator().evaluate(state, Conditions.YouDescendedThisTurn(atLeast), context)
    }

    fun GameTestDriver.move(entityId: EntityId, destination: Zone) {
        val result = ZoneTransitionService.moveToZone(
            state = state,
            entityId = entityId,
            destinationZone = destination
        )
        replaceState(result.state)
    }

    test("starts at zero and condition is false") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.descendCount(player) shouldBe 0
        driver.evalDescended() shouldBe false
    }

    test("creature destroyed (battlefield → graveyard) counts as descend") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.descendCount(player) shouldBe 0

        driver.move(bears, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 1
        driver.evalDescended() shouldBe true
    }

    test("permanent card discarded (hand → graveyard) counts as descend") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.descendCount(player) shouldBe 0

        driver.move(bears, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 1
        driver.evalDescended() shouldBe true
    }

    test("permanent card milled (library → graveyard) counts as descend") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Put a known permanent card on top of the library.
        val courser = driver.putCardOnTopOfLibrary(player, "Centaur Courser")
        driver.descendCount(player) shouldBe 0

        driver.move(courser, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 1
    }

    test("land going to graveyard counts as descend (lands are permanent cards)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val forest = driver.putCardInHand(player, "Forest")
        driver.move(forest, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 1
    }

    test("non-permanent card (instant) going to graveyard does NOT count") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bolt = driver.putCardInHand(player, "Lightning Bolt")
        driver.move(bolt, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 0
        driver.evalDescended() shouldBe false
    }

    test("token going to graveyard does NOT count, even though it's a permanent card type") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Build a Saproling token entity by hand — `putCreatureOnBattlefield` won't
        // attach `TokenComponent`, and that's the marker the descend hook checks.
        val tokenId = EntityId.generate()
        val tokenContainer = ComponentContainer.of(
            CardComponent(
                cardDefinitionId = "token:Saproling",
                name = "Saproling Token",
                manaCost = ManaCost.ZERO,
                typeLine = TypeLine.parse("Creature - Saproling"),
                baseStats = CreatureStats(1, 1),
                colors = setOf(Color.GREEN),
                ownerId = player
            ),
            OwnerComponent(player),
            TokenComponent,
            ControllerComponent(player),
            SummoningSicknessComponent
        )
        driver.replaceState(
            driver.state
                .withEntity(tokenId, tokenContainer)
                .addToZone(ZoneKey(player, Zone.BATTLEFIELD), tokenId)
        )

        driver.move(tokenId, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 0
        driver.evalDescended() shouldBe false
    }

    test("counts accumulate across multiple permanent cards entering the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        val courser = driver.putCardInHand(player, "Centaur Courser")
        val forest = driver.putCardInHand(player, "Forest")

        driver.move(bears, Zone.GRAVEYARD)
        driver.move(courser, Zone.GRAVEYARD)
        driver.move(forest, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 3
        driver.evalDescended(atLeast = 1) shouldBe true
        driver.evalDescended(atLeast = 3) shouldBe true
        driver.evalDescended(atLeast = 4) shouldBe false
    }

    test("count is keyed on owner — opponent's descends do not satisfy your condition") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        val opponentBears = driver.putCardInHand(opponent, "Grizzly Bears")
        driver.move(opponentBears, Zone.GRAVEYARD)

        driver.descendCount(opponent) shouldBe 1
        driver.descendCount(player) shouldBe 0
        driver.evalDescended() shouldBe false // active player has not descended
    }

    test("condition stays true after the card has left the graveyard (CR 700.11)") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val bears = driver.putCardInHand(player, "Grizzly Bears")
        driver.move(bears, Zone.GRAVEYARD)

        driver.descendCount(player) shouldBe 1
        driver.evalDescended() shouldBe true

        // Exile the card out of the graveyard — descended is still true for the turn.
        driver.move(bears, Zone.EXILE)

        driver.descendCount(player) shouldBe 1
        driver.evalDescended() shouldBe true
    }

    test("count resets at end of turn") {
        val driver = createDriver()
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val bears = driver.putCreatureOnBattlefield(player, "Grizzly Bears")
        driver.move(bears, Zone.GRAVEYARD)
        driver.descendCount(player) shouldBe 1

        // Advance to the end step, then bothPass through cleanup — TurnManager
        // wipes the descend counter as the turn rolls over.
        driver.passPriorityUntil(Step.END, maxPasses = 200)
        driver.bothPass()
        driver.activePlayer shouldBe opponent

        driver.descendCount(player) shouldBe 0
    }
})
