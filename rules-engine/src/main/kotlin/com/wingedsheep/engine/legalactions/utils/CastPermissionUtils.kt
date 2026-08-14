package com.wingedsheep.engine.legalactions.utils

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.DynamicAmountEvaluator
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.battlefield.CastFromTopOfLibraryUsesThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.EnteredThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.GraveyardPlayPermissionUsedComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
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
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.MayPlayLandsFromGraveyard
import com.wingedsheep.sdk.scripting.MayPlayPermanentsFromGraveyard
import com.wingedsheep.sdk.scripting.PlayFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlayLandsAndCastFilteredFromTopOfLibrary
import com.wingedsheep.sdk.scripting.PlotFromTopOfLibrary
import com.wingedsheep.engine.mechanics.ExhaustActivationWaiver
import com.wingedsheep.engine.mechanics.FlashTypeGrants
import com.wingedsheep.sdk.scripting.IgnoreExhaustActivationLimit
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.PreventCycling
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.RestrictSpellsCastPerTurn
import com.wingedsheep.sdk.scripting.filters.unified.Scope
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
    /**
     * @param isExhaustAbility whether the ability being checked is an exhaust ability (CR 702.177).
     *   Only [ActivationRestriction.Once] reads it: an exhaust ability's once-only memory can be
     *   waived by [IgnoreExhaustActivationLimit] (Elvish Refueler), while a plain `Once` restriction
     *   on a non-exhaust ability never is. Defaults to false, which is the restrictive answer — a
     *   call site that forgets to pass it withholds a permission rather than granting one.
     */
    fun checkActivationRestriction(
        state: GameState,
        playerId: EntityId,
        restriction: ActivationRestriction,
        sourceId: EntityId? = null,
        abilityId: AbilityId? = null,
        isExhaustAbility: Boolean = false
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
                    tracker == null || !tracker.hasActivated(abilityId) ||
                        (isExhaustAbility && isExhaustActivationLimitWaived(state, playerId))
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
                checkActivationRestriction(state, playerId, it, sourceId, abilityId, isExhaustAbility)
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
        // An alternative-cost play-from-top permission (Gwenom, Remorseless) also lets any card be
        // played from the top — printed or granted durationally.
        return playFromTopAlternativeCost(state, playerId) != null
    }

    /**
     * The effective [com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost] permission for
     * [playerId] (Gwenom's "play from the top; pay life equal to a spell's mana value rather than its
     * mana cost"), or null. Printed on a permanent the player controls, or granted durationally in
     * `grantedStaticAbilities` anchored to a permanent they control. Mirrors the printed-or-granted
     * scan used for `MayCastFromGraveyard`.
     */
    fun playFromTopAlternativeCost(
        state: GameState,
        playerId: EntityId
    ): com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            cardDef.script.staticAbilities
                .firstOrNull { it is com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost }
                ?.let { return it as com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost }
        }
        for (grant in state.grantedStaticAbilities) {
            val ability = grant.ability
            if (ability !is com.wingedsheep.sdk.scripting.PlayFromTopWithAlternativeCost) continue
            val anchor = state.getEntity(grant.entityId) ?: continue
            if (anchor.get<ControllerComponent>()?.playerId != playerId) continue
            return ability
        }
        return null
    }

    /**
     * Resolve a static ability that may be gated by a [com.wingedsheep.sdk.scripting.ConditionalStaticAbility]
     * (e.g. The Lunar Whale's "as long as it attacked this turn, you may play the top card of your
     * library"). Returns the underlying ability when it is currently active: unconditional abilities
     * pass through unchanged, while a conditional one is honored only while its condition holds
     * against the granting permanent ([sourceId], controlled by [playerId]). Returns null when a
     * conditional gate is currently false.
     */
    private fun activeStaticAbility(
        state: GameState,
        ability: com.wingedsheep.sdk.scripting.StaticAbility,
        sourceId: EntityId,
        playerId: EntityId
    ): com.wingedsheep.sdk.scripting.StaticAbility? = when (ability) {
        is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
            val context = EffectContext(sourceId = sourceId, controllerId = playerId)
            if (conditionEvaluator.evaluate(state, ability.condition, context)) ability.ability else null
        }
        else -> ability
    }

    fun hasPlayLandsFromTopOfLibrary(state: GameState, playerId: EntityId): Boolean {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            if (cardDef.script.staticAbilities.any {
                    activeStaticAbility(state, it, entityId, playerId) is PlayLandsAndCastFilteredFromTopOfLibrary
                }) {
                return true
            }
        }
        // "You may play cards from the top of your library" (Gwenom) also permits land plays from top.
        return playFromTopAlternativeCost(state, playerId) != null
    }

    fun getCastFilteredFromTopOfLibraryFilter(state: GameState, playerId: EntityId): GameObjectFilter? {
        for (entityId in state.getBattlefield(playerId)) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in cardDef.script.staticAbilities) {
                val active = activeStaticAbility(state, ability, entityId, playerId)
                if (active is PlayLandsAndCastFilteredFromTopOfLibrary) {
                    return active.spellFilter
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
                    val uses = state.getEntity(entityId)
                        ?.get<CastFromTopOfLibraryUsesThisTurnComponent>()?.uses ?: 0
                    val maxCasts = ability.maxCastsPerTurn
                    if (maxCasts != null && uses >= maxCasts) continue
                    if (ability.filter == GameObjectFilter.Any) return GameObjectFilter.Any
                    filter = filter?.let { it or ability.filter } ?: ability.filter
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
                        if (!FlashTypeGrants.nthGateAllows(state, spellOwner, ability, predicateEvaluator)) continue
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
    fun equipCostReduction(
        state: GameState,
        playerId: EntityId,
        equipTargetId: EntityId? = null,
        abilitySourceId: EntityId? = null
    ): Int =
        sumActiveEquipReductions(state, playerId, equipTargetId, abilitySourceId)

    /**
     * Reduce the generic portion of [cost] when [ability] is an equip ability and [playerId] has
     * one or more active [ReduceEquipCost] grants. Floors at {0} and leaves colored pips intact.
     * Shared by the enumerator (offered/displayed cost) and [ActivateAbilityHandler] (paid cost)
     * so the two always agree. Applied before [applyFreeFirstEquipDiscount].
     *
     * [equipTargetId] is the creature the equip ability targets, when known. A target-restricted
     * grant ([ReduceEquipCost.onlyIfTargetIsSource]) only reduces the cost when the equip targets
     * the permanent bearing the grant. Pass the chosen target at payment time so the paid cost is
     * exact; pass `null` at enumeration (before the target is chosen) to offer the discount
     * optimistically.
     *
     * [abilitySourceId] is the permanent whose equip ability is being activated. A self-restricted
     * grant ([ReduceEquipCost.onlyOwnEquip], Firion's token) only reduces its own bearer's equip
     * abilities, so it counts only when its bearer equals [abilitySourceId]. Pass the ability's
     * source at both enumeration and payment.
     */
    fun applyEquipCostReduction(
        cost: AbilityCost,
        ability: ActivatedAbility,
        state: GameState,
        playerId: EntityId,
        equipTargetId: EntityId? = null,
        abilitySourceId: EntityId? = null
    ): AbilityCost {
        if (!ability.isEquipAbility) return cost
        val reduction = equipCostReduction(state, playerId, equipTargetId, abilitySourceId)
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
     * Replace [cost] with {0} when [ability] is an equip ability, [playerId] has an active
     * [FreeFirstEquipEachTurn] grant (Forge Anew), and this is their first equip this turn
     * (`EquipActivationsThisTurnComponent.count == 0`). Shared by the enumerator (offered/displayed
     * cost) and [ActivateAbilityHandler] (paid cost) so the two always agree. "Pay {0} rather than
     * pay the equip cost" is an alternative cost for the activation, so it replaces every part of
     * the equip cost, including nonmana costs such as paying life.
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
        return AbilityCost.Atom(CostAtom.Mana(ManaCost.ZERO))
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
     * True when some permanent [playerId] controls waives the "activate only once" memory that an
     * exhaust ability carries (CR 702.177a) — Elvish Refueler's "you may activate exhaust abilities
     * as though they haven't been activated".
     *
     * Scans printed and granted [IgnoreExhaustActivationLimit] statics on [playerId]'s battlefield
     * and evaluates each one's condition in the granting permanent's controller's context, so
     * Elvish Refueler's "During your turn, as long as you haven't activated an exhaust ability this
     * turn" gate is re-checked every frame — the waiver disappears the moment the turn's first
     * exhaust ability is activated. Consulted by both this class's restriction check (the
     * enumerators' offered actions) and [ActivateAbilityHandler]'s (the executed action), so the
     * two can't drift.
     */
    fun isExhaustActivationLimitWaived(state: GameState, playerId: EntityId): Boolean =
        ExhaustActivationWaiver.isWaivedFor(state, playerId, cardRegistry, conditionEvaluator)

    /**
     * Sum the [ReduceEquipCost] amounts across [playerId]'s battlefield, unwrapping a
     * [ConditionalStaticAbility] and evaluating its condition against the granting permanent.
     * Mirrors [hasActiveEquipPermission] but accumulates an amount instead of short-circuiting.
     */
    private fun sumActiveEquipReductions(
        state: GameState,
        playerId: EntityId,
        equipTargetId: EntityId?,
        abilitySourceId: EntityId? = null
    ): Int {
        var total = 0
        for (entityId in state.getBattlefield(playerId)) {
            // A self-restricted grant (onlyOwnEquip) only discounts its own bearer's equip
            // abilities: skip the whole entity when it isn't the equip ability's source. At
            // enumeration the source is known (the permanent whose ability is listed), so this
            // stays exact.
            fun countsForSource(ability: com.wingedsheep.sdk.scripting.ReduceEquipCost): Boolean =
                !ability.onlyOwnEquip || abilitySourceId == null || entityId == abilitySourceId
            // Printed static abilities (from the card definition), plus any granted to this entity
            // via GameState.grantedStaticAbilities (tokens have no CardDefinition — Firion's copy).
            val card = state.getEntity(entityId)?.get<CardComponent>()
            val classLevel = state.getEntity(entityId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            val printed = card?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.script?.effectiveStaticAbilities(classLevel).orEmpty()
            val granted = state.grantedStaticAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            for (ability in printed + granted) {
                when (ability) {
                    is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
                        val inner = ability.ability as? com.wingedsheep.sdk.scripting.ReduceEquipCost ?: continue
                        if (!countsForSource(inner)) continue
                        if (!equipReductionApplies(state, entityId, inner, equipTargetId)) continue
                        val context = com.wingedsheep.engine.handlers.EffectContext(
                            sourceId = entityId,
                            controllerId = playerId
                        )
                        if (conditionEvaluator.evaluate(state, ability.condition, context)) total += inner.amount
                    }
                    is com.wingedsheep.sdk.scripting.ReduceEquipCost ->
                        if (countsForSource(ability) &&
                            equipReductionApplies(state, entityId, ability, equipTargetId)) total += ability.amount
                    else -> {}
                }
            }
        }
        return total
    }

    /**
     * Whether a [ReduceEquipCost] grant borne by [sourceId] applies to an equip activation.
     * Unrestricted grants (Éowyn) always apply. A target-restricted grant
     * ([ReduceEquipCost.onlyIfTargetIsSource], Cloud) applies only when the equip targets the
     * source permanent: at payment ([equipTargetId] known) the ids must match; at enumeration
     * ([equipTargetId] null) it applies optimistically whenever the source is currently a
     * creature — i.e. a permanent the controller could legally choose as the equip's target — so
     * the discounted cost is never withheld before the target is chosen.
     */
    private fun equipReductionApplies(
        state: GameState,
        sourceId: EntityId,
        grant: com.wingedsheep.sdk.scripting.ReduceEquipCost,
        equipTargetId: EntityId?
    ): Boolean {
        if (!grant.onlyIfTargetIsSource) return true
        return if (equipTargetId != null) equipTargetId == sourceId
        else state.projectedState.isCreature(sourceId)
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
     *
     * [isExhaustAbility] is the activated ability's `isExhaust` flag (CR 702.177). A static with
     * [ReduceActivatedAbilityCost.exhaustOnly] applies only when it is true — Boom Scholar's "Exhaust
     * abilities of other permanents you control cost {2} less to activate" leaves those permanents'
     * ordinary activated abilities at full price. [isPowerUpAbility] is the same story for `isPowerUp`
     * (CR 702.193) and [ReduceActivatedAbilityCost.powerUpOnly] (Hulk, Gamma Goliath), and it
     * additionally switches on power-up's *own* cost reduction — see
     * [applyPowerUpSelfReduction], which is pip-wise rather than generic-only and so runs as its own
     * step before the statics above. CR 601.2f lets multiple cost reductions apply in any order.
     */
    fun applyActivatedAbilityCostReduction(
        cost: AbilityCost,
        state: GameState,
        sourceId: EntityId?,
        isExhaustAbility: Boolean = false,
        isPowerUpAbility: Boolean = false
    ): AbilityCost {
        if (sourceId == null) return cost
        val reduced =
            if (isPowerUpAbility) applyPowerUpSelfReduction(cost, state, sourceId) else cost
        val (net, manaFloor) =
            sumActivatedAbilityCostModifications(state, sourceId, isExhaustAbility, isPowerUpAbility)
        if (net == 0) return reduced
        // net > 0 reduces (floored), net < 0 taxes. A reduction can only shrink mana that is
        // already there; a tax applies to *every* activated ability, so a cost with no mana part
        // (`{T}:`, sacrifice, crew) gains one — "{T}:" taxed by {2} becomes "{2}, {T}:".
        val modify: (ManaCost) -> ManaCost =
            if (net > 0) { mana -> mana.reduceGenericWithManaFloor(net, manaFloor) }
            else { mana -> mana.increaseGeneric(-net) }
        mapFirstManaComponent(reduced, modify)?.let { return it }
        // No mana part to modify (Tap, TapAttachedCreature, Loyalty, Craft, …): a reduction is a
        // no-op, while a tax prepends the mana the player must now also pay. The prepend flattens
        // into an existing Composite rather than nesting one inside it — payment and enumeration
        // walk `Composite.costs` expecting atoms, not a sub-composite.
        if (net >= 0) return reduced
        val taxAtom = AbilityCost.Atom(CostAtom.Mana(modify(ManaCost.ZERO)))
        return when (reduced) {
            is AbilityCost.Composite -> AbilityCost.Composite(listOf(taxAtom) + reduced.costs)
            else -> AbilityCost.Composite(listOf(taxAtom, reduced))
        }
    }

    /**
     * Apply power-up's own cost reduction (CR 702.193a): *"If this permanent entered this turn, this
     * ability's cost is reduced by this permanent's mana cost."*
     *
     * Two things make this unlike every other reduction in the engine, and both are load-bearing:
     *  - **It is pip-wise, not generic-only.** CR 702.193b subtracts the whole printed mana cost,
     *    colored and colorless pips included, so it goes through [ManaCost.subtract] (CR 118.7)
     *    rather than [ManaCost.reduceGenericWithManaFloor]. Thanos's `{C}{W}{U}{B}{R}{G}` reduced by
     *    `{R}{W}{B}` is `{C}{U}{G}`; a generic-only reduction would leave it untouched.
     *  - **It is conditional on the turn the permanent entered**, which is exactly
     *    [EnteredThisTurnComponent] — the same marker `Conditions.SourceEnteredThisTurn` reads.
     *    Note this is "entered", not "you've controlled it since your last turn": a permanent that
     *    entered under an opponent's control and changed hands this turn still gets the discount.
     *
     * The mana cost read is the one on the permanent's [CardComponent], which a copy effect
     * replaces outright (keeping the original in `CopyOfComponent`), so a permanent that is a copy
     * of something else is reduced by the *copied* mana cost. Face-down permanents never reach here
     * at all: turning face down doesn't touch [CardComponent], but a face-down permanent has no
     * abilities (CR 708.2), so it has no power-up to activate.
     */
    private fun applyPowerUpSelfReduction(
        cost: AbilityCost,
        state: GameState,
        sourceId: EntityId
    ): AbilityCost {
        val container = state.getEntity(sourceId) ?: return cost
        if (!container.has<EnteredThisTurnComponent>()) return cost
        val printedCost = container.get<CardComponent>()?.manaCost ?: return cost
        if (printedCost.isEmpty()) return cost
        return mapFirstManaComponent(cost) { it.subtract(printedCost) } ?: cost
    }

    /**
     * Rewrite the first mana component of [cost] through [modify], or return null when the cost has
     * no mana part at all. Null rather than the unchanged cost so callers can tell "nothing to
     * reduce" from "reduced to {0}" — a cost increase has to *add* a mana component in the first
     * case, while a reduction is simply a no-op.
     */
    private fun mapFirstManaComponent(
        cost: AbilityCost,
        modify: (ManaCost) -> ManaCost
    ): AbilityCost? = when (cost) {
        is AbilityCost.Atom ->
            cost.manaCostOrNull?.let { AbilityCost.Atom(CostAtom.Mana(modify(it))) }
        is AbilityCost.Composite -> {
            var applied = false
            val modified = AbilityCost.Composite(cost.costs.map { sub ->
                val subMana = sub.manaCostOrNull
                if (!applied && subMana != null) {
                    applied = true
                    AbilityCost.Atom(CostAtom.Mana(modify(subMana)))
                } else sub
            })
            if (applied) modified else null
        }
        else -> null
    }

    /**
     * Net the generic cost modification (and take the most restrictive mana floor) from every
     * [ReduceActivatedAbilityCost] / [com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost]
     * static on the battlefield whose filter matches the ability source [sourceId]. The returned
     * delta is **positive for a net reduction** and negative for a net tax — reductions and
     * increases on the same ability cancel before either is applied to the cost.
     *
     * The filter scope is resolved directly: `Scope.Self` → the static's own source;
     * `Scope.AttachedTo` → the permanent the static's source (an Aura/Equipment) is attached to;
     * any other (battlefield) scope → the source's base filter matched against [sourceId] under
     * projected state.
     *
     * An `exhaustOnly` reduction contributes nothing unless [isExhaustAbility] is set, and likewise
     * a `powerUpOnly` one unless [isPowerUpAbility] is set.
     */
    private fun sumActivatedAbilityCostModifications(
        state: GameState,
        sourceId: EntityId,
        isExhaustAbility: Boolean,
        isPowerUpAbility: Boolean
    ): Pair<Int, Int> {
        var net = 0
        var floor = 0
        val evaluator = DynamicAmountEvaluator()
        for (entityId in state.getBattlefield()) {
            val card = state.getEntity(entityId)?.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val controllerId by lazy { state.getEntity(entityId)?.get<ControllerComponent>()?.playerId }
            for (ability in cardDef.script.staticAbilities) {
                when (ability) {
                    is com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost -> {
                        if (ability.exhaustOnly && !isExhaustAbility) continue
                        if (ability.powerUpOnly && !isPowerUpAbility) continue
                        if (!activatedAbilityReductionApplies(state, entityId, ability.filter, sourceId)) continue
                        val owner = controllerId ?: continue
                        net += evaluator.evaluate(
                            state,
                            ability.amount,
                            EffectContext(sourceId = entityId, controllerId = owner)
                        ).coerceAtLeast(0)
                        floor = maxOf(floor, ability.manaFloor)
                    }
                    is com.wingedsheep.sdk.scripting.IncreaseActivatedAbilityCost -> {
                        if (!activatedAbilityReductionApplies(state, entityId, ability.filter, sourceId)) continue
                        val owner = controllerId ?: continue
                        net -= evaluator.evaluate(
                            state,
                            ability.amount,
                            EffectContext(sourceId = entityId, controllerId = owner)
                        ).coerceAtLeast(0)
                    }
                    else -> continue
                }
            }
        }
        return net to floor
    }

    /**
     * Whether a [ReduceActivatedAbilityCost] on [staticSourceId] reaches the ability source [sourceId].
     *
     * `filter.excludeSelf` is honored here rather than left to the projection layer (which never sees
     * this static): a `GroupFilter(..., excludeSelf = true)` is the "**other** permanents you control"
     * wording, so the static's own source must not discount its own abilities (Boom Scholar's own
     * exhaust ability costs full price).
     */
    private fun activatedAbilityReductionApplies(
        state: GameState,
        staticSourceId: EntityId,
        filter: com.wingedsheep.sdk.scripting.filters.unified.GroupFilter,
        sourceId: EntityId
    ): Boolean = if (filter.excludeSelf && staticSourceId == sourceId) false else when (filter.scope) {
        is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> staticSourceId == sourceId
        is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo ->
            state.getEntity(staticSourceId)
                ?.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()
                ?.targetId == sourceId
        else -> {
            val projected = state.projectedState
            predicateEvaluator.matches(
                state, projected, sourceId, filter.baseFilter,
                // sourceId = the *static's* permanent, not the ability's: name-keyed predicates
                // (`NameEqualsChosenComponent`, Skyseer's Chariot) read the chosen name off the
                // permanent that carries the static.
                PredicateContext(
                    sourceId = staticSourceId,
                    controllerId = state.getEntity(staticSourceId)?.get<ControllerComponent>()?.playerId ?: staticSourceId
                )
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
     * True when any [PreventActivatedAbilities] static ability — printed on a battlefield
     * permanent (Cursed Totem, Damping Matrix, ...) or granted via
     * [com.wingedsheep.engine.event.GrantedStaticAbility] (Braided Net's durational
     * "its activated abilities can't be activated") — matches [sourceId] under projected state.
     *
     * Granted instances behave exactly like printed ones, anchored to the entity they are
     * granted to: the filter is evaluated with the grant's holder as the source and its
     * controller as the perspective. A self-scoped grant
     * (`PreventActivatedAbilities(GameObjectFilter.Permanent.sourceItself())`) therefore locks
     * the holder's own abilities. Grants with a conditional "for as long as …" duration
     * ([Duration.WhileAffectedTapped]) are gated here per-frame; the one-way latch (CR 611.2b)
     * is enforced by `EndedDurationExpiryCheck`, which physically removes the grant the moment
     * its condition fails.
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
        // Granted statics (GameState.grantedStaticAbilities) — same semantics as printed,
        // anchored to the holder entity instead of a card definition.
        for (grant in state.grantedStaticAbilities) {
            val prevent = grant.ability as? PreventActivatedAbilities ?: continue
            if (prevent.nonManaAbilitiesOnly && abilityIsManaAbility) continue
            if (!state.getBattlefield().contains(grant.entityId)) continue
            // Per-frame gate for conditional durations — the mirror of StateProjector's gate
            // for floating effects. EndedDurationExpiryCheck supplies the one-way latch.
            if (grant.duration is Duration.WhileAffectedTapped &&
                state.getEntity(grant.entityId)?.has<TappedComponent>() != true
            ) {
                continue
            }
            val holderController = projected.getController(grant.entityId)
                ?: state.getEntity(grant.entityId)?.get<ControllerComponent>()?.playerId
                ?: continue
            val context = PredicateContext(controllerId = holderController, sourceId = grant.entityId)
            if (predicateEvaluator.matches(state, projected, sourceId, prevent.filter, context)) {
                return true
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
        return LandDropUtils.getAdditionalLandDrops(state, playerId, cardRegistry, conditionEvaluator)
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
            for (rawAbility in com.wingedsheep.engine.state.components.identity.RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                // A grant can be gated by a ConditionalStaticAbility (Nature's Embrace: the land host
                // gains "{T}: Add two mana of any one color" only while it is a land). Unwrap the
                // condition against the granter here — otherwise the raw wrapper is not a
                // GrantActivatedAbility and the grant is silently dropped. Skip when the gate is false.
                val ability = when (rawAbility) {
                    is com.wingedsheep.sdk.scripting.ConditionalStaticAbility -> {
                        val granterController = state.projectedState.getController(permanentId)
                            ?: container.get<ControllerComponent>()?.playerId
                            ?: continue
                        val ctx = EffectContext(sourceId = permanentId, controllerId = granterController)
                        if (!conditionEvaluator.evaluate(state, rawAbility.condition, ctx)) continue
                        rawAbility.ability
                    }
                    else -> rawAbility
                }
                // "[filter] have all activated abilities of the [creature] cards exiled with/to craft
                // this": pull every activated ability off each card in the granter's exile pile (linked
                // or crafted, per `ability.source`) and grant it to each matching permanent. Self filter
                // = Territory Forge / Locus of Enlightenment (grants to itself); a battlefield filter =
                // Agatha's Soul Cauldron (grants to other creatures you control with +1/+1 counters). The
                // granter recorded is the *receiver* so the ability's `{T}`/self-references bind to the
                // permanent that gained it (CR-faithful). When `oncePerTurnEach` is set (Locus), each
                // ability is re-stamped with an exiled-card-derived AbilityId so duplicate materials don't
                // collapse and each gets its own once-per-turn budget (see exiledCardsActivatedAbilities).
                if (ability is com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfExiledCards) {
                    val receives = when (val scope = ability.filter.scope) {
                        is com.wingedsheep.sdk.scripting.filters.unified.Scope.Self -> permanentId == entityId
                        is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific -> scope.entityId == entityId
                        is com.wingedsheep.sdk.scripting.filters.unified.Scope.AttachedTo ->
                            container.get<com.wingedsheep.engine.state.components.battlefield.AttachedToComponent>()?.targetId == entityId
                        is com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair ->
                            com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, permanentId, entityId)
                        is com.wingedsheep.sdk.scripting.filters.unified.Scope.Battlefield -> {
                            if (ability.filter.excludeSelf && permanentId == entityId) false
                            else {
                                val granterController = state.projectedState.getController(permanentId)
                                granterController != null && predicateEvaluator.matches(
                                    state, state.projectedState, entityId, ability.filter.baseFilter,
                                    PredicateContext(controllerId = granterController, sourceId = permanentId)
                                )
                            }
                        }
                    }
                    if (receives) {
                        for (granted in exiledCardsActivatedAbilities(
                            state, permanentId, cardRegistry, ability.source, ability.creatureCardsOnly, ability.oncePerTurnEach
                        )) {
                            result.add(StaticGrantedAbility(granted, entityId))
                        }
                    }
                    continue
                }
                // "This permanent has all activated and triggered abilities of the last chosen card
                // exiled with it" (Koh, the Face Stealer): self-scoped grant of the chosen card's
                // *activated* abilities to the source, which is recorded as granter.
                if (ability is com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard) {
                    if (ability.grantActivated && permanentId == entityId) {
                        for (granted in chosenLinkedExiledActivatedAbilities(state, permanentId, cardRegistry)) {
                            result.add(StaticGrantedAbility(granted, entityId))
                        }
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
                    // Soulbond payoff (CR 702.95b): "each of those creatures has …" hands the
                    // ability to both halves of the granter's pair — Deadeye Navigator's blink
                    // ability appears on the Navigator and on the creature it's paired with. The
                    // granter stays the Navigator, so the ability's cost and controller are read
                    // from it, while `{T}` / self-references bind to the receiver.
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair -> {
                        if (com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, permanentId, entityId)) {
                            result.add(StaticGrantedAbility(ability.ability, permanentId))
                        }
                    }
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.Specific -> {
                        if (scope.entityId == entityId) result.add(StaticGrantedAbility(ability.ability, permanentId))
                    }
                }
            }
        }

        // Granted GrantActivatedAbility statics (CR 611): a permanent that was itself *granted* an
        // ability-granting static — e.g. Roar of the Fifth People chapter II. Shared with
        // ActivateAbilityHandler so the enumerator and the handler never drift.
        result.addAll(getGrantedStaticGrantActivatedAbilities(entityId, state))

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
     * Granted [GrantActivatedAbility] statics (CR 611): a permanent that was itself *granted* an
     * ability-granting static — not one printed on its card — confers the inner activated ability to
     * the matching group exactly like a printed [GrantActivatedAbility]. The outer static lands in
     * [GameState.grantedStaticAbilities] (via GrantStaticAbilityEffect) and the granter is the
     * permanent that gained it. Example: Roar of the Fifth People chapter II, "This Saga gains
     * 'Creatures you control have "{T}: Add {R}, {G}, or {W}."'".
     *
     * Shared by [getStaticGrantedAbilitiesWithGranter] (the enumerator) and `ActivateAbilityHandler`
     * (the activation handler) so both agree on which entity receives which granted ability.
     */
    fun getGrantedStaticGrantActivatedAbilities(
        entityId: EntityId,
        state: GameState
    ): List<StaticGrantedAbility> {
        val result = mutableListOf<StaticGrantedAbility>()
        for (grantedStatic in state.grantedStaticAbilities) {
            val grantAbility = grantedStatic.ability as? GrantActivatedAbility ?: continue
            val granterId = grantedStatic.entityId
            val granter = state.getEntity(granterId) ?: continue
            if (!state.getBattlefield().contains(granterId)) continue
            if (granter.has<FaceDownComponent>()) continue
            when (val scope = grantAbility.filter.scope) {
                is Scope.Battlefield -> {
                    if (grantAbility.filter.excludeSelf && granterId == entityId) continue
                    val granterController = state.projectedState.getController(granterId) ?: continue
                    val matches = predicateEvaluator.matches(
                        state,
                        state.projectedState,
                        entityId,
                        grantAbility.filter.baseFilter,
                        PredicateContext(controllerId = granterController, sourceId = granterId)
                    )
                    if (matches) result.add(StaticGrantedAbility(grantAbility.ability, granterId))
                }
                is Scope.AttachedTo -> {
                    val attachedTo = granter.get<AttachedToComponent>()
                    if (attachedTo != null && attachedTo.targetId == entityId) {
                        result.add(StaticGrantedAbility(grantAbility.ability, granterId))
                    }
                }
                is Scope.Self -> {
                    if (granterId == entityId) result.add(StaticGrantedAbility(grantAbility.ability, granterId))
                }
                is Scope.SoulbondPair -> {
                    if (com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, granterId, entityId)) {
                        result.add(StaticGrantedAbility(grantAbility.ability, granterId))
                    }
                }
                is Scope.Specific -> {
                    if (scope.entityId == entityId) result.add(StaticGrantedAbility(grantAbility.ability, granterId))
                }
            }
        }
        return result
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
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair ->
                        com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, granterId, entityId)
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
                    is com.wingedsheep.sdk.scripting.filters.unified.Scope.SoulbondPair ->
                        com.wingedsheep.engine.mechanics.SoulbondPairing.isInPairOf(state, granterId, sourceId)
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
     * True when a [com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells] static controlled by
     * [playerId] matches the card [cardId] they are casting — i.e. mana of any type may be spent on
     * that spell's mana cost (Vizier of the Menagerie: "You can spend mana of any type to cast
     * creature spells"). Callers relax the cost via
     * [com.wingedsheep.sdk.core.ManaCost.relaxColors] when this returns true (CR 118.14 / 609.4b).
     *
     * Zone-agnostic by design: the printed wording covers every creature spell you cast, so this is
     * consulted for hand casts, top-of-library casts and may-play casts alike. The card is matched
     * against the filter in projected state, so a permanent that is only a creature *because* of a
     * continuous effect still counts.
     */
    fun canSpendAnyManaTypeForSpell(state: GameState, playerId: EntityId, cardId: EntityId): Boolean {
        val projected = state.projectedState
        for (granterId in state.getBattlefield()) {
            val granter = state.getEntity(granterId) ?: continue
            if (granter.has<com.wingedsheep.engine.state.components.identity.FaceDownComponent>()) continue
            if (projected.getController(granterId) != playerId) continue
            val card = granter.get<CardComponent>() ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            val classLevel = granter.get<com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent>()?.currentLevel
            for (ability in cardDef.script.effectiveStaticAbilities(classLevel)) {
                val any = ability as? com.wingedsheep.sdk.scripting.SpendAnyManaTypeForSpells ?: continue
                val matches = predicateEvaluator.matches(
                    state, projected, cardId, any.filter,
                    PredicateContext(controllerId = playerId, sourceId = granterId)
                )
                if (matches) return true
            }
        }
        return false
    }

    /**
     * [cost] with its colored/hybrid/Phyrexian/colorless requirements relaxed to generic when
     * [canSpendAnyManaTypeForSpell] holds for this cast, otherwise [cost] unchanged. Use for
     * affordability checks, the auto-tap solver and payment — **not** for the cost string shown to
     * the client, which must stay the printed cost.
     */
    fun relaxSpellCostColorsIfAny(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        cost: ManaCost
    ): ManaCost =
        if (canSpendAnyManaTypeForSpell(state, playerId, cardId)) cost.relaxColors() else cost

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
 * The activated abilities of every card in [sourceId]'s exile pile — the engine half of
 * [com.wingedsheep.sdk.scripting.HasAllActivatedAbilitiesOfExiledCards]. [source] selects the pile:
 * [ExiledCardsSource.LINKED] reads the source's `LinkedExileComponent` (Territory Forge, Agatha's
 * Soul Cauldron); [ExiledCardsSource.CRAFTED] reads its `CraftedFromExiledComponent` (Locus of
 * Enlightenment; CR 702.167c — "the exiled cards used to craft it"). It looks up each exiled card's
 * definition and returns its `activatedAbilities`. The caller grants each with the *receiver* as the
 * granter so the ability activates against that permanent (its `{T}` taps it, self-references bind to
 * it — CR 113.7, faithful to the ruling that the exiled card's "this card" references become
 * references to the permanent that has the ability).
 *
 * When [oncePerTurnEach] is true (Locus of Enlightenment's "only once each turn"), each returned
 * ability is re-stamped with a **synthesized [AbilityId] derived from the exiled card's entity id**
 * (`exiled_<exiledEntity>_<printedAbilityId>`) and gains an
 * [com.wingedsheep.sdk.scripting.ActivationRestriction.OncePerTurn]. The re-stamp is load-bearing:
 *  - It stops the caller's `distinctBy { it.ability.id }` dedup from collapsing two exiled copies of
 *    the *same* printed card (which share one printed `AbilityId`) into a single granted ability.
 *  - It makes the standard once-per-turn tracker (`AbilityActivatedThisTurnComponent`, keyed by
 *    receiver-entity + `AbilityId`) give each exiled card its *own* once-each-turn budget for free,
 *    with no bespoke tracker.
 *
 * When [oncePerTurnEach] is false (Territory Forge, Agatha), abilities are returned unmodified — the
 * dedup collapses duplicate copies, which is fine when there's no per-card budget to keep apart.
 */
fun exiledCardsActivatedAbilities(
    state: GameState,
    sourceId: EntityId,
    cardRegistry: CardRegistry,
    source: com.wingedsheep.sdk.scripting.ExiledCardsSource,
    creatureCardsOnly: Boolean = false,
    oncePerTurnEach: Boolean = false
): List<com.wingedsheep.sdk.scripting.ActivatedAbility> {
    val exiledIds = when (source) {
        com.wingedsheep.sdk.scripting.ExiledCardsSource.LINKED -> state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent>()?.exiledIds
        com.wingedsheep.sdk.scripting.ExiledCardsSource.CRAFTED -> state.getEntity(sourceId)
            ?.get<com.wingedsheep.engine.state.components.battlefield.CraftedFromExiledComponent>()?.exiledIds
    } ?: return emptyList()
    return exiledIds.flatMap { exiledId ->
        val card = state.getEntity(exiledId)?.get<CardComponent>()
        // Agatha's "all *creature* cards exiled" restricts the pile by the exiled card's printed
        // type; the other users (creatureCardsOnly = false) take every exiled card.
        if (creatureCardsOnly && card?.typeLine?.isCreature != true) return@flatMap emptyList()
        val cardDef = card?.cardDefinitionId?.let { cardRegistry.getCard(it) }
        val abilities = cardDef?.script?.activatedAbilities ?: emptyList()
        if (!oncePerTurnEach) abilities
        else abilities.map { ability ->
            ability.copy(
                id = com.wingedsheep.sdk.scripting.AbilityId("exiled_${exiledId.value}_${ability.id.value}"),
                restrictions = ability.restrictions + com.wingedsheep.sdk.scripting.ActivationRestriction.OncePerTurn
            )
        }
    }
}

/**
 * The [com.wingedsheep.sdk.model.CardDefinition] of the single card the source most recently chose
 * from its linked-exile pile — its "last chosen card" per [ChosenLinkedExileComponent] — or null if
 * it has never chosen, or the chosen card is no longer in exile (last-known safety guard). Backs
 * [com.wingedsheep.sdk.scripting.HasAbilitiesOfChosenLinkedExiledCard] (Koh, the Face Stealer).
 */
fun chosenLinkedExiledCardDef(
    state: GameState,
    sourceId: EntityId,
    cardRegistry: CardRegistry
): com.wingedsheep.sdk.model.CardDefinition? {
    val chosenId = state.getEntity(sourceId)
        ?.get<com.wingedsheep.engine.state.components.battlefield.ChosenLinkedExileComponent>()
        ?.chosenId ?: return null
    // The chosen card must still be in exile — nothing normally removes it from Koh's pile, but
    // guard so a card that somehow left doesn't keep lending abilities.
    val stillExiled = state.zones.any { (zone, cards) ->
        zone.zoneType == com.wingedsheep.sdk.core.Zone.EXILE && chosenId in cards
    }
    if (!stillExiled) return null
    val card = state.getEntity(chosenId)?.get<CardComponent>() ?: return null
    return cardRegistry.getCard(card.cardDefinitionId)
}

/** The activated abilities of the source's last chosen linked-exiled card (empty if none). */
fun chosenLinkedExiledActivatedAbilities(
    state: GameState,
    sourceId: EntityId,
    cardRegistry: CardRegistry
): List<com.wingedsheep.sdk.scripting.ActivatedAbility> =
    chosenLinkedExiledCardDef(state, sourceId, cardRegistry)?.script?.activatedAbilities ?: emptyList()

/** The triggered abilities of the source's last chosen linked-exiled card (empty if none). */
fun chosenLinkedExiledTriggeredAbilities(
    state: GameState,
    sourceId: EntityId,
    cardRegistry: CardRegistry
): List<com.wingedsheep.sdk.scripting.TriggeredAbility> =
    chosenLinkedExiledCardDef(state, sourceId, cardRegistry)?.script?.triggeredAbilities ?: emptyList()
