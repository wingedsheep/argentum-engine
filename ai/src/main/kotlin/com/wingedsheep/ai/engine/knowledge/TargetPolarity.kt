package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import java.util.concurrent.ConcurrentHashMap

/**
 * Which side of the table a target slot wants: whether what the effect does to the object it
 * targets is **good for that object's controller** or **bad for it**.
 *
 * Not "removal vs pump" — a property of the effect read structurally, so it covers every card the
 * engine can load rather than the one shape [com.wingedsheep.ai.engine.TargetSelection.ranker]
 * used to special-case (a fixed, non-negative `ModifyStatsEffect` on an activated ability). Before
 * this, everything else ranked targets as removal: a Daru Healer's "prevent the next 1 damage to
 * any target" went on the opponent's flier, because an opponent's permanent is the best *removal*
 * target there is.
 */
enum class TargetPolarity {
    /** Bad for the target's controller — destroy, damage, tap, counter, steal, a Pacifism. */
    HARMFUL,

    /** Good for the target's controller — a pump, a damage shield, an evasion-granting Aura. */
    BENEFICIAL,

    /**
     * Mixed, unread, or genuinely either-way (a bounce can save your own creature; a flicker is
     * an ETB engine). Every consumer keeps its pre-polarity behaviour on this, which is the
     * "no better than before, never confidently wrong" contract [CardIntentAnalyzer] keeps.
     */
    UNKNOWN,
}

/**
 * Derives [TargetPolarity] per target requirement from a card's effect tree.
 *
 * ## How a leaf is matched to a slot
 *
 * A leaf names the object it acts on through an [EffectTarget]. A
 * [EffectTarget.BoundVariable] matches the requirement with that `id` (the DSL's `target(...)`
 * handle); an [EffectTarget.ContextTarget] matches by position. Two leaves act on "the target"
 * without naming it — a [CounterEffect] or [ExileTargetSpellEffect] whose spell is the chosen one —
 * and are attributed only when there is exactly one requirement for them to mean.
 *
 * A slot whose matched leaves all agree takes that polarity; any disagreement, any leaf this file
 * does not classify, or no matched leaf at all, reads [TargetPolarity.UNKNOWN]. That makes the
 * classification table below an allow-list of *confident* readings, not a list of special cases:
 * an effect missing from it degrades to the old behaviour rather than to a wrong one.
 *
 * ## Auras
 *
 * An Aura's target is the creature it will enchant, and what it does to that creature lives in its
 * static abilities (scoped to [Scope.AttachedTo]) and in triggers aimed at
 * [EffectTarget.EnchantedCreature] — Blossombind's "tap enchanted creature". Those are read the
 * same way and combined the same way.
 *
 * Memoized by card name and ability id, like the intent analysis: the answer is a pure function of
 * the definition.
 */
object TargetPolarityAnalyzer {

    private val spellCache = ConcurrentHashMap<String, List<TargetPolarity>>()
    private val abilityCache = ConcurrentHashMap<Pair<String, String>, List<TargetPolarity>>()

    /**
     * Polarity of each target slot of [card] cast as a spell, in requirement order — or an empty
     * list when the card's targeting cannot be read as one script (a split card, an Adventure, a
     * modal DFC: which face is being cast is not something this reading can know).
     */
    fun forSpell(card: CardDefinition): List<TargetPolarity> = spellCache.getOrPut(card.name) {
        if (card.cardFaces.isNotEmpty()) return@getOrPut emptyList()
        val script = card.script
        val aura = script.auraTarget
        when {
            aura != null && script.targetRequirements.isEmpty() -> listOf(auraPolarity(card))
            script.targetRequirements.isNotEmpty() ->
                forRequirements(script.spellEffect, script.targetRequirements)
            else -> emptyList()
        }
    }

    /** Polarity of each target slot of the activated [ability] printed on [card]. */
    fun forAbility(card: CardDefinition, ability: ActivatedAbility): List<TargetPolarity> =
        abilityCache.getOrPut(card.name to ability.id.value) {
            forRequirements(ability.effect, ability.targetRequirements)
        }

    /** Polarity of each of [requirements], read off [effect]. Not memoized — see the callers. */
    fun forRequirements(effect: Effect?, requirements: List<TargetRequirement>): List<TargetPolarity> {
        if (requirements.isEmpty()) return emptyList()
        if (effect == null) return requirements.map { TargetPolarity.UNKNOWN }
        val perSlot = List(requirements.size) { mutableListOf<TargetPolarity>() }
        for (leaf in EffectWalker.leaves(effect)) {
            for ((target, polarity) in classify(leaf)) {
                val slot = slotOf(target, requirements) ?: continue
                perSlot[slot] += polarity
            }
            if (requirements.size == 1) implicitTargetPolarity(leaf)?.let { perSlot[0] += it }
        }
        return perSlot.map(::combine)
    }

