package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.conditions.AllConditions
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * "What is that going to do to *that* creature?" — an Aura whose whole payoff is an entry trigger
 * gated on the creature it enchants, cast onto a creature that fails the gate.
 *
 * Earthbind is the card: "When this Aura enters, if enchanted creature has flying, this Aura deals
 * 2 damage to that creature and this Aura gains 'Enchanted creature loses flying.'" Enchant creature
 * is any creature, so a non-flyer is a legal target — but the intervening-if (CR 603.4) is false on
 * entry, the trigger never goes on the stack, and the Aura sits there doing nothing. The leaf prices
 * a noncreature permanent on our side above a card in hand, so the AI cast it anyway.
 *
 * [TargetlessEntry] is the same question for a non-Aura permanent whose entry has no target; this is
 * its Aura twin, and its claim is the same [TimingVerdict.NoWindow] kind — structurally certain — so
 * the shape it reads is as narrow:
 *
 *  - an **Aura** with no faces, no static, activated or replacement abilities and no keywords but
 *    flash: anything the Aura does on its own is a payoff whatever its trigger does;
 *  - every triggered ability is **its own entering**, with no "otherwise" branch, gated on the
 *    enchanted permanent matching a filter;
 *  - and the creature the cast is aimed at matches **none** of those filters right now. With
 *    something on the stack this declines, because a spell resolving first could change that.
 *
 * Carried on [com.wingedsheep.ai.engine.AiProfile.refuseWastedPayoffs].
 */
internal object UnmetAuraEntry {

    fun holds(
        state: GameState,
        playerId: EntityId,
        definition: CardDefinition,
        castCardId: EntityId,
        targets: List<ChosenTarget>,
        predicates: PredicateEvaluator,
    ): Boolean {
        if (state.stack.isNotEmpty()) return false
        if (!definition.typeLine.isAura) return false
        if (definition.cardFaces.isNotEmpty() || definition.backFace != null || definition.flipSide != null) return false
        if (definition.keywords.any { it != Keyword.FLASH }) return false
        if (definition.keywordAbilities.isNotEmpty() || definition.flags.isNotEmpty()) return false

        val script = definition.script
        if (script.auraTarget == null || script.spellEffect != null || script.staticAbilities.isNotEmpty() ||
            script.activatedAbilities.isNotEmpty() || script.replacementEffects.isNotEmpty() ||
            script.stateTriggeredAbilities.isNotEmpty()
        ) {
            return false
        }

        val triggers = script.triggeredAbilities
        if (triggers.isEmpty()) return false
        val gates = triggers.map { trigger ->
            if (!isOwnEntry(trigger) || trigger.elseEffect != null) return false
            enchantedGate(trigger.triggerCondition) ?: return false
        }

        val host = (targets.singleOrNull() as? ChosenTarget.Permanent)?.entityId ?: return false
        val projected = state.projectedState
        val context = PredicateContext(controllerId = playerId, sourceId = castCardId)
        return gates.none { filter ->
            // An unreadable filter is "it might match", never a veto.
            runCatching { predicates.matches(state, projected, host, filter, context) }.getOrDefault(true)
        }
    }

    /** "When this enters" — the trigger bound to the permanent itself, on its way onto the battlefield. */
    private fun isOwnEntry(trigger: TriggeredAbility): Boolean {
        if (trigger.binding != TriggerBinding.SELF) return false
        val pattern = trigger.trigger as? EventPattern.ZoneChangeEvent ?: return false
        return pattern.to == Zone.BATTLEFIELD
    }

    /**
     * The filter an "if enchanted creature …" gate reads, or null when [condition] is anything else
     * (or absent). Inside an all-of, the enchanted-permanent conjunct alone is enough: it failing
     * fails the whole gate.
     */
    private fun enchantedGate(condition: Condition?): GameObjectFilter? = when (condition) {
        is EntityMatches -> condition.filter.takeIf {
            condition.entity == EffectTarget.EnchantedPermanent || condition.entity == EffectTarget.EnchantedCreature
        }
        is AllConditions -> condition.conditions.firstNotNullOfOrNull(::enchantedGate)
        else -> null
    }
}
