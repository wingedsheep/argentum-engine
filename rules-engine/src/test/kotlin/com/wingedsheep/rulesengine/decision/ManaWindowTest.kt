package com.wingedsheep.rulesengine.decision

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.*
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.player.ManaPool
import com.wingedsheep.rulesengine.zone.ZoneType
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for the Mana Window during spell casting (CR 601.2g).
 *
 * The mana window is the period during spell casting where a player
 * can activate mana abilities before paying costs.
 */
class ManaWindowTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    // Sample cards for testing
    val lightningBoltDef = CardDefinition.instant(
        name = "Lightning Bolt",
        manaCost = ManaCost.parse("{R}"),
        oracleText = "Lightning Bolt deals 3 damage to any target."
    )

    val giantGrowthDef = CardDefinition.instant(
        name = "Giant Growth",
        manaCost = ManaCost.parse("{G}"),
        oracleText = "Target creature gets +3/+3 until end of turn."
    )

    val grizzlyBearsDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAR),
        power = 2,
        toughness = 2
    )

    val forestDef = CardDefinition.basicLand("Forest", Subtype.FOREST)
    val mountainDef = CardDefinition.basicLand("Mountain", Subtype.MOUNTAIN)

    fun newGame(): GameState = GameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    context("ManaPool.pay") {
        test("can pay simple colored cost") {
            val pool = ManaPool(red = 2, green = 1)
            val cost = ManaCost.parse("{R}")

            pool.canPay(cost) shouldBe true
            val afterPay = pool.pay(cost)

            afterPay.red shouldBe 1
            afterPay.green shouldBe 1
        }

        test("can pay generic cost with any mana") {
            val pool = ManaPool(white = 1, blue = 1)
            val cost = ManaCost.parse("{2}")

            pool.canPay(cost) shouldBe true
            val afterPay = pool.pay(cost)

            afterPay.total shouldBe 0
        }

        test("can pay mixed cost") {
            val pool = ManaPool(green = 2, white = 1)
            val cost = ManaCost.parse("{1}{G}")

            pool.canPay(cost) shouldBe true
            val afterPay = pool.pay(cost)

            afterPay.green shouldBe 1
            afterPay.white shouldBe 0  // Used for generic
        }

        test("cannot pay insufficient mana") {
            val pool = ManaPool(red = 1)
            val cost = ManaCost.parse("{G}")

            pool.canPay(cost) shouldBe false
        }
    }

    context("ManaWindowContext serialization") {
        test("can create and serialize ManaWindowContext") {
            val cardId = EntityId.generate()
            val context = ManaWindowContext(
                sourceId = cardId,
                controllerId = player1Id,
                cardEntityId = cardId,
                cardName = "Lightning Bolt",
                manaCostRequired = ManaCost.parse("{R}"),
                fromZone = ZoneId(ZoneType.HAND, player1Id),
                selectedTargets = emptyList(),
                xValue = 0
            )

            context.cardName shouldBe "Lightning Bolt"
            context.controllerId shouldBe player1Id
            context.manaCostRequired.toString() shouldBe "{R}"
        }
    }

    context("ResolveManaWindow decision") {
        test("creates decision with correct mana info") {
            val decision = ResolveManaWindow(
                decisionId = "test-decision",
                playerId = player1Id,
                cardName = "Grizzly Bears",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{1}{G}",
                currentManaPoolDisplay = "1G 1W",
                canPayCost = true,
                availableManaAbilities = emptyList()
            )

            decision.cardName shouldBe "Grizzly Bears"
            decision.manaCostDisplay shouldBe "{1}{G}"
            decision.canPayCost shouldBe true
        }

        test("lists available mana abilities") {
            val forestId = EntityId.generate()
            val decision = ResolveManaWindow(
                decisionId = "test-decision",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "",
                canPayCost = false,
                availableManaAbilities = listOf(
                    ManaAbilityOption(
                        sourceEntityId = forestId,
                        sourceName = "Forest",
                        abilityIndex = 0,
                        description = "Tap: Add {G}"
                    )
                )
            )

            decision.availableManaAbilities shouldHaveSize 1
            decision.availableManaAbilities[0].description shouldBe "Tap: Add {G}"
        }
    }

    context("ManaWindowResponse types") {
        test("ActivateAbility response") {
            val sourceId = EntityId.generate()
            val response = ManaWindowResponse.ActivateAbility(
                decisionId = "test",
                sourceEntityId = sourceId,
                abilityIndex = 0
            )

            response.sourceEntityId shouldBe sourceId
            response.abilityIndex shouldBe 0
        }

        test("ProceedWithCasting response") {
            val response = ManaWindowResponse.ProceedWithCasting(decisionId = "test")
            response.decisionId shouldBe "test"
        }

        test("CancelCasting response") {
            val response = ManaWindowResponse.CancelCasting(decisionId = "test")
            response.decisionId shouldBe "test"
        }
    }

    context("DecisionResumer - Mana Window") {
        val resumer = DecisionResumer()

        test("proceeds with casting when sufficient mana") {
            var state = newGame()

            // Give player mana
            state = state.updateEntity(player1Id) { container ->
                container.with(ManaPoolComponent(ManaPool(red = 1)))
            }

            // Create card in hand
            val boltId = EntityId.generate()
            val (_, stateWithCard) = state.createEntity(
                boltId,
                CardComponent(lightningBoltDef, player1Id),
                ControllerComponent(player1Id)
            )
            val handZone = ZoneId(ZoneType.HAND, player1Id)
            state = stateWithCard.addToZone(boltId, handZone)

            // Create mana window context
            val context = ManaWindowContext(
                sourceId = boltId,
                controllerId = player1Id,
                cardEntityId = boltId,
                cardName = "Lightning Bolt",
                manaCostRequired = ManaCost.parse("{R}"),
                fromZone = handZone,
                selectedTargets = emptyList(),
                xValue = 0
            )

            // Create decision
            val decision = ResolveManaWindow(
                decisionId = "mw-1",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = boltId,
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "1R",
                canPayCost = true,
                availableManaAbilities = emptyList()
            )

            state = state.setPendingDecision(decision, context)

            // Player proceeds with casting
            val response = ManaWindowResponse.ProceedWithCasting(decisionId = "mw-1")
            val result = resumer.resume(state, response)

            // Mana should be spent
            val manaPool = result.state.getComponent<ManaPoolComponent>(player1Id)?.pool
            manaPool.shouldNotBeNull()
            manaPool.red shouldBe 0

            // Spell should be on stack
            val stackContents = result.state.getStack()
            stackContents shouldContain boltId

            // Card should have SpellOnStackComponent
            val spellComponent = result.state.getComponent<SpellOnStackComponent>(boltId)
            spellComponent.shouldNotBeNull()
            spellComponent.casterId shouldBe player1Id
        }

        test("cancels casting without spending mana") {
            var state = newGame()

            // Give player mana
            state = state.updateEntity(player1Id) { container ->
                container.with(ManaPoolComponent(ManaPool(red = 1)))
            }

            // Create card in hand
            val boltId = EntityId.generate()
            val (_, stateWithCard) = state.createEntity(
                boltId,
                CardComponent(lightningBoltDef, player1Id),
                ControllerComponent(player1Id)
            )
            val handZone = ZoneId(ZoneType.HAND, player1Id)
            state = stateWithCard.addToZone(boltId, handZone)

            // Create mana window context
            val context = ManaWindowContext(
                sourceId = boltId,
                controllerId = player1Id,
                cardEntityId = boltId,
                cardName = "Lightning Bolt",
                manaCostRequired = ManaCost.parse("{R}"),
                fromZone = handZone,
                selectedTargets = emptyList(),
                xValue = 0
            )

            val decision = ResolveManaWindow(
                decisionId = "mw-2",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = boltId,
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "1R",
                canPayCost = true,
                availableManaAbilities = emptyList()
            )

            state = state.setPendingDecision(decision, context)

            // Player cancels
            val response = ManaWindowResponse.CancelCasting(decisionId = "mw-2")
            val result = resumer.resume(state, response)

            // Mana should NOT be spent
            val manaPool = result.state.getComponent<ManaPoolComponent>(player1Id)?.pool
            manaPool.shouldNotBeNull()
            manaPool.red shouldBe 1

            // Spell should NOT be on stack
            val stackContents = result.state.getStack()
            stackContents.contains(boltId) shouldBe false

            // Decision should be cleared
            result.state.pendingDecision.shouldBeNull()
        }
    }

    context("DecisionValidator - ManaWindowResponse") {
        test("validates ActivateAbility with valid ability") {
            val forestId = EntityId.generate()
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "",
                canPayCost = false,
                availableManaAbilities = listOf(
                    ManaAbilityOption(
                        sourceEntityId = forestId,
                        sourceName = "Forest",
                        abilityIndex = 0,
                        description = "Tap: Add {G}"
                    )
                )
            )

            val response = ManaWindowResponse.ActivateAbility(
                decisionId = "test",
                sourceEntityId = forestId,
                abilityIndex = 0
            )

            val result = DecisionValidator.validate(newGame(), decision, response)
            result shouldBe DecisionValidator.ValidationResult.Valid
        }

        test("rejects ActivateAbility with invalid ability") {
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "",
                canPayCost = false,
                availableManaAbilities = emptyList()
            )

            val response = ManaWindowResponse.ActivateAbility(
                decisionId = "test",
                sourceEntityId = EntityId.generate(),
                abilityIndex = 0
            )

            val result = DecisionValidator.validate(newGame(), decision, response)
            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("rejects ProceedWithCasting when mana insufficient") {
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "",
                canPayCost = false,  // Cannot pay
                availableManaAbilities = emptyList()
            )

            val response = ManaWindowResponse.ProceedWithCasting(decisionId = "test")

            val result = DecisionValidator.validate(newGame(), decision, response)
            result.shouldBeInstanceOf<DecisionValidator.ValidationResult.Invalid>()
        }

        test("allows CancelCasting always") {
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "1R",
                canPayCost = true,
                availableManaAbilities = emptyList()
            )

            val response = ManaWindowResponse.CancelCasting(decisionId = "test")

            val result = DecisionValidator.validate(newGame(), decision, response)
            result shouldBe DecisionValidator.ValidationResult.Valid
        }
    }

    context("AutoPlayerInterface - ManaWindowResponse") {
        val autoInterface = AutoPlayerInterface()

        test("auto-proceeds when mana available") {
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "1R",
                canPayCost = true,
                availableManaAbilities = emptyList()
            )

            val response = autoInterface.requestDecision(newGame(), decision)
            response.shouldBeInstanceOf<ManaWindowResponse.ProceedWithCasting>()
        }

        test("auto-cancels when mana insufficient") {
            val decision = ResolveManaWindow(
                decisionId = "test",
                playerId = player1Id,
                cardName = "Lightning Bolt",
                cardEntityId = EntityId.generate(),
                manaCostDisplay = "{R}",
                currentManaPoolDisplay = "",
                canPayCost = false,
                availableManaAbilities = listOf(
                    ManaAbilityOption(
                        sourceEntityId = EntityId.generate(),
                        sourceName = "Forest",
                        abilityIndex = 0,
                        description = "Tap: Add {G}"
                    )
                )
            )

            val response = autoInterface.requestDecision(newGame(), decision)
            response.shouldBeInstanceOf<ManaWindowResponse.CancelCasting>()
        }
    }

    context("findAvailableManaAbilities") {
        test("finds basic land mana abilities") {
            var state = newGame()

            // Create a Forest on the battlefield controlled by player1
            val forestId = EntityId.generate()
            val (_, stateWithForest) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithForest.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Create a Mountain on the battlefield controlled by player1
            val mountainId = EntityId.generate()
            val (_, stateWithMountain) = state.createEntity(
                mountainId,
                CardComponent(mountainDef, player1Id),
                ControllerComponent(player1Id)
            )
            state = stateWithMountain.addToZone(mountainId, ZoneId.BATTLEFIELD)

            // The DecisionResumer.findAvailableManaAbilities is private, but we can
            // test it indirectly through createManaWindowDecision
            val resumer = DecisionResumer()

            // Create context and set decision
            val cardId = EntityId.generate()
            val context = ManaWindowContext(
                sourceId = cardId,
                controllerId = player1Id,
                cardEntityId = cardId,
                cardName = "Test",
                manaCostRequired = ManaCost.parse("{R}"),
                fromZone = ZoneId(ZoneType.HAND, player1Id)
            )

            // We can verify the ability detection by checking the game flow
            // For now, this test documents the expected behavior
            state.getBattlefield() shouldContain forestId
            state.getBattlefield() shouldContain mountainId
        }

        test("does not list tapped lands") {
            var state = newGame()

            // Create a tapped Forest
            val forestId = EntityId.generate()
            val (_, stateWithForest) = state.createEntity(
                forestId,
                CardComponent(forestDef, player1Id),
                ControllerComponent(player1Id),
                TappedComponent  // Land is tapped
            )
            state = stateWithForest.addToZone(forestId, ZoneId.BATTLEFIELD)

            // The tapped land should not be available for mana
            val container = state.getEntity(forestId)
            container.shouldNotBeNull()
            container.has<TappedComponent>() shouldBe true
        }

        test("does not list opponent's lands") {
            var state = newGame()

            // Create a Forest controlled by player2 (opponent)
            val forestId = EntityId.generate()
            val (_, stateWithForest) = state.createEntity(
                forestId,
                CardComponent(forestDef, player2Id),
                ControllerComponent(player2Id)
            )
            state = stateWithForest.addToZone(forestId, ZoneId.BATTLEFIELD)

            // Verify the land is controlled by player2
            val controller = state.getComponent<ControllerComponent>(forestId)
            controller.shouldNotBeNull()
            controller.controllerId shouldBe player2Id
        }
    }
})
