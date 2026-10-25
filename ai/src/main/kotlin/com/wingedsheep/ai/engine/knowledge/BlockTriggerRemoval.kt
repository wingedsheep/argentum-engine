package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.effects.composite.asConditional
import com.wingedsheep.engine.handlers.effects.composite.asMayDecide
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import java.util.Collections
import java.util.IdentityHashMap

/**
 * "When this creature blocks a creature, destroy that creature" — a blocker whose block trigger
 * kills the attacker it blocks, whatever the damage says. Read off the trigger's shape, never off
 * the card's name.
 *
 * Alaborn Zealot is the card (2026-10-25 AI playtest, four-player game 1, turn 29): a hexproof 6/5
 * Leviathan token attacked into an untapped 1/1 Zealot ("When this creature blocks a creature,
 * destroy both creatures") and traded for it. The attack evaluator predicts the defender's blocks
 * with the stats-and-keywords heuristic, which read the 1/1 as a chump it would never make, so the
 * simulation never put the Zealot in front of the token and the trigger was never priced.
 *
 * The shape is narrow on purpose:
 *  - a [TriggerBinding.SELF] [EventPattern.BlockEvent] **with an attacker filter** ("blocks a
 *    creature") — the only shape the engine fires once per blocked attacker with that attacker as
 *    the triggering entity; a bare "blocks" trigger gets an empty trigger context, so its
 *    [EffectTarget.TriggeringEntity] resolves to nothing — that is not a batch, not "blocks two or
 *    more", and carries no intervening-if or restriction;
 *  - **no targets**, so hexproof, shroud and protection-from-targeting cannot stop it;
 *  - an effect that is an unconditional destroy of [EffectTarget.TriggeringEntity], alone or in a
 *    plain composite (no "you may", no "if") — optionally beside a destroy of [EffectTarget.Self]
 *    (the Zealot's "destroy both creatures"), which [Reading.destroysItself] reports.
 *
 * An indestructible attacker shrugs it off. Regeneration is not read: it is rare on attackers and
 * the simulation that follows the predicted blocks resolves it correctly anyway.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.blockTriggersKill].
 */
internal object BlockTriggerRemoval {

    /** What a matching block trigger does to the attacker it blocks and to the blocker itself. */
    data class Reading(val destroysItself: Boolean)

    /** Abilities come from card definitions and grants, so the shape is cached by identity. */
    private val cache: MutableMap<TriggeredAbility, Slot> =
        Collections.synchronizedMap(IdentityHashMap())

    /** A nullable cache slot: a null [reading] means "read, and not this shape". */
    private class Slot(val reading: Reading?)

    /**
     * What [blocker]'s block trigger does to [attacker] if it blocks it, or null when it has no
     * such trigger (or the attacker would survive it). Reads the projected state: a blocker that
     * has lost its abilities or is face down has no trigger, and an indestructible attacker survives.
     */
    fun against(
        state: GameState,
        projected: ProjectedState,
        cardRegistry: CardRegistry,
        predicateEvaluator: PredicateEvaluator,
        blocker: EntityId,
        attacker: EntityId,
    ): Reading? {
        if (projected.hasLostAllAbilities(blocker)) return null
        if (Keyword.INDESTRUCTIBLE.name in projected.getKeywords(attacker)) return null
        val entity = state.getEntity(blocker) ?: return null
        if (entity.has<FaceDownComponent>()) return null
        val card = entity.get<CardComponent>() ?: return null
        val printed = cardRegistry.getCard(card.cardDefinitionId)?.script?.triggeredAbilities.orEmpty()
        val granted = state.grantedTriggeredAbilities.filter { it.entityId == blocker }.map { it.ability }
        if (printed.isEmpty() && granted.isEmpty()) return null

        var best: Reading? = null
        for (ability in printed + granted) {
            val trigger = ability.trigger as? EventPattern.BlockEvent ?: continue
            val reading = cache.getOrPut(ability) { Slot(read(ability)) }.reading ?: continue
            val filter = trigger.attackerFilter
            if (filter != null) {
                val controller = projected.getController(blocker) ?: continue
                val context = PredicateContext(controllerId = controller, sourceId = blocker)
                if (!predicateEvaluator.matches(state, projected, attacker, filter, context)) continue
            }
            // A trigger that kills the attacker and spares the blocker is the better reading.
            if (best == null || (best.destroysItself && !reading.destroysItself)) best = reading
        }
        return best
    }

    private fun read(ability: TriggeredAbility): Reading? {
        val trigger = ability.trigger as? EventPattern.BlockEvent ?: return null
        if (ability.binding != TriggerBinding.SELF) return null
        if (trigger.batch || trigger.minBlockedAttackers != 1) return null
        if (trigger.attackerFilter == null) return null
        if (ability.targetRequirement != null || ability.additionalTargetRequirements.isNotEmpty()) return null
        if (ability.interveningIf != null || ability.triggerRestriction != null) return null
        val leaves = plainLeaves(ability.effect) ?: return null
        val destroys = leaves.filterIsInstance<MoveToZoneEffect>()
            .filter { it.byDestruction && it.destination == Zone.GRAVEYARD }
            .map { it.target }
        if (EffectTarget.TriggeringEntity !in destroys) return null
        return Reading(destroysItself = EffectTarget.Self in destroys)
    }

    /** The leaves of a gate-free composite tree; null when any part is gated or conditional. */
    private fun plainLeaves(effect: Effect): List<Effect>? {
        if (effect is CompositeEffect) {
            return effect.effects.map { plainLeaves(it) ?: return null }.flatten()
        }
        if (effect.asConditional() != null || effect.asMayDecide() != null) return null
        return listOf(effect)
    }
}
