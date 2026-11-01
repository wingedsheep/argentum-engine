package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantProtectionFromCardType
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.AdaptAsThoughNoCountersEffect
import com.wingedsheep.sdk.scripting.effects.AdaptEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.BecomePreparedEffect
import com.wingedsheep.sdk.scripting.effects.UnprepareEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
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
/** Looks a token's definition up by name — a Role token, in practice. */
typealias CardLookup = (String) -> CardDefinition?

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
 * same way and combined the same way. Land Auras count too: Overgrowth's "whenever enchanted land
 * is tapped for mana, its controller adds an additional {G}{G}" is ramp for whoever's land it
 * enchants. "Enchant creature or Vehicle" Auras name their host as [EffectTarget.EnchantedPermanent]
 * (Flood the Engine's "tap enchanted permanent"), which reads like "enchanted creature".
 *
 * An Aura whose slot still reads [TargetPolarity.UNKNOWN] keeps the removal ranking on purpose:
 * the unread Auras are curses (Imprison, Brainwash) as often as gifts (Curiosity, Spirit Link), so
 * neither side of the table is the safer default.
 *
 * An Aura's own activated abilities aimed at the creature it enchants count too: Strands of
 * Undeath's "{B}: Regenerate enchanted creature" can only be paid by the Aura's controller, so it
 * is a reason to enchant a creature of ours.
 *
 * ## Equipment
 *
 * Equip (and fortify, and reconfigure) is "attach this permanent to target creature you control",
 * so its slot reads exactly like an Aura's enchant target: whatever the Equipment does to the
 * creature it equips — +2 Mace's "+2/+2" is a gift. The attach names no effect of its own, so the
 * reading needs the card the ability is printed on, which [forAbility] has and a bare effect does
 * not. Unread, the slot took the removal ranking, which orders our own creatures worst first.
 *
 * ## Role tokens
 *
 * "Create a Young Hero Role token attached to target creature" does to that creature whatever the
 * Role does to the creature it enchants, so the slot reads as the Role's own Aura polarity — a
 * Monster Role's +1/+1 and trample is a gift, a Cursed Role's base 1/1 a curse. The Role is a
 * token, so reading it needs its definition: the analyzer takes a [CardLookup] lookup (the card
 * registry, in play) and leaves the slot unread without one.
 *
 * Memoized by card name and ability id, like the intent analysis: the answer is a pure function of
 * the definition (and of whether token definitions could be looked up).
 */
object TargetPolarityAnalyzer {

    /** No token definitions: a Role-creating slot stays unread. */
    val NO_DEFINITIONS: CardLookup = { null }

    private val spellCache = ConcurrentHashMap<Pair<String, Boolean>, List<TargetPolarity>>()
    private val abilityCache = ConcurrentHashMap<Triple<String, String, Boolean>, List<TargetPolarity>>()
    private val modeCache = ConcurrentHashMap<Triple<String, List<Int>, Boolean>, List<TargetPolarity>>()

    /**
     * Polarity of each target slot of [card] cast as a spell, in requirement order — or an empty
     * list when the card's targeting cannot be read as one script (a split card, an Adventure, a
     * modal DFC: which face is being cast is not something this reading can know).
     */
    fun forSpell(card: CardDefinition, definitions: CardLookup = NO_DEFINITIONS): List<TargetPolarity> =
        spellCache.getOrPut(card.name to (definitions !== NO_DEFINITIONS)) {
            if (card.cardFaces.isNotEmpty()) return@getOrPut emptyList()
            val script = card.script
            val aura = script.auraTarget
            when {
                aura != null && script.targetRequirements.isEmpty() -> listOf(attachmentPolarity(card, definitions))
                script.targetRequirements.isNotEmpty() ->
                    forRequirements(script.spellEffect, script.targetRequirements, definitions)
                else -> emptyList()
            }
        }

    /**
     * Polarity of each target slot of the modes [modeIndices] of [card]'s modal spell — a
     * choose-one spell is offered once per mode, and a tiered spell is a choose-one whose modes
     * carry costs — in the order the chosen modes' requirements are surfaced. Empty when the card
     * is not a plain modal spell (its targets are not the modes'), or an index is out of range.
     */
    fun forSpellModes(
        card: CardDefinition,
        modeIndices: List<Int>,
        definitions: CardLookup = NO_DEFINITIONS,
    ): List<TargetPolarity> = modeCache.getOrPut(Triple(card.name, modeIndices, definitions !== NO_DEFINITIONS)) {
        if (card.cardFaces.isNotEmpty() || card.script.targetRequirements.isNotEmpty()) return@getOrPut emptyList()
        val modal = card.script.spellEffect as? ModalEffect ?: return@getOrPut emptyList()
        val modes = modeIndices.map { modal.modes.getOrNull(it) ?: return@getOrPut emptyList() }
        modes.flatMap { forMode(it, definitions) }
    }

    /** Polarity of each target slot of one [mode] of a modal spell or triggered ability. */
    fun forMode(mode: Mode, definitions: CardLookup = NO_DEFINITIONS): List<TargetPolarity> =
        forRequirements(mode.effect, mode.targetRequirements, definitions)

    /** Polarity of each target slot of the activated [ability] printed on [card]. */
    fun forAbility(
        card: CardDefinition,
        ability: ActivatedAbility,
        definitions: CardLookup = NO_DEFINITIONS,
    ): List<TargetPolarity> =
        abilityCache.getOrPut(Triple(card.name, ability.id.value, definitions !== NO_DEFINITIONS)) {
            forRequirements(ability.effect, ability.targetRequirements, definitions, source = card)
        }

    /**
     * What [effect] does to the permanent its source is attached to — "regenerate enchanted
     * creature", "tap equipped creature" — read like a target slot over the leaves aimed at the
     * host. [TargetPolarity.UNKNOWN] when nothing in it acts on the host, or its leaves disagree.
     */
    fun forAttachedHost(effect: Effect, definitions: CardLookup = NO_DEFINITIONS): TargetPolarity {
        val readings = EffectWalker.payoffLeaves(effect).flatMap { leaf ->
            classify(leaf, definitions).filter { (target, _) -> target in HOST_TARGETS }.map { it.second }
        }
        return combine(readings)
    }

    /**
     * Polarity of each of [requirements], read off [effect]. Not memoized — see the callers.
     *
     * [source] is the card the effect is printed on, when the caller knows it: "attach this
     * Equipment to target creature" is read off what that card does to the creature it equips, so
     * without it the attach's slot stays unread.
     */
    fun forRequirements(
        effect: Effect?,
        requirements: List<TargetRequirement>,
        definitions: CardLookup = NO_DEFINITIONS,
        source: CardDefinition? = null,
    ): List<TargetPolarity> {
        if (requirements.isEmpty()) return emptyList()
        if (effect == null) return requirements.map { TargetPolarity.UNKNOWN }
        val perSlot = List(requirements.size) { mutableListOf<TargetPolarity>() }
        val recursions = mutableSetOf<Int>()
        for (leaf in EffectWalker.payoffLeaves(effect)) {
            // "Destroy up to two target artifacts and/or enchantments" (Force of Vigor) runs its
            // body once per target, each seeing its target as `ContextTarget(0)`. With one slot
            // that is every target of that slot, so the body reads as the slot's; with several,
            // `ContextTarget(0)` walks all of them and names no one slot.
            if (leaf is ForEachEffect && leaf.space == IterationSpace.Targets) {
                if (requirements.size == 1) perSlot[0] += forRequirements(leaf.body, requirements, definitions, source)
                continue
            }
            // "… to each of X target creatures" (Spinning Wheel Kick) visits one declaration's
            // targets as `IterationEntity`; the body's other references keep their own slots.
            val space = (leaf as? ForEachEffect)?.space
            if (space is IterationSpace.TargetsOf) {
                val iterated = requirements.indexOfFirst { it.id == space.group }
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        val slot = if (target == EffectTarget.IterationEntity) iterated.takeIf { it >= 0 }
                            else slotOf(target, requirements)
                        slot?.let { perSlot[it] += polarity }
                    }
                }
                continue
            }
            val recursion = recursionSlot(leaf, requirements)
            if (recursion != null) {
                recursions += recursion
            } else {
                for ((target, polarity) in classify(leaf, definitions, source)) {
                    val slot = slotOf(target, requirements) ?: continue
                    perSlot[slot] += polarity
                }
            }
            if (requirements.size == 1) implicitTargetPolarity(leaf)?.let { perSlot[0] += it }
        }
        // Getting a card back is the point of a recursion slot: a finality or -1/-1 counter it
        // comes back with (Rite of the Moth, Soulcoil Viper) is the price, not the other side of it.
        return perSlot.mapIndexed { slot, readings -> if (slot in recursions) TargetPolarity.BENEFICIAL else combine(readings) }
    }

    /** One polarity for a slot, from every leaf that acts on it. */
    private fun combine(readings: List<TargetPolarity>): TargetPolarity =
        readings.distinct().singleOrNull() ?: TargetPolarity.UNKNOWN

    /** Whether [this] names a chosen target rather than a fixed player or object. */
    private fun EffectTarget.isSlot(): Boolean = this is EffectTarget.BoundVariable || this is EffectTarget.ContextTarget

    private fun slotOf(target: EffectTarget, requirements: List<TargetRequirement>): Int? = when (target) {
        is EffectTarget.BoundVariable -> requirements.indexOfFirst { it.id == target.name }.takeIf { it >= 0 }
        is EffectTarget.ContextTarget -> target.index.takeIf { it in requirements.indices }
        else -> null
    }

    /**
     * The slot a **recursion** names — a card moved out of a graveyard into a hand or onto the
     * battlefield ("return target creature card from your graveyard to your hand") — or null.
     *
     * Read here rather than in [classify] because the move alone often cannot say where the card
     * comes from: `Effects.ReturnToHand` leaves `fromZone` unset and the *requirement* is what puts
     * the target in a graveyard. Getting a card back is good for whoever gets it — the polarity's
     * "controller" of a card in a graveyard is its owner — so the slot reads beneficial and the
     * best card is the one to return. Unread, the removal ranking ranked a card that is no
     * opponent's permanent by its negated value, and returned the worst one.
     */
    private fun recursionSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        if (leaf !is MoveToZoneEffect || leaf.faceDown != null) return null
        if (leaf.destination != Zone.HAND && leaf.destination != Zone.BATTLEFIELD) return null
        val slot = slotOf(leaf.target, requirements) ?: return null
        val fromGraveyard = leaf.fromZone == Zone.GRAVEYARD ||
            (leaf.fromZone == null && (requirements[slot] as? TargetObject)?.filter?.zone == Zone.GRAVEYARD)
        return slot.takeIf { fromGraveyard }
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
    private fun classify(
        leaf: Effect,
        definitions: CardLookup = NO_DEFINITIONS,
        source: CardDefinition? = null,
    ): List<Pair<EffectTarget, TargetPolarity>> {
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
            // "Target creature has base power and toughness 0/0 until end of turn" (Multiply by
            // Zero). A larger base names nothing, so the slot's other effects decide it: "becomes a
            // 4/4 and gains indestructible" on a creature of ours is the indestructible's call.
            is SetBaseStatsEffect -> leaf.target to (baseStatsPolarity(leaf.power, leaf.toughness) ?: return emptyList())
            // "<fighter> fights target creature": the creature fought is dealt damage, the victim's
            // half of the fight. The fighter's half names nothing: its power is the weapon and the
            // damage dealt back is the price, so which creature of ours fights best is a board
            // question, not a side of the table.
            is FightEffect -> leaf.target2 to TargetPolarity.HARMFUL
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
            is AdaptEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is AdaptAsThoughNoCountersEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is AddDynamicCountersEffect -> leaf.target to counterPolarity(leaf.counterType)
            // Equip, fortify, reconfigure: "attach this permanent to target creature" does to that
            // creature whatever this Equipment does to the creature it equips — the same reading an
            // Aura's enchant target gets. When that can't be read (no source card, see
            // [forRequirements], or an Equipment whose bonus is mixed) the attach names nothing, so
            // a sibling leaf can still read the slot — The Aetherspark's "+1/+1 counter on that creature".
            is AttachEquipmentEffect -> source?.let { attachmentPolarity(it, definitions) }
                ?.takeIf { it != TargetPolarity.UNKNOWN }
                ?.let { leaf.target to it } ?: return emptyList()
            // The Aura token does to its object what any Aura does to the one it enchants.
            is CreateAuraTokenEffect -> leaf.target to definitions(leaf.tokenName)
                ?.let { attachmentPolarity(it, definitions) }
                ?.takeIf { it != TargetPolarity.UNKNOWN }
            // "Target creature gains '<trigger>' until end of turn" (Sygg, Wanderwine Wisdom): the
            // granted ability is controlled by the creature's controller, so it reads exactly like
            // the same trigger granted by an Aura.
            is GrantTriggeredAbilityEffect -> leaf.target to grantedTriggerPolarity(leaf.ability)
                .takeIf { it != TargetPolarity.UNKNOWN }
            // Only a creature with a prepare spell can become prepared, and its controller is the
            // one who may cast that spell (Hexhaven Dueling Arena); unpreparing takes it away.
            is BecomePreparedEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is UnprepareEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Target player draws / gains / loses …" (Inspiration, Sign in Blood). Only a slot is
            // read here: a draw for "you" is not a target, and [grantedTriggerPolarity] reads it
            // through [controllerPolarity] instead. Sign in Blood's draw and life loss disagree
            // and combine to UNKNOWN, which is right — it is aimed at either side.
            is DrawCardsEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.BENEFICIAL else return emptyList()
            is GainLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.BENEFICIAL else return emptyList()
            is LoseLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Look at target player's hand" reveals what it holds — information only worth having
            // about someone else. Beside a "that player draws" (Vendilion Clique, Oildeep Gearhulk)
            // the two disagree and the slot stays two-sided, as Sign in Blood's does.
            is LookAtTargetHandEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Target player creates two Treasure tokens" (Ashling's Command): a Treasure, Food,
            // Clue or Gold is the controller's to use, and the board evaluator does not price one —
            // so without this the slot is a coin toss between giving it to us or to them.
            is CreatePredefinedTokenEffect -> leaf.controller?.takeIf { it.isSlot() }
                ?.let { it to TargetPolarity.BENEFICIAL } ?: return emptyList()
            else -> return emptyList()
        }
        val (target, reading) = polarity
        // An effect we recognise but cannot call either way still *names* the slot, and says so:
        // a slot touched by a bounce and a pump is not a pump slot.
        return listOf(target to (reading ?: TargetPolarity.UNKNOWN))
    }

    /**
     * A base-P/T change by what it is set to: a 1/1 or smaller shrinks nearly anything it lands on
     * — the same line [staticPolarity] draws for the Cursed Role's static. A larger base is a gift
     * or a curse depending on the creature, and a dynamic one depends on the board, so both stay
     * unread. A side left null is unchanged.
     */
    private fun baseStatsPolarity(power: DynamicAmount?, toughness: DynamicAmount?): TargetPolarity? {
        val set = listOfNotNull(power, toughness)
        if (set.isEmpty()) return null
        val small = set.all { (it as? DynamicAmount.Fixed)?.amount?.let { amount -> amount <= 1 } == true }
        return if (small) TargetPolarity.HARMFUL else null
    }

    private fun statsPolarity(power: DynamicAmount, toughness: DynamicAmount): TargetPolarity? {
        // A dynamic "+X/+X" reads like a fixed one when X cannot go negative — see [AmountSign].
        return when (statsSign(power, toughness)) {
            AmountSign.GAIN -> TargetPolarity.BENEFICIAL
            AmountSign.LOSS -> TargetPolarity.HARMFUL
            // +N/-N and friends: depends on the board.
            AmountSign.ZERO, AmountSign.UNKNOWN -> null
        }
    }

    /**
     * A counter kind's polarity for the permanent it lands on. Stat counters read by their sign,
     * a shield or keyword counter is a gift, and a stun or finality counter is a curse. Every
     * other kind — charge, time, loyalty, the bespoke ones — means whatever its card makes of it,
     * and stays unread.
     */
    private fun counterPolarity(type: CounterType): TargetPolarity? {
        type.ptDelta?.let { (power, toughness) ->
            return when {
                power >= 0 && toughness >= 0 -> TargetPolarity.BENEFICIAL
                power <= 0 && toughness <= 0 -> TargetPolarity.HARMFUL
                else -> null
            }
        }
        return when (type) {
            in BENEFICIAL_COUNTERS -> TargetPolarity.BENEFICIAL
            in HARMFUL_COUNTERS -> TargetPolarity.HARMFUL
            else -> null
        }
    }

    private val BENEFICIAL_COUNTERS = setOf(
        // Loyalty is what a planeswalker spends and what damage takes away: more is always better
        // for its controller (Teyo's "put a loyalty counter on it if it's a planeswalker").
        CounterType.SHIELD, CounterType.LOYALTY,
        CounterType.FLYING, CounterType.FIRST_STRIKE, CounterType.DOUBLE_STRIKE, CounterType.VIGILANCE,
        CounterType.LIFELINK, CounterType.INDESTRUCTIBLE, CounterType.DEATHTOUCH, CounterType.TRAMPLE,
        CounterType.HEXPROOF, CounterType.REACH, CounterType.HASTE, CounterType.MENACE, CounterType.SHADOW,
    )

    private val HARMFUL_COUNTERS = setOf(
        CounterType.STUN, CounterType.FINALITY,
    )

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
     * What an Aura or Equipment does to the permanent it is attached to: its [Scope.AttachedTo]
     * statics plus every triggered or activated effect aimed at its host ([HOST_TARGETS] —
     * "enchanted creature", "equipped creature"), combined like any other slot. An activated one
     * counts because the attachment's controller is who can activate it.
     */
    private fun attachmentPolarity(card: CardDefinition, definitions: CardLookup): TargetPolarity {
        val readings = mutableListOf<TargetPolarity>()
        for (static in card.script.staticAbilities) staticPolarity(static)?.let(readings::add)
        val abilityEffects = card.script.triggeredAbilities.map { it.effect } +
            card.script.activatedAbilities.map { it.effect }
        for (effect in abilityEffects) {
            for (leaf in EffectWalker.payoffLeaves(effect)) {
                for ((target, polarity) in classify(leaf, definitions)) {
                    // "Enchanted creature" and "enchanted permanent" (Flood the Engine's "tap
                    // enchanted permanent") both name the object the Aura is cast on.
                    if (target in HOST_TARGETS) readings += polarity
                }
            }
        }
        // A replacement effect on an Aura (Sandskin's "prevent all combat damage dealt to and by
        // enchanted creature") is read nowhere above, and is often the whole card. Without it the
        // statics alone could call an either-way Aura one-sided, so its presence is a veto — except
        // for an entry choice (Traveler's Cloak's land type), which only parameterises a static.
        // A self-only zone redirect (disturb's "if this would be put into a graveyard, exile it
        // instead") moves the Aura itself and never touches the creature, so it is no veto either.
        if (card.script.replacementEffects.any { !it.affectsOnlyTheAuraItself() }) readings += TargetPolarity.UNKNOWN
        return combine(readings)
    }

    private fun ReplacementEffect.affectsOnlyTheAuraItself(): Boolean = when (this) {
        is EntersWithChoice -> true
        is RedirectZoneChange -> selfOnly
        is RedirectZoneChangeWith -> selfOnly
        else -> false
    }

    /**
     * What a triggered ability granted to the enchanted creature does for that creature's
     * controller, who controls the granted ability: its leaves aimed at the creature itself
     * ([EffectTarget.Self] inside the quotes) read like any other slot, and a card draw or life
     * gain for its controller is a benefit. A granted ability with no readable leaf, or with any
     * leaf this cannot call, reads [TargetPolarity.UNKNOWN] — "enchanted creature has '…'" can be a
     * curse as easily as a gift.
     */
    private fun grantedTriggerPolarity(ability: TriggeredAbility): TargetPolarity {
        val readings = mutableListOf<TargetPolarity>()
        for (leaf in EffectWalker.payoffLeaves(ability.effect)) {
            val classified = classify(leaf)
            val reading = when {
                classified.isNotEmpty() -> classified.map { (target, polarity) ->
                    if (target == EffectTarget.Self) polarity else TargetPolarity.UNKNOWN
                }
                else -> listOf(controllerPolarity(leaf) ?: TargetPolarity.UNKNOWN)
            }
            readings += reading
        }
        return combine(readings)
    }

    /** A player-facing leaf's polarity for the ability's controller, when it is aimed at them. */
    private fun controllerPolarity(leaf: Effect): TargetPolarity? {
        fun isController(target: EffectTarget) =
            target == EffectTarget.Controller || target == EffectTarget.PlayerRef(Player.You)
        return when (leaf) {
            is DrawCardsEffect -> if (isController(leaf.target)) TargetPolarity.BENEFICIAL else null
            is GainLifeEffect -> if (isController(leaf.target)) TargetPolarity.BENEFICIAL else null
            is LoseLifeEffect -> if (isController(leaf.target)) TargetPolarity.HARMFUL else null
            else -> null
        }
    }

    private fun staticPolarity(static: StaticAbility): TargetPolarity? = when (static) {
        is ModifyStats -> if (static.filter.scope != Scope.AttachedTo) null
            else statsPolarity(DynamicAmount.Fixed(static.powerBonus), DynamicAmount.Fixed(static.toughnessBonus))
        is GrantKeyword -> if (static.filter.scope != Scope.AttachedTo) null else keywordPolarity(static.keyword)
        is GrantLandwalkOfChosenType -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is CantAttack -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        is CantBlock -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        is GrantTriggeredAbility -> if (static.filter.scope != Scope.AttachedTo) null else grantedTriggerPolarity(static.ability)
        is GrantWard -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtection -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtectionFromCardType -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is CantBeBlocked -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantDynamicStats -> if (static.filter.scope != Scope.AttachedTo) null
            else statsPolarity(static.powerBonus, static.toughnessBonus)
        // An ability only the enchanted permanent's controller can activate, and only when they
        // choose to — Abundant Growth's mana ability, Squirrel Nest's token maker — is theirs to use.
        is GrantActivatedAbility -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        // "Loses all abilities" (Lignify, Imprisoned in the Moon) is the curse shape.
        is LoseAllAbilities -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        // "Whenever enchanted land is tapped for mana, its controller adds an additional {G}{G}"
        // (Overgrowth, Wild Growth, Fertile Ground): the bonus goes to the land's controller, so
        // it is ramp for whoever's land it enchants. No filter: it only ever reads the host.
        is AdditionalManaOnTap -> TargetPolarity.BENEFICIAL
        // Control Magic, Annex, Confiscate: the host changes sides.
        ControlEnchantedPermanent -> TargetPolarity.HARMFUL
        // A condition gates the ability without turning it around ("as long as enchanted creature
        // is a Vampire, it gets +2/+2"); rival conditional halves disagree and combine to UNKNOWN.
        is ConditionalStaticAbility -> staticPolarity(static.ability)
        is CompositeStaticAbility -> static.abilities.mapNotNull(::staticPolarity).takeIf { it.isNotEmpty() }?.let(::combine)
        // "Has base power and toughness 1/1" (Cursed Role) shrinks nearly anything it lands on; a
        // larger base is a gift or a curse depending on the creature, so it stays unread.
        is SetBasePowerToughnessStatic -> when {
            static.filter.scope != Scope.AttachedTo -> null
            (static.power ?: 0) <= 1 && (static.toughness ?: 0) <= 1 -> TargetPolarity.HARMFUL
            else -> null
        }
        else -> null
    }

    /** How an ability names the permanent its source is attached to. */
    private val HOST_TARGETS = setOf(
        EffectTarget.EnchantedCreature, EffectTarget.EquippedCreature, EffectTarget.EnchantedPermanent,
    )

    private val BENEFICIAL_KEYWORDS = setOf(
        Keyword.FLYING, Keyword.MENACE, Keyword.INTIMIDATE, Keyword.FEAR, Keyword.SHADOW,
        Keyword.HORSEMANSHIP, Keyword.SWAMPWALK, Keyword.FORESTWALK, Keyword.ISLANDWALK,
        Keyword.MOUNTAINWALK, Keyword.PLAINSWALK, Keyword.FIRST_STRIKE, Keyword.DOUBLE_STRIKE,
        Keyword.TRAMPLE, Keyword.DEATHTOUCH, Keyword.LIFELINK, Keyword.VIGILANCE, Keyword.REACH,
        Keyword.HASTE, Keyword.INDESTRUCTIBLE, Keyword.HEXPROOF, Keyword.SHROUD, Keyword.WARD,
        Keyword.PROTECTION,
    )
}
