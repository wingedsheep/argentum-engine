package com.wingedsheep.rulesengine.ecs.combat

import com.wingedsheep.rulesengine.card.CardDefinition
import com.wingedsheep.rulesengine.core.ManaCost
import com.wingedsheep.rulesengine.core.Subtype
import com.wingedsheep.rulesengine.ecs.EcsGameState
import com.wingedsheep.rulesengine.ecs.EntityId
import com.wingedsheep.rulesengine.ecs.ZoneId
import com.wingedsheep.rulesengine.ecs.components.*
import com.wingedsheep.rulesengine.ecs.layers.*
import com.wingedsheep.rulesengine.game.Step
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.types.shouldBeInstanceOf

class EcsCombatValidatorTest : FunSpec({

    val player1Id = EntityId.of("player1")
    val player2Id = EntityId.of("player2")

    val bearDef = CardDefinition.creature(
        name = "Grizzly Bears",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype.BEAST),
        power = 2,
        toughness = 2
    )

    fun newGame(): EcsGameState = EcsGameState.newGame(
        listOf(player1Id to "Alice", player2Id to "Bob")
    )

    fun EcsGameState.addCreatureToBattlefield(
        def: CardDefinition,
        controllerId: EntityId,
        hasSummoningSickness: Boolean = true
    ): Pair<EntityId, EcsGameState> {
        val (creatureId, state1) = createEntity(
            EntityId.generate(),
            CardComponent(def, controllerId),
            ControllerComponent(controllerId)
        )
        val state2 = if (hasSummoningSickness) {
            state1.addComponent(creatureId, SummoningSicknessComponent)
        } else state1

        return creatureId to state2.addToZone(creatureId, ZoneId.BATTLEFIELD)
    }

    fun createGameInDeclareBlockersStep(): EcsGameState {
        var state = newGame()
        // Advance to Combat
        state = state.copy(turnState = state.turnState.copy(step = Step.DECLARE_BLOCKERS))
        // Start combat with Player 1 attacking, Player 2 defending
        state = state.startCombat(player2Id)
        return state
    }

    context("Custom restrictions") {
        test("creature with cantBlock view property cannot block") {
            var state = createGameInDeclareBlockersStep()
            // Attacker (Player 1)
            val (attackerId, state1) = state.addCreatureToBattlefield(bearDef, player1Id, false)
            // Blocker (Player 2)
            val (blockerId, state2) = state1.addCreatureToBattlefield(bearDef, player2Id, false)

            // Declare attacker
            state = state2
                .updateEntity(attackerId) { it.with(AttackingComponent.attackingPlayer(player2Id)) }

            // FIXED: Use copy/combat!!.addAttacker instead of updateCombat
            state = state.copy(combat = state.combat!!.addAttacker(attackerId))

            // Inject a modifier provider that restricts the blocker
            val modifierProvider = object : ModifierProvider {
                override fun getModifiers(state: EcsGameState): List<Modifier> {
                    return listOf(
                        Modifier(
                            layer = Layer.ABILITY,
                            sourceId = blockerId, // self-imposed
                            timestamp = 1,
                            modification = Modification.AddCantBlockRestriction,
                            filter = ModifierFilter.Specific(blockerId)
                        )
                    )
                }
            }

            // Perform validation
            val result = EcsCombatValidator.canDeclareBlocker(
                state, blockerId, attackerId, player2Id, modifierProvider
            )

            // This confirms that EcsCombatValidator checks view.canBlock or view.cantBlock
            result.shouldBeInstanceOf<EcsCombatValidator.BlockValidationResult.Invalid>()
        }
    }
})
