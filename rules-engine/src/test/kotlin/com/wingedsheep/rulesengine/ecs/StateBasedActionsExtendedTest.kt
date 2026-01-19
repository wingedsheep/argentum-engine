package com.wingedsheep.rulesengine.ecs

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.CounterType
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.action.*
import com.wingedsheep.rulesengine.ecs.components.*
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class StateBasedActionsExtendedTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(
            player1Id to "Alice",
            player2Id to "Bob"
        )
    )

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    val legendaryDragonDef = CardDefinition.creature(
        name = "Legendary Dragon",
        manaCost = ManaCost.parse("{3}{R}{R}"),
        subtypes = setOf(Subtype.DRAGON),
        power = 5,
        toughness = 5,
        supertypes = setOf(Supertype.LEGENDARY)
    )

    val auraDef = CardDefinition(
        name = "Holy Strength",
        manaCost = ManaCost.parse("{W}"),
        typeLine = TypeLine(
            cardTypes = setOf(CardType.ENCHANTMENT),
            subtypes = setOf(Subtype.AURA)
        ),
        creatureStats = null,
        keywords = emptySet()
    )

    context("Legend Rule (704.5j)") {
        test("does nothing with only one legendary") {
            var state = newGame()

            // Create one legendary creature
            val dragon1 = EntityId.generate()
            val (_, stateWithDragon) = state.createEntity(
                dragon1,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val stateOnBattlefield = stateWithDragon.addToZone(dragon1, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(stateOnBattlefield)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state
            newState.getBattlefield().shouldContain(dragon1)
            newState.hasPendingLegendRuleChoices.shouldBeFalse()
        }

        test("creates pending choice when player controls duplicate legendaries") {
            var state = newGame()

            // Create two legendary creatures with the same name controlled by player1
            val dragon1 = EntityId.generate()
            val dragon2 = EntityId.generate()

            val (_, s1) = state.createEntity(
                dragon1,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s2 = s1.addToZone(dragon1, ZoneId.BATTLEFIELD)

            val (_, s3) = s2.createEntity(
                dragon2,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s4 = s3.addToZone(dragon2, ZoneId.BATTLEFIELD)

            // Should have 2 dragons on battlefield
            s4.getBattlefield().shouldHaveSize(2)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s4)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Both should still be on battlefield (waiting for player choice)
            newState.getBattlefield().shouldHaveSize(2)
            newState.getBattlefield().shouldContain(dragon1)
            newState.getBattlefield().shouldContain(dragon2)

            // Should have a pending choice
            newState.hasPendingLegendRuleChoices.shouldBeTrue()
            newState.pendingLegendRuleChoices.shouldHaveSize(1)

            val pendingChoice = newState.pendingLegendRuleChoices.first()
            pendingChoice.controllerId shouldBe player1Id
            pendingChoice.legendaryName shouldBe "Legendary Dragon"
            pendingChoice.duplicateIds.shouldContain(dragon1)
            pendingChoice.duplicateIds.shouldContain(dragon2)
        }

        test("player can choose which legendary to keep") {
            var state = newGame()

            // Create two legendary creatures
            val dragon1 = EntityId.generate()
            val dragon2 = EntityId.generate()

            val (_, s1) = state.createEntity(
                dragon1,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s2 = s1.addToZone(dragon1, ZoneId.BATTLEFIELD)

            val (_, s3) = s2.createEntity(
                dragon2,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s4 = s3.addToZone(dragon2, ZoneId.BATTLEFIELD)

            // Run SBAs to create pending choice
            val sbaResult = EcsGameEngine.checkStateBasedActions(s4)
            val stateWithChoice = (sbaResult as EcsActionResult.Success).state

            // Player chooses to keep dragon2
            val resolveResult = EcsGameEngine.executeAction(
                stateWithChoice,
                EcsResolveLegendRule(
                    controllerId = player1Id,
                    legendaryName = "Legendary Dragon",
                    keepEntityId = dragon2
                )
            )
            resolveResult.shouldBeInstanceOf<EcsActionResult.Success>()

            val finalState = (resolveResult as EcsActionResult.Success).state

            // dragon2 should be on battlefield, dragon1 in graveyard
            finalState.getBattlefield().shouldHaveSize(1)
            finalState.getBattlefield().shouldContain(dragon2)
            finalState.getGraveyard(player1Id).shouldContain(dragon1)

            // Pending choice should be resolved
            finalState.hasPendingLegendRuleChoices.shouldBeFalse()
        }

        test("different controllers can each have one legendary with same name") {
            var state = newGame()

            // Create legendary for player1
            val dragon1 = EntityId.generate()
            val (_, s1) = state.createEntity(
                dragon1,
                CardComponent(legendaryDragonDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s2 = s1.addToZone(dragon1, ZoneId.BATTLEFIELD)

            // Create same legendary for player2
            val dragon2 = EntityId.generate()
            val (_, s3) = s2.createEntity(
                dragon2,
                CardComponent(legendaryDragonDef, player2Id),
                ControllerComponent(player2Id)
            )
            val s4 = s3.addToZone(dragon2, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s4)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Both should remain on battlefield (different controllers)
            newState.getBattlefield().shouldHaveSize(2)
            newState.getBattlefield().shouldContain(dragon1)
            newState.getBattlefield().shouldContain(dragon2)

            // No pending choices (each player only has one)
            newState.hasPendingLegendRuleChoices.shouldBeFalse()
        }
    }

    context("Aura Validity (704.5m, 704.5n)") {
        test("aura goes to graveyard when enchanted creature leaves battlefield") {
            var state = newGame()

            // Create creature
            val bearId = EntityId.generate()
            val (_, s1) = state.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s2 = s1.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Create aura attached to creature
            val auraId = EntityId.generate()
            val (_, s3) = s2.createEntity(
                auraId,
                CardComponent(auraDef, player1Id),
                ControllerComponent(player1Id),
                AttachedToComponent(bearId)
            )
            val s4 = s3.addToZone(auraId, ZoneId.BATTLEFIELD)

            // Now remove the creature from battlefield
            val s5 = s4
                .removeFromZone(bearId, ZoneId.BATTLEFIELD)
                .addToZone(bearId, ZoneId.graveyard(player1Id))

            // The aura is still on battlefield but target is gone
            s5.getBattlefield().shouldContain(auraId)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s5)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Aura should be in graveyard
            newState.getBattlefield().contains(auraId).shouldBeFalse()
            newState.getGraveyard(player1Id).shouldContain(auraId)
        }

        test("aura stays when enchanted creature is still on battlefield") {
            var state = newGame()

            // Create creature
            val bearId = EntityId.generate()
            val (_, s1) = state.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id)
            )
            val s2 = s1.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Create aura attached to creature
            val auraId = EntityId.generate()
            val (_, s3) = s2.createEntity(
                auraId,
                CardComponent(auraDef, player1Id),
                ControllerComponent(player1Id),
                AttachedToComponent(bearId)
            )
            val s4 = s3.addToZone(auraId, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s4)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Both should still be on battlefield
            newState.getBattlefield().shouldContain(bearId)
            newState.getBattlefield().shouldContain(auraId)
        }
    }

    context("Token Cessation (704.5d)") {
        test("token in graveyard ceases to exist") {
            var state = newGame()

            // Create a token creature on battlefield
            val tokenId = EntityId.generate()
            val (_, s1) = state.createEntity(
                tokenId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                TokenComponent(createdBy = null)
            )
            val s2 = s1.addToZone(tokenId, ZoneId.BATTLEFIELD)

            // Verify token exists
            s2.hasEntity(tokenId).shouldBeTrue()

            // Move token to graveyard (simulating death)
            val s3 = s2
                .removeFromZone(tokenId, ZoneId.BATTLEFIELD)
                .addToZone(tokenId, ZoneId.graveyard(player1Id))

            // Token is in graveyard
            s3.getGraveyard(player1Id).shouldContain(tokenId)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s3)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Token should be completely removed
            newState.hasEntity(tokenId).shouldBeFalse()
            newState.getGraveyard(player1Id).contains(tokenId).shouldBeFalse()
        }

        test("token on battlefield remains") {
            var state = newGame()

            // Create a token creature on battlefield
            val tokenId = EntityId.generate()
            val (_, s1) = state.createEntity(
                tokenId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                TokenComponent(createdBy = null)
            )
            val s2 = s1.addToZone(tokenId, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s2)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Token should still exist on battlefield
            newState.hasEntity(tokenId).shouldBeTrue()
            newState.getBattlefield().shouldContain(tokenId)
        }
    }

    context("Counter Cancellation (704.5q)") {
        test("plus and minus counters cancel each other") {
            var state = newGame()

            // Create creature with 3 +1/+1 and 2 -1/-1 counters
            val bearId = EntityId.generate()
            val counters = CountersComponent()
                .add(CounterType.PLUS_ONE_PLUS_ONE, 3)
                .add(CounterType.MINUS_ONE_MINUS_ONE, 2)

            val (_, s1) = state.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                counters
            )
            val s2 = s1.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Verify initial counters
            val initialCounters = s2.getComponent<CountersComponent>(bearId)
            initialCounters.shouldNotBeNull()
            initialCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
            initialCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 2

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s2)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Should have 1 +1/+1 counter and 0 -1/-1 counters
            val finalCounters = newState.getComponent<CountersComponent>(bearId)
            finalCounters.shouldNotBeNull()
            finalCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
            finalCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
        }

        test("counter cancellation removes equal amounts") {
            var state = newGame()

            // Create creature with 2 +1/+1 and 2 -1/-1 counters
            val bearId = EntityId.generate()
            val counters = CountersComponent()
                .add(CounterType.PLUS_ONE_PLUS_ONE, 2)
                .add(CounterType.MINUS_ONE_MINUS_ONE, 2)

            val (_, s1) = state.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                counters
            )
            val s2 = s1.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s2)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Should have 0 of both types
            val finalCounters = newState.getComponent<CountersComponent>(bearId)
            finalCounters.shouldNotBeNull()
            finalCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 0
            finalCounters.getCount(CounterType.MINUS_ONE_MINUS_ONE) shouldBe 0
        }

        test("no cancellation when only one type present") {
            var state = newGame()

            // Create creature with only +1/+1 counters
            val bearId = EntityId.generate()
            val counters = CountersComponent()
                .add(CounterType.PLUS_ONE_PLUS_ONE, 3)

            val (_, s1) = state.createEntity(
                bearId,
                CardComponent(bearDef, player1Id),
                ControllerComponent(player1Id),
                counters
            )
            val s2 = s1.addToZone(bearId, ZoneId.BATTLEFIELD)

            // Run SBAs
            val result = EcsGameEngine.checkStateBasedActions(s2)
            result.shouldBeInstanceOf<EcsActionResult.Success>()

            val newState = (result as EcsActionResult.Success).state

            // Should still have 3 +1/+1 counters
            val finalCounters = newState.getComponent<CountersComponent>(bearId)
            finalCounters.shouldNotBeNull()
            finalCounters.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 3
        }
    }
})
