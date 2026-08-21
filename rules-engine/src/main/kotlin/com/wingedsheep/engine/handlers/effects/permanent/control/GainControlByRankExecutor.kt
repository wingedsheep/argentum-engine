package com.wingedsheep.engine.handlers.effects.permanent.control

import com.wingedsheep.engine.core.ControlChangedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.layers.Layer
import com.wingedsheep.engine.mechanics.layers.SerializableModification
import com.wingedsheep.engine.mechanics.layers.addFloatingEffect
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.GainControlByRankEffect
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.effects.PlayerRankDirection
import com.wingedsheep.sdk.scripting.effects.PlayerRankMetric
import com.wingedsheep.sdk.scripting.effects.RankTieBreak
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlin.reflect.KClass

/**
 * Executor for [GainControlByRankEffect].
 *
 * Ranks the players still in the game by the effect's [PlayerRankMetric] and hands the target to
 * whoever sits at the [GainControlByRankEffect.direction] end of that rank. A tie is resolved by
 * the effect's [RankTieBreak]: [RankTieBreak.NONE] leaves control alone (Ghazbán Ogre's and
 * Thoughtbound Primoc's "more than each other player" intervening condition, which a tie fails),
 * while [RankTieBreak.CONTROLLER_CHOOSES] asks the ability's controller to pick one of the tied
 * players (Loxodon Peacekeeper).
 *
 * The tie-break choice is not built here. It is lowered into a [ChooseActionEffect] with one
 * labelled option per tied player and handed back to the effect executor, so the existing
 * choose-an-option decision, its continuation and its resumer are reused wholesale rather than
 * duplicated — and choosing stays separated from acting. The permanent is pinned into each option
 * by entity id, so the choice can't drift onto a different permanent between the prompt and the
 * answer.
 *
 * Ranking uses [GameState.activePlayers], not `turnOrder`: turn order retains players who have
 * lost, and a player on 0 life who is out of the game would otherwise win every LEAST rank
 * outright.
 */
class GainControlByRankExecutor(
    private val effectExecutor: (GameState, Effect, EffectContext) -> EffectResult
) : EffectExecutor<GainControlByRankEffect> {

    override val effectType: KClass<GainControlByRankEffect> = GainControlByRankEffect::class

    override fun execute(
        state: GameState,
        effect: GainControlByRankEffect,
        context: EffectContext
    ): EffectResult {
        val targetId = context.resolveTarget(effect.target)
            ?: return EffectResult.error(state, "No valid target for control change")

        val targetContainer = state.getEntity(targetId)
            ?: return EffectResult.error(state, "Target permanent no longer exists")

        if (targetContainer.get<CardComponent>() == null) {
            return EffectResult.error(state, "Target is not a card")
        }

        // "Other players can't gain control of it" (Guardian Beast) — checked before the rank is
        // even offered, so a tie-break prompt is never raised for a permanent that cannot move.
        if (state.projectedState.hasKeyword(targetId, AbilityFlag.CANT_GAIN_CONTROL)) {
            return EffectResult.success(state)
        }

        val valueByPlayer = state.activePlayers.associateWith { playerId ->
            metricValue(state, effect.metric, playerId)
        }
        if (valueByPlayer.isEmpty()) return EffectResult.success(state)

        val extreme = when (effect.direction) {
            PlayerRankDirection.MOST -> valueByPlayer.values.max()
            PlayerRankDirection.LEAST -> valueByPlayer.values.min()
        }

        // Nobody controls a creature of the subtype, so nobody "controls the most" of them. Only a
        // MOST rank is empty this way — being tied on zero *is* the fewest.
        if (effect.direction == PlayerRankDirection.MOST &&
            effect.metric is PlayerRankMetric.CreaturesOfSubtype &&
            extreme == 0
        ) {
            return EffectResult.success(state)
        }

        val tied = valueByPlayer.filterValues { it == extreme }.keys.toList()

        return when {
            tied.size == 1 -> changeController(state, targetId, tied.single(), context)
            effect.tieBreak == RankTieBreak.NONE -> EffectResult.success(state)
            else -> effectExecutor(state, tieBreakChoice(state, targetId, tied), context)
        }
    }

    /**
     * "You choose one of them, and that player gains control" as a [ChooseActionEffect] over the
     * tied players. Both ends are pinned to concrete entity ids because the set was computed from
     * the state at resolution; nothing about it is re-derivable after the player answers.
     */
    private fun tieBreakChoice(
        state: GameState,
        targetId: EntityId,
        tied: List<EntityId>
    ): Effect = ChooseActionEffect(
        choices = tied.map { playerId ->
            EffectChoice(
                // The enclosing prompt only names the source ("Choose one for Loxodon
                // Peacekeeper"), so each option has to say what picking it does — a bare list of
                // player names would leave the player guessing which way the choice runs.
                label = "${state.getEntity(playerId)?.get<PlayerComponent>()?.name ?: "Player"} " +
                    "gains control",
                effect = GiveControlToTargetPlayerEffect(
                    permanent = EffectTarget.SpecificEntity(targetId),
                    newController = EffectTarget.SpecificEntity(playerId)
                )
            )
        },
        player = EffectTarget.Controller
    )

    /**
     * Install the control change as a permanent Layer 2 floating effect, replacing any earlier one
     * this same source put on this same permanent (so a repeating upkeep trigger moves the
     * permanent rather than stacking grants).
     */
    private fun changeController(
        state: GameState,
        targetId: EntityId,
        newControllerId: EntityId,
        context: EffectContext
    ): EffectResult {
        val cardName = state.getEntity(targetId)?.get<CardComponent>()?.name
            ?: return EffectResult.error(state, "Target is not a card")

        // Projected controller, so an earlier control-changing effect is respected.
        val currentControllerId = state.projectedState.getController(targetId)
            ?: state.getEntity(targetId)?.get<ControllerComponent>()?.playerId
        if (currentControllerId == newControllerId) return EffectResult.success(state)

        val filteredEffects = state.floatingEffects.filter { floating ->
            !(
                floating.sourceId == context.sourceId &&
                    floating.effect.layer == Layer.CONTROL &&
                    targetId in floating.effect.affectedEntities
                )
        }

        val controlContext = context.copy(controllerId = newControllerId)
        // Rule 302.6: the new controller hasn't controlled this permanent since their most recent
        // turn began.
        val newState = state.copy(floatingEffects = filteredEffects)
            .addFloatingEffect(
                layer = Layer.CONTROL,
                modification = SerializableModification.ChangeController(newControllerId),
                affectedEntities = setOf(targetId),
                duration = Duration.Permanent,
                context = controlContext
            )
            .updateEntity(targetId) { it.with(SummoningSicknessComponent) }
            .let { clearRingBearerOnControlChange(it, targetId, newControllerId) }

        val events = listOf(
            ControlChangedEvent(
                permanentId = targetId,
                permanentName = cardName,
                oldControllerId = currentControllerId ?: context.controllerId,
                newControllerId = newControllerId
            )
        )

        return EffectResult.success(newState, events)
    }

    private fun metricValue(state: GameState, metric: PlayerRankMetric, playerId: EntityId): Int =
        when (metric) {
            is PlayerRankMetric.LifeTotal ->
                state.lifeTotal(playerId) // CR 810.9a — team's shared total
            is PlayerRankMetric.CreaturesOfSubtype ->
                // Projected state so type-changing effects (a permanent animated/typeshifted
                // into the subtype) and stolen control are counted correctly.
                state.projectedState.getBattlefieldControlledBy(playerId).count { entityId ->
                    state.projectedState.hasSubtype(entityId, metric.subtype.value)
                }
        }
}
