package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PreventCycling

/**
 * Extracted permission-checking helpers from LegalActionsCalculator.
 * These methods check cast restrictions, activation restrictions, flash grants, etc.
 */
class CastPermissionUtils(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator
) {
    fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        restriction: ActivationRestriction,
        sourceId: EntityId? = null,
        abilityId: AbilityId? = null
    ): Boolean {
        return when (restriction) {
            is ActivationRestriction.AnyPlayerMay -> true
            is ActivationRestriction.OnlyDuringYourTurn -> state.activePlayerId == playerId
            is ActivationRestriction.BeforeStep -> state.step.ordinal < restriction.step.ordinal
            is ActivationRestriction.DuringPhase -> state.phase == restriction.phase
            is ActivationRestriction.DuringStep -> state.step == restriction.step
            is ActivationRestriction.OnlyIfCondition -> {
                val opponentId = state.turnOrder.firstOrNull { it != playerId }
                val context = EffectContext(
                    sourceId = null,
                    controllerId = playerId,
                    opponentId = opponentId,
                    targets = emptyList(),
                    xValue = 0
                )
                conditionEvaluator.evaluate(state, restriction.condition, context)
            }
            is ActivationRestriction.OncePerTurn -> {
                if (sourceId == null || abilityId == null) true
                else {
                    val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                    tracker == null || !tracker.hasActivated(abilityId)
                }
            }
            is ActivationRestriction.All -> restriction.restrictions.all {
                checkActivationRestriction(state, playerId, it, sourceId, abilityId)
            }
        }
    }

    fun checkCastRestrictions(
        state: GameState,
        playerId: EntityId,
        restrictions: List<CastRestriction>
    ): Boolean {
        if (restrictions.isEmpty()) return true

        val opponentId = state.turnOrder.firstOrNull { it != playerId }
        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
            opponentId = opponentId,
            targets = emptyList(),
            xValue = 0
        )

        for (restriction in restrictions) {
            val satisfied = when (restriction) {
                is CastRestriction.OnlyDuringStep -> state.step == restriction.step
                is CastRestriction.OnlyDuringPhase -> state.phase == restriction.phase
                is CastRestriction.OnlyIfCondition -> conditionEvaluator.evaluate(state, restriction.condition, context)
                is CastRestriction.TimingRequirement -> true
                is CastRestriction.All -> restriction.restrictions.all { subRestriction ->
                    checkCastRestrictions(state, playerId, listOf(subRestriction))
                }
            }
            if (!satisfied) return false
        }
        return true
    }

    fun hasPlayFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayFromTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    fun getCastFromTopOfLibraryFilter(state: GameState, playerId: EntityId): GameObjectFilter? {
        var filter: GameObjectFilter? = null
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is CastSpellTypesFromTopOfLibrary) {
                    if (ability.filter == GameObjectFilter.Any) return GameObjectFilter.Any
                    filter = ability.filter
                }
            }
        }
        return filter
    }

    fun hasGrantedFlash(state: GameState, spellCardId: EntityId): Boolean {
        val spellOwner = state.getEntity(spellCardId)?.get<ControllerComponent>()?.playerId
            ?: return false

        val spellCard = state.getEntity(spellCardId)?.get<CardComponent>()
        val spellDef = spellCard?.let { cardRegistry.getCard(it.cardDefinitionId) }
        val conditionalFlash = spellDef?.script?.conditionalFlash
        if (conditionalFlash != null) {
            val opponentId = state.turnOrder.firstOrNull { it != spellOwner }
            val effectContext = EffectContext(
                sourceId = spellCardId,
                controllerId = spellOwner,
                opponentId = opponentId
            )
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) {
                return true
            }
        }

        val context = PredicateContext(controllerId = spellOwner)
        for (playerId in state.turnOrder) {
            for (entityId in state.getBattlefield(playerId)) {
                val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
                val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
                for (ability in cardDef.script.staticAbilities) {
                    if (ability is GrantFlashToSpellType) {
                        if (ability.controllerOnly && playerId != spellOwner) continue
                        if (predicateEvaluator.matches(state, spellCardId, ability.filter, context)) {
                            return true
                        }
                    }
                }
            }
        }
        return false
    }

    fun isCyclingPrevented(state: GameState): Boolean {
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PreventCycling }) {
                return true
            }
        }
        return false
    }

    fun getMaxLoyaltyActivations(state: GameState, playerId: EntityId): Int {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val controller = container.get<ControllerComponent>()?.playerId ?: continue
            if (controller != playerId) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is ExtraLoyaltyActivation }) {
                return 2
            }
        }
        return 1
    }

    fun hasGraveyardPlayPermissionForType(
        state: GameState,
        playerId: EntityId,
        typeName: String
    ): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is MayPlayPermanentsFromGraveyard }) {
                val tracker = state.getEntity(entityId)?.get<GraveyardPlayPermissionUsedComponent>()
                if (tracker == null || !tracker.hasUsedType(typeName)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents.
     */
    fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<com.wingedsheep.sdk.scripting.ActivatedAbility> {
        val targetContainer = state.getEntity(entityId) ?: return emptyList()
        val targetCard = targetContainer.get<CardComponent>() ?: return emptyList()

        val result = mutableListOf<com.wingedsheep.sdk.scripting.ActivatedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.staticAbilities) {
                when (ability) {
                    is com.wingedsheep.sdk.scripting.GrantActivatedAbilityToCreatureGroup -> {
                        val filter = ability.filter.baseFilter
                        val matchesAll = filter.cardPredicates.all { predicate ->
                            when (predicate) {
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.IsCreature ->
                                    targetCard.typeLine.isCreature
                                is com.wingedsheep.sdk.scripting.predicates.CardPredicate.HasSubtype ->
                                    targetCard.typeLine.hasSubtype(predicate.subtype)
                                else -> true
                            }
                        }
                        if (matchesAll) {
                            result.add(ability.ability)
                        }
                    }
                    is com.wingedsheep.sdk.scripting.GrantActivatedAbilityToAttachedCreature -> {
                        val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(ability.ability)
                        }
                    }
                    else -> {}
                }
            }
        }

        return result
    }
}
