package com.wingedsheep.engine.event

import com.wingedsheep.engine.handlers.ConditionEvaluator
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.durations.GrantDurationGate
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceStatics
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers

/**
 * Every [AdditionalSourceTriggers] doubler functioning on the battlefield (CR 603.2d "triggers an
 * additional time"), read from both places a permanent can have one:
 *
 * - **printed** — the card's script (and any unlocked Room face), the Twinflame Travelers shape;
 * - **granted** — [GameState.grantedStaticAbilities], a static a resolving effect handed to a
 *   permanent for a [com.wingedsheep.sdk.scripting.Duration] (Lwaxana Troi, Charming Guest: "for as
 *   long as Lwaxana Troi remains tapped, if a triggered ability of that creature triggers, that
 *   ability triggers an additional time"), or a token's own statics, which live there because
 *   tokens have no card definition. A granted doubler is the holder's own ability, so its filter is
 *   read relative to the holder and it lapses with its duration ([GrantDurationGate]).
 *
 * Shared by the trigger detector and the triggered-mana-ability path so the two agree on which
 * doublers exist.
 */
internal class SourceTriggerDoublers(
    private val cardRegistry: CardRegistry,
    private val predicateEvaluator: PredicateEvaluator,
    private val conditionEvaluator: ConditionEvaluator,
) {
    /** One functioning doubler: [holderId] has [ability], controlled by [controllerId]. */
    data class Doubler(
        val holderId: EntityId,
        val controllerId: EntityId,
        val ability: AdditionalSourceTriggers,
    )

    fun collect(state: GameState): List<Doubler> {
        val projected = state.projectedState
        val doublers = mutableListOf<Doubler>()
        for (permanentId in state.getBattlefield()) {
            val container = state.getEntity(permanentId) ?: continue
            val card = container.get<CardComponent>() ?: continue
            if (container.has<FaceDownComponent>() || projected.hasLostAllAbilities(permanentId)) continue
            val controllerId = projected.getController(permanentId) ?: continue
            val cardDef = cardRegistry.getCard(card.cardDefinitionId) ?: continue
            for (ability in RoomFaceStatics.activeStaticAbilities(container, cardDef)) {
                if (ability is AdditionalSourceTriggers) addIfActive(state, permanentId, controllerId, ability, doublers)
            }
        }
        if (state.grantedStaticAbilities.isNotEmpty()) {
            val battlefield = state.getBattlefield().toSet()
            for (grant in state.grantedStaticAbilities) {
                val ability = grant.ability as? AdditionalSourceTriggers ?: continue
                val holderId = grant.entityId
                if (holderId !in battlefield || projected.hasLostAllAbilities(holderId)) continue
                // A record mirroring a token's own statics (no layer timestamp) is a printed-style
                // ability and is gone face down; an ability gained from an effect is not.
                if (grant.layerTimestamp == null && state.getEntity(holderId)?.has<FaceDownComponent>() == true) continue
                if (!GrantDurationGate.holds(state, holderId, grant.sourceId, grant.duration)) continue
                val controllerId = projected.getController(holderId) ?: continue
                addIfActive(state, holderId, controllerId, ability, doublers)
            }
        }
        return doublers
    }

    /** True when [doubler] doubles a triggered ability of [triggerSourceId] controlled by [triggerControllerId]. */
    fun doubles(state: GameState, doubler: Doubler, triggerSourceId: EntityId, triggerControllerId: EntityId): Boolean {
        if (triggerControllerId != doubler.controllerId) return false
        val ability = doubler.ability
        // `alsoSource` doubles the doubler's own triggered abilities regardless of the filter — the
        // "or ~ itself" leg of "a triggered ability of ~ or an Equipment attached to it" (Cloud).
        if (ability.alsoSource && triggerSourceId == doubler.holderId) return true
        if (ability.excludeSelf && triggerSourceId == doubler.holderId) return false
        return predicateEvaluator.matches(
            state, state.projectedState, triggerSourceId, ability.sourceFilter,
            PredicateContext(controllerId = doubler.controllerId, sourceId = doubler.holderId)
        )
    }

    private fun addIfActive(
        state: GameState,
        holderId: EntityId,
        controllerId: EntityId,
        ability: AdditionalSourceTriggers,
        into: MutableList<Doubler>,
    ) {
        // Optional gate ("As long as Cloud is equipped, …") — evaluated against the holder.
        val gate = ability.condition
        if (gate != null && !conditionEvaluator.evaluate(
                state, gate, EffectContext(sourceId = holderId, controllerId = controllerId)
            )
        ) return
        into.add(Doubler(holderId, controllerId, ability))
    }
}
