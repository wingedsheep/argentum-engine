package com.wingedsheep.engine.legalactions

import com.wingedsheep.engine.core.TurnManager
import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.legalactions.enumerators.*
import com.wingedsheep.engine.mechanics.mana.CostCalculator
import com.wingedsheep.engine.mechanics.mana.ManaSolver
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.model.EntityId

/**
 * Coordinator that enumerates all legal actions for a player.
 *
 * This is the engine-level equivalent of the game-server's LegalActionsCalculator.
 * It delegates to specialized ActionEnumerators for each action category, mirrors
 * the ActionProcessor/ActionHandlerRegistry pattern for enumeration.
 */
class LegalActionEnumerator(
    private val cardRegistry: CardRegistry,
    private val manaSolver: ManaSolver,
    private val costCalculator: CostCalculator,
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator,
    private val turnManager: TurnManager
) {
    private val combatEnumerator = CombatEnumerator()

    private val enumerators: List<ActionEnumerator> = listOf(
        PassPriorityEnumerator(),
        PlayLandEnumerator(),
        MorphCastEnumerator(),
        CastSpellEnumerator(),
        CyclingEnumerator(),
        CastFromZoneEnumerator(),
        ManaAbilityEnumerator(),
        TurnFaceUpEnumerator(),
        ActivatedAbilityEnumerator(),
        CrewEnumerator(),
        GraveyardAbilityEnumerator()
    )

    /**
     * Enumerate all legal actions for the given player in the given state.
     *
     * @param state The current game state
     * @param playerId The player to enumerate actions for
     * @return All legal actions (including unaffordable ones marked with affordable=false)
     */
    fun enumerate(state: GameState, playerId: EntityId): List<LegalAction> {
        val context = EnumerationContext(
            state = state,
            playerId = playerId,
            cardRegistry = cardRegistry,
            manaSolver = manaSolver,
            costCalculator = costCalculator,
            predicateEvaluator = predicateEvaluator,
            conditionEvaluator = conditionEvaluator,
            turnManager = turnManager
        )

        // Combat declaration steps are exclusive — only combat actions, no spells/abilities/pass
        if (combatEnumerator.isCombatDeclarationStep(context)) {
            return combatEnumerator.enumerate(context)
        }

        // Normal priority: enumerate all action categories
        return enumerators.flatMap { it.enumerate(context) }
    }

    companion object {
        /**
         * Create a LegalActionEnumerator with the same dependencies as LegalActionsCalculator.
         */
        fun create(
            cardRegistry: CardRegistry,
            manaSolver: ManaSolver = ManaSolver(cardRegistry),
            costCalculator: CostCalculator = CostCalculator(cardRegistry),
            predicateEvaluator: PredicateEvaluator = PredicateEvaluator(),
            conditionEvaluator: ConditionEvaluator = ConditionEvaluator(),
            turnManager: TurnManager = TurnManager(
                combatManager = com.wingedsheep.engine.mechanics.combat.CombatManager(cardRegistry),
                cardRegistry = cardRegistry
            )
        ): LegalActionEnumerator {
            return LegalActionEnumerator(
                cardRegistry = cardRegistry,
                manaSolver = manaSolver,
                costCalculator = costCalculator,
                predicateEvaluator = predicateEvaluator,
                conditionEvaluator = conditionEvaluator,
                turnManager = turnManager
            )
        }
    }
}
