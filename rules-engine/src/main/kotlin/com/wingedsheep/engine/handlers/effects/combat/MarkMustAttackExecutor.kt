package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MustAttackNextCombatComponent
import com.wingedsheep.engine.state.components.combat.MustAttackPlayerThisTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackWindow
import kotlin.reflect.KClass

/**
 * Executor for [MarkMustAttackEffect]: puts the requirement for its window on the target creature
 * — [MustAttackThisTurnComponent] for this turn, [MustAttackNextCombatComponent] for its
 * controller's next combat phase, or [MustAttackPlayerThisTurnComponent] when the effect names the
 * player to attack (encore).
 */
class MarkMustAttackExecutor : EffectExecutor<MarkMustAttackEffect> {

    override val effectType: KClass<MarkMustAttackEffect> = MarkMustAttackEffect::class

    override fun execute(
        state: GameState,
        effect: MarkMustAttackEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.success(state)

        if (targetId !in state.getBattlefield()) {
            return EffectResult.success(state)
        }

        val newState = when (effect.window) {
            MustAttackWindow.ThisTurn -> {
                val defenderTarget = effect.defender
                if (defenderTarget == null) {
                    state.updateEntity(targetId) { it.with(MustAttackThisTurnComponent) }
                } else {
                    // "Attacks that opponent this turn if able" names a player. A player who has
                    // since left the game binds nothing: the attack check only counts opponents still in it.
                    val defenderId = context.resolvePlayerTarget(defenderTarget, state)
                        ?.takeIf { state.getEntity(it)?.has<PlayerComponent>() == true }
                        ?: return EffectResult.success(state)
                    state.updateEntity(targetId) { container ->
                        val existing = container.get<MustAttackPlayerThisTurnComponent>()?.defenderIds.orEmpty()
                        container.with(MustAttackPlayerThisTurnComponent(existing + defenderId))
                    }
                }
            }
            MustAttackWindow.ControllersNextCombat -> {
                // Past the beginning of combat on the controller's own turn, this combat's attackers
                // are already declared, so it is not the "next" combat phase.
                val inControllersDeclaredCombat = state.step.phase == Phase.COMBAT &&
                    state.step != Step.BEGIN_COMBAT &&
                    state.projectedState.getController(targetId)?.let { state.isActiveTurnFor(it) } == true
                state.updateEntity(targetId) {
                    it.with(MustAttackNextCombatComponent(waitsOutCurrentCombat = inControllersDeclaredCombat))
                }
            }
        }
        return EffectResult.success(newState)
    }
}
