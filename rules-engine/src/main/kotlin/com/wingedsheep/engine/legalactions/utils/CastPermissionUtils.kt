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
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.player.CantCastSpellsComponent
import com.wingedsheep.engine.state.components.player.EquipActivationsThisTurnComponent
import com.wingedsheep.engine.state.components.player.FlashGrantsThisTurnComponent
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.costs.CostAtom
import com.wingedsheep.sdk.scripting.costs.manaCostOrNull
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CantCastSpellsSharingColorWithLastCast
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.CastSpellTypesFromTopOfLibrary
import com.wingedsheep.sdk.scripting.ExtraLoyaltyActivation
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.EquipAbilitiesAtInstantSpeed
import com.wingedsheep.sdk.scripting.FreeFirstEquipEachTurn
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlotFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.PreventCycling
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.references.Player

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
            is ActivationRestriction.OnlyDuringYourTurn -> state.isActiveTurnFor(playerId)
            is ActivationRestriction.BeforeStep -> state.step.ordinal < restriction.step.ordinal
            is ActivationRestriction.DuringPhase -> state.phase == restriction.phase
            is ActivationRestriction.DuringStep -> state.step == restriction.step
            is ActivationRestriction.OnlyIfCondition -> {
                val context = EffectContext(
                    sourceId = sourceId,
                    controllerId = playerId,
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
            is ActivationRestriction.ControlledSinceYourMostRecentTurn -> {
                // "Controlled continuously since the beginning of your most recent turn" — the
                // summoning-sickness condition (CR 302.6) generalized to any permanent. The engine
                // re-stamps SummoningSicknessComponent on entry and on every control change and
                // clears it at the controller's untap, so its absence is exactly this predicate.
                if (sourceId == null) true
                else state.getEntity(sourceId)
                    ?.has<com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent>() != true
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

        val context = EffectContext(
            sourceId = null,
            controllerId = playerId,
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
     * Whether [playerId] has already cast as many spells this turn as a [RestrictSpellsCastPerTurn]
     * permanent allows. Two scopes are folded:
     *
     *  - **controller-scoped** ([RestrictSpellsCastPerTurn.eachPlayer] = false) — only counts
     *    permanents [playerId] themselves controls (Yawgmoth's Agenda: "You can't cast more than
     *    one spell each turn.").
     *  - **global** ([RestrictSpellsCastPerTurn.eachPlayer] = true) — counts any such permanent
     *    anywhere on the battlefield, binding every player (High Noon: "Each player can't cast
     *    more than one spell each turn.").
     *
     * When several such permanents apply, the most restrictive (smallest
     * [RestrictSpellsCastPerTurn.maxPerTurn]) applies. Returns false when no permanent restricts
     * [playerId].
     */
    fun hasReachedSpellCastLimit(state: GameState, playerId: EntityId): Boolean {
        var limit: Int? = null
        // Permanents the player controls restrict them whether eachPlayer is true or false.
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa is RestrictSpellsCastPerTurn) {
                    limit = minOf(limit ?: sa.maxPerTurn, sa.maxPerTurn)
                }
            }
        }
        // Global (eachPlayer) restrictions bind every player regardless of who controls them.
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa is RestrictSpellsCastPerTurn && sa.eachPlayer) {
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
     * The reason [playerId] can't cast [spellCardId] right now, or `null` if they can. The single
     * cast-legality chokepoint: every cast enumerator (via [EnumerationContext.cantCastSpell]) and
     * [com.wingedsheep.engine.handlers.actions.spell.CastSpellHandler] route through here, so a new
     * cast restriction is enforced uniformly across every casting zone (hand, flashback, harmonize,
     * exile, top of library) by adding a case here — not by sprinkling checks at each site.
     *
     * Folds the blanket per-player locks (a [CantCastSpellsComponent] from a Silence-style effect,
     * the [RestrictSpellsCastPerTurn] per-turn limit) and the per-spell static restrictions
     * (Mana Maze's [CantCastSpellsSharingColorWithLastCast], any [PlayersCantCastSpells]).
     */
    fun reasonCannotCast(state: GameState, playerId: EntityId, spellCardId: EntityId): String? {
        if (state.getEntity(playerId)?.has<CantCastSpellsComponent>() == true) {
            return "You can't cast spells right now"
        }
        if (hasReachedSpellCastLimit(state, playerId)) {
            return "You can't cast another spell this turn"
        }
        if (sharesColorWithMostRecentCast(state, spellCardId)) {
            return "You can't cast a spell that shares a color with the spell most recently cast this turn"
        }
        if (blockedByPlayersCantCastSpells(state, playerId, spellCardId)) {
            return "An effect prevents you from casting that spell right now"
        }
        if (lacksLegendaryControlForLegendarySpell(state, playerId, spellCardId)) {
            return "You can cast a legendary instant or sorcery only if you control a legendary creature or planeswalker"
        }
        return null
    }

    /**
     * CR 205.4e: an instant or sorcery spell with the "legendary" supertype can be cast only if its
     * controller controls a legendary creature or a legendary planeswalker. Returns true when
     * [spellCardId] is such a spell and [playerId] controls neither — i.e. the cast must be blocked.
     *
     * Cheap in the common case: the supertype/card-type gate short-circuits before the battlefield
     * scan, so only a legendary instant/sorcery ever pays for [controlsLegendaryCreatureOrPlaneswalker].
     * The spell's type line is read from base state (it's being cast from a non-battlefield zone).
     */
    fun lacksLegendaryControlForLegendarySpell(
        state: GameState,
        playerId: EntityId,
        spellCardId: EntityId
    ): Boolean {
        val typeLine = state.getEntity(spellCardId)?.get<CardComponent>()?.typeLine ?: return false
        if (!typeLine.isLegendary) return false
        if (!typeLine.isInstant && !typeLine.isSorcery) return false
        return !controlsLegendaryCreatureOrPlaneswalker(state, playerId)
    }

    /**
     * Whether [playerId] controls at least one legendary creature or legendary planeswalker, read
     * from projected state (CR 205.4e). Type, supertype, and control can all be altered by
     * continuous effects, so this must go through the projection rather than base components.
     */
    fun controlsLegendaryCreatureOrPlaneswalker(state: GameState, playerId: EntityId): Boolean {
        val projected = state.projectedState
        return projected.getBattlefieldControlledBy(playerId).any { id ->
            projected.isLegendary(id) && (projected.isCreature(id) || projected.isPlaneswalker(id))
        }
    }

    /**
     * The per-spell slice of [reasonCannotCast] — restrictions that depend on *which* spell is
     * being cast (Mana Maze's color sharing, a filtered [PlayersCantCastSpells]). Separated from
     * the blanket per-player locks so enumeration can cache the latter once per pass and only pay
     * the per-card scan when [anyPerSpellCastRestrictionPresent] says a relevant static is in play.
     */
    fun spellSpecificallyRestricted(state: GameState, playerId: EntityId, spellCardId: EntityId): Boolean =
        sharesColorWithMostRecentCast(state, spellCardId) ||
            blockedByPlayersCantCastSpells(state, playerId, spellCardId)

    /**
     * Cheap guard: does any battlefield permanent carry a per-spell cast restriction
     * ([CantCastSpellsSharingColorWithLastCast] or [PlayersCantCastSpells])? Lets enumeration skip
     * the per-card [spellSpecificallyRestricted] scan entirely in the common case where none is in
     * play. Cached once per enumeration pass by [EnumerationContext].
     */
    fun anyPerSpellCastRestrictionPresent(state: GameState): Boolean =
        state.getBattlefield().any { id ->
            val cardDef = state.getEntity(id)?.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
            cardDef?.script?.staticAbilities?.any {
                it is CantCastSpellsSharingColorWithLastCast || it is PlayersCantCastSpells
            } == true
        }

    /**
     * True when a [PlayersCantCastSpells] static forbids [castingPlayerId] from casting the card
     * [spellCardId] — i.e. some battlefield permanent's ability whose [affected][PlayersCantCastSpells.affected]
     * group (relative to the granter's controller) includes the caster, whose
     * [condition][PlayersCantCastSpells.condition] holds in the controller's context, and whose
     * [spellFilter][PlayersCantCastSpells.spellFilter] matches the card. Control is read from
     * projected state; face-down permanents (no abilities) are skipped.
     */
    private fun blockedByPlayersCantCastSpells(
        state: GameState,
        castingPlayerId: EntityId,
        spellCardId: EntityId
    ): Boolean {
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            val cardDef = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa !is PlayersCantCastSpells) continue
                val controller = projected.getController(permanentId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: continue
                if (!affectedPlayerMatches(sa.affected, controller, castingPlayerId)) continue
                val condition = sa.condition
                if (condition != null) {
                    val ctx = EffectContext(
                        sourceId = permanentId,
                        controllerId = controller,
                    )
                    if (!conditionEvaluator.evaluate(state, condition, ctx)) continue
                }
                // Match the spell filter against the card being cast (card predicates apply in any zone).
                if (predicateEvaluator.matches(
                        state, projected, spellCardId, sa.spellFilter,
                        PredicateContext(controllerId = castingPlayerId)
                    )
                ) {
                    return true
                }
            }
        }
        return false
    }

    /** Whether [affected] (interpreted relative to [controllerId]) includes [castingPlayerId]. */
    private fun affectedPlayerMatches(affected: Player, controllerId: EntityId, castingPlayerId: EntityId): Boolean =
        when (affected) {
            is Player.You -> castingPlayerId == controllerId
            is Player.EachOpponent -> castingPlayerId != controllerId
            is Player.Each, is Player.Any, is Player.ActivePlayerFirst -> true
            // Target-bound references (TargetPlayer, …) have no meaning for a continuous static.
            else -> false
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

    /**
     * If [playerId] controls a permanent granting [PlotFromTopOfLibrary] (Fblthp), the filter the
     * top card must match to be plottable from the library; null if no such permission is active.
     */
    fun getPlotFromTopOfLibraryFilter(state: GameState, playerId: EntityId): GameObjectFilter? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                if (ability is PlotFromTopOfLibrary) return ability.filter
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
            val effectContext = EffectContext(
                sourceId = spellCardId,
                controllerId = spellOwner,
            )
            if (conditionEvaluator.evaluate(state, conditionalFlash, effectContext)) {
                return true
            }
        }

        val context = PredicateContext(controllerId = spellOwner)

        // Turn-scoped grants on the spell owner (Borne Upon a Wind etc., via
        // GrantFlashToSpellsEffect → FlashGrantsThisTurnComponent).
        val turnGrants = state.getEntity(spellOwner)?.get<FlashGrantsThisTurnComponent>()
        if (turnGrants != null) {
            for (filter in turnGrants.filters) {
                if (predicateEvaluator.matches(state, state.projectedState, spellCardId, filter, context)) {
                    return true
                }
            }
        }

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

    /**
     * True when [playerId] controls a permanent granting [EquipAbilitiesAtInstantSpeed]
     * (Forge Anew, Leonin Shikari) whose condition — if wrapped in [ConditionalStaticAbility]
     * (Forge Anew's "During your turn") — currently holds. Equip is otherwise sorcery-speed.
     */
    fun canEquipAtInstantSpeed(state: GameState, playerId: EntityId): Boolean =
        hasActiveEquipPermission(state, playerId) { it is EquipAbilitiesAtInstantSpeed }

    /**
     * True when [playerId] controls a permanent granting [FreeFirstEquipEachTurn] whose
     * condition (if any) currently holds. The caller still gates the discount on
     * `EquipActivationsThisTurnComponent.count == 0` so only the turn's *first* equip is free.
     */
    fun hasFreeFirstEquip(state: GameState, playerId: EntityId): Boolean =
        hasActiveEquipPermission(state, playerId) { it is FreeFirstEquipEachTurn }

    /**
     * Total generic-mana reduction [playerId] has for activating equip abilities, summed across
     * every controlled [ReduceEquipCost] grant whose condition (if any) currently holds
     * (Éowyn, Lady of Rohan). Multiple sources stack additively. Returns 0 when none apply.
     */
    fun equipCostReduction(state: GameState, playerId: EntityId): Int =
        sumActiveEquipReductions(state, playerId)

    /**
     * Reduce the generic portion of [cost] when [ability] is an equip ability and [playerId] has
     * one or more active [ReduceEquipCost] grants. Floors at {0} and leaves colored pips intact.
     * Shared by the enumerator (offered/displayed cost) and [ActivateAbilityHandler] (paid cost)
     * so the two always agree. Applied before [applyFreeFirstEquipDiscount].
     */
    fun applyEquipCostReduction(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        playerId: EntityId
    ): AbilityCost {
        if (!ability.isEquipAbility) return cost
        val reduction = equipCostReduction(state, playerId)
        if (reduction <= 0) return cost
        return when (cost) {
            is AbilityCost.Atom -> cost.manaCostOrNull
                ?.let { AbilityCost.Atom(CostAtom.Mana(it.reduceGeneric(reduction))) } ?: cost
            is AbilityCost.Composite -> {
                var applied = false
                AbilityCost.Composite(cost.costs.map { sub ->
                    val subMana = sub.manaCostOrNull
                    if (!applied && subMana != null) {
                        applied = true
                        AbilityCost.Atom(CostAtom.Mana(subMana.reduceGeneric(reduction)))
                    } else sub
                })
            }
            else -> cost
        }
    }

    /**
     * Zero the mana cost of [cost] when [ability] is an equip ability, [playerId] has an active
     * [FreeFirstEquipEachTurn] grant (Forge Anew), and this is their first equip this turn
     * (`EquipActivationsThisTurnComponent.count == 0`). Shared by the enumerator (offered/displayed
     * cost) and [ActivateAbilityHandler] (paid cost) so the two always agree. "Pay {0} rather than
     * pay the equip cost" zeroes the whole cost, including any colored pips.
     */
    fun applyFreeFirstEquipDiscount(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        playerId: EntityId
    ): AbilityCost {
        if (!ability.isEquipAbility) return cost
        val activations = state.getEntity(playerId)?.get<EquipActivationsThisTurnComponent>()?.count ?: 0
        if (activations > 0) return cost
        if (!hasFreeFirstEquip(state, playerId)) return cost
        return when (cost) {
            is AbilityCost.Atom ->
                if (cost.manaCostOrNull != null) AbilityCost.Atom(CostAtom.Mana(ManaCost.ZERO)) else cost
            is AbilityCost.Composite -> AbilityCost.Composite(cost.costs.map {
                if (it.manaCostOrNull != null) AbilityCost.Atom(CostAtom.Mana(ManaCost.ZERO)) else it
            })
            else -> cost
        }
    }

    /**
     * Scan [playerId]'s battlefield for a static ability matching [predicate], unwrapping a
     * [ConditionalStaticAbility] and evaluating its condition against the granting permanent.
     * Mirrors the permission-scan shape used by [hasGraveyardPlayPermissionForType].
     */
    private fun hasActiveEquipPermission(
        state: GameState,
        playerId: EntityId,
        predicate: (com.wingedsheep.sdk.scripting.StaticAbility) -> Boolean
    ): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                when (ability) {
                    is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
                        if (!predicate(ability.ability)) continue
                        val context = com.wingedsheep.engine.handlers.EffectContext(
                            sourceId = entityId,
                            controllerId = playerId,
                        )
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) return true
                    }
                    else -> if (predicate(ability)) return true
                }
            }
        }
        return false
    }

    /**
     * Sum the [ReduceEquipCost] amounts across [playerId]'s battlefield, unwrapping a
     * [ConditionalStaticAbility] and evaluating its condition against the granting permanent.
     * Mirrors [hasActiveEquipPermission] but accumulates an amount instead of short-circuiting.
     */
    private fun sumActiveEquipReductions(state: GameState, playerId: EntityId): Int {
        var total = 0
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                when (ability) {
                    is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
                        val inner = ability.ability as? com.wingedsheep.sdk.scripting.ReduceEquipCost ?: continue
                        val context = com.wingedsheep.engine.handlers.EffectContext(
                            sourceId = entityId,
                            controllerId = playerId
                        )
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) total += inner.amount
                    }
                    is com.wingedsheep.sdk.scripting.ReduceEquipCost -> total += ability.amount
                    else -> {}
                }
            }
        }
        return total
    }

    /**
     * Reduce the generic portion of an activated ability's [cost] by any [ReduceActivatedAbilityCost]
     * static on the battlefield whose [filter] matches the ability's source ([sourceId]) — e.g.
     * Power Artifact reducing the enchanted artifact's activated abilities by {2} (floored so the
     * mana in the cost stays at least one mana). Shared by the enumerator (offered/displayed cost)
     * and [ActivateAbilityHandler] (paid cost) so the two always agree. Returns [cost] unchanged
     * when no reduction applies, the ability has no mana cost, or the source can't be resolved.
     *
     * Generic-only (colored pips untouched, CR 118.7); reductions stack additively and the most
     * restrictive (largest) [ReduceActivatedAbilityCost.manaFloor] is applied as the floor.
     */
    fun applyActivatedAbilityCostReduction(
        cost: AbilityCost,
        state: GameState,
        sourceId: EntityId?
    ): AbilityCost {
        if (sourceId == null) return cost
        val (amount, manaFloor) = sumActivatedAbilityCostReductions(state, sourceId)
        if (amount <= 0) return cost
        return when (cost) {
            is AbilityCost.Atom -> cost.manaCostOrNull
                ?.let { AbilityCost.Atom(CostAtom.Mana(it.reduceGenericWithManaFloor(amount, manaFloor))) } ?: cost
            is AbilityCost.Composite -> {
                var applied = false
                AbilityCost.Composite(cost.costs.map { sub ->
                    val subMana = sub.manaCostOrNull
                    if (!applied && subMana != null) {
                        applied = true
                        AbilityCost.Atom(CostAtom.Mana(subMana.reduceGenericWithManaFloor(amount, manaFloor)))
                    } else sub
                })
            }
            else -> cost
        }
    }

    /**
     * Sum the generic reduction (and take the most restrictive mana floor) from every
     * [ReduceActivatedAbilityCost] static on the battlefield whose [ReduceActivatedAbilityCost.filter]
     * matches the ability source [sourceId]. The filter scope is resolved directly:
     * `Scope.Self` → the static's own source; `Scope.AttachedTo` → the permanent the static's
     * source (an Aura/Equipment) is attached to; any other (battlefield) scope → the source's
     * base filter matched against [sourceId] under projected state.
     */
    private fun sumActivatedAbilityCostReductions(state: GameState, sourceId: EntityId): Pair<Int, Int> {
        var totalAmount = 0
        var floor = 0
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                val reduce = ability as? com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost ?: continue
                if (activatedAbilityReductionApplies(state, entityId, reduce.filter, sourceId)) {
                    totalAmount += reduce.amount
                    floor = maxOf(floor, reduce.manaFloor)
                }
            }
        }
        return totalAmount to floor
    }

    /** Whether a [ReduceActivatedAbilityCost] on [staticSourceId] reaches the ability source [sourceId]. */
    private fun activatedAbilityReductionApplies(
        state: GameState,
        staticSourceId: EntityId,
        filter: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter,
        sourceId: EntityId
    ): Boolean = when (filter.scope) {
        is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> staticSourceId == sourceId
        is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo ->
            state.getEntity(staticSourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                ?.targetId == sourceId
        else -> {
            val projected = state.projectedState
            predicateEvaluator.matches(
                state, projected, sourceId, filter.baseFilter,
                PredicateContext(controllerId = state.getEntity(staticSourceId)?.get<ControllerComponent>()?.playerId ?: staticSourceId)
            )
        }
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
    fun isActivationPrevented(
        state: GameState,
        sourceId: EntityId,
        abilityIsManaAbility: Boolean = false
    ): Boolean {
        val projected = state.projectedState
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            // Evaluate the filter from the *granting permanent's* controller's perspective, so a
            // controller-relative predicate like `opponentControls()` ("lands your opponents
            // control" on Sharkey) means opponents of the static's controller, not of the land.
            val granterController = projected.getController(entityId)
                ?: state.getEntity(entityId)?.get<ControllerComponent>()?.playerId
                ?: continue
            val context = PredicateContext(controllerId = granterController, sourceId = entityId)
            for (ability in cardDef.script.staticAbilities) {
                val prevent = ability as? PreventActivatedAbilities ?: continue
                // "… can't be activated unless they're mana abilities" — exempt mana abilities.
                if (prevent.nonManaAbilitiesOnly && abilityIsManaAbility) continue
                if (predicateEvaluator.matches(state, projected, sourceId, prevent.filter, context)) {
                    return true
                }
            }
        }
        return false
    }

    /**
     * True when a [PlayersCantActivateAbilities] static forbids [activatingPlayerId] from
     * activating an ability of the permanent [sourceId] — i.e. some battlefield permanent's
     * ability whose [affected][PlayersCantActivateAbilities.affected] group (relative to the
     * granter's controller) includes the activating player, whose
     * [condition][PlayersCantActivateAbilities.condition] holds in the granter's controller's
     * context, and whose [permanentFilter][PlayersCantActivateAbilities.permanentFilter] matches
     * the source permanent in projected state.
     *
     * Grand Abolisher's activate clause flows through here: "During your turn, your opponents
     * can't activate abilities of artifacts, creatures, or enchantments." Mirrors
     * [isActivationPrevented] (Cursed Totem's who/when-blind block), but additionally scopes by
     * who is activating and when. Face-down permanents (no abilities) are skipped as granters.
     */
    fun isActivationPreventedForPlayer(
        state: GameState,
        sourceId: EntityId,
        activatingPlayerId: EntityId
    ): Boolean {
        val projected = state.projectedState
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            if (container.has<FaceDownComponent>()) continue
            val cardDef = container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) } ?: continue
            for (sa in cardDef.script.staticAbilities) {
                if (sa !is PlayersCantActivateAbilities) continue
                val controller = projected.getController(permanentId)
                    ?: container.get<ControllerComponent>()?.playerId
                    ?: continue
                if (!affectedPlayerMatches(sa.affected, controller, activatingPlayerId)) continue
                val condition = sa.condition
                if (condition != null) {
                    val ctx = EffectContext(sourceId = permanentId, controllerId = controller)
                    if (!conditionEvaluator.evaluate(state, condition, ctx)) continue
                }
                if (predicateEvaluator.matches(
                        state, projected, sourceId, sa.permanentFilter,
                        PredicateContext(controllerId = activatingPlayerId)
                    )
                ) {
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
            // Crucible of Worlds style: unlimited land plays from graveyard (land-drop is the limit).
            // Unwrap mode/condition-gated abilities (e.g. Glacierwood Siege's Sultai mode) and honor
            // the gate against this source permanent.
            if (typeName == com.wingedsheep.sdk.core.CardType.LAND.name) {
                val classLevel = state.getEntity(entityId)
                    ?.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
                for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                    if (ability is com.wingedsheep.sdk.scripting.ConditionalStaticAbility) {
                        if (ability.ability is MayPlayLandsFromGraveyard) {
                            val context = com.wingedsheep.engine.handlers.EffectContext(
                                sourceId = entityId,
                                controllerId = playerId,
                            )
                            if (conditionEvaluator.evaluate(state, ability.condition, context)) return true
                        }
                    } else if (ability is MayPlayLandsFromGraveyard) {
                        return true
                    }
                }
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
            // Include unlocked Room face statics (CR 709.5) so a Room that grants activated
            // abilities (e.g. Greenhouse) only hands them out once its door is unlocked.
            for (ability in com.wingedsheep.engine.state.components.identity.RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                // "This permanent has all activated abilities of the exiled card" (Territory Forge):
                // pull every activated ability off each linked-exiled card and grant it to the source.
                if (ability is com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfLinkedExiledCard) {
                    if (permanentId != entityId) continue
                    for (granted in linkedExiledActivatedAbilities(state, permanentId, cardRegistry)) {
                        result.add(StaticGrantedAbility(granted, permanentId))
                    }
                    continue
                }
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

        // GainActivatedAbilitiesOfPermanents (Sharkey, Tyrant of the Shire): permanents matching
        // [grantedTo] gain copies of the activated abilities of permanents matching [sourceFilter].
        result.addAll(getGainedAbilitiesOfPermanents(entityId, state))

        // Multiple granters can hand the same ability to a permanent — e.g., two Brightcap
        // Badgers each grant Saproling tokens "{T}: Add {G}." The cards share a CardDefinition
        // and therefore reference the same ActivatedAbility instance (same `id`), so the
        // duplicate grants are functionally identical: the same one-shot ability surfaced as
        // two buttons confuses the UI and adds nothing in play (you can only tap once anyway).
        // Collapse them to a single entry, keeping the first granter we found.
        return result.distinctBy { it.ability.id }
    }

    /**
     * Resolve [com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents] grants for
     * [entityId]: for every battlefield permanent bearing this static whose `grantedTo` filter
     * matches [entityId], copy the printed activated abilities of every permanent matching its
     * `sourceFilter` (dropping mana abilities unless `includeManaAbilities`). The granter is the
     * permanent bearing the static (e.g., Sharkey), so a copied ability's `SacrificeSelf` / `{T}`
     * refers to the gainer — CR 113.7 (the source of an ability is the object that generated it).
     */
    fun getGainedAbilitiesOfPermanents(
        entityId: EntityId,
        state: GameState
    ): List<StaticGrantedAbility> {
        val projected = state.projectedState
        val result = mutableListOf<StaticGrantedAbility>()

        for (granterId in state.getBattlefield()) {
            val granter = state.getEntity(granterId) ?: continue
            if (granter.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue
            val card = granter.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = granter.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                val gain = ability as? com.wingedsheep.sdk.scripting.GainActivatedAbilitiesOfPermanents ?: continue
                val granterController = projected.getController(granterId) ?: continue

                // Does [entityId] match the grantedTo filter of this static?
                val gainsAbilities = when (val scope = gain.grantedTo.scope) {
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> granterId == entityId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific -> scope.entityId == entityId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo ->
                        granter.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId == entityId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> {
                        if (gain.grantedTo.excludeSelf && granterId == entityId) false
                        else predicateEvaluator.matches(
                            state, projected, entityId, gain.grantedTo.baseFilter,
                            PredicateContext(controllerId = granterController, sourceId = granterId)
                        )
                    }
                }
                if (!gainsAbilities) continue

                // Collect copies of the source permanents' printed activated abilities.
                for (sourceId in state.getBattlefield()) {
                    if (sourceId == entityId) continue // a permanent doesn't copy its own abilities here
                    val sourceEntity = state.getEntity(sourceId) ?: continue
                    if (sourceEntity.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue
                    if (projected.hasLostAllAbilities(sourceId)) continue
                    val matches = predicateEvaluator.matches(
                        state, projected, sourceId, gain.sourceFilter,
                        PredicateContext(controllerId = granterController, sourceId = granterId)
                    )
                    if (!matches) continue
                    val sourceCard = sourceEntity.get<CardComponent>() ?: continue
                    val sourceDef = cardRegistry.getCard(sourceCard.cardDefinitionId) ?: continue
                    val sourceClassLevel = sourceEntity.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
                    for (copied in sourceDef.script.effectiveActivatedAbilities(sourceClassLevel)) {
                        if (copied.activateFromZone != com.wingedsheep.sdk.core.Zone.BATTLEFIELD) continue
                        if (!gain.includeManaAbilities && copied.isManaAbility) continue
                        result.add(StaticGrantedAbility(copied, granterId))
                    }
                }
            }
        }
        return result
    }

    /**
     * True when a [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities] static on
     * the battlefield applies to [sourceId] — i.e. mana of any type may be spent to pay the mana
     * portion of [sourceId]'s activated-ability costs (Sharkey, Tyrant of the Shire). Callers
     * relax the colored/colorless requirements of the ability's mana cost via
     * [com.wingedsheep.sdk.core.ManaCost.relaxColors] when this returns true (CR 118.14 / 609.4b).
     */
    fun canSpendAnyManaTypeForAbilities(state: GameState, sourceId: EntityId): Boolean {
        val projected = state.projectedState
        for (granterId in state.getBattlefield()) {
            val granter = state.getEntity(granterId) ?: continue
            if (granter.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue
            val card = granter.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = granter.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                val any = ability as? com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities ?: continue
                val granterController = projected.getController(granterId) ?: continue
                val applies = when (val scope = any.filter.scope) {
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> granterId == sourceId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific -> scope.entityId == sourceId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo ->
                        granter.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId == sourceId
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> {
                        if (any.filter.excludeSelf && granterId == sourceId) false
                        else predicateEvaluator.matches(
                            state, projected, sourceId, any.filter.baseFilter,
                            PredicateContext(controllerId = granterController, sourceId = granterId)
                        )
                    }
                }
                if (applies) return true
            }
        }
        return false
    }

    /**
     * If [sourceId] is under a [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForActivatedAbilities]
     * static, return [cost] with the colored/hybrid/Phyrexian/colorless requirements of its mana
     * portion relaxed to generic ("mana of any type"); otherwise return [cost] unchanged. Non-mana
     * cost components (tap, sacrifice, …) are left intact.
     */
    fun relaxAbilityCostColorsIfAny(
        state: GameState,
        sourceId: EntityId,
        cost: AbilityCost
    ): AbilityCost {
        if (!canSpendAnyManaTypeForAbilities(state, sourceId)) return cost
        return when (cost) {
            is AbilityCost.Atom -> {
                val mana = cost.manaCostOrNull ?: return cost
                AbilityCost.Atom(CostAtom.Mana(mana.relaxColors()))
            }
            is AbilityCost.Composite -> AbilityCost.Composite(cost.costs.map { sub ->
                val mana = sub.manaCostOrNull
                if (mana != null) AbilityCost.Atom(CostAtom.Mana(mana.relaxColors())) else sub
            })
            else -> cost
        }
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

/**
 * The activated abilities of every card in [sourceId]'s linked-exile pile — the engine half of
 * [com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfLinkedExiledCard] (Territory Forge).
 *
 * Reads the source's [com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent],
 * looks up each exiled card's definition, and returns its `activatedAbilities`. The caller grants
 * each with [sourceId] as the granter so the ability activates against the source permanent (its
 * `{T}` taps the source, self-references bind to the source — CR-faithful to the Territory Forge
 * ruling that the exiled card's "this card" references become references to the source).
 */
fun linkedExiledActivatedAbilities(
    state: GameState,
    sourceId: EntityId,
    cardRegistry: CardRegistry
): List<com.wingedsheep.sdk.scripting.ActivatedAbility> {
    val exiledIds = state.getEntity(sourceId)
        ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()
        ?.exiledIds ?: return emptyList()
    return exiledIds.flatMap { exiledId ->
        val defId = state.getEntity(exiledId)?.get<CardComponent>()?.cardDefinitionId
        val cardDef = defId?.let { cardRegistry.getCard(it) }
        cardDef?.script?.activatedAbilities ?: emptyList()
    }
}