    /** One polarity for a slot, from every leaf that acts on it. */
    private fun combine(readings: List<TargetPolarity>): TargetPolarity =
        readings.distinct().singleOrNull() ?: TargetPolarity.UNKNOWN

    private fun slotOf(target: EffectTarget, requirements: List<TargetRequirement>): Int? = when (target) {
        is EffectTarget.BoundVariable -> requirements.indexOfFirst { it.id == target.name }.takeIf { it >= 0 }
        is EffectTarget.ContextTarget -> target.index.takeIf { it in requirements.indices }
        else -> null
    }

    /**
     * The leaves that act on "target spell" without naming it: the counter and the spell-exile.
     * Only "counter *target* spell" — one that counters the triggering spell aims at nothing chosen.
     */
    private fun implicitTargetPolarity(leaf: Effect): TargetPolarity? = when (leaf) {
        is CounterEffect ->
            if (leaf.targetSource == CounterTargetSource.Chosen) TargetPolarity.HARMFUL else null
        is ExileTargetSpellEffect -> TargetPolarity.HARMFUL
        else -> null
    }

    /**
     * What [leaf] does to each object it names. Effects absent from this table name nothing, which
     * leaves their slot unattributed — and a slot with no attributed leaf is [TargetPolarity.UNKNOWN].
     */
    private fun classify(leaf: Effect): List<Pair<EffectTarget, TargetPolarity>> {
        val polarity: Pair<EffectTarget, TargetPolarity?> = when (leaf) {
            // Off the battlefield to a graveyard or exile is removal. To hand or library is *not*
            // classified: bouncing your own creature in response to removal is a real play
            // (`lastchance-05`), and a move *onto* the battlefield or out of a graveyard is
            // recursion, whose polarity is about the card's owner rather than a controller.
            is MoveToZoneEffect -> leaf.target to when {
                leaf.fromZone != null && leaf.fromZone != Zone.BATTLEFIELD -> null
                leaf.destination == Zone.GRAVEYARD || leaf.destination == Zone.EXILE -> TargetPolarity.HARMFUL
                else -> null
            }
            is MoveUntilSourceLeavesEffect -> leaf.target to TargetPolarity.HARMFUL
            is ExileUntilLeavesEffect -> leaf.target to TargetPolarity.HARMFUL
            is DealDamageEffect -> leaf.target to TargetPolarity.HARMFUL
            is GainControlEffect -> leaf.target to TargetPolarity.HARMFUL
            is CantBlockEffect -> leaf.target to TargetPolarity.HARMFUL
            is CantBeRegeneratedEffect -> leaf.target to TargetPolarity.HARMFUL
            is MarkExileOnDeathEffect -> leaf.target to TargetPolarity.HARMFUL
            is TapUntapEffect -> leaf.target to
                if (leaf.tap) TargetPolarity.HARMFUL else TargetPolarity.BENEFICIAL
            is ModifyStatsEffect -> leaf.target to statsPolarity(leaf.powerModifier, leaf.toughnessModifier)
            is RegenerateEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is PreventDamageEffect -> leaf.target to when {
                !leaf.preventDamage -> null
                leaf.direction == PreventionDirection.ToTarget -> TargetPolarity.BENEFICIAL
                leaf.direction == PreventionDirection.FromTarget -> TargetPolarity.HARMFUL
                else -> null
            }
            is GrantKeywordEffect -> leaf.target to keywordPolarity(leaf.keyword)
            is GrantEvasionKeywordEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is GrantProtectionFromChosenColorEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is GrantHexproofFromChosenColorEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is AddCountersEffect -> leaf.target to counterPolarity(leaf.counterType)
            is AddDynamicCountersEffect -> leaf.target to counterPolarity(leaf.counterType)
            else -> return emptyList()
        }
        val (target, reading) = polarity
        // An effect we recognise but cannot call either way still *names* the slot, and says so:
        // a slot touched by a bounce and a pump is not a pump slot.
        return listOf(target to (reading ?: TargetPolarity.UNKNOWN))
    }

