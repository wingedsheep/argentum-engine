package com.wingedsheep.engine.handlers.effects.player

import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.player.CombatDeclarationControlComponent
import com.wingedsheep.engine.state.components.player.CombatDeclarationGrant
import com.wingedsheep.sdk.scripting.effects.ControlCombatDeclarationsThisTurnEffect
import com.wingedsheep.sdk.scripting.references.Player
import kotlin.reflect.KClass

/**
 * Executor for [ControlCombatDeclarationsThisTurnEffect] (Master Warcraft, Brutal Hordechief): adds
 * a turn-stamped [CombatDeclarationGrant] to the controller naming the declarations it takes over
 * and the players who owe them, resolved now. Grants from earlier turns are dropped on the way;
 * the rest lapse by themselves when the turn ends. Among grants covering the same declaration the
 * latest wins, by timestamp. Routing the declarations is
 * [com.wingedsheep.engine.mechanics.combat.CombatDeclarationControl].
 */
class ControlCombatDeclarationsExecutor : EffectExecutor<ControlCombatDeclarationsThisTurnEffect> {

    override val effectType: KClass<ControlCombatDeclarationsThisTurnEffect> =
        ControlCombatDeclarationsThisTurnEffect::class

    override fun execute(
        state: GameState,
        effect: ControlCombatDeclarationsThisTurnEffect,
        context: EffectContext
    ): EffectResult {
        val ticked = state.tick()
        val controller = context.controllerId
        val declaringPlayers = when (effect.declaringPlayers) {
            Player.EachOpponent -> ticked.getOpponents(controller)
            Player.EachOtherPlayer -> ticked.getOtherPlayers(controller)
            Player.You -> listOf(controller)
            Player.Each -> ticked.activePlayers
            else -> error("ControlCombatDeclarationsThisTurnEffect can't resolve ${effect.declaringPlayers}")
        }.toSet()
        val grant = CombatDeclarationGrant(
            turnNumber = ticked.turnNumber,
            timestamp = ticked.timestamp,
            declarations = effect.declarations,
            declaringPlayers = declaringPlayers
        )
        val newState = ticked.updateEntity(controller) { container ->
            val current = container.get<CombatDeclarationControlComponent>()?.grants
                ?.filter { it.turnNumber == ticked.turnNumber }
                .orEmpty()
            container.with(CombatDeclarationControlComponent(current + grant))
        }
        return EffectResult.success(newState)
    }
}
