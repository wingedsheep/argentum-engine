package com.wingedsheep.engine.legality

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.actions.spell.CastZoneResolver
import com.wingedsheep.engine.mechanics.OnceOnlyActivationAllowance
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.ExileEntryTurnComponent
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.battlefield.MayCastFromLinkedExileUsedThisTurnComponent
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.CastRestriction
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantMayCastFromLinkedExile
import com.wingedsheep.sdk.scripting.StaticAbility

/**
 * The one place that decides whether an [ActivationRestriction] or a [CastRestriction] holds.
 *
 * Before this existed, the `when` over [ActivationRestriction] was written four times — in
 * `ActivateAbilityHandler`, in a private copy inside `ManaSolver`, in `CastPermissionUtils`, and
 * partly again in `ActivatedAbilityEnumerator` — and the one over [CastRestriction] twice
 * (`CastSpellHandler` and `CastPermissionUtils`). What the client was offered and what the server
 * accepted were separate implementations that only agreed by discipline. Now the handlers'
 * `validate`, the legal-action enumerators and the auto-tap solver all ask this kernel, so a new
 * restriction is one branch here and every path enforces it the same way.
 * `LegalityKernelBoundaryTest` keeps the dispatch from growing back anywhere else.
 *
 * Each check comes in two spellings over the same branch: a `…Failure` form returning the reason
 * shown to the player (handlers), and a Boolean form (enumerators, the solver).
 *
 * It also owns the linked-exile cast permission ([GrantMayCastFromLinkedExile]), which the cast
 * handler, the cast and land-play enumerators, and the client view each used to find for
 * themselves — the view from base control and printed abilities only.
 */
