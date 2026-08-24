package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.combat.rules.BlockCheckContext
import com.wingedsheep.engine.mechanics.combat.rules.defaultBlockEvasionRules
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SwapBlockingAssignmentsEffect
import kotlin.reflect.KClass

/**
 * Executor for [SwapBlockingAssignmentsEffect] — Sorrow's Path.
 *
 * Reads the two chosen targets, verifies at resolution (CR 608.2b) that both are still blocking
 * creatures under the same controller and that that controller is an *opponent* of the activating
 * player, and then applies the printed gate: the swap happens only if
 * **each** creature could legally block **every** attacker the other is currently blocking. The
 * check runs the same `defaultBlockEvasionRules` a declared block goes through, so an evasion
 * ability that would have stopped the block at declaration stops it here too.
 *
 * If either direction is illegal the effect does nothing at all — not a partial swap. That is the
 * printed behaviour, and the reason this card is famously hard to use.
 */
class SwapBlockingAssignmentsExecutor(
    private val cardRegistry: CardRegistry
) : EffectExecutor<SwapBlockingAssignmentsEffect> {

    override val effectType: KClass<SwapBlockingAssignmentsEffect> =
        SwapBlockingAssignmentsEffect::class

    private val evasionRules = defaultBlockEvasionRules()

    override fun execute(
        state: GameState,
        effect: SwapBlockingAssignmentsEffect,
        context: EffectContext
    ): EffectResult {
        val first = context.resolveTarget(com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(0), state)
            ?: return EffectResult.success(state)
        val second = context.resolveTarget(com.wingedsheep.sdk.scripting.targets.EffectTarget.ContextTarget(1), state)
            ?: return EffectResult.success(state)
        if (first == second) return EffectResult.success(state)

        val firstBlocking = state.getEntity(first)?.get<BlockingComponent>()?.blockedAttackerIds
            ?: return EffectResult.success(state)
        val secondBlocking = state.getEntity(second)?.get<BlockingComponent>()?.blockedAttackerIds
            ?: return EffectResult.success(state)

        // "…controlled by the same opponent", re-checked here because targets are re-validated on
        // resolution (CR 608.2b). Both halves matter: the two blockers must share a controller, and
        // that controller must not be the activating player. The target filter already carries the
        // opponent half — only this side can relate the two targets to each other.
        //
        // Control is read from projected state rather than the base ControllerComponent, matching
        // the rest of the engine's battlefield reads: control change is a continuous effect that
        // base state can't see, and `firstController` is handed straight to `canBlockAll` as the
        // blocking player. The two answers can't diverge today — CR 506.4 removes a creature from
        // combat when its controller changes, so the `BlockingComponent` reads above have already
        // refused any such target — but reading the base component here would be a divergence
        // waiting for the first control-change effect that doesn't remove from combat.
        val firstController = state.projectedState.getController(first)
            ?: state.getEntity(first)?.get<ControllerComponent>()?.playerId
        val secondController = state.projectedState.getController(second)
            ?: state.getEntity(second)?.get<ControllerComponent>()?.playerId
        if (firstController == null || firstController != secondController) {
            return EffectResult.success(state)
        }
        if (firstController == context.controllerId) return EffectResult.success(state)

        // The gate: each must be able to block everything the other is blocking.
        if (!canBlockAll(state, first, secondBlocking, firstController)) return EffectResult.success(state)
        if (!canBlockAll(state, second, firstBlocking, secondController)) return EffectResult.success(state)

        val swapped = state
            .updateEntity(first) { it.with(BlockingComponent(secondBlocking)) }
            .updateEntity(second) { it.with(BlockingComponent(firstBlocking)) }

        return EffectResult.success(swapped)
    }

    /** True if [blockerId] could legally block every attacker in [attackerIds]. */
    private fun canBlockAll(
        state: GameState,
        blockerId: EntityId,
        attackerIds: Collection<EntityId>,
        blockingPlayer: EntityId,
    ): Boolean {
        val projected = state.projectedState
        return attackerIds.all { attackerId ->
            val ctx = BlockCheckContext(
                state = state,
                projected = projected,
                attackerId = attackerId,
                blockerId = blockerId,
                blockingPlayer = blockingPlayer,
                cardRegistry = cardRegistry,
            )
            evasionRules.all { it.check(ctx) == null }
        }
    }
}
