package com.wingedsheep.gym.trainer.defaults

import com.wingedsheep.gym.trainer.spi.StateFeaturizer
import com.wingedsheep.gym.trainer.spi.TrainerContext
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable

/**
 * A deliberately simple featurizer that walks the game state and emits a
 * stable `Map<String, Float>` of structural features:
 *
 *  - `me.life`, `them.life`
 *  - `me.hand_size`, `them.hand_size`
 *  - `me.library_size`, `them.library_size`
 *  - `me.graveyard_size`, `them.graveyard_size`
 *  - `me.mana.white`, … `me.mana.colorless` (six fields)
 *  - `me.creatures.count`, `me.creatures.power_total`, `me.creatures.toughness_total`
 *  - the same for `them.*`
 *  - `me.tapped_count`, `them.tapped_count`
 *  - `turn`, `is_my_turn`
 *
 * Serves two purposes: it gets a training loop running end-to-end with
 * *some* signal (useful for regression-testing the plumbing), and it
 * documents the pattern a project-specific featurizer should follow.
 *
 * Real training runs should replace this with a representation tuned to
 * the target network architecture.
 */
class StructuralStateFeaturizer : StateFeaturizer<StructuralFeatures> {

    override fun featurize(ctx: TrainerContext): StructuralFeatures {
        val state = ctx.state
        val me = ctx.playerId
        val them = state.turnOrder.firstOrNull { it != me }

        val out = HashMap<String, Float>(32)
        val projected = state.projectedState

        // Player-level scalars
        out += playerScalars(state, me, projected, prefix = "me.")
        if (them != null) out += playerScalars(state, them, projected, prefix = "them.")

        // Turn / priority
        out["turn"] = state.turnNumber.toFloat()
        out["is_my_turn"] = if (state.activePlayerId == me) 1f else 0f
        out["priority_is_mine"] = if (state.priorityPlayerId == me) 1f else 0f
        out["mid_decision"] = if (ctx.pendingDecision != null) 1f else 0f

        return StructuralFeatures(out)
    }

    private fun playerScalars(
        state: com.wingedsheep.engine.state.GameState,
        pid: EntityId,
        projected: com.wingedsheep.engine.mechanics.layers.ProjectedState,
        prefix: String
    ): Map<String, Float> {
        val container = state.getEntity(pid)
        val life = container?.get<LifeTotalComponent>()?.life ?: 0
        val mana = container?.get<ManaPoolComponent>()

        val battlefield = state.getBattlefield()
            .filter { projected.getController(it) == pid }
        var creatureCount = 0
        var powerTotal = 0
        var toughnessTotal = 0
        var tappedCount = 0
        for (entity in battlefield) {
            val c = state.getEntity(entity) ?: continue
            if (c.get<TappedComponent>() != null) tappedCount += 1
            val card = c.get<CardComponent>() ?: continue
            if (projected.isCreature(entity)) {
                creatureCount += 1
                powerTotal += projected.getPower(entity) ?: 0
                toughnessTotal += projected.getToughness(entity) ?: 0
            }
        }

        return mapOf(
            "${prefix}life" to life.toFloat(),
            "${prefix}hand_size" to state.getHand(pid).size.toFloat(),
            "${prefix}library_size" to state.getLibrary(pid).size.toFloat(),
            "${prefix}graveyard_size" to state.getGraveyard(pid).size.toFloat(),
            "${prefix}exile_size" to state.getExile(pid).size.toFloat(),
            "${prefix}mana.white" to (mana?.white ?: 0).toFloat(),
            "${prefix}mana.blue" to (mana?.blue ?: 0).toFloat(),
            "${prefix}mana.black" to (mana?.black ?: 0).toFloat(),
            "${prefix}mana.red" to (mana?.red ?: 0).toFloat(),
            "${prefix}mana.green" to (mana?.green ?: 0).toFloat(),
            "${prefix}mana.colorless" to (mana?.colorless ?: 0).toFloat(),
            "${prefix}creatures.count" to creatureCount.toFloat(),
            "${prefix}creatures.power_total" to powerTotal.toFloat(),
            "${prefix}creatures.toughness_total" to toughnessTotal.toFloat(),
            "${prefix}tapped_count" to tappedCount.toFloat()
        )
    }
}

/**
 * Wraps a `Map<String, Float>` so it can be serialized as JSON to a remote
 * evaluator without the user having to pick a collection serializer.
 */
@Serializable
data class StructuralFeatures(val values: Map<String, Float>)
