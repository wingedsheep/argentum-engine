package com.wingedsheep.engine.mechanics

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.AbilityActivatedEverComponent
import com.wingedsheep.engine.state.components.battlefield.ClassLevelComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.ExtraOnceOnlyActivations
import com.wingedsheep.sdk.scripting.OnceOnlyAbilityKind

/**
 * How many times a player may activate a keyword-prefixed "Activate this ability only once" ability
 * — exhaust (CR 702.177) and power-up (CR 702.193) — or a boast ability (CR 702.142, once *each
 * turn*) given the [ExtraOnceOnlyActivations] permissions currently on their battlefield.
 *
 * Lives here rather than in `CastPermissionUtils` because three activation-legality paths need the
 * same answer and must not drift: the legal-action enumerators, `ActivateAbilityHandler`, and
 * `ManaSolver`'s restriction check for auto-tapping, which deliberately doesn't depend on the
 * legalactions module. All three reach it through the legality kernel.
 */
object OnceOnlyActivationAllowance {

    /**
     * How many activations beyond the printed one [playerId] currently has for [kind] abilities.
     * `null` means "no limit at all" — at least one permission waives the memory outright rather
     * than raising it.
     *
     * Both printed statics (from the card definition, honoring the permanent's current Class level)
     * and statics granted at runtime are scanned. Each permission's condition is evaluated in the
     * granting permanent's controller's context and re-checked every call, so Elvish Refueler's
     * "During your turn, as long as you haven't activated an exhaust ability this turn" gate stops
     * applying the instant the turn's first exhaust ability is activated. Counted permissions sum,
     * so two Wonder Men grant two extra power-up activations.
     *
     * Two known gaps, both shared with the other player-scoped scans in `CastPermissionUtils` and
     * both unreachable by any printed card today, so they are deliberately left for an engine-wide
     * change rather than fixed here: this reads the **base** `getBattlefield(playerId)` rather than
     * `controlledBattlefield(playerId)`, so a permission source stolen by an opponent (a Layer-2
     * `ChangeController` that never moves the zone entry) keeps applying to its original
     * controller; and entries from `state.grantedStaticAbilities` get no `Duration` gate.
     */
    fun extraActivationsFor(
        state: GameState,
        playerId: EntityId,
        kind: OnceOnlyAbilityKind,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator
    ): Int? {
        var total = 0
        for (entityId in state.getBattlefield(playerId)) {
            val container = state.getEntity(entityId)
            // CR 708.2/708.2a — a face-down permanent is a 2/2 with no text, so it grants nothing
            // its card printed. Abilities *granted* to it at runtime are a separate continuous
            // effect and do still apply, so only the printed half is suppressed here.
            val printed = if (container?.has<FaceDownComponent>() == true) {
                emptyList()
            } else {
                val card = container?.get<CardComponent>()
                val classLevel = container?.get<ClassLevelComponent>()?.currentLevel
                card?.let { cardRegistry.getCard(it.cardDefinitionId) }
                    ?.script?.effectiveStaticAbilities(classLevel).orEmpty()
            }
            val granted = state.grantedStaticAbilities
                .filter { it.entityId == entityId }
                .map { it.ability }
            for (ability in printed + granted) {
                val permission = ability as? ExtraOnceOnlyActivations ?: continue
                if (permission.kind != kind) continue
                val condition = permission.condition
                if (condition != null) {
                    val context = EffectContext(sourceId = entityId, controllerId = playerId)
                    if (!conditionEvaluator.evaluate(state, condition, context)) continue
                }
                val extra = permission.extraActivations ?: return null
                total += extra
            }
        }
        return total
    }

    /**
     * Whether [playerId] may activate [ability] of [sourceId] given its
     * [com.wingedsheep.sdk.scripting.ActivationRestriction.Once]. True while the object's recorded
     * activation count is below `1 + `[extraActivationsFor], and always true when some permission
     * waives the limit outright.
     *
     * An ability that is neither exhaust nor power-up gets the plain once-only answer: no
     * permission in the SDK reaches a `Once` an ordinary ability printed for itself.
     */
    fun mayActivate(
        state: GameState,
        playerId: EntityId,
        sourceId: EntityId,
        ability: ActivatedAbility,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator
    ): Boolean {
        val timesActivated = state.getEntity(sourceId)
            ?.get<AbilityActivatedEverComponent>()
            ?.activationCount(ability.id)
            ?: 0
        if (timesActivated == 0) return true
        val kind = ability.onceOnlyKind?.takeIf { it != OnceOnlyAbilityKind.BOAST } ?: return false
        val extra = extraActivationsFor(state, playerId, kind, cardRegistry, conditionEvaluator)
            ?: return true
        return timesActivated < 1 + extra
    }

    /**
     * Whether [playerId] may activate the boast [ability] again this turn, having already activated
     * it [timesThisTurn] times — the per-turn twin of [mayActivate], for the
     * [com.wingedsheep.sdk.scripting.ActivationRestriction.OncePerTurn] boast installs. Any other
     * ability gets the plain once-each-turn answer.
     */
    fun mayActivateThisTurn(
        state: GameState,
        playerId: EntityId,
        timesThisTurn: Int,
        ability: ActivatedAbility,
        cardRegistry: CardRegistry,
        conditionEvaluator: ConditionEvaluator
    ): Boolean {
        if (timesThisTurn == 0) return true
        if (!ability.isBoast) return false
        val extra = extraActivationsFor(state, playerId, OnceOnlyAbilityKind.BOAST, cardRegistry, conditionEvaluator)
            ?: return true
        return timesThisTurn < 1 + extra
    }
}
