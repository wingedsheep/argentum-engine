package com.wingedsheep.rulesengine.sets.portal

import com.wingedsheep.rulesengine.ability.AbilityRegistry
import com.wingedsheep.rulesengine.ability.register
import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.GameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.combat.CombatValidator
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.script.ScriptModifierProvider
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class JungleLionScenarioTest : FunSpec({

    val player1Id = EntityId.of("player1") // Alice
    val player2Id = EntityId.of("player2") // Bob

    // Helper to setup a game with Portal Set abilities registered
    fun setupGame(): Pair<GameState, ScriptModifierProvider> {
        val state = GameState.newGame(listOf(player1Id to "Alice", player2Id to "Bob"))

        // Register Portal abilities
        val registry = AbilityRegistry()
        // We iterate specifically over the PortalSet to populate the registry for this test
        PortalSet.getCardScripts().forEach { registry.register(it) }

        return state to ScriptModifierProvider(registry)
    }

    // Helper to create a specific card on the battlefield
    fun GameState.addCardToBattlefield(
        cardName: String,
        controllerId: EntityId
    ): Pair<EntityId, GameState> {
        val def = PortalSet.getCardDefinition(cardName)
            ?: throw IllegalArgumentException("Card $cardName not found in PortalSet")

        val (id, s1) = createEntity(
            EntityId.generate(),
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        // Add required state components
        val s2 = if(def.isCreature) s1.addComponent(id, SummoningSicknessComponent) else s1

        return id to s2.addToZone(id, ZoneId.BATTLEFIELD)
    }

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    // Helper to add a generic bear (for the opponent)
    fun GameState.addGenericBear(controllerId: EntityId): Pair<EntityId, GameState> {
        val (id, s1) = createEntity(
            EntityId.generate(),
            CardComponent(bearDef, controllerId),
            ControllerComponent(controllerId)
        )
        return id to s1.addToZone(id, ZoneId.BATTLEFIELD)
    }

    context("Jungle Lion Gameplay Mechanics") {

        test("Jungle Lion cannot be declared as a blocker") {
            val (initialState, modifierProvider) = setupGame()

            // Setup:
            // Player 1 (Alice) controls Jungle Lion (untapped, ready to block)
            // Player 2 (Bob) is active player, declared an attacker

            var state = initialState

            // Give Alice a Jungle Lion
            val (lionId, s1) = state.addCardToBattlefield("Jungle Lion", player1Id)
            state = s1.updateEntity(lionId) { it.without<SummoningSicknessComponent>() }

            // Give Bob a Bear
            val (attackerId, s2) = state.addGenericBear(player2Id)
            state = s2.updateEntity(attackerId) { it.without<SummoningSicknessComponent>() }

            // Advance to Declare Blockers step (Bob is active)
            // 1. Set active player to Bob
            state = state.copy(turnState = state.turnState.copy(activePlayer = player2Id))
            // 2. Start Combat
            state = state.startCombat(defendingPlayerId = player1Id)
            // 3. Declare Bob's bear as attacker (ECS pattern: add AttackingComponent to entity)
            state = state.updateEntity(attackerId) {
                it.with(AttackingComponent.attackingPlayer(player1Id))
            }

            // 4. Move to Declare Blockers
            state = state.copy(turnState = state.turnState.copy(step = Step.DECLARE_BLOCKERS))

            // ACT: Alice tries to block Bob's bear with Jungle Lion
            val validationResult = CombatValidator.canDeclareBlocker(
                state = state,
                blockerId = lionId,
                attackerId = attackerId,
                playerId = player1Id,
                modifierProvider = modifierProvider
            )

            // ASSERT: Should be invalid due to "This creature can't block"
            validationResult.shouldBeInstanceOf<CombatValidator.BlockValidationResult.Invalid>()
        }

        test("Jungle Lion CAN attack (it is not a defender)") {
            val (initialState, modifierProvider) = setupGame()

            // Setup: Alice controls Jungle Lion, it is her turn, declare attackers step
            var state = initialState
            val (lionId, s1) = state.addCardToBattlefield("Jungle Lion", player1Id)
            state = s1.updateEntity(lionId) { it.without<SummoningSicknessComponent>() }

            // Set Step
            state = state.copy(turnState = state.turnState.copy(
                activePlayer = player1Id,
                step = Step.DECLARE_ATTACKERS
            ))

            // ACT
            val validationResult = CombatValidator.canDeclareAttacker(
                state = state,
                creatureId = lionId,
                playerId = player1Id,
                modifierProvider = modifierProvider
            )

            // ASSERT
            validationResult.shouldBeInstanceOf<CombatValidator.ValidationResult.Valid>()
        }
    }
})
