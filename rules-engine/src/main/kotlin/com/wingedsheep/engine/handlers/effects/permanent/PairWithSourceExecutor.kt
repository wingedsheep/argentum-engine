package com.wingedsheep.engine.handlers.effects.permanent

import com.wingedsheep.engine.core.CreaturesPairedEvent
import com.wingedsheep.engine.core.EffectResult
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.effects.EffectExecutor
import com.wingedsheep.engine.mechanics.SoulbondPairing
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.PairedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.PairWithSourceEffect
import kotlin.reflect.KClass

/**
 * Executor for [PairWithSourceEffect] — soulbond's pairing step (CR 702.95a).
 *
 * Pairs the first still-eligible creature in the effect's collection with the ability's source,
 * stamping a symmetric [PairedComponent] on both halves.
 *
 * The CR checks all live here, at resolution:
 *  - **702.95c** — if either half is no longer a creature, no longer on the battlefield, or no
 *    longer controlled by the ability's controller, *neither* becomes paired. This is why the
 *    source is validated before the partner and a failure returns the state untouched rather than
 *    stamping one side.
 *  - **702.95d** — a creature can be paired with only one other creature, so an already-paired
 *    half is skipped instead of having its existing partner stolen. In practice the candidate
 *    filter already excludes paired creatures; this is the resolution-time backstop for the gap
 *    between the ability triggering and resolving.
 *
 * An empty collection is a legal no-op: it is where a declined "you may pair" and a board with no
 * eligible partner both land.
 */
class PairWithSourceExecutor : EffectExecutor<PairWithSourceEffect> {

    override val effectType: KClass<PairWithSourceEffect> = PairWithSourceEffect::class

    override fun execute(
        state: GameState,
        effect: PairWithSourceEffect,
        context: EffectContext
    ): EffectResult {
        val sourceId = context.sourceId ?: return EffectResult.success(state)
        val controllerId = context.controllerId

        // CR 702.95c, source half.
        if (!isPairableCreature(state, sourceId, controllerId)) return EffectResult.success(state)
        // CR 702.95d — the source picking up a second partner is not a thing. Asked through
        // SoulbondPairing so a component left over from a pair that has already broken (its other
        // half gone this same resolution) doesn't block a legal new pairing.
        if (SoulbondPairing.isPaired(state, sourceId)) return EffectResult.success(state)

        val partnerId = (context.pipeline.storedCollections[effect.from] ?: emptyList())
            .firstOrNull { candidate ->
                candidate != sourceId &&
                    isPairableCreature(state, candidate, controllerId) &&
                    !SoulbondPairing.isPaired(state, candidate)
            }
            ?: return EffectResult.success(state)

        val newState = state
            .updateEntity(sourceId) { it.with(PairedComponent(partnerId = partnerId)) }
            .updateEntity(partnerId) { it.with(PairedComponent(partnerId = sourceId)) }

        return EffectResult.success(
            newState,
            listOf(
                CreaturesPairedEvent(
                    firstId = sourceId,
                    firstName = nameOf(newState, sourceId),
                    secondId = partnerId,
                    secondName = nameOf(newState, partnerId),
                    controllerId = controllerId
                )
            )
        )
    }

    /**
     * CR 702.95c's three requirements for one half of a would-be pair: on the battlefield, still a
     * creature (read off projected types, so an animated or de-animated permanent is judged by what
     * it currently is), and controlled by the player controlling the soulbond ability.
     */
    private fun isPairableCreature(state: GameState, entityId: EntityId, controllerId: EntityId): Boolean {
        if (entityId !in state.getBattlefield()) return false
        if (state.projectedState.getController(entityId) != controllerId) return false
        return state.projectedState.isCreature(entityId)
    }

    private fun nameOf(state: GameState, entityId: EntityId): String =
        state.getEntity(entityId)?.get<CardComponent>()?.name ?: "Creature"
}
