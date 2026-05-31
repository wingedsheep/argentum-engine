package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantCastSpellsSharingColorWithLastCast
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.OpponentsCantCastSpells
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.PreventCycling
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn

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
                    sourceId = sourceId,
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
            is ActivationRestriction.MaxPerTurn -> {
                if (sourceId == null || abilityId == null) true
                else {
                    val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
                    (tracker?.activationCount(abilityId) ?: 0) < restriction.count
                }
            }
            is ActivationRestriction.Once -> {
                if (sourceId == null || abilityId == null) true
                else {
                    val tracker = state.getEntity(sourceId)?.get<AbilityActivatedEverComponent>()
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

    /**
     * Whether [playerId] has already cast as many spells this turn as a permanent they
     * control with [RestrictSpellsCastPerTurn] allows (e.g., Yawgmoth's Agenda: "You can't
     * cast more than one spell each turn."). When several such permanents are in play, the
     * most restrictive (smallest [RestrictSpellsCastPerTurn.maxPerTurn]) applies. Returns
     * false when no such permanent is controlled.
     */
    fun hasReachedSpellCastLimit(state: GameState, playerId: EntityId): Boolean {
        var limit: Int? = null
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa is RestrictSpellsCastPerTurn) {
                    limit = minOf(limit ?: sa.maxPerTurn, sa.maxPerTurn)
                }
            }
        }
        if (limit == null) return false
        val castThisTurn = state.playerSpellsCastThisTurn[playerId] ?: 0
        return castThisTurn >= limit
    }

    /**
     * Mana Maze (CR via [CantCastSpellsSharingColorWithLastCast]): true when a spell can't be cast
     * because its colors overlap those of the spell most recently cast this turn.
     *
     * Returns false unless (a) some permanent on the battlefield has the restriction static ability,
     * (b) a colored spell was cast earlier this turn ([GameState.lastCastSpellColors] is non-null and
     * non-empty), and (c) the candidate spell shares at least one of those colors. A colorless spell
     * never shares a color, so it is always castable.
     */
    fun sharesColorWithMostRecentCast(state: GameState, spellCardId: EntityId): Boolean {
        val lastColors = state.lastCastSpellColors
        if (lastColors.isNullOrEmpty()) return false

        val restrictionActive = state.getBattlefield().any { permanentId ->
            val card = state.getEntity(permanentId)?.get<CardComponent>()
            val cardDef = card?.let { cardRegistry.getCard(it.cardDefinitionId) }
            cardDef?.script?.staticAbilities?.any { it is CantCastSpellsSharingColorWithLastCast } == true
        }
        if (!restrictionActive) return false

        val spellColors = state.getEntity(spellCardId)?.get<CardComponent>()?.colors ?: return false
        return spellColors.any { it in lastColors }
    }

    /**
     * Voice of Victory (CR via [OpponentsCantCastSpells]): true when [playerId] is forbidden from
     * casting any spell because an *opponent* controls a permanent with that static ability — and,
     * for the [OpponentsCantCastSpells.onlyDuringYourTurn] form, that opponent is the active player.
     *
     * Scans the battlefield once. Control is read from projected state, so a control-changing
     * effect flips which player is restricted (gaining control of Voice of Victory turns the lock
     * onto its original controller). Face-down permanents have no abilities and are skipped. This
     * is OR'd into the central `cantCastSpells` gate, so it covers every casting zone uniformly.
     */
    fun cantCastSpellsDueToOpponentRestriction(state: GameState, playerId: EntityId): Boolean {
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            if (container.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue
            val card = container.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa !is OpponentsCantCastSpells) continue
                val controller = state.projectedState.getController(permanentId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: continue
                if (controller == playerId) continue
                if (sa.onlyDuringYourTurn && state.activePlayerId != controller) continue
                return true
            }
        }
        return false
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

    fun hasPlayLandsFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any { it is PlayLandsAndCastFilteredFromTopOfLibrary }) {
                return true
            }
        }
        return false
    }

    fun getCastFilteredFromTopOfLibraryFilter(state: GameState, playerId: EntityId): GameObjectFilter? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is PlayLandsAndCastFilteredFromTopOfLibrary) {
                    return ability.spellFilter
                }
            }
        }
        return null
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
                        if (predicateEvaluator.matches(state, state.projectedState, spellCardId, ability.filter, context)) {
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

    /**
     * True when any [PreventActivatedAbilities] static ability on the battlefield matches
     * [sourceId] under projected state (Cursed Totem, Damping Matrix, ...).
     *
     * Callers should skip both mana and non-mana activated abilities of [sourceId] when this
     * returns true. Loyalty abilities of planeswalkers are not blocked (Cursed Totem's filter
     * is `Creature`); abilities of noncreature permanents that animate them into creatures
     * (e.g. Vehicle Crew) are also unaffected by a `Creature` filter because the source isn't
     * yet a creature in projected state when the ability is activated.
     */
    fun isActivationPrevented(state: GameState, sourceId: EntityId): Boolean {
        val projected = state.projectedState
        val controllerId = projected.getController(sourceId)
            ?: state.getEntity(sourceId)?.get<ControllerComponent>()?.playerId
            ?: return false
        val context = PredicateContext(controllerId = controllerId)
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                val prevent = ability as? PreventActivatedAbilities ?: continue
                if (predicateEvaluator.matches(state, projected, sourceId, prevent.filter, context)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * Count additional land drops granted by static abilities on permanents
     * controlled by the given player (e.g., GrantAdditionalLandDrop from Hugs, Grisly Guardian).
     * Multiple sources are additive.
     */
    fun getAdditionalLandDrops(state: GameState, playerId: EntityId): Int {
        return LandDropUtils.getAdditionalLandDrops(state, playerId, cardRegistry)
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
            // Crucible of Worlds style: unlimited land plays from graveyard (land-drop is the limit)
            if (typeName == com.wingedsheep.sdk.core.CardType.LAND.name &&
                cardDef.script.staticAbilities.any { it is MayPlayLandsFromGraveyard }
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents,
     * paired with the EntityId of the permanent that granted each ability.
     *
     * The granter ID is required by AbilityCost.ExileGrantingPermanent to know which permanent
     * to exile when paying the cost (e.g., The Dominion Bracelet exiles itself when its granted
     * activated ability is paid for).
     */
    fun getStaticGrantedAbilitiesWithGranter(
        entityId: EntityId,
        state: GameState
    ): List<StaticGrantedAbility> {
        if (state.getEntity(entityId) == null) return emptyList()

        val result = mutableListOf<StaticGrantedAbility>()

        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue

            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = container.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                if (ability !is com.wingedsheep.sdk.scripting.GrantActivatedAbility) continue
                when (val scope = ability.filter.scope) {
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> {
                        if (ability.filter.excludeSelf && permanentId == entityId) continue
                        val granterController = state.projectedState.getController(permanentId) ?: continue
                        val matches = predicateEvaluator.matches(
                            state,
                            state.projectedState,
                            entityId,
                            ability.filter.baseFilter,
                            PredicateContext(controllerId = granterController, sourceId = permanentId)
                        )
                        if (matches) {
                            result.add(StaticGrantedAbility(ability.ability, permanentId))
                        }
                    }
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo -> {
                        val attachedTo = container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                        if (attachedTo != null && attachedTo.targetId == entityId) {
                            result.add(StaticGrantedAbility(ability.ability, permanentId))
                        }
                    }
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> {
                        if (permanentId == entityId) result.add(StaticGrantedAbility(ability.ability, permanentId))
                    }
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific -> {
                        if (scope.entityId == entityId) result.add(StaticGrantedAbility(ability.ability, permanentId))
                    }
                }
            }
        }

        // Multiple granters can hand the same ability to a permanent — e.g., two Brightcap
        // Badgers each grant Saproling tokens "{T}: Add {G}." The cards share a CardDefinition
        // and therefore reference the same ActivatedAbility instance (same `id`), so the
        // duplicate grants are functionally identical: the same one-shot ability surfaced as
        // two buttons confuses the UI and adds nothing in play (you can only tap once anyway).
        // Collapse them to a single entry, keeping the first granter we found.
        return result.distinctBy { it.ability.id }
    }

    /**
     * Get activated abilities granted to an entity by static abilities on battlefield permanents.
     */
    fun getStaticGrantedActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<com.wingedsheep.sdk.scripting.ActivatedAbility> =
        getStaticGrantedAbilitiesWithGranter(entityId, state).map { it.ability }
}

/**
 * An activated ability granted to an entity by a static ability, paired with the
 * permanent that granted it.
 */
data class StaticGrantedAbility(
    val ability: com.wingedsheep.sdk.scripting.ActivatedAbility,
    val granterId: EntityId
)