    private fun statsPolarity(power: DynamicAmount, toughness: DynamicAmount): TargetPolarity? {
        val p = (power as? DynamicAmount.Fixed)?.amount ?: return null
        val t = (toughness as? DynamicAmount.Fixed)?.amount ?: return null
        return when {
            p >= 0 && t >= 0 && (p > 0 || t > 0) -> TargetPolarity.BENEFICIAL
            p <= 0 && t <= 0 && (p < 0 || t < 0) -> TargetPolarity.HARMFUL
            // +N/-N and friends: depends on the board.
            else -> null
        }
    }

    private fun counterPolarity(type: CounterType): TargetPolarity? = when (type) {
        CounterType.PLUS_ONE_PLUS_ONE -> TargetPolarity.BENEFICIAL
        CounterType.MINUS_ONE_MINUS_ONE -> TargetPolarity.HARMFUL
        else -> null
    }

    /**
     * A keyword or ability flag's polarity. Evasion, combat and protective keywords are good for
     * their holder; "doesn't untap", "can't become untapped" and DEFENDER are the Aura-lock shapes
     * and are bad. Anything else — flash, a casting keyword, an unrecognised flag — is unread.
     */
    private fun keywordPolarity(name: String): TargetPolarity? {
        runCatching { Keyword.valueOf(name.uppercase()) }.getOrNull()?.let { keyword ->
            return when (keyword) {
                in BENEFICIAL_KEYWORDS -> TargetPolarity.BENEFICIAL
                Keyword.DEFENDER -> TargetPolarity.HARMFUL
                else -> null
            }
        }
        return when (runCatching { AbilityFlag.valueOf(name.uppercase()) }.getOrNull()) {
            AbilityFlag.CANT_BE_BLOCKED -> TargetPolarity.BENEFICIAL
            AbilityFlag.DOESNT_UNTAP, AbilityFlag.CANT_BECOME_UNTAPPED,
            AbilityFlag.ASSIGNS_NO_COMBAT_DAMAGE -> TargetPolarity.HARMFUL
            else -> null
        }
    }

    /**
     * What an Aura does to the creature it enchants: its [Scope.AttachedTo] statics plus every
     * triggered effect aimed at [EffectTarget.EnchantedCreature], combined like any other slot.
     */
    private fun auraPolarity(card: CardDefinition): TargetPolarity {
        val readings = mutableListOf<TargetPolarity>()
        for (static in card.script.staticAbilities) staticPolarity(static)?.let(readings::add)
        for (trigger in card.script.triggeredAbilities) {
            for (leaf in EffectWalker.leaves(trigger.effect)) {
                for ((target, polarity) in classify(leaf)) {
                    if (target == EffectTarget.EnchantedCreature) readings += polarity
                }
            }
        }
        // A replacement effect on an Aura (Sandskin's "prevent all combat damage dealt to and by
        // enchanted creature") is read nowhere above, and is often the whole card. Without it the
        // statics alone could call an either-way Aura one-sided, so its presence is a veto — except
        // for an entry choice (Traveler's Cloak's land type), which only parameterises a static.
        if (card.script.replacementEffects.any { it !is EntersWithChoice }) readings += TargetPolarity.UNKNOWN
        return combine(readings)
    }

    private fun staticPolarity(static: StaticAbility): TargetPolarity? = when (static) {
        is ModifyStats -> if (static.filter.scope != Scope.AttachedTo) null
            else statsPolarity(DynamicAmount.Fixed(static.powerBonus), DynamicAmount.Fixed(static.toughnessBonus))
        is GrantKeyword -> if (static.filter.scope != Scope.AttachedTo) null else keywordPolarity(static.keyword)
        is GrantLandwalkOfChosenType -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is CantAttack -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        is CantBlock -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        else -> null
    }

    private val BENEFICIAL_KEYWORDS = setOf(
        Keyword.FLYING, Keyword.MENACE, Keyword.INTIMIDATE, Keyword.FEAR, Keyword.SHADOW,
        Keyword.HORSEMANSHIP, Keyword.SWAMPWALK, Keyword.FORESTWALK, Keyword.ISLANDWALK,
        Keyword.MOUNTAINWALK, Keyword.PLAINSWALK, Keyword.FIRST_STRIKE, Keyword.DOUBLE_STRIKE,
        Keyword.TRAMPLE, Keyword.DEATHTOUCH, Keyword.LIFELINK, Keyword.VIGILANCE, Keyword.REACH,
        Keyword.HASTE, Keyword.INDESTRUCTIBLE, Keyword.HEXPROOF, Keyword.SHROUD, Keyword.WARD,
        Keyword.PROTECTION,
    )
}
