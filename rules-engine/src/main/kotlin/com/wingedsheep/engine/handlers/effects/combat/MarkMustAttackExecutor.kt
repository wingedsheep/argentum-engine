package com.wingedsheep.engine.handlers.effects.combat

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.combat.MustAttackNextCombatComponent
import com.wingedsheep.engine.state.components.combat.MustAttackNextTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackDefendersThisTurnComponent
import com.wingedsheep.engine.state.components.combat.MustAttackThisTurnComponent
import com.wingedsheep.engine.state.components.combat.NamedAttackDefenders
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MustAttackWindow
import kotlin.reflect.KClass

/**
 * Executor for [MarkMustAttackEffect]: puts the requirement for its window on the target creature
 * — [MustAttackThisTurnComponent] for this turn, [MustAttackDefendersThisTurnComponent] when the
 * effect names the player or planeswalker to attack (encore), [MustAttackNextCombatComponent] for
 * its controller's next combat phase, or [MustAttackNextTurnComponent] for its controller's next
 * turn (Gideon, Battle-Forged).
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
                val defender = effect.defender
                if (defender == null) {
                    state.updateEntity(targetId) { it.with(MustAttackThisTurnComponent) }
                } else {
                    val named = resolveDefender(state, defender, context)
                        ?: return EffectResult.success(state)
                    state.updateEntity(targetId) { container ->
                        val existing = container.get<MustAttackDefendersThisTurnComponent>()?.defenders ?: NamedAttackDefenders()
                        container.with(MustAttackDefendersThisTurnComponent(existing + named))
                    }
                }
            }
            MustAttackWindow.ControllersNextTurn -> {
                val named = effect.defender?.let {
                    resolveDefender(state, it, context) ?: return EffectResult.success(state)
                }
                state.updateEntity(targetId) { container ->
                    val existing = container.get<MustAttackNextTurnComponent>() ?: MustAttackNextTurnComponent()
                    container.with(
                        if (named == null) existing.copy(anyDefender = true)
                        else existing.copy(defenders = existing.defenders + named)
                    )
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

    /**
     * The defender a directed mark names: a player still in the game ("attacks that opponent",
     * encore) or a permanent on the battlefield ("attacks Gideon", the ability's own source), kept
     * as the object it is now. One that is already gone binds nothing.
     */
    private fun resolveDefender(state: GameState, defender: EffectTarget, context: EffectContext): NamedAttackDefenders? {
        val id = context.resolveTarget(defender, state)
            ?.takeIf { it in state.getBattlefield() || state.getEntity(it)?.has<PlayerComponent>() == true }
            ?: context.resolvePlayerTarget(defender, state)
            ?: return null
        if (state.getEntity(id)?.has<PlayerComponent>() == true) return NamedAttackDefenders(playerIds = setOf(id))
        if (id !in state.getBattlefield()) return null
        val ref = state.objectRef(id) ?: return null
        return NamedAttackDefenders(permanents = setOf(ref))
    }
}
