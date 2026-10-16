package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.handlers.TargetFinder
import com.wingedsheep.engine.handlers.TargetingSourceType
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect

/**
 * "What is that going to exile?" — a permanent spell whose whole payoff is an enters-the-battlefield
 * trigger that has nothing to target.
 *
 * Banishing Light is the card (2026-10-15, Edge of Eternities sealed, game `cfe8d858`, turn 5): cast
 * into a human with no nonland permanent at all, its trigger was removed from the stack for want of
 * a target (CR 603.3d) and the enchantment sat there doing nothing — and it was the AI's only answer
 * to the Thrumming Hivepool that arrived later. The leaf scores the line on the board it leaves, and
 * a noncreature permanent on our side of the table carries a board prior of its own, so "an
 * enchantment that will never do anything" priced above "a removal spell in hand".
 *
 * [EmptyPayoff] is the same question asked of an activation; this is the cast. The claim is the
 * [TimingVerdict.NoWindow] kind — structurally certain, never a preference — so the shape it reads
 * is narrow:
 *
 *  - a **noncreature, nonland** permanent with no faces, no static, activated or replacement
 *    abilities and no keywords but flash: a creature's body, an anthem, an equip ability or a mana
 *    ability is a payoff of its own, whatever its trigger does;
 *  - every triggered ability is either **its own entering** with a target, or **its own leaving**
 *    whose only work is to hand back what the entry exiled (the linked-exile return an
 *    "until this leaves the battlefield" clause lowers to) — nothing else is left to happen;
 *  - and some entry trigger has a **required target slot nothing can fill** right now. With the
 *    stack empty the board it would see is this one; with something on the stack this declines,
 *    because a spell resolving first could make a target.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs].
 */
internal object TargetlessEntry {

    fun holds(
        state: GameState,
        playerId: EntityId,
        definition: CardDefinition,
        castCardId: EntityId,
        predicates: PredicateEvaluator,
    ): Boolean {
        if (state.stack.isNotEmpty()) return false
        val type = definition.typeLine
        if (!type.isPermanent || type.isCreature || type.isLand || type.isAura) return false
        if (definition.cardFaces.isNotEmpty() || definition.backFace != null || definition.flipSide != null) return false
        if (definition.keywords.any { it != Keyword.FLASH }) return false
        if (definition.keywordAbilities.isNotEmpty() || definition.flags.isNotEmpty() || definition.equipCost != null) {
            return false
        }

        val script = definition.script
        if (script.spellEffect != null || script.staticAbilities.isNotEmpty() ||
            script.activatedAbilities.isNotEmpty() || script.replacementEffects.isNotEmpty() ||
            script.stateTriggeredAbilities.isNotEmpty() || script.auraTarget != null
        ) {
            return false
        }

        val triggers = script.triggeredAbilities
        val entries = triggers.filter(::isOwnEntry)
        if (entries.isEmpty()) return false
        if (!triggers.all { it in entries || handsBackTheEntry(it) }) return false

        val finder = TargetFinder(predicates)
        return entries.all { entry ->
            if (entry.elseEffect != null) return@all false
            val requirements = listOfNotNull(entry.targetRequirement) + entry.additionalTargetRequirements
            requirements.any { requirement ->
                requirement.effectiveMinCount > 0 &&
                    runCatching {
                        finder.findLegalTargets(
                            state, requirement, playerId,
                            sourceId = castCardId,
                            targetingSourceType = TargetingSourceType.TRIGGERED_ABILITY,
                        ).isEmpty()
                    }.getOrDefault(false)
            }
        }
    }

    /** "When this enters" — the trigger bound to the permanent itself, on its way onto the battlefield. */
    private fun isOwnEntry(trigger: TriggeredAbility): Boolean {
        if (trigger.binding != TriggerBinding.SELF) return false
        val pattern = trigger.trigger as? EventPattern.ZoneChangeEvent ?: return false
        return pattern.to == Zone.BATTLEFIELD
    }

    /**
     * "When this leaves the battlefield, return the exiled card" — a leave trigger of its own whose
     * every leaf reads the permanent's linked exile and moves what it gathered. With nothing exiled
     * it moves nothing.
     */
    private fun handsBackTheEntry(trigger: TriggeredAbility): Boolean {
        if (trigger.binding != TriggerBinding.SELF) return false
        val pattern = trigger.trigger as? EventPattern.ZoneChangeEvent ?: return false
        if (pattern.from != Zone.BATTLEFIELD) return false
        val leaves = EffectWalker.leaves(trigger.effect)
        val gathered = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { it.source is CardSource.FromLinkedExile }
            .map { it.storeAs }
            .toSet()
        return gathered.isNotEmpty() && leaves.all { leaf ->
            (leaf is GatherCardsEffect && leaf.source is CardSource.FromLinkedExile) ||
                (leaf is MoveCollectionEffect && leaf.from in gathered)
        }
    }
}