class LegalityKernel(
    private val cardRegistry: CardRegistry,
    private val conditionEvaluator: ConditionEvaluator
) {

    // =========================================================================
    // Activation restrictions
    // =========================================================================

    /**
     * Why [playerId] can't activate [ability] of [sourceId] under [restriction] right now, or
     * `null` when the restriction is satisfied.
     *
     * @param ability the ability being checked — the single source of both the per-ability
     *   identity the turn trackers key on ([ActivatedAbility.id], read by
     *   [ActivationRestriction.OncePerTurn] / [ActivationRestriction.MaxPerTurn]) and the
     *   `isExhaust`/`isPowerUp` flags [ActivationRestriction.Once] reads: the once-only memory those
     *   keywords install can be raised or waived by an `ExtraOnceOnlyActivations` permission
     *   (Elvish Refueler, Wonder Man), while a plain `Once` on an ordinary ability never is.
     */
    fun activationRestrictionFailure(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        restriction: ActivationRestriction,
        ability: ActivatedAbility,
    ): String? = when (restriction) {
        // A permission, not a restriction: it widens who may activate, which [anyPlayerMay] reports.
        is ActivationRestriction.AnyPlayerMay -> null
        // CR 805.5a — "your turn" is the active team's turn in Two-Headed Giant.
        is ActivationRestriction.OnlyDuringYourTurn ->
            if (!state.isActiveTurnFor(playerId)) "This ability can only be activated during your turn" else null
        is ActivationRestriction.BeforeStep ->
            if (state.step.ordinal >= restriction.step.ordinal)
                "This ability can only be activated before ${restriction.step.displayName}"
            else null
        is ActivationRestriction.DuringPhase ->
            if (state.phase != restriction.phase)
                "This ability can only be activated during ${restriction.phase.displayName}"
            else null
        is ActivationRestriction.DuringStep ->
            if (state.step != restriction.step)
                "This ability can only be activated during ${restriction.step.displayName}"
            else null
        is ActivationRestriction.OnlyIfCondition -> {
            val context = EffectContext(sourceId = sourceId, controllerId = playerId, targets = emptyList(), xValue = 0)
            if (!conditionEvaluator.evaluate(state, restriction.condition, context)) "Activation condition not met" else null
        }
        is ActivationRestriction.OncePerTurn -> {
            val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
            when {
                tracker == null || !tracker.hasActivated(ability.id) -> null
                // Boast's once-each-turn can be raised ("Creatures you control can boast twice").
                ability.isBoast && OnceOnlyActivationAllowance.mayActivateThisTurn(
                    state, playerId, tracker.activationCount(ability.id), ability, cardRegistry, conditionEvaluator
                ) -> null
                else -> "This ability can only be activated once each turn"
            }
        }
        is ActivationRestriction.MaxPerTurn -> {
            val tracker = state.getEntity(sourceId)?.get<AbilityActivatedThisTurnComponent>()
            if ((tracker?.activationCount(ability.id) ?: 0) >= restriction.count)
                "This ability can't be activated more than ${restriction.count} times each turn"
            else null
        }
        is ActivationRestriction.Once ->
            if (!OnceOnlyActivationAllowance.mayActivate(state, playerId, sourceId, ability, cardRegistry, conditionEvaluator))
                "This ability can only be activated once"
            else null
        // "Controlled continuously since the beginning of your most recent turn" — the
        // summoning-sickness condition (CR 302.6) generalized to any permanent. The engine re-stamps
        // SummoningSicknessComponent on entry and on every control change and clears it at the
        // controller's untap, so its absence is exactly this predicate.
        is ActivationRestriction.ControlledSinceYourMostRecentTurn ->
            if (state.getEntity(sourceId)?.has<SummoningSicknessComponent>() == true)
                "You must have controlled this permanent continuously since your most recent turn began"
            else null
        is ActivationRestriction.All -> restriction.restrictions.firstNotNullOfOrNull {
            activationRestrictionFailure(state, playerId, sourceId, it, ability)
        }
    }

    /** The first of [ability]'s own restrictions that fails, or `null` when all of them hold. */
    fun activationRestrictionsFailure(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        ability: ActivatedAbility,
    ): String? = ability.restrictions.firstNotNullOfOrNull {
        activationRestrictionFailure(state, playerId, sourceId, it, ability)
    }

    /** Boolean spelling of [activationRestrictionFailure]. */
    fun activationRestrictionMet(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        restriction: ActivationRestriction,
        ability: ActivatedAbility,
    ): Boolean = activationRestrictionFailure(state, playerId, sourceId, restriction, ability) == null

    /** Boolean spelling of [activationRestrictionsFailure]. */
    fun activationRestrictionsMet(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        ability: ActivatedAbility,
    ): Boolean = activationRestrictionsFailure(state, playerId, sourceId, ability) == null

    // =========================================================================
    // Cast restrictions
    // =========================================================================

    /** Why [playerId] can't cast a spell carrying [restrictions] right now, or `null` if they can. */
    fun castRestrictionsFailure(
        state: GameState,
        playerId: EntityId,
        restrictions: List<CastRestriction>,
    ): String? {
        if (restrictions.isEmpty()) return null
        val context = EffectContext(sourceId = null, controllerId = playerId, targets = emptyList(), xValue = 0)
        return restrictions.firstNotNullOfOrNull { castRestrictionFailure(state, it, context) }
    }

    /**
     * The largest X [playerId] may announce casting [spellCardId] with ("X can't be greater
     * than ..."), or `null` when [script] sets no cap. Read as X is announced (CR 601.2b) — the
     * enumerator's slider ceiling and the validator's check — and never re-read afterwards.
     */
    fun spellXMaximum(state: GameState, playerId: EntityId, spellCardId: EntityId, script: CardScript): Int? {
        val cap = script.xMaximum ?: return null
        val context = EffectContext(sourceId = spellCardId, controllerId = playerId)
        return conditionEvaluator.amounts.evaluate(state, cap, context).coerceAtLeast(0)
    }

    /** Boolean spelling of [castRestrictionsFailure]. */
    fun castRestrictionsMet(state: GameState, playerId: EntityId, restrictions: List<CastRestriction>): Boolean =
        castRestrictionsFailure(state, playerId, restrictions) == null

    private fun castRestrictionFailure(state: GameState, restriction: CastRestriction, context: EffectContext): String? =
        when (restriction) {
            is CastRestriction.OnlyDuringStep ->
                if (state.step != restriction.step)
                    "Can only be cast during the ${restriction.step.name.lowercase().replace('_', ' ')} step"
                else null
            is CastRestriction.OnlyDuringPhase ->
                if (state.phase != restriction.phase)
                    "Can only be cast during the ${restriction.phase.name.lowercase().replace('_', ' ')} phase"
                else null
            is CastRestriction.OnlyIfCondition ->
                if (!conditionEvaluator.evaluate(state, restriction.condition, context)) "Casting condition not met" else null
            // Timing is the caller's sorcery-speed check; this entry only documents the requirement.
            is CastRestriction.TimingRequirement -> null
            is CastRestriction.All -> restriction.restrictions.firstNotNullOfOrNull {
                castRestrictionFailure(state, it, context)
            }
        }

    // =========================================================================
    // Linked-exile cast permission (GrantMayCastFromLinkedExile)
    // =========================================================================

    /** A permanent whose [GrantMayCastFromLinkedExile] lets its controller cast from [exiledIds]. */
    data class LinkedExileGranter(
        val granterId: EntityId,
        val ability: GrantMayCastFromLinkedExile,
        val exiledIds: List<EntityId>,
    )

    /**
     * Every permanent [playerId] controls whose [GrantMayCastFromLinkedExile] currently functions,
     * in battlefield order — the order that decides which granter a cast or land play spends.
     *
     * Control is read from projected state, and the grant must be one the permanent actually has:
     * a face-down permanent or one that lost all abilities keeps no printed grant, while a grant
     * another effect gave it (`GameState.grantedStaticAbilities`) or an unlocked Room door's counts.
     *
     * @param usableNow when true (every legality path), drop grants whose timing ("during your
     *   turn") or once-per-turn allowance is closed right now. The client view passes false: it
     *   shows a pile's cards as castable-later ghosts even while the window is shut.
     */
    fun linkedExileGranters(state: GameState, playerId: EntityId, usableNow: Boolean = true): List<LinkedExileGranter> {
        val projected = state.projectedState
        val result = mutableListOf<LinkedExileGranter>()
        for (entityId in state.getBattlefield()) {
            val container = state.getEntity(entityId) ?: continue
            val controller = projected.getController(entityId) ?: container.get<ControllerComponent>()?.playerId
            if (controller != playerId) continue
            val linked = container.get<LinkedExileComponent>() ?: continue
            val grant = functioningStaticAbilities(state, entityId, container, playerId)
                .firstNotNullOfOrNull { it as? GrantMayCastFromLinkedExile } ?: continue
            if (usableNow) {
                if (grant.duringYourTurnOnly && !state.isActiveTurnFor(playerId)) continue
                // The allowance belongs to the permanent: a cast and a land play spend the same marker.
                if (grant.oncePerTurn && container.has<MayCastFromLinkedExileUsedThisTurnComponent>()) continue
            }
            result.add(LinkedExileGranter(entityId, grant, linked.exiledIds))
        }
        return result
    }

    /**
     * Whether [granter]'s grant covers [cardId] for [playerId]: the card is in its pile, and passes
     * the grant's ownership, "exiled this turn", mana-value cap and card filter. The mana-value cap
     * is dynamic, so it is only enforced when [usableNow].
     */
    fun linkedExileAdmits(
        state: GameState,
        playerId: EntityId,
        granter: LinkedExileGranter,
        cardId: EntityId,
        usableNow: Boolean = true,
    ): Boolean {
        if (cardId !in granter.exiledIds) return false
        val cardContainer = state.getEntity(cardId) ?: return false
        val card = cardContainer.get<CardComponent>() ?: return false
        val grant = granter.ability
        if (grant.ownedByYou && card.ownerId != playerId) return false
        if (grant.exiledThisTurnOnly && cardContainer.get<ExileEntryTurnComponent>()?.turnNumber != state.turnNumber) {
            return false
        }
        if (usableNow) {
            val cap = grant.maxManaValue?.let { amount ->
                conditionEvaluator.amounts.evaluate(state, amount, EffectContext(sourceId = granter.granterId, controllerId = playerId))
            }
            if (cap != null && card.manaCost.cmc > cap) return false
        }
        return CastZoneResolver.matchesCardFilter(
            card, grant.filter, state, granter.granterId, state.projectedState.colorSetOf(cardId),
            subtypes = CastZoneResolver.effectiveSubtypes(state, cardId, card),
        )
    }

    /** The first granter (battlefield order) whose grant lets [playerId] cast [cardId], or `null`. */
    fun linkedExileGranterFor(
        state: GameState,
        playerId: EntityId,
        cardId: EntityId,
        usableNow: Boolean = true,
    ): LinkedExileGranter? = linkedExileGranters(state, playerId, usableNow)
        .firstOrNull { linkedExileAdmits(state, playerId, it, cardId, usableNow) }

    /**
     * The static abilities functioning on the permanent [entityId]: its printed ones (and unlocked
     * Room doors') unless it is face down or has lost all abilities, plus any granted to it, with a
     * [ConditionalStaticAbility] resolved against its current condition.
     */
    private fun functioningStaticAbilities(
        state: GameState,
        entityId: EntityId,
        container: ComponentContainer,
        controllerId: EntityId,
    ): Sequence<StaticAbility> {
        val printed = if (container.has<FaceDownComponent>() || state.projectedState.hasLostAllAbilities(entityId)) {
            emptyList()
        } else {
            container.get<CardComponent>()
                ?.let { cardRegistry.getCard(it.cardDefinitionId) }
                ?.let { RoomFaceStatics.activeStaticAbilities(container, it) }
                ?: emptyList()
        }
        val granted = state.grantedStaticAbilities.asSequence().filter { it.entityId == entityId }.map { it.ability }
        return (printed.asSequence() + granted).mapNotNull { ability ->
            if (ability !is ConditionalStaticAbility) ability
            else ability.ability.takeIf {
                conditionEvaluator.evaluate(state, ability.condition, EffectContext(sourceId = entityId, controllerId = controllerId))
            }
        }
    }

    companion object {
        /** [restriction] and, through [ActivationRestriction.All], everything nested inside it. */
        private fun leaves(restriction: ActivationRestriction): Sequence<ActivationRestriction> =
            if (restriction is ActivationRestriction.All) restriction.restrictions.asSequence().flatMap(::leaves)
            else sequenceOf(restriction)

        private fun ActivatedAbility.restrictionLeaves(): Sequence<ActivationRestriction> =
            restrictions.asSequence().flatMap(::leaves)

        /**
         * Whether [ability] opens itself to players other than its source's controller. Recursive
         * through `All`, because the permission is routinely *narrowed* by a companion restriction
         * rather than standing alone — Merseine's "only the controller of the enchanted creature
         * may activate this ability" is AnyPlayerMay + a condition.
         */
        fun anyPlayerMay(ability: ActivatedAbility): Boolean =
            ability.restrictionLeaves().any { it is ActivationRestriction.AnyPlayerMay }

        /** Whether activating [ability] must be tallied on the source's per-turn tracker. */
        fun tracksActivationsPerTurn(ability: ActivatedAbility): Boolean =
            ability.restrictionLeaves().any {
                it is ActivationRestriction.OncePerTurn || it is ActivationRestriction.MaxPerTurn
            }

        /** Whether activating [ability] must be recorded in the source's once-ever memory. */
        fun tracksActivationsEver(ability: ActivatedAbility): Boolean =
            ability.restrictionLeaves().any { it is ActivationRestriction.Once }

        /**
         * Whether [ability] carries any per-turn or once-ever activation limit, so activating it
         * repeatedly in one batch could overrun that limit.
         */
        fun hasActivationCountLimit(ability: ActivatedAbility): Boolean =
            tracksActivationsPerTurn(ability) || tracksActivationsEver(ability)
    }
}
