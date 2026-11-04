package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.NotCondition
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
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeTotalsEffect
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
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.GainControlEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.LockPlayerCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantFlashbackEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSpellOrPermanentToOwnersHandEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.HealDamageEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.filters.unified.Scope
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
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
     * Understood, but useful on either side: bounce can rescue our creature and blink can
     * remove their token. Keep both sides eligible and let simulation choose the target.
     */
    FLEXIBLE,

    /**
     * Mixed or unread. Every consumer keeps its pre-polarity behaviour on this, which is the
     * "no better than before, never confidently wrong" contract [CardIntentAnalyzer] keeps.
     */
    UNKNOWN,
}

/**
 * An Aura whose gift or curse turns on the creature type of what it enchants — Sea Legs' "+0/+2 as
 * long as it's a Pirate. Otherwise, it gets -2/-0", Gift of Fangs' Vampire, Bonds of Faith's Human.
 * Its enchant slot is [TargetPolarity.FLEXIBLE] as a whole, but each candidate has one side:
 * [whenIs] for a creature that has [subtype], [otherwise] for one that doesn't.
 */
data class HostSplit(val subtype: Subtype, val whenIs: TargetPolarity, val otherwise: TargetPolarity) {
    fun polarityFor(hostHasSubtype: Boolean): TargetPolarity = if (hostHasSubtype) whenIs else otherwise
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
 * A slot whose matched leaves all agree takes that polarity. A flexible step alongside other
 * understood steps remains flexible; opposing one-sided readings, any unread matched leaf, or
 * no matched leaf at all reads [TargetPolarity.UNKNOWN]. That makes the
 * classification table below an allow-list of *confident* readings, not a list of special cases:
 * an effect missing from it degrades to the old behaviour rather than to a wrong one.
 * A recognized two-sided effect reads [TargetPolarity.FLEXIBLE]: unlike UNKNOWN it is understood,
 * but neither polarity may prune candidates. Ranking retains the offensive default, bounded
 * simulation includes candidates from both sides, and optional slots may still be left empty.
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
 * An Aura whose only harm to its host is damage beside a lasting gift (Tilonalli's Crown's "deals 1
 * damage to enchanted creature" and "+3/+0 and trample") reads [TargetPolarity.FLEXIBLE]: a buff on
 * a creature that survives the damage, removal on one that doesn't, which the simulation compares.
 *
 * An Aura whose statics turn on the enchanted creature's type ("as long as it's a Pirate …
 * Otherwise, …") is read twice, once for a host of that type and once for any other host. Two
 * opposite one-sided readings make the slot [TargetPolarity.FLEXIBLE] and give it a [HostSplit],
 * which the target selector resolves per candidate; a world in which nothing applies ("+2/+2 as
 * long as it's a Vampire", no otherwise) reads as the other world.
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
    private val hostSplitCache = ConcurrentHashMap<Pair<String, Boolean>, List<HostSplit>>()
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
     * The per-candidate reading of an Aura whose enchant slot reads [TargetPolarity.FLEXIBLE]
     * because what it does turns on the host's creature type — see [HostSplit]. Null for every
     * other card.
     */
    fun hostSplit(card: CardDefinition, definitions: CardLookup = NO_DEFINITIONS): HostSplit? =
        hostSplitCache.getOrPut(card.name to (definitions !== NO_DEFINITIONS)) {
            val script = card.script
            if (card.cardFaces.isNotEmpty() || script.auraTarget == null || script.targetRequirements.isNotEmpty()) {
                return@getOrPut emptyList()
            }
            val subtype = hostSubtype(card) ?: return@getOrPut emptyList()
            val (whenIs, otherwise) = hostWorlds(card, definitions)
            val oneSided = setOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            if (whenIs in oneSided && otherwise in oneSided && whenIs != otherwise) {
                listOf(HostSplit(subtype, whenIs!!, otherwise!!))
            } else emptyList()
        }.singleOrNull()

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
        val blinks = EffectWalker.blinkMoves(effect)
        for (move in blinks) slotOf(move.target, requirements)?.let { slot ->
            // An unrestricted blink can remove their token or recover a stolen permanent.
            // Only an own-side requirement makes protection a safe target-side restriction.
            perSlot[slot] += if (isOwnSide(requirements[slot])) TargetPolarity.BENEFICIAL
                else TargetPolarity.FLEXIBLE
        }
        // Only an unconditional sequence proves the creature is ours before its follow-up gifts.
        // Do not flatten conditionals: a theft in one branch cannot justify a gift in another.
        val stolen = mutableSetOf<EffectTarget>()
        fun theftFollowUps(node: Effect): List<Effect> {
            val gifts = mutableListOf<Effect>()
            fun visit(part: Effect) {
                when (part) {
                    is com.wingedsheep.sdk.scripting.effects.CompositeEffect -> part.effects.forEach(::visit)
                    is GainControlEffect -> stolen += part.target
                    is TapUntapEffect -> if (!part.tap && part.target in stolen) gifts += part else stolen.clear()
                    is GrantKeywordEffect -> if (part.keyword == Keyword.HASTE.name && part.target in stolen) gifts += part else stolen.clear()
                    else -> stolen.clear()
                }
            }
            visit(node)
            return gifts
        }
        val theftGifts = theftFollowUps(effect)
        val biteSources = mutableSetOf<Int>()
        for (leaf in targetLeaves(effect)) {
            if (blinks.any { it === leaf } || theftGifts.any { it === leaf }) continue
            // "Destroy up to two target artifacts and/or enchantments" (Force of Vigor) runs its
            // body once per target, each seeing its target as `ContextTarget(0)`. With one slot
            // that is every target of that slot, so the body reads as the slot's; with several,
            // `ContextTarget(0)` walks all of them and names no one slot.
            if (leaf is ForEachEffect && leaf.space == IterationSpace.Targets) {
                if (requirements.size == 1) {
                    // A bounce can save our creature from removal even when the spell can
                    // choose several targets. Preserve the body's either-way reading so target
                    // simulation can compare rescue against offensive tempo.
                    perSlot[0] += forRequirements(leaf.body, requirements, definitions, source)
                }
                continue
            }
            // A gate another player decides (Clash of Elements: "its owner may put it on top of
            // their library … if they didn't, they put it on the bottom") is their call, so neither
            // branch alone is what the card does to a slot. What both branches do to it happens
            // either way, so a slot both read the same way reads that way.
            if (leaf is GatedEffect && leaf.decisionMaker != null) {
                val otherwise = leaf.otherwise ?: continue
                val taken = forRequirements(leaf.then, requirements, definitions, source)
                val declined = forRequirements(otherwise, requirements, definitions, source)
                for (slot in requirements.indices) {
                    val reading = taken[slot]
                    if (reading != TargetPolarity.UNKNOWN && reading == declined[slot]) perSlot[slot] += reading
                }
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
            // "One or two target creatures you control each deal damage equal to their power to
            // target creature an opponent controls" (Common Goal): the pipeline gathers its own
            // chosen targets and loops over them, so `IterationEntity` spans several slots and names
            // none. The body's other references keep their own slots (the victim); a bite drawing
            // its damage from the iterated creature lends the power of every own-side slot.
            if (space is IterationSpace.Collection && space.collection in chosenTargetCollections(effect)) {
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        slotOf(target, requirements)?.let { perSlot[it] += polarity }
                    }
                    if (inner is DealDamageEffect && inner.damageSource == EffectTarget.IterationEntity) {
                        biteSources += requirements.indices.filter { isOwnSide(requirements[it]) }
                    }
                }
                continue
            }
            // "Creatures target player controls get +3/+3" / "… deals 2 damage to each creature
            // target player controls" (the Lorwyn Eclipsed Commands): the group is that player's
            // creatures, so whatever the body does to each of them it does to the player's side.
            // The pump wants us, the sweep wants them. The body's other references keep their slots.
            val groupOwner = (space as? IterationSpace.Group)?.let { groupOwnerSlot(it, requirements) }
            if (groupOwner != null) {
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        val slot = if (target == EffectTarget.IterationEntity) groupOwner else slotOf(target, requirements)
                        slot?.let { perSlot[it] += polarity }
                    }
                }
                continue
            }
            if (leaf is GatherCardsEffect) {
                val top = leaf.source as? CardSource.TopOfLibrary
                if (top?.isMill == true) {
                    val slot = when (val player = top.player) {
                        Player.TargetPlayer, Player.TargetOpponent -> requirements.indices.filter {
                            requirements[it] is TargetPlayer || requirements[it] is TargetOpponent
                        }.singleOrNull()
                        is Player.ContextPlayer -> player.index.takeIf { it in requirements.indices }
                        is Player.BoundVariable -> requirements.indexOfFirst { it.id == player.name }
                            .takeIf { it >= 0 }
                        else -> null
                    }
                    slot?.let { perSlot[it] += TargetPolarity.HARMFUL }
                }
            }
            val recursion = recursionSlot(leaf, requirements)
            val copy = ownCopySlot(leaf, requirements)
            val gift = tokenGiftSlot(leaf, requirements)
            if (recursion != null) {
                recursions += recursion
            } else if (copy != null) {
                perSlot[copy] += TargetPolarity.BENEFICIAL
            } else if (gift != null) {
                perSlot[gift.first] += gift.second
            } else {
                for ((target, polarity) in classify(withRequirementZone(leaf, requirements), definitions, source)) {
                    val slot = slotOf(target, requirements) ?: continue
                    perSlot[slot] += polarity
                }
                biteSourceSlot(leaf, requirements)?.let { biteSources += it }
            }
            if (requirements.size == 1) implicitTargetPolarity(leaf)?.let { perSlot[0] += it }
        }
        // Getting a card back is the point of a recursion slot: a finality or -1/-1 counter it
        // comes back with (Rite of the Moth, Soulcoil Viper) is the price, not the other side of it.
        // A bite's source only lends its power while nothing else touches it: Self-Destruct deals
        // the damage back to it and Burn Together sacrifices it — a price, not a free lend. An
        // unread leaf counts too: it leaves no reading in the slot, but it still names it.
        val leaves = targetLeaves(effect)
        for (slot in ownerRecastSlots(effect, requirements)) perSlot[slot] += TargetPolarity.FLEXIBLE
        for (slot in lossSlots(leaves, requirements)) {
            if (TargetPolarity.BENEFICIAL !in perSlot[slot]) perSlot[slot] += TargetPolarity.HARMFUL
        }
        for (slot in largeBaseSlots(leaves, requirements)) {
            val alone = perSlot[slot].isEmpty() &&
                leaves.none { it !is SetBaseStatsEffect && touchesOtherwise(it, requirements[slot], slot, definitions, source) }
            if (alone) perSlot[slot] += TargetPolarity.FLEXIBLE
        }
        for (slot in biteSources) {
            val untouched = perSlot[slot].isEmpty() &&
                leaves.none { touchesOtherwise(it, requirements[slot], slot, definitions, source) }
            if (untouched) perSlot[slot] += TargetPolarity.BENEFICIAL
        }
        return perSlot.mapIndexed { slot, readings ->
            when {
                slot in recursions -> TargetPolarity.BENEFICIAL
                // Damage paid on our own creature alongside a gift is understood, but still
                // costs toughness. Simulation must compare survival instead of assuming a boon.
                isOwnSide(requirements[slot]) &&
                    readings.toSet() == setOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL) &&
                    leaves.filter { leaf -> classify(leaf, definitions, source).any { (target, polarity) ->
                        slotOf(target, requirements) == slot && polarity == TargetPolarity.HARMFUL
                    } }.let { harmful -> harmful.isNotEmpty() && harmful.all { it is DealDamageEffect } } ->
                    TargetPolarity.FLEXIBLE
                drawsForItsLife(slot, readings, leaves, requirements, definitions, source) -> TargetPolarity.BENEFICIAL
                else -> combine(readings)
            }
        }
    }

    /**
     * "Target player draws N cards and loses N life" (Sign in Blood, Vault Plunderer, Blood Pact):
     * the life is the price of the cards, as the discard is a loot's ([lossSlots]), so the slot is a
     * gift aimed at ourselves. Read two-sided, a triggered draw — scored with the ability still on
     * the stack, both seats alike — fell to the removal tie-break and handed the opponent the cards.
     * Only a draw beside nothing but life loss qualifies; any other harm keeps the slot mixed.
     */
    private fun drawsForItsLife(
        slot: Int,
        readings: List<TargetPolarity>,
        leaves: List<Effect>,
        requirements: List<TargetRequirement>,
        definitions: CardLookup,
        source: CardDefinition?,
    ): Boolean {
        if (readings.toSet() != setOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL)) return false
        val onSlot = leaves.mapNotNull { leaf ->
            classify(leaf, definitions, source)
                .filter { (target, _) -> slotOf(target, requirements) == slot }
                .takeIf { it.isNotEmpty() }
                ?.let { leaf to it.map { (_, polarity) -> polarity } }
        }
        val beneficial = onSlot.filter { (_, polarities) -> TargetPolarity.BENEFICIAL in polarities }.map { it.first }
        val harmful = onSlot.filter { (_, polarities) -> TargetPolarity.HARMFUL in polarities }.map { it.first }
        return beneficial.isNotEmpty() && beneficial.all { it is DrawCardsEffect } &&
            harmful.isNotEmpty() && harmful.all { it is LoseLifeEffect }
    }

    /**
     * [EffectWalker.payoffLeaves], except that a known two-way tap-or-untap choice stays one
     * operation instead of splitting into its mutually exclusive leaves.
     */
    private fun targetLeaves(effect: Effect): List<Effect> = EffectWalker.leaves(effect).flatMap { leaf ->
        when {
            tapOrUntapTargets(leaf).isNotEmpty() -> listOf(leaf)
            leaf is com.wingedsheep.sdk.scripting.effects.ConditionalOnCollectionEffect ->
                listOfNotNull(leaf.ifNotEmpty, leaf.ifEmpty).flatMap(::targetLeaves)
            leaf is com.wingedsheep.sdk.scripting.effects.RepeatDynamicTimesEffect -> targetLeaves(leaf.body)
            leaf is ChooseActionEffect && EffectWalker.choosesOwnBranch(leaf) ->
                leaf.choices.flatMap { targetLeaves(it.effect) }
            leaf !is com.wingedsheep.sdk.scripting.effects.GatedEffect || leaf.decisionMaker != null -> listOf(leaf)
            else -> listOfNotNull(leaf.then, leaf.otherwise).flatMap(::targetLeaves)
        }
    }

    /** A controller-chosen tap/untap pair can help either side; sequential tap then untap cannot. */
    private fun tapOrUntapTargets(effect: Effect): List<EffectTarget> {
        val branches = when (effect) {
            is ChooseActionEffect -> if (EffectWalker.choosesOwnBranch(effect)) effect.choices.map { it.effect } else return emptyList()
            is ModalEffect -> if (effect.chooseCount == 1 && !effect.chooseAtRandom && !effect.chooseAllIfBlightPaid &&
                effect.dynamicChooseCount == null && effect.modes.all { it.targetRequirements.isEmpty() })
                effect.modes.map { it.effect } else return emptyList()
            else -> return emptyList()
        }
        val taps = branches.map { it as? TapUntapEffect ?: return emptyList() }
        return if (taps.size == 2 && taps[0].target == taps[1].target && taps[0].tap != taps[1].tap)
            listOf(taps[0].target) else emptyList()
    }

    /** One polarity for a slot, from every leaf that acts on it. */
    private fun combine(readings: List<TargetPolarity>): TargetPolarity = when {
        // Flexible is a known two-sided operation, not an unread one. Adding a known boon or
        // harm still needs both sides compared; it cannot make the slot safely one-sided.
        // An unread step must stay unread, even beside an understood flexible operation.
        readings.isEmpty() || TargetPolarity.UNKNOWN in readings -> TargetPolarity.UNKNOWN
        TargetPolarity.FLEXIBLE in readings -> TargetPolarity.FLEXIBLE
        else -> readings.distinct().singleOrNull() ?: TargetPolarity.UNKNOWN
    }

    /** Whether two bound handles are picks of the same count-N requirement (`t0[0]` and `t0[1]`). */
    private fun sameRequirement(a: EffectTarget, b: EffectTarget): Boolean =
        a is EffectTarget.BoundVariable && b is EffectTarget.BoundVariable &&
            a.name.substringBefore('[') == b.name.substringBefore('[')

    /** Whether [this] names a chosen target rather than a fixed player or object. */
    private fun EffectTarget.isSlot(): Boolean = this is EffectTarget.BoundVariable || this is EffectTarget.ContextTarget

    private fun slotOf(target: EffectTarget, requirements: List<TargetRequirement>): Int? = when (target) {
        // A count-N requirement's handles are `id[0]`, `id[1]`, … (the DSL's destructured
        // `targets(TargetPlayer(count = 2))`): each one is a pick of that requirement's slot.
        is EffectTarget.BoundVariable -> target.name.substringBefore('[')
            .let { id -> requirements.indexOfFirst { it.id == id } }.takeIf { it >= 0 }
        is EffectTarget.ContextTarget -> target.index.takeIf { it in requirements.indices }
        else -> null
    }

    /**
     * The slot a **recursion** names — a card moved out of a graveyard into a hand or onto the
     * battlefield, or back into its library — or null.
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
        if (leaf.destination != Zone.HAND && leaf.destination != Zone.BATTLEFIELD && leaf.destination != Zone.LIBRARY) return null
        val slot = slotOf(leaf.target, requirements) ?: return null
        val filter = (requirements[slot] as? TargetObject)?.filter
        val fromGraveyard = leaf.fromZone == Zone.GRAVEYARD ||
            (leaf.fromZone == null && filter?.zone == Zone.GRAVEYARD)
        // Into a library is recursion only out of our own graveyard (Mortuary Mire); from any
        // graveyard it is graveyard hate (Junktroller, Sundering Archaic) that may hit either side.
        if (leaf.destination == Zone.LIBRARY && !ownGraveyardOnly(filter)) return null
        return slot.takeIf { fromGraveyard }
    }

    internal fun ownGraveyardOnly(filter: com.wingedsheep.sdk.scripting.filters.unified.TargetFilter?): Boolean =
        filter?.baseFilter?.controllerPredicate.let {
            it == ControllerPredicate.OwnedByYou || it == ControllerPredicate.ControlledByYou
        }

    /**
     * The player slot a **token for a target player** names — "target player creates a 1/1 …
     * creature token" (Emeritus of Truce, Verdant Command) — with its reading, or null.
     *
     * A creature is a body on its controller's side of the table, so a slot that may be either
     * player is [TargetPolarity.BENEFICIAL]: the token is ours to take. A slot only an opponent can
     * fill ("target opponent creates a 3/3 Dinosaur", Baffling End, Necromentia, the Hunted cycle)
     * is the card's price, not a side to pick — it reads [TargetPolarity.FLEXIBLE], which is
     * understood but prunes nothing. Read as a gift, it would veto casting a spell whose every legal
     * target is an opponent ([com.wingedsheep.ai.engine.TargetSelection.aimsOnlyAtTheWrongSide]).
     */
    private fun tokenGiftSlot(leaf: Effect, requirements: List<TargetRequirement>): Pair<Int, TargetPolarity>? {
        if (leaf !is CreateTokenEffect) return null
        val controller = leaf.controller?.takeIf { it.isSlot() } ?: return null
        val slot = slotOf(controller, requirements) ?: return null
        return slot to if (requirements[slot] is TargetOpponent) TargetPolarity.FLEXIBLE else TargetPolarity.BENEFICIAL
    }

    /**
     * The slot a **copy of our own permanent** names — "create a token that's a copy of target
     * creature you control" (Electroduplicate, Kiki-Jiki) — or null.
     *
     * A copy does nothing to the object it copies; it hands a second one to the effect's
     * controller. Where the slot's filter keeps it on that controller's side, the copied object's
     * controller is the one gaining it, so the slot reads beneficial and the best permanent is the
     * one to copy — unless the leaf hands the token to someone else ([CreateTokenCopyOfTargetEffect.controller]). Unread, the removal ranking ordered our own creatures worst first. A copy of
     * *any* creature stays unattributed: the best copy may be an opponent's, which is a board
     * question the two-sided simulation answers. Read off the requirement, not the leaf, as
     * [recursionSlot] does — the leaf names only the slot.
     */
    private fun ownCopySlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        // "Target player creates a copy" (Echocasting Symposium): the copy is not ours to gain.
        if (leaf !is CreateTokenCopyOfTargetEffect || leaf.controller != null) return null
        return ownSideSlot(leaf.target, requirements)
    }

    /**
     * The slot a one-sided **bite** draws its damage from — "target creature you control deals
     * damage equal to its power to target creature you don't control" (Rabid Bite, Chandra's
     * Ignition) — or null.
     *
     * The source only lends its power, so the best creature is the one to bite with and the slot
     * reads beneficial — when no other leaf names it ([forRequirements] applies that last).
     * Unread, the removal ranking ordered our own creatures worst first and bit with the weakest.
     * Only a source whose filter keeps it on our side: Breaking of the Fellowship turns an
     * opponent's creature on its teammate, where which one is a board question.
     *
     * A fight's fighter ([FightEffect.target1]: "target creature you control fights …", Prey Upon,
     * Yoshimaru) lends its power the same way. It is dealt damage back, but that is the fight's
     * own price, and the creature that hits hardest is also the one likeliest to survive it — the
     * weakest, which the removal ranking put first, dies for nothing. A fighter any player's
     * creature can be stays unread.
     */
    private fun biteSourceSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? = when (leaf) {
        is DealDamageEffect -> leaf.damageSource?.takeIf { it.isSlot() }?.let { ownSideSlot(it, requirements) }
        is FightEffect -> ownSideSlot(leaf.target1, requirements)
        else -> null
    }

    /**
     * The player slots whose **hand** or **permanents** a pipeline among [leaves] takes cards out
     * of — "target player discards two cards" (Mind Rot), "target opponent reveals their hand; you
     * choose a nonland card from it; exile that card" (Null Summoner), "target opponent sacrifices a
     * creature or planeswalker with the greatest mana value" (Break Under Pressure), Liliana of the
     * Veil's piles: a gather from the slot's hand, or from the permanents the slot controls, whose
     * cards, or the ones picked from among them, go to a graveyard, exile or a library. Losing cards
     * from hand or permanents from the battlefield is bad for the player who held them, so the slot
     * reads harmful.
     *
     * A slot that also draws keeps its draw's reading ([forRequirements] adds this only when it has
     * none): "target player draws three cards, then discards a card" is a loot aimed at ourselves,
     * and calling it two-sided would undo what the draw already reads right.
     */
    private fun lossSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> {
        val slotOfHand = HashMap<String, Int>()
        for (leaf in leaves) {
            if (leaf !is GatherCardsEffect) continue
            val player = when (val source = leaf.source) {
                is CardSource.FromZone -> source.player.takeIf { source.zone == Zone.HAND }
                is CardSource.ControlledPermanents -> source.player
                else -> null
            } ?: continue
            val slot = when (player) {
                is Player.ContextPlayer -> player.index.takeIf { it in requirements.indices }
                is Player.BoundVariable -> requirements.indexOfFirst { it.id == player.name }.takeIf { it >= 0 }
                Player.TargetPlayer, Player.TargetOpponent ->
                    requirements.indices.filter { requirements[it] is TargetPlayer || requirements[it] is TargetOpponent }.singleOrNull()
                else -> null
            } ?: continue
            slotOfHand[leaf.storeAs] = slot
        }
        if (slotOfHand.isEmpty()) return emptySet()
        do {
            val before = slotOfHand.size
            for (leaf in leaves) {
                for ((name, slot) in slotOfHand.toList()) {
                    CardIntentAnalyzer.derivedCollections(leaf, setOf(name)).forEach { slotOfHand.putIfAbsent(it, slot) }
                }
            }
        } while (slotOfHand.size != before)
        return leaves.mapNotNullTo(HashSet()) { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@mapNotNullTo null
            val zone = (move.destination as? CardDestination.ToZone)?.zone
            slotOfHand[move.from]?.takeIf { zone == Zone.GRAVEYARD || zone == Zone.EXILE || zone == Zone.LIBRARY }
        }
    }

    /**
     * Whether [leaf] does something to slot [index] that [classify] cannot read — any mention
     * other than a bite drawing its damage from it. A loop is read through its body.
     */
    private fun touchesOtherwise(
        leaf: Effect,
        requirement: TargetRequirement,
        index: Int,
        definitions: CardLookup,
        source: CardDefinition?,
    ): Boolean = when {
        leaf is ForEachEffect -> EffectWalker.payoffLeaves(leaf.body)
            .any { touchesOtherwise(it, requirement, index, definitions, source) }
        leaf is DealDamageEffect -> namesSlot(leaf.target.toString(), requirement, index)
        else -> classify(leaf, definitions, source).isEmpty() && namesSlot(leaf.toString(), requirement, index)
    }

    /** Whether an effect's rendered form names slot [index] (the reading audit's own test). */
    private fun namesSlot(text: String, requirement: TargetRequirement, index: Int): Boolean =
        (requirement.id != null && "BoundVariable(name=${requirement.id})" in text) ||
            "ContextTarget(index=$index)" in text

    /**
     * The target-player slot whose permanents [space] iterates — "each creature target player
     * controls" — or null. An explicit reference names its slot; the implicit "target player"
     * predicate is the spell's player target, read only when there is exactly one.
     */
    private fun groupOwnerSlot(space: IterationSpace.Group, requirements: List<TargetRequirement>): Int? {
        val filter = space.filter.baseFilter
        if (filter.anyOf.isNotEmpty()) return null
        return when (val controller = filter.controllerPredicate) {
            is ControllerPredicate.ControlledByReferencedPlayer -> slotOf(controller.target, requirements)
                ?.takeIf { (requirements[it] is TargetPlayer || requirements[it] is TargetOpponent) }
            ControllerPredicate.ControlledByTargetPlayer, ControllerPredicate.ControlledByTargetOpponent ->
                requirements.indices.filter { (requirements[it] is TargetPlayer || requirements[it] is TargetOpponent) }.singleOrNull()
            else -> null
        }
    }

    /** The slot [target] names when its requirement's filter keeps it on our side of the table. */
    private fun ownSideSlot(target: EffectTarget, requirements: List<TargetRequirement>): Int? {
        val slot = slotOf(target, requirements) ?: return null
        return slot.takeIf { isOwnSide(requirements[slot]) }
    }

    /** The declared slot confines this target to our own permanents. */
    internal fun targetsOwnSide(target: EffectTarget, requirements: List<TargetRequirement>): Boolean =
        slotOf(target, requirements)?.let { isOwnSide(requirements[it]) } == true

    /** Whether [requirement]'s filter keeps its target on our side of the table ("… you control"). */
    internal fun isOwnSide(requirement: TargetRequirement): Boolean {
        val filter = (((requirement as? TargetOther)?.baseRequirement ?: requirement) as? TargetObject)?.filter
            ?: return false
        return filter.alternatives.isEmpty() &&
            filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou
    }

    /**
     * A move onto a library that leaves its starting zone unsaid, with that zone filled in from its
     * target's requirement: `Effects.PutOnTopOfLibrary(target)` over "target nonland permanent" is a
     * tuck off the battlefield, over "target card in your graveyard" a recursion. Only a library move
     * is filled in: a move to exile or a graveyard already reads as removal from any zone. A tuck
     * kept on our own side ("put target creature you control on top of its owner's library",
     * Sunscape Apprentice) is a re-buy, not removal, and stays unread.
     */
    private fun withRequirementZone(leaf: Effect, requirements: List<TargetRequirement>): Effect {
        if (leaf !is MoveToZoneEffect || leaf.fromZone != null || leaf.destination != Zone.LIBRARY) return leaf
        val slot = slotOf(leaf.target, requirements) ?: return leaf
        if (isOwnSide(requirements[slot])) return leaf
        val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
        val zone = (requirement as? TargetObject)?.filter?.zone ?: return leaf
        return leaf.copy(fromZone = zone)
    }

    /** Whether [requirement]'s filter keeps its target on an opponent's side ("… an opponent controls"). */
    private fun isOpponentSide(requirement: TargetRequirement): Boolean {
        val filter = (((requirement as? TargetOther)?.baseRequirement ?: requirement) as? TargetObject)?.filter
            ?: return false
        return filter.alternatives.isEmpty() &&
            filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByOpponent
    }

    /**
     * The pipeline collections of [effect] that hold its own chosen targets: gathered from
     * [CardSource.ChosenTargets], or filtered out of such a collection.
     */
    private fun chosenTargetCollections(effect: Effect): Set<String> {
        val leaves = EffectWalker.leaves(effect)
        val names = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { it.source == CardSource.ChosenTargets }.mapTo(HashSet()) { it.storeAs }
        val filters = leaves.filterIsInstance<FilterCollectionEffect>()
        do {
            val grown = filters.filter { it.from in names }
                .flatMap { listOfNotNull(it.storeMatching, it.storeNonMatching) }
        } while (names.addAll(grown))
        return names
    }

    /**
     * Whether [effect] exiles its own chosen targets and lets each card's **owner** cast it from
     * exile again — Airbend ("its owner may cast it for {2}"), Release to the Wind ("its owner may
     * cast it without paying its mana cost"), Suspend Aggression. The recast is a bounce that skips
     * the hand: tempo removal on their permanent, a save or an ETB re-buy on ours. A grant to the
     * caster instead ("exile target … you may cast that card", Abstruse Appropriation) is a steal,
     * not this, and stays unread. Riders on the same cards (a mana-cost waiver, a fixed or taxed
     * recast cost) only change the price of the recast.
     */
    internal fun ownerRecastsChosenTargets(effect: Effect): Boolean {
        val leaves = EffectWalker.payoffLeaves(effect)
        val chosen = chosenTargetCollections(effect)
        val exiled = leaves.filterIsInstance<MoveCollectionEffect>()
            .filter { it.from in chosen && (it.destination as? CardDestination.ToZone)?.zone == Zone.EXILE }
            .flatMapTo(HashSet()) { listOfNotNull(it.from, it.storeMovedAs) }
        return exiled.isNotEmpty() && leaves.any {
            it is GrantMayPlayFromExileEffect && it.ownerControls && it.from in exiled
        }
    }

    /**
     * The slots an [ownerRecastsChosenTargets] pipeline acts on: every permanent slot, read
     * [TargetPolarity.FLEXIBLE] as a bounce is ([classify]'s move to hand). A slot whose card sits in
     * another zone (a graveyard card exiled beside it) is not bounced, and a player slot is no card.
     */
    private fun ownerRecastSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        if (!ownerRecastsChosenTargets(effect)) return emptyList()
        return requirements.indices.filter { slot ->
            val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
            when (requirement) {
                is TargetPlayer, is TargetOpponent -> false
                is TargetObject -> requirement.filter.zone.let { it == null || it == Zone.BATTLEFIELD }
                else -> true
            }
        }
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
        val tapChoices = tapOrUntapTargets(leaf)
        if (tapChoices.isNotEmpty()) return tapChoices.map { it to TargetPolarity.FLEXIBLE }
        // Counters moved or removed whatever their kind — "move all counters from The Ozolith onto
        // target creature", "put its counters on target creature you control", "remove a counter
        // from target permanent" (Etched Slith): the counters carry the meaning, and they are only
        // known on the board. +1/+1, loyalty or keyword counters are a gift where they land and a
        // loss where they leave; -1/-1, stun or time counters the reverse. Neither side is fixed,
        // so both stay open and the simulation weighs the counters each candidate holds.
        if (leaf is MoveAllCountersEffect) {
            return listOf(leaf.source to TargetPolarity.FLEXIBLE, leaf.destination to TargetPolarity.FLEXIBLE)
        }
        val polarity: Pair<EffectTarget, TargetPolarity?> = when (leaf) {
            // Off the battlefield to a graveyard or exile is removal, and so is a tuck onto a
            // library (Expel from Orazca's "on top of its owner's library instead", Chaos Warp): it
            // costs its owner the draw that gets it back, a worse bounce. A tuck counts only when the
            // move is known to start on the battlefield ([withRequirementZone], or the permanent an
            // Aura enchants: Dramatic Accusation's "shuffle enchanted creature into its owner's
            // library") — "put target card
            // from your graveyard on top of your library" (Academy Ruins) is a recursion. To hand is
            // flexible: bouncing your own creature in response to removal is a real play
            // (`lastchance-05`). A move onto the battlefield or out of a graveyard is
            // recursion, whose polarity is about the card's owner rather than a controller.
            is MoveToZoneEffect -> leaf.target to when {
                leaf.fromZone != null && leaf.fromZone != Zone.BATTLEFIELD -> null
                leaf.destination == Zone.GRAVEYARD || leaf.destination == Zone.EXILE -> TargetPolarity.HARMFUL
                leaf.destination == Zone.LIBRARY &&
                    (leaf.fromZone == Zone.BATTLEFIELD || leaf.target in HOST_TARGETS) -> TargetPolarity.HARMFUL
                leaf.destination == Zone.HAND -> TargetPolarity.FLEXIBLE
                else -> null
            }
            is MoveUntilSourceLeavesEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Return target spell or creature to its owner's hand" (Fatehold Charm, Unsubstantiate):
            // the bounce above, reaching the stack too — it rescues our creature from removal as
            // readily as it sets back their spell or blocker.
            is ReturnSpellOrPermanentToOwnersHandEffect -> leaf.target to TargetPolarity.FLEXIBLE
            // "Its owner puts it on their choice of the top or bottom of their library" (Dire
            // Downdraft, Aether Gust, Plan for All Outcomes): the owner picks the position, so at
            // best they draw it again in place of a fresh card — the tuck above, a worse bounce.
            is PutOnLibraryPositionOfChoiceEffect -> leaf.target to TargetPolarity.HARMFUL
            is ExileUntilLeavesEffect -> leaf.target to TargetPolarity.HARMFUL
            is DealDamageEffect -> leaf.target to TargetPolarity.HARMFUL
            is GainControlEffect -> leaf.target to TargetPolarity.HARMFUL
            is CantBlockEffect -> leaf.target to TargetPolarity.HARMFUL
            // Forcing a block can trade the target into a blocker or draw blockers away from its
            // teammates. Neither the one-blocker nor the all-blockers form fixes a target side;
            // keep both sides available to simulation, including beside a pump (Emergent Growth).
            is MustBeBlockedEffect -> leaf.target to TargetPolarity.FLEXIBLE
            is CantBeRegeneratedEffect -> leaf.target to TargetPolarity.HARMFUL
            // "That player can't get counters" (Suncleanser): a prohibition on the player — the
            // energy, experience and other counters they build toward stop coming. Locking out
            // poison would help them, but the cards aim it at an opponent to shut off their engine.
            is LockPlayerCountersEffect -> leaf.target to TargetPolarity.HARMFUL
            is MarkExileOnDeathEffect -> leaf.target to TargetPolarity.HARMFUL
            is TapUntapEffect -> leaf.target to
                if (leaf.tap) TargetPolarity.HARMFUL else TargetPolarity.BENEFICIAL
            is ModifyStatsEffect -> leaf.target to statsPolarity(leaf.powerModifier, leaf.toughnessModifier)
            // "Target creature has base power and toughness 0/0" (Multiply by Zero) is removal. A
            // larger fixed base names nothing here, so the slot's other effects decide it (Taranika's
            // 4/4 + indestructible), and alone it reads flexible ([largeBaseSlots]). A dynamic base (Exuberant Wolfbear copies its own power) can
            // grow or shrink the target, so both sides stay open.
            is SetBaseStatsEffect -> leaf.target to (baseStatsPolarity(leaf.power, leaf.toughness)
                ?: (if (listOfNotNull(leaf.power, leaf.toughness).any { it !is DynamicAmount.Fixed })
                    TargetPolarity.FLEXIBLE else return emptyList()))
            // "<fighter> fights target creature": the creature fought is dealt damage, the victim's
            // half of the fight. The fighter's half names nothing here: its power is the weapon and
            // the damage dealt back is the price. A fighter kept on our side reads like a bite's
            // source ([biteSourceSlot]).
            is FightEffect -> leaf.target2 to TargetPolarity.HARMFUL
            is RegenerateEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is HealDamageEffect -> leaf.target to TargetPolarity.BENEFICIAL
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
            // "Target creature you control explores" (Enter the Unknown): the explorer gets a +1/+1
            // counter, or its controller the revealed land in hand — either way a gift to that side.
            is ExploreEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is AdaptAsThoughNoCountersEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is AddDynamicCountersEffect -> leaf.target to counterPolarity(leaf.counterType)
            is MoveAllLastKnownCountersEffect -> leaf.target to TargetPolarity.FLEXIBLE
            is RemoveAnyNumberOfCountersEffect -> leaf.target to TargetPolarity.FLEXIBLE
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
            // "If a triggered ability of that creature triggers, it triggers an additional time"
            // (Lwaxana Troi, Charming Guest): a doubler only ever repeats its holder's controller's
            // own triggers, so it is aimed at a creature of yours.
            is GrantStaticAbilityEffect -> if (leaf.ability is AdditionalSourceTriggers) leaf.target to TargetPolarity.BENEFICIAL
                else return emptyList()
            // Only a creature with a prepare spell can become prepared, and its controller is the
            // one who may cast that spell (Hexhaven Dueling Arena); unpreparing takes it away.
            is BecomePreparedEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is UnprepareEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Target instant or sorcery card in your graveyard gains flashback": its owner may cast
            // it again, so the best card is the one to give it to — a recursion slot.
            is GrantFlashbackEffect -> leaf.target to TargetPolarity.BENEFICIAL
            // "Target land gains '{T}: Add {C}{C}'": the mana is its controller's to spend. Other
            // granted abilities do whatever their effect does and stay unread here.
            is GrantActivatedAbilityEffect -> if (leaf.ability.isManaAbility) leaf.target to TargetPolarity.BENEFICIAL
                else return emptyList()
            // "Target player draws / gains / loses …" (Inspiration, Sign in Blood). Only a slot is
            // read here: a draw for "you" is not a target, and [grantedTriggerPolarity] reads it
            // through [controllerPolarity] instead. Sign in Blood's draw and life loss disagree
            // and combine to UNKNOWN, which is right — it is aimed at either side.
            is DrawCardsEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.BENEFICIAL else return emptyList()
            is GainLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.BENEFICIAL else return emptyList()
            is LoseLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Target player can't cast spells this turn" (Orim's Chant, Xanathar): a ban is only ever
            // worth putting on someone else.
            is CantCastSpellsEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Target player sacrifices a creature" (Diabolic Edict, Liliana of the Veil's −2): an
            // edict costs the player it names a permanent, whichever one they pick.
            is ForceSacrificeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Exchange life totals with target player" (Psychic Transfer, Mister Negative): swapping
            // with yourself does nothing, so the slot is the opponent the controller trades with.
            // "Two target players exchange life totals" only pays its controller off when they are
            // one of the two, so the player side is theirs and the other side an opponent's. When
            // both sides are picks of one count-2 requirement (Axis of Mortality's `t0[0]`/`t0[1]`)
            // they share a slot, and it reads as the player side: the controller picks itself first.
            is ExchangeLifeTotalsEffect -> return when {
                leaf.player.isSlot() && leaf.target.isSlot() ->
                    if (sameRequirement(leaf.player, leaf.target)) listOf(leaf.player to TargetPolarity.BENEFICIAL)
                    else listOf(leaf.player to TargetPolarity.BENEFICIAL, leaf.target to TargetPolarity.HARMFUL)
                leaf.player.isSlot() -> listOf(leaf.player to TargetPolarity.BENEFICIAL)
                leaf.target.isSlot() -> listOf(leaf.target to TargetPolarity.HARMFUL)
                else -> emptyList()
            }
            // "Look at target player's hand" reveals what it holds — information only worth having
            // about someone else. Beside a "that player draws" (Vendilion Clique, Oildeep Gearhulk)
            // the two disagree and the slot stays two-sided, as Sign in Blood's does.
            is LookAtTargetHandEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Target opponent reveals their hand" (Null Summoner, Solve for Disappointment): the
            // same information, shown to everyone — worth having only about someone else.
            is RevealHandEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
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
     * The slots a fixed base-P/T change larger than [baseStatsPolarity]'s curse line names. Such a
     * base grows a small creature and shrinks a big one (Square Up's 4/4, Perfected Theory's 4/5),
     * so on its own the slot reads [TargetPolarity.FLEXIBLE] and the simulation compares the
     * sides. Beside an effect that reads the slot one way (Taranika's indestructible) that effect
     * decides, which is why [classify] leaves the leaf unattributed and this is applied last.
     */
    private fun largeBaseSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> =
        leaves.filterIsInstance<SetBaseStatsEffect>()
            .filter { leaf ->
                val set = listOfNotNull(leaf.power, leaf.toughness)
                set.isNotEmpty() && set.all { it is DynamicAmount.Fixed } && baseStatsPolarity(leaf.power, leaf.toughness) == null
            }
            .mapNotNullTo(HashSet()) { slotOf(it.target, requirements) }

    /**
     * A base-P/T change by what it is set to: a 1/1 or smaller shrinks nearly anything it lands on
     * — the same line [staticPolarity] draws for the Cursed Role's static. A larger fixed base is a
     * gift or a curse depending on the creature ([largeBaseSlots]), and a dynamic one depends on the
     * board, so neither has a side here. A side left null is unchanged.
     */
    private fun baseStatsPolarity(power: DynamicAmount?, toughness: DynamicAmount?): TargetPolarity? {
        val set = listOfNotNull(power, toughness)
        if (set.isEmpty()) return null
        val small = set.all { (it as? DynamicAmount.Fixed)?.amount?.let { amount -> amount <= 1 } == true }
        // A base with no power left and little toughness (Sorceress Queen's and Rowan's 0/2) takes
        // away everything the creature does in combat but block a 2/2 — the same curse.
        val toothless = (power as? DynamicAmount.Fixed)?.amount == 0 &&
            (toughness as? DynamicAmount.Fixed)?.amount?.let { it <= 2 } == true
        return if (small || toothless) TargetPolarity.HARMFUL else null
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
    fun counterPolarity(type: CounterType): TargetPolarity? {
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
    ) + CardIntentAnalyzer.MARKED_FOR_REMOVAL_COUNTERS

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
        val reading = hostReading(card, definitions)
        // "Enchant creature an opponent controls" (Dead Man's Chest):
        // only an opponent's permanent can wear it, so whatever it does pays its controller off
        // from the opponent's side of the table. With nothing on the host to read (Dead Man's
        // Chest's whole card is a dies trigger for its controller) the enchant slot is aimed at an
        // opponent's creature, the removal ranking's side.
        val aura = card.script.auraTarget
        // Only when nothing at all reads the host — an UNKNOWN from an unread host effect
        // (Honest Work, Relic Bind) stays a gap rather than hiding behind the enchant restriction.
        if (reading == TargetPolarity.UNKNOWN && aura != null && isOpponentSide(aura) &&
            attachmentReadings(card, definitions, hostIs = null).isEmpty()
        ) return TargetPolarity.HARMFUL
        return reading
    }

    private fun hostReading(card: CardDefinition, definitions: CardLookup): TargetPolarity {
        if (hostSubtype(card) == null) {
            val readings = attachmentReadings(card, definitions, hostIs = null)
            // "When this Aura enters, it deals 1 damage to enchanted creature. Enchanted creature
            // gets +3/+0 and has trample" (Tilonalli's Crown): the lasting gift is the point on a
            // creature that survives the damage, and the damage is the point on one that doesn't —
            // their X/1 dies with the Crown on it. Both sides are understood, so the slot is
            // FLEXIBLE and the simulation, which sees who survives, picks the host. Any other harm
            // (a -N/-N, a "can't block") still makes the gift a contradiction and stays unread.
            if (readings.toSet() == setOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL) && hostHarmIsOnlyDamage(card, definitions)) {
                return TargetPolarity.FLEXIBLE
            }
            return combine(readings)
        }
        // Read once per kind of host; a world where nothing applies takes the other's reading.
        val (whenIs, otherwise) = hostWorlds(card, definitions)
        return when {
            whenIs == null || otherwise == null -> whenIs ?: otherwise ?: TargetPolarity.UNKNOWN
            whenIs == otherwise -> whenIs
            TargetPolarity.UNKNOWN in listOf(whenIs, otherwise) -> TargetPolarity.UNKNOWN
            else -> TargetPolarity.FLEXIBLE
        }
    }

    /**
     * [card]'s attachment reading for a host of its [hostSubtype], and for any other host — null
     * for a world in which none of its abilities applies.
     */
    private fun hostWorlds(card: CardDefinition, definitions: CardLookup): Pair<TargetPolarity?, TargetPolarity?> {
        fun world(hostIs: Boolean) =
            attachmentReadings(card, definitions, hostIs).takeIf { it.isNotEmpty() }?.let(::combine)
        return world(hostIs = true) to world(hostIs = false)
    }

    /**
     * The one creature type every conditional static of [card] turns on — "as long as enchanted
     * creature is a Pirate" or its "otherwise" — or null when it has none, or any conditional
     * static reads something else.
     */
    private fun hostSubtype(card: CardDefinition): Subtype? {
        val conditional = card.script.staticAbilities.filterIsInstance<ConditionalStaticAbility>()
        if (conditional.isEmpty()) return null
        val subtypes = conditional.map { hostSubtypeOf(it.condition) ?: return null }
        return subtypes.distinct().singleOrNull()
    }

    private fun hostSubtypeOf(condition: Condition): Subtype? = when (condition) {
        is EnchantedCreatureHasSubtype -> condition.subtype
        is NotCondition -> (condition.condition as? EnchantedCreatureHasSubtype)?.subtype
        else -> null
    }

    /**
     * Every reading [attachmentPolarity] combines. With [hostIs] set, a static gated on the host's
     * creature type ([hostSubtype]) counts only in the world where its condition holds.
     */
    private fun attachmentReadings(card: CardDefinition, definitions: CardLookup, hostIs: Boolean?): List<TargetPolarity> {
        val readings = mutableListOf<TargetPolarity>()
        for (static in card.script.staticAbilities) {
            if (hostIs != null && static is ConditionalStaticAbility) {
                val holds = when (static.condition) {
                    is NotCondition -> !hostIs
                    else -> hostIs
                }
                if (!holds) continue
            }
            staticPolarity(static)?.let(readings::add)
        }
        val abilityEffects = card.script.triggeredAbilities.map { it.effect } +
            card.script.activatedAbilities.map { it.effect }
        for (effect in abilityEffects) {
            // "Enchanted creature" and "enchanted permanent" (Flood the Engine's "tap
            // enchanted permanent") both name the object the Aura is cast on.
            val hostReadings = EffectWalker.payoffLeaves(effect).flatMap { leaf ->
                classify(leaf, definitions).filter { (target, _) -> target in HOST_TARGETS }.map { it.second }
            }
            // "+1/-1 or -1/+1", its controller's pick on resolution, serves whichever side the
            // host is on: branches that don't add up to one side are a choice, not a veto on the
            // other abilities.
            if (choosesOwnBranchSomewhere(effect) && combine(hostReadings) == TargetPolarity.UNKNOWN) continue
            readings += hostReadings
        }
        // A replacement effect on an Aura (Sandskin's "prevent all combat damage dealt to and by
        // enchanted creature") is read nowhere above, and is often the whole card. Without it the
        // statics alone could call an either-way Aura one-sided, so its presence is a veto — except
        // for an entry choice (Traveler's Cloak's land type), which only parameterises a static.
        // A self-only zone redirect (disturb's "if this would be put into a graveyard, exile it
        // instead") moves the Aura itself and never touches the creature, so it is no veto either.
        if (card.script.replacementEffects.any { !it.affectsOnlyTheAuraItself() }) readings += TargetPolarity.UNKNOWN
        return readings
    }

    /**
     * Whether every harm [card] does to its host is damage from one of its triggered or activated
     * abilities — no static of it reads as a curse, and no harmful host leaf is anything but a
     * [DealDamageEffect].
     */
    private fun hostHarmIsOnlyDamage(card: CardDefinition, definitions: CardLookup): Boolean {
        if (card.script.staticAbilities.any { staticPolarity(it) == TargetPolarity.HARMFUL }) return false
        val harmful = (card.script.triggeredAbilities.map { it.effect } + card.script.activatedAbilities.map { it.effect })
            .flatMap(EffectWalker::payoffLeaves)
            .filter { leaf ->
                classify(leaf, definitions).any { (target, polarity) -> target in HOST_TARGETS && polarity == TargetPolarity.HARMFUL }
            }
        return harmful.isNotEmpty() && harmful.all { it is DealDamageEffect }
    }

    private fun choosesOwnBranchSomewhere(effect: Effect): Boolean =
        EffectWalker.leaves(effect).any { it is ChooseActionEffect && EffectWalker.choosesOwnBranch(it) }

    private fun ReplacementEffect.affectsOnlyTheAuraItself(): Boolean = when (this) {
        is EntersWithChoice -> true
        is RedirectZoneChange -> selfOnly
        is RedirectZoneChangeWith -> selfOnly
        else -> false
    }

    /**
     * What a triggered ability granted to the enchanted creature does for that creature's
     * controller, who controls the granted ability: its leaves aimed at the creature itself
     * ([EffectTarget.Self] inside the quotes) read like any other slot, and a card draw, life
     * gain or token for its controller is a benefit. A target the granted ability names is its
     * controller's to pick — Spare Dagger's "deals 1 damage to any target" goes where they aim it —
     * so a slot reads as a benefit too, unless its filter keeps it on their own side and the leaf
     * hurts it ("sacrifice target creature you control" is a price). A granted ability with no
     * readable leaf, or with any leaf this cannot call, reads [TargetPolarity.UNKNOWN] —
     * "enchanted creature has '…'" can be a curse as easily as a gift.
     */
    internal fun grantedTriggerPolarity(ability: TriggeredAbility): TargetPolarity =
        grantedEffectPolarity(ability.effect, listOfNotNull(ability.targetRequirement))

    private fun grantedEffectPolarity(effect: Effect, requirements: List<TargetRequirement>): TargetPolarity {
        val readings = mutableListOf<TargetPolarity>()
        for (leaf in EffectWalker.payoffLeaves(effect)) {
            // "You may sacrifice Spare Dagger. When you do, it deals 1 damage to any target": the
            // optional action is a price the controller opts into, the reflexive effect the payoff.
            if (leaf is ReflexiveTriggerEffect && leaf.optional) {
                readings += grantedEffectPolarity(leaf.reflexiveEffect, leaf.reflexiveTargetRequirements)
                continue
            }
            // "Whenever this creature attacks, it gains your choice of trample or deathtouch"
            // (Hunter's Axe): the trigger's controller picks the mode, so it does what its modes
            // do — when they agree. Each mode's slots are its own.
            if (leaf is ModalEffect) {
                readings += leaf.modes.map { mode ->
                    grantedEffectPolarity(mode.effect, mode.targetRequirements)
                }
                continue
            }
            val classified = classify(leaf)
            val reading = when {
                classified.isNotEmpty() -> classified.map { (target, polarity) ->
                    when {
                        target == EffectTarget.Self -> polarity
                        target.isSlot() -> controllersPick(target, polarity, requirements)
                        else -> TargetPolarity.UNKNOWN
                    }
                }
                else -> listOf(controllerPolarity(leaf) ?: TargetPolarity.UNKNOWN)
            }
            readings += reading
        }
        return combine(readings)
    }

    /**
     * A slot of an ability its controller resolves: they aim a harmful leaf at someone else and a
     * helpful one at their own, so either way it serves them — except a harmful leaf whose filter
     * allows only their own objects. A slot whose requirement isn't in [requirements] (a reflexive
     * trigger's own target), or isn't an object filter ("any target"), has no such filter to keep
     * it home.
     */
    private fun controllersPick(
        target: EffectTarget,
        polarity: TargetPolarity,
        requirements: List<TargetRequirement>,
    ): TargetPolarity {
        if (polarity != TargetPolarity.HARMFUL) return TargetPolarity.BENEFICIAL
        // "Another target creature you control" keeps the same side as "target creature you control".
        val requirement = slotOf(target, requirements)?.let { requirements[it] }
            ?.let { (it as? TargetOther)?.baseRequirement ?: it } ?: return TargetPolarity.BENEFICIAL
        val filter = (requirement as? TargetObject)?.filter ?: return TargetPolarity.BENEFICIAL
        val ownSideOnly = filter.alternatives.isEmpty() &&
            filter.baseFilter.controllerPredicate == ControllerPredicate.ControlledByYou
        return if (ownSideOnly) TargetPolarity.HARMFUL else TargetPolarity.BENEFICIAL
    }

    /** A player-facing leaf's polarity for the ability's controller, when it is aimed at them. */
    private fun controllerPolarity(leaf: Effect): TargetPolarity? {
        fun isController(target: EffectTarget) =
            target == EffectTarget.Controller || target == EffectTarget.PlayerRef(Player.You)
        return when (leaf) {
            is DrawCardsEffect -> if (isController(leaf.target)) TargetPolarity.BENEFICIAL else null
            is GainLifeEffect -> if (isController(leaf.target)) TargetPolarity.BENEFICIAL else null
            is LoseLifeEffect -> if (isController(leaf.target)) TargetPolarity.HARMFUL else null
            // A token with no other controller named is the ability controller's (Ceremonial
            // Knife's Blood, Infantry Shield's mobilized Warriors).
            is CreateTokenEffect -> if (leaf.controller.let { it == null || isController(it) }) TargetPolarity.BENEFICIAL else null
            is CreatePredefinedTokenEffect -> if (leaf.controller.let { it == null || isController(it) }) TargetPolarity.BENEFICIAL else null
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
