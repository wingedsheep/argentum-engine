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
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTriggeringObjectTargetsEffect
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeTotalsEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.StaticAbility
import com.wingedsheep.sdk.scripting.effects.AdaptAsThoughNoCountersEffect
import com.wingedsheep.sdk.scripting.effects.AdaptEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect
import com.wingedsheep.sdk.scripting.effects.CounterKindSource
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.AddDynamicCountersEffect
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect
import com.wingedsheep.sdk.scripting.effects.BecomePreparedEffect
import com.wingedsheep.sdk.scripting.effects.UnprepareEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DividedDamageEffect
import com.wingedsheep.sdk.scripting.effects.DistributeCountersAmongTargetsEffect
import com.wingedsheep.sdk.scripting.effects.DoubleCountersEffect
import com.wingedsheep.sdk.scripting.effects.SkipUntapEffect
import com.wingedsheep.sdk.scripting.effects.EachPermanentBecomesCopyOfTargetEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ExileTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.ExileUntilLeavesEffect
import com.wingedsheep.sdk.scripting.effects.ExploreEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeControlEffect
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
import com.wingedsheep.sdk.scripting.effects.MarkMustBlockThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.GatedEffect
import com.wingedsheep.sdk.scripting.effects.ForceSacrificeEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveAllLastKnownCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveCountersEachKindMissingEffect
import com.wingedsheep.sdk.scripting.effects.MoveChosenCountersToTargetEffect
import com.wingedsheep.sdk.scripting.effects.MoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.PutOnLibraryPositionOfChoiceEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSpellOrPermanentToOwnersHandEffect
import com.wingedsheep.sdk.scripting.effects.ReturnSpellToOwnersHandEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllAbilitiesEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAnyNumberOfCountersEffect
import com.wingedsheep.sdk.scripting.effects.RemoveAllCountersOfTypeEffect
import com.wingedsheep.sdk.scripting.effects.RemoveCountersEffect
import com.wingedsheep.sdk.scripting.effects.MoveUntilSourceLeavesEffect
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.ProliferateEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.HealDamageEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
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
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent
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
 * handle); an [EffectTarget.ContextTarget] matches by position. Three leaves act on "the target"
 * without naming it — a [CounterEffect], [ExileTargetSpellEffect] or [ChangeTargetEffect] whose spell
 * is the chosen one — and are attributed only when there is exactly one requirement for them to mean.
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
     * The slots of [requirements] whose card [effect] takes out of **any** graveyard and puts onto
     * the battlefield under our control — "put target creature card from a graveyard onto the
     * battlefield under your control" (Rise from the Grave), or Boneyard Parley's chosen targets,
     * however its piles split them. Such a card is ours once the effect resolves, whoever owns it,
     * so a [TargetPolarity.BENEFICIAL] reading of the slot ("good for the card's owner") must not
     * keep it on its owner's side: an opponent's best creature card is the best one to name. A card
     * we cast out of an opponent's graveyard ([castGraveyardSlots], Tinybones) is taken the same way.
     *
     * A move that leaves the card under its owner's control (a return to its owner's hand, a
     * reanimation without "under your control"), or one only out of our own graveyard, takes
     * nothing; every slot of an unreadable effect is left out.
     */
    fun takenSlots(effect: Effect?, requirements: List<TargetRequirement>): Set<Int> {
        if (effect == null || requirements.isEmpty()) return emptySet()
        if (CardIntentAnalyzer.reanimatesChosenGraveyardTargets(effect, requirements)) {
            return requirements.indices.filterTo(HashSet()) { !ownGraveyardOnly(objectFilter(requirements[it])) }
        }
        // A card we cast out of a graveyard is ours on the stack and on the battlefield (Tinybones).
        castGraveyardSlots(effect, requirements).filterNot { ownGraveyardOnly(objectFilter(requirements[it])) }
            .takeIf { it.isNotEmpty() }?.let { return it.toSet() }
        return EffectWalker.leaves(effect).mapNotNullTo(HashSet()) { leaf ->
            val move = leaf as? MoveToZoneEffect ?: return@mapNotNullTo null
            if (move.destination != Zone.BATTLEFIELD || move.faceDown != null) return@mapNotNullTo null
            if (move.controllerOverride != EffectTarget.Controller) return@mapNotNullTo null
            val slot = slotOf(move.target, requirements) ?: return@mapNotNullTo null
            val filter = objectFilter(requirements[slot])
            val fromGraveyard = move.fromZone == Zone.GRAVEYARD || (move.fromZone == null && filter?.zone == Zone.GRAVEYARD)
            slot.takeIf { fromGraveyard && !ownGraveyardOnly(filter) }
        }
    }

    /** [takenSlots] of [card] cast as a spell; empty for a card with faces, as [forSpell] is. */
    fun takenSlotsForSpell(card: CardDefinition): Set<Int> =
        if (card.cardFaces.isNotEmpty()) emptySet()
        else takenSlots(card.script.spellEffect, card.script.targetRequirements)

    private fun objectFilter(requirement: TargetRequirement) =
        (((requirement as? TargetOther)?.baseRequirement ?: requirement) as? TargetObject)?.filter

    /**
     * The graveyard slots of [requirements] whose card [effect] lets us cast or play — "you may cast
     * target Zombie creature card from your graveyard this turn" (Zul Ashur), "you may cast target
     * instant card from your graveyard without paying its mana cost" (Torrential Gearhulk), "… from
     * that player's graveyard" (Tinybones): a pipeline that gathers its chosen targets and casts them
     * on resolution ([CastFromCollectionWithoutPayingCostEffect], paid or free) or grants the cast
     * ([GrantMayPlayFromExileEffect]). The card named is the one we get to cast — recursion through
     * the stack — so the best card is the one to name, whoever's graveyard holds it. A grant to the
     * card's owner (airbend's [GrantMayPlayFromExileEffect.ownerControls]) is not ours, and a gather
     * that mixes in a battlefield slot names no graveyard card alone, so both read nothing here.
     * Like [CardIntentAnalyzer.reanimatesChosenGraveyardTargets], a cast of only a selected or
     * filtered part of the chosen cards still reads every graveyard slot: each is a card we may cast.
     */
    internal fun castGraveyardSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        val slots = objectSlots(requirements)
        if (slots.isEmpty() || slots.any { objectFilter(requirements[it])?.zone != Zone.GRAVEYARD }) return emptyList()
        // The cast is often the action of its own "if you do" (Ogre Battlecaster), which the plain walk stops at.
        val leaves = CardIntentAnalyzer.gateInteriorLeaves(effect)
        val chosen = allChosenTargets(effect).toMutableSet()
        if (chosen.isEmpty()) return emptyList()
        do {
            val before = chosen.size
            for (leaf in leaves) chosen += CardIntentAnalyzer.derivedCollections(leaf, chosen)
        } while (chosen.size != before)
        val casts = leaves.any { leaf ->
            (leaf is CastFromCollectionWithoutPayingCostEffect && leaf.from in chosen) ||
                (leaf is GrantMayPlayFromExileEffect && !leaf.ownerControls && leaf.from in chosen)
        }
        return if (casts) slots else emptyList()
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
        val stored = storedAmounts(effect)
        val blinks = EffectWalker.blinkMoves(effect) + EffectWalker.delayedBlinks(targetLeaves(effect))
        for (move in blinks.filterIsInstance<MoveToZoneEffect>()) slotOf(move.target, requirements)?.let { slot ->
            // An unrestricted blink can remove their token or recover a stolen permanent.
            // Only an own-side requirement makes protection a safe target-side restriction.
            perSlot[slot] += if (isOwnSide(requirements[slot])) TargetPolarity.BENEFICIAL
                else TargetPolarity.FLEXIBLE
        }
        // Only an unconditional sequence proves the creature is ours before its follow-up gifts.
        // Do not flatten conditionals: a theft in one branch cannot justify a gift in another.
        val stolenOnto = mutableSetOf<EffectTarget>()
        val stolen = mutableSetOf<EffectTarget>()
        fun theftFollowUps(node: Effect): List<Effect> {
            val gifts = mutableListOf<Effect>()
            fun visit(part: Effect) {
                when (part) {
                    is com.wingedsheep.sdk.scripting.effects.CompositeEffect -> {
                        // "Untap target creature and gain control of it until end of turn"
                        // (Threaten) untaps before it steals: an unconditional theft anywhere in
                        // the sequence makes the whole sequence's untap and haste part of it.
                        stolen += part.effects.filterIsInstance<GainControlEffect>().map { it.target }
                        part.effects.forEach(::visit)
                    }
                    is GainControlEffect -> stolen += part.target
                    is TapUntapEffect -> if (!part.tap && part.target in stolen) gifts += part else stolen.clear()
                    // Haste to attack with it, and the trample or lifelink some thefts add (Traitorous
                    // Blood, Zidane) — every keyword but a drawback goes to the creature we took.
                    is GrantKeywordEffect -> if (part.keyword != Keyword.DEFENDER.name && part.target in stolen) gifts += part else stolen.clear()
                    // "If that creature is a Goat, it also gets +3/+0 until end of turn" (Goatnap):
                    // a boost to the creature we took is ours to attack with, part of the theft.
                    is ModifyStatsEffect ->
                        if (part.target in stolen && raisesOnly(part)) gifts += part else stolen.clear()
                    // "You may attach an Equipment you control to that creature" (Unexpected Request):
                    // the Equipment rides the stolen creature into the attack, part of the theft.
                    // "If that artifact is an Equipment, attach it to this creature" (Thieving
                    // Skydiver) moves the stolen Equipment itself: the theft's own follow-up too.
                    is AttachTargetEquipmentToCreatureEffect -> when {
                        part.creatureTarget in stolen -> { gifts += part; stolenOnto += part.creatureTarget }
                        part.equipmentTarget in stolen -> gifts += part
                        else -> stolen.clear()
                    }
                    // A gate nobody else decides, with nothing on its other branch, runs its follow-ups
                    // on the theft before it; a theft inside it justifies nothing after the gate.
                    is com.wingedsheep.sdk.scripting.effects.GatedEffect -> {
                        if (part.decisionMaker == null && part.otherwise == null) visit(part.then)
                        stolen.clear()
                    }
                    else -> stolen.clear()
                }
            }
            visit(node)
            return gifts
        }
        val theftGifts = theftFollowUps(effect)
        val biteSources = mutableSetOf<Int>()
        // What the effect does to "its controller" — the controller of the first target.
        val controllerReadings = mutableListOf<TargetPolarity>()
        // "That player shuffles, then draws a card for each card exiled from their hand this way"
        // (Lost Legacy): the refund is the price of the hand stripped, not a gift that would aim
        // the spell at us. [lossSlots] reads the slot.
        val refunds = CardIntentAnalyzer.handRefundDraws(effect)
        // "Target creature blocks this turn if able. You may have it gain reach until end of turn"
        // (Academic Dispute): the reach lets the forced blocker meet our flier — part of dragging it
        // into the block, not a gift to it.
        val forcedBlockers = targetLeaves(effect).filterIsInstance<MarkMustBlockThisTurnEffect>().mapTo(HashSet()) { it.target }
        for (leaf in targetLeaves(effect)) {
            if (blinks.any { it === leaf }) continue
            if (leaf is GrantKeywordEffect && leaf.keyword == Keyword.REACH.name && leaf.target in forcedBlockers) continue
            if (leaf is DrawCardsEffect && leaf in refunds) continue
            if (theftGifts.any { it === leaf }) {
                // Stolen Uniform attaches the Equipment it took to a creature of ours: the Equipment
                // slot reads as the theft, the creature slot still as the gift it is.
                if (leaf is AttachTargetEquipmentToCreatureEffect && leaf.creatureTarget !in stolenOnto) {
                    slotOf(leaf.creatureTarget, requirements)?.let { perSlot[it] += TargetPolarity.BENEFICIAL }
                }
                continue
            }
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
                // A gate the targeted player decides themselves — the punisher: "target opponent may
                // have you draw three cards. If the player doesn't, … deals damage to that player"
                // (Combustible Gearhulk, Risk Factor, Palantír of Orthanc) — has one branch about
                // that player and one about us. The branch that says nothing of the slot is the
                // price they let us have, so the slot reads as the branch that does.
                val decider = leaf.decisionMaker?.let { slotOf(it, requirements) }
                for (slot in requirements.indices) {
                    val reading = taken[slot]
                    if (reading != TargetPolarity.UNKNOWN && reading == declined[slot]) perSlot[slot] += reading
                    else if (slot == decider && (reading == TargetPolarity.UNKNOWN) != (declined[slot] == TargetPolarity.UNKNOWN)) {
                        perSlot[slot] += if (reading == TargetPolarity.UNKNOWN) declined[slot] else reading
                    }
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
            // A loop over a collection the pipeline gathered without targeting — "attach any number
            // of Equipment you control to target creature you control" (Beatrix, Loyal General) —
            // visits untargeted objects as `IterationEntity`, which names no slot either; the body's
            // other references (the creature) keep theirs.
            // "For each kind of counter on target permanent, put another counter of that kind on it
            // or remove one from it" (Dramatist's Puppet, Quarry Hauler): which kinds are there is
            // only known on resolution, and each is ours to grow or strip — more +1/+1 or loyalty on
            // our own, a -1/-1 or stun taken off it, the reverse on theirs. Either side, as bounce is.
            if (space is IterationSpace.CounterKindsOn && readsPerCounterKind((leaf as ForEachEffect).body)) {
                slotOf(space.entity, requirements)?.let { perSlot[it] += TargetPolarity.FLEXIBLE }
                continue
            }
            if (space is IterationSpace.Collection) {
                val chosenTargets = space.collection in chosenTargetCollections(effect)
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        slotOf(target, requirements)?.let { perSlot[it] += polarity }
                    }
                    if (chosenTargets && inner is DealDamageEffect && inner.damageSource == EffectTarget.IterationEntity) {
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
                if (top != null) {
                    val slot = when (val player = top.player) {
                        Player.TargetPlayer, Player.TargetOpponent -> requirements.indices.filter {
                            requirements[it] is TargetPlayer || requirements[it] is TargetOpponent
                        }.singleOrNull()
                        is Player.ContextPlayer -> player.index.takeIf { it in requirements.indices }
                        is Player.BoundVariable -> requirements.indexOfFirst { it.id == player.name }
                            .takeIf { it >= 0 }
                        else -> null
                    }
                    val reading = if (top.isMill) TargetPolarity.HARMFUL else libraryTopPolarity(leaf.storeAs, effect)
                    if (reading != null) slot?.let { perSlot[it] += reading }
                }
            }
            val offenceWatch = offenceWatchSlot(leaf, requirements)
            if (offenceWatch != null) {
                perSlot[offenceWatch] += TargetPolarity.BENEFICIAL
                continue
            }
            val recursion = recursionSlot(leaf, requirements)
            val copy = ownCopySlot(leaf, requirements)
            val gift = tokenGiftSlot(leaf, requirements)
            val exchange = exchangeSlots(leaf, requirements)
            // A card exiled or tucked out of a graveyard that may be an opponent's (Scavenging Ooze,
            // Junktroller): graveyard hate, aimed at the card a recursion would want back. Exiled to
            // be used (The Scarab God's token copy, Dimir Doppelganger) it is the best card on
            // either side: the slot reads as the recursion it is.
            val graveyardSlot = (leaf as? MoveToZoneEffect)
                ?.takeIf { CardIntentAnalyzer.graveyardMoveOf(it, requirements) == CardIntentAnalyzer.GraveyardMove.HATE }
                ?.let { slotOf(it.target, requirements) }
            val reused = graveyardSlot != null && CardIntentAnalyzer.usesTheExiledCard(EffectWalker.payoffLeaves(effect))
            val hate = graveyardSlot.takeIf { !reused }
            if (reused) recursions += graveyardSlot!!
            if (recursion != null) {
                recursions += recursion
            } else if (hate != null) {
                perSlot[hate] += TargetPolarity.HARMFUL
            } else if (copy != null) {
                perSlot[copy] += TargetPolarity.BENEFICIAL
                // What becomes the copy of our best permanent is still the board's question.
                if (leaf is EachPermanentBecomesCopyOfTargetEffect) {
                    leaf.affected?.let { slotOf(it, requirements) }?.let { perSlot[it] += TargetPolarity.FLEXIBLE }
                }
            } else if (gift != null) {
                perSlot[gift.first] += gift.second
            } else if (exchange != null) {
                for ((slot, polarity) in exchange) perSlot[slot] += polarity
            } else {
                for ((target, polarity) in classify(withStoredNumbers(withRequirementZone(leaf, requirements), stored), definitions, source)) {
                    if (target == EffectTarget.TargetController) controllerReadings += polarity
                    val slot = slotOf(target, requirements) ?: continue
                    perSlot[slot] += polarity
                }
                biteSourceSlot(leaf, requirements)?.let { biteSources += it }
            }
            if (requirements.size == 1) implicitTargetPolarity(leaf)?.let { perSlot[0] += it }
            // "Untap up to two target creatures and up to two target lands" (Nissa, Genesis Mage)
            // gathers every chosen target and taps or untaps the lot: each object slot gets what the
            // single-target [TapUntapEffect] gives its one — an untap helps, a tap hurts.
            if (leaf is TapUntapCollectionEffect && leaf.collectionName in allChosenTargets(effect)) {
                val polarity = if (leaf.tap) TargetPolarity.HARMFUL else TargetPolarity.BENEFICIAL
                for (slot in objectSlots(requirements)) perSlot[slot] += polarity
            }
            // "You may destroy another target creature. If a creature is destroyed this way, you gain
            // life equal to its toughness" (Noxious Gearhulk) gathers its chosen targets and moves
            // them to a graveyard or exile itself, to track what left: the single-target destroy or
            // exile it is, on every permanent slot. A blink that brings them back is not this.
            if (leaf is MoveCollectionEffect && removesChosenTargets(leaf, effect)) {
                for (slot in permanentSlots(requirements)) perSlot[slot] += TargetPolarity.HARMFUL
            }
            // "Return any number of target nonland, nontoken permanents you control to their owners'
            // hands. Draw a card for each card returned to your hand this way" (Paradoxical Outcome)
            // gathers its chosen targets and bounces the lot, to count what reached a hand: the
            // single-target bounce ([classify]'s move to hand) on every permanent slot. Even kept on
            // our own side it stays two-sided: a save and an ETB re-buy, but one paid for in tempo,
            // so which of ours are worth the card is the simulation's question, not "our best first".
            if (leaf is MoveCollectionEffect && leaf.from in allChosenTargets(effect) &&
                (leaf.destination as? CardDestination.ToZone)?.zone == Zone.HAND
            ) {
                for (slot in permanentSlots(requirements)) perSlot[slot] += TargetPolarity.FLEXIBLE
            }
            // "N damage divided as you choose among …" across several requirements (the single-slot
            // case is [implicitTargetPolarity]'s): damage to every target of the requirement it is
            // scoped to (Magma Opus's `among`), or to every target of the ability when unscoped.
            if (leaf is DividedDamageEffect && requirements.size > 1) {
                val scoped = leaf.among?.let { id -> requirements.indices.filter { requirements[it].id == id } }
                for (slot in scoped ?: requirements.indices.toList()) perSlot[slot] += TargetPolarity.HARMFUL
            }
            // "Distribute N counters among …" names no target either: the counter it divides is what
            // each target gets (Biogenic Upgrade's +1/+1, Armament Corps' +1/+1 among ours).
            if (leaf is DistributeCountersAmongTargetsEffect) {
                counterPolarity(leaf.counterType)?.let { polarity -> requirements.indices.forEach { perSlot[it] += polarity } }
            }
        }
        // Getting a card back is the point of a recursion slot: a finality or -1/-1 counter it
        // comes back with (Rite of the Moth, Soulcoil Viper) is the price, not the other side of it.
        // A bite's source only lends its power while nothing else touches it: Self-Destruct deals
        // the damage back to it and Burn Together sacrifices it — a price, not a free lend. An
        // unread leaf counts too: it leaves no reading in the slot, but it still names it.
        val leaves = targetLeaves(effect)
        // "Deals damage to target spell's controller equal to that spell's mana value" (Refuse),
        // "that creature deals damage equal to its power to its controller" (Backlash): harm aimed
        // at the first target's controller is harm to that object's side, so the object to name is
        // an opponent's. Only when nothing else reads the slot and the harm is all there is — a
        // gift to the controller beside removal ("its controller gains life") is the price.
        if (perSlot[0].isEmpty() && controllerReadings.isNotEmpty() &&
            controllerReadings.all { it == TargetPolarity.HARMFUL } &&
            requirements[0].let { it !is TargetPlayer && it !is TargetOpponent }
        ) perSlot[0] += TargetPolarity.HARMFUL
        // Graveyard cards a pipeline brings onto our battlefield, however it splits them first
        // (Boneyard Parley's piles): the best creature card in any graveyard is the one to name.
        if (CardIntentAnalyzer.reanimatesChosenGraveyardTargets(effect, requirements)) recursions += requirements.indices
        // Graveyard cards a pipeline exiles to use — "exile X target creature cards from your
        // graveyard; for each, create a token that's a copy of it" (Hour of Eternity): the single-card
        // exile-and-copy above (The Scarab God), as a pipeline. The best card is the one to name.
        if (CardIntentAnalyzer.exilesChosenGraveyardTargetsToUse(effect, requirements)) recursions += requirements.indices
        // Graveyard cards a pipeline lets us cast (Zul Ashur, Torrential Gearhulk): recursion by way of the stack.
        recursions += castGraveyardSlots(effect, requirements)
        for (slot in ownerRecastSlots(effect, requirements)) perSlot[slot] += TargetPolarity.FLEXIBLE
        // "Exile up to two target creatures you control, then return those cards to the battlefield"
        // (Illusionist's Stratagem) as a pipeline: the blink [EffectWalker.blinkMoves] reads in its
        // single-move spelling — a save and an ETB re-buy on ours, either way on anyone's.
        for (slot in pipelineBlinkSlots(effect, requirements)) {
            perSlot[slot] += if (isOwnSide(requirements[slot])) TargetPolarity.BENEFICIAL else TargetPolarity.FLEXIBLE
        }
        // "Exile target nonland card from your graveyard. Search your library for any number of cards
        // with the same name as that card … put them into your hand" (Secret Salvage): a tutor keyed on
        // a card of ours, so the slot is our own pick — the name with copies left to find.
        for (slot in namedTutorSlots(effect, requirements)) perSlot[slot] += TargetPolarity.BENEFICIAL
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
        // Equip reaches only its controller's creatures (CR 702.6a), so even an Equipment whose bonus
        // can't be read one-sidedly — Blade of Shared Souls' copy trigger, a mixed +N/-N — is a
        // question of which of our creatures wears it best: open for the simulation, which sees each
        // candidate wearing it. Only when nothing else reads the slot, so a sibling leaf's reading wins.
        for (equip in leaves.filterIsInstance<AttachEquipmentEffect>()) {
            val slot = slotOf(equip.target, requirements) ?: continue
            if (source != null && perSlot[slot].isEmpty() && isOwnSide(requirements[slot])) {
                perSlot[slot] += TargetPolarity.FLEXIBLE
            }
        }
        // "Remove all counters from target creature" (Perfect Intimidation) strips an opponent's
        // +1/+1 counters or frees our creature of its -1/-1 counters (blight): which side it helps is
        // the counters on the board, so both stay open for the simulation. Only when nothing else
        // reads the slot: Suncleanser's "target opponent loses all counters" is read by its lock.
        for (strip in leaves.filterIsInstance<RemoveAllCountersEffect>()) {
            val slot = slotOf(strip.target, requirements) ?: continue
            if (perSlot[slot].isEmpty()) perSlot[slot] += TargetPolarity.FLEXIBLE
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
                // "Loses all abilities. Put a flying, first strike and lifelink counter on it"
                // (Abigale): a vanilla creature only gains, a bomb mostly loses — the trade is the
                // creature's own abilities, which only the board shows, so both sides stay open.
                readings.toSet() == setOf(TargetPolarity.HARMFUL, TargetPolarity.BENEFICIAL) &&
                    leaves.filter { leaf -> classify(leaf, definitions, source).any { (target, polarity) ->
                        slotOf(target, requirements) == slot && polarity == TargetPolarity.HARMFUL
                    } }.let { harmful -> harmful.isNotEmpty() && harmful.all { it is RemoveAllAbilitiesEffect } } ->
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

    /** A stat change that only adds: "+3/+0", never "+2/-1" or an unread "+X". */
    private fun raisesOnly(effect: ModifyStatsEffect): Boolean {
        val signs = listOf(AmountSign.of(effect.powerModifier), AmountSign.of(effect.toughnessModifier))
        return AmountSign.GAIN in signs && signs.all { it == AmountSign.GAIN || it == AmountSign.ZERO }
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
            // "Target creature gains your choice of double strike or lifelink" (Practiced Offense,
            // Manifold Mouse): modes that declare no targets of their own all act on a slot the
            // ability declared, so whichever is chosen, the slot reads as the modes agree — the
            // controller-chosen branch shape above.
            leaf is ModalEffect && sharesAbilityTargets(leaf) -> leaf.modes.flatMap { targetLeaves(it.effect) }
            leaf !is com.wingedsheep.sdk.scripting.effects.GatedEffect || leaf.decisionMaker != null -> listOf(leaf)
            else -> listOfNotNull(leaf.then, leaf.otherwise).flatMap(::targetLeaves)
        }
    }

    /**
     * The punisher on a chosen player — "target opponent loses 3 life unless they sacrifice a
     * nonland permanent or discard a card" — written as an unless-clause that player pays
     * ([PayOrSufferEffect], Starseer Mentor) or as a choice of branches that player makes
     * ([ChooseActionEffect], Thornplate Intimidator). Every way out costs them something: a payment
     * is a loss, so the slot is harmful whenever what they suffer for not paying is; a branch choice
     * is harmful when every branch read on that player hurts them (an unread branch — a discard
     * pipeline — doesn't make it a gift). Null when the shape isn't one, or its reading isn't known.
     */
    private fun punisherReading(
        leaf: Effect,
        definitions: CardLookup,
        source: CardDefinition?,
    ): List<Pair<EffectTarget, TargetPolarity>>? {
        fun readings(effect: Effect) = targetLeaves(effect).flatMap { classify(it, definitions, source) }
        fun hurts(on: List<Pair<EffectTarget, TargetPolarity>>, player: EffectTarget) =
            on.filter { it.first == player }.let { mine -> mine.isNotEmpty() && mine.all { it.second == TargetPolarity.HARMFUL } }
        return when {
            leaf is PayOrSufferEffect && leaf.player.isSlot() ->
                readings(leaf.suffer).takeIf { hurts(it, leaf.player) }
            leaf is ChooseActionEffect && !EffectWalker.choosesOwnBranch(leaf) && leaf.player.isSlot() ->
                leaf.choices.flatMap { readings(it.effect) }.takeIf { hurts(it, leaf.player) }
            else -> null
        }
    }

    /** A choose-one the controller makes whose modes declare no targets: they all act on the ability's own slots. */
    private fun sharesAbilityTargets(effect: ModalEffect): Boolean =
        effect.chooseCount == 1 && !effect.chooseAtRandom && !effect.chooseAllIfBlightPaid &&
            effect.dynamicChooseCount == null && effect.modes.all { it.targetRequirements.isEmpty() }

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

    /**
     * A body over [IterationSpace.CounterKindsOn] that only adds or removes one counter of the
     * iterated kind — or lets its controller choose between the two. Shared with
     * [CardIntentAnalyzer], which reads it as the counter manipulation it is.
     */
    internal fun readsPerCounterKind(body: Effect): Boolean {
        // [targetLeaves] opens a choice its controller makes onto every branch.
        val leaves = targetLeaves(body)
        return leaves.isNotEmpty() && leaves.all {
            (it is com.wingedsheep.sdk.scripting.effects.AddCountersOfChosenKindEffect &&
                it.kindFrom == com.wingedsheep.sdk.scripting.effects.CounterKindSource.ITERATION) ||
                it is com.wingedsheep.sdk.scripting.effects.RemoveCountersOfIteratedKindEffect
        }
    }

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
     * The slots an **exchange of control** names — "exchange control of target artifact you control
     * and target artifact or creature you don't control" (Shrewd Negotiation, Chromeshell Crab,
     * Spawnbroker) — with their readings, or null.
     *
     * The permanent taken from a side only an opponent's permanent can fill is a steal,
     * [TargetPolarity.HARMFUL], as [GainControlEffect] is: their best is the one to take. The
     * permanent handed over is the price of it, not a side to pick, so it reads
     * [TargetPolarity.FLEXIBLE] — understood, but pruning nothing — and the removal ranking still
     * gives away our worst. Read harmful, a slot only our own permanents fill would veto the cast
     * ([com.wingedsheep.ai.engine.TargetSelection.aimsOnlyAtTheWrongSide]). An exchange of two
     * permanents either side may hold (Switcheroo's "two target creatures") is flexible on both.
     */
    private fun exchangeSlots(leaf: Effect, requirements: List<TargetRequirement>): List<Pair<Int, TargetPolarity>>? {
        if (leaf !is ExchangeControlEffect) return null
        return listOf(leaf.target1, leaf.target2).mapNotNull { target ->
            val slot = slotOf(target, requirements) ?: return@mapNotNull null
            slot to if (theirsOnly(requirements[slot])) TargetPolarity.HARMFUL else TargetPolarity.FLEXIBLE
        }
    }

    /** Whether [requirement]'s filter keeps its target off our side ("… you don't control", "… an opponent controls"). */
    private fun theirsOnly(requirement: TargetRequirement): Boolean {
        val filter = objectFilter(requirement) ?: return false
        return filter.alternatives.isEmpty() && CardIntentAnalyzer.opponentsOnly(filter.baseFilter.controllerPredicate)
    }

    /**
     * The slot a **copy of our own permanent** names — "create a token that's a copy of target
     * creature you control" (Electroduplicate, Kiki-Jiki) — or null.
     *
     * A copy does nothing to the object it copies; it hands a second one to the effect's
     * controller. Where the slot's filter keeps it on that controller's side, the copied object's
     * controller is the one gaining it, so the slot reads beneficial and the best permanent is the
     * one to copy — unless the leaf hands the token to someone else ([CreateTokenCopyOfTargetEffect.controller]). Unread, the removal ranking ordered our own creatures worst first. A copy of
     * *any* creature reads [TargetPolarity.FLEXIBLE] in [classify]: the best copy may be an
     * opponent's, which is a board question the two-sided simulation answers. Read off the requirement, not the leaf, as
     * [recursionSlot] does — the leaf names only the slot.
     */
    private fun ownCopySlot(leaf: Effect, requirements: List<TargetRequirement>): Int? = when {
        // "Target player creates a copy" (Echocasting Symposium): the copy is not ours to gain.
        leaf is CreateTokenCopyOfTargetEffect && leaf.controller == null -> ownSideSlot(leaf.target, requirements)
        // "Equipped creature becomes a copy of up to one other target creature you control" (Blade of
        // Shared Souls), "each other creature you control becomes a copy of target creature you
        // control" (Naga Fleshcrafter): the same gain without a token — the best of ours to copy.
        leaf is EachPermanentBecomesCopyOfTargetEffect -> ownSideSlot(leaf.target, requirements)
        else -> null
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
     * Veil's piles, "target opponent exiles a creature they control" (Debt to the Kami, Doomfall —
     * a gather from that player's battlefield zone): a gather from the slot's hand, or from the permanents the slot controls, whose
     * cards, or the ones picked from among them, go to a graveyard, exile or a library. Losing cards
     * from hand or permanents from the battlefield is bad for the player who held them, so the slot
     * reads harmful.
     *
     * A slot's **graveyard** exiled — "exile target player's graveyard" (Tormod's Crypt, Bojuka
     * Bog) — is the same loss: the cards a recursion or flashback would have used. Only exile
     * counts there; a graveyard shuffled back into its library (Elixir of Immortality's kin) is
     * a refill, not hate.
     *
     * A slot that also draws keeps its draw's reading ([forRequirements] adds this only when it has
     * none): "target player draws three cards, then discards a card" is a loot aimed at ourselves,
     * and calling it two-sided would undo what the draw already reads right.
     */
    private fun lossSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> {
        val slotOfHand = HashMap<String, Int>()
        val graveyards = HashSet<String>()
        for (leaf in leaves) {
            if (leaf !is GatherCardsEffect) continue
            val player = when (val source = leaf.source) {
                is CardSource.FromZone -> source.player.takeIf {
                    source.zone == Zone.HAND || source.zone == Zone.GRAVEYARD || source.zone == Zone.BATTLEFIELD
                }
                is CardSource.ControlledPermanents -> source.player
                // "Search target player's graveyard, hand, and library for all cards with that name
                // and exile them" (Cranial Extraction, Dispossess): the hand and graveyard it reaches
                // are that player's to lose.
                is CardSource.FromMultipleZones -> source.player.takeIf { Zone.HAND in source.zones || Zone.GRAVEYARD in source.zones }
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
            if ((leaf.source as? CardSource.FromZone)?.zone == Zone.GRAVEYARD) graveyards += leaf.storeAs
        }
        if (slotOfHand.isEmpty()) return emptySet()
        do {
            val before = slotOfHand.size
            for (leaf in leaves) {
                for ((name, slot) in slotOfHand.toList()) {
                    CardIntentAnalyzer.derivedCollections(leaf, setOf(name)).forEach {
                        if (slotOfHand.putIfAbsent(it, slot) == null && name in graveyards) graveyards += it
                    }
                }
            }
        } while (slotOfHand.size != before)
        return leaves.mapNotNullTo(HashSet()) { leaf ->
            // "Target opponent blights 2" (Champion of the Weird): a -1/-1 counter on a creature
            // that player controls is theirs to lose, as a sacrifice from among them would be.
            if (leaf is AddCountersToCollectionEffect) {
                return@mapNotNullTo slotOfHand[leaf.collectionName]
                    ?.takeIf { counterPolarity(leaf.counterType) == TargetPolarity.HARMFUL }
            }
            val move = leaf as? MoveCollectionEffect ?: return@mapNotNullTo null
            val zone = (move.destination as? CardDestination.ToZone)?.zone
            slotOfHand[move.from]?.takeIf {
                // Exiled to be cast (Mnemonic Betrayal's kin) is a theft, not a loss to read here.
                if (move.from in graveyards) zone == Zone.EXILE && !CardIntentAnalyzer.usesTheExiledCard(leaves)
                else zone == Zone.GRAVEYARD || zone == Zone.EXILE || zone == Zone.LIBRARY
            }
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

    /**
     * Whether [target] is a chosen slot our own permanents can fill — any slot not kept on an
     * opponent's side ("target creature", "another target permanent"). A beneficial leaf on such a
     * slot is aimed at our side, as one confined there ([targetsOwnSide]) is.
     */
    internal fun ownSideCanFill(target: EffectTarget, requirements: List<TargetRequirement>): Boolean =
        slotOf(target, requirements)?.let { slot ->
            val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
            requirement is TargetObject && !isOpponentSide(requirement)
        } == true

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
        // "Put target spell or nonland permanent into its owner's library second from the top"
        // (Commit): off the battlefield it is the tuck above, and off the stack the spell never
        // resolves — a counterspell that hands the card back a draw later. Both are removal.
        if (requirement is TargetSpellOrPermanent) return leaf.copy(fromZone = Zone.BATTLEFIELD)
        val zone = (requirement as? TargetObject)?.filter?.zone ?: return leaf
        return leaf.copy(fromZone = zone)
    }

    /**
     * [leaf] with a "+X/+X" whose X is a number the pipeline stored earlier read as that number's
     * own amount: "the chosen creatures get +X/+X, where X is the difference between their powers"
     * (Spry and Mighty) stores X once and pumps by it, and a bare variable reads as either sign.
     * Through arithmetic too: "-1/-1 for each {E} paid this way" (Die Young) is the negated count
     * of energy paid — see [storedAmounts].
     */
    private fun withStoredNumbers(leaf: Effect, stored: Map<String, DynamicAmount>): Effect {
        if (leaf !is ModifyStatsEffect || stored.isEmpty()) return leaf
        return leaf.copy(
            powerModifier = resolveStored(leaf.powerModifier, stored),
            toughnessModifier = resolveStored(leaf.toughnessModifier, stored),
        )
    }

    /**
     * What the cards gathered off a target player's library top into [collection] do to that player,
     * when the gather is no mill of its own: "target player mills five cards, then puts each Goblin
     * card milled this way into their hand" (Grub's Command), "exile the top two cards of target
     * opponent's library" (Maralen, Fae Ascendant). Cards that only leave for a graveyard, exile or
     * our hand are their loss, [TargetPolarity.HARMFUL]; some of them handed back to their own hand
     * makes it [TargetPolarity.FLEXIBLE] — which library it is worth digging depends on the cards.
     * Null when nothing moves them out (a look, a reorder).
     */
    private fun libraryTopPolarity(collection: String, effect: Effect): TargetPolarity? {
        val leaves = EffectWalker.leaves(effect)
        val names = hashSetOf(collection)
        do {
            val before = names.size
            for (leaf in leaves) names += CardIntentAnalyzer.derivedCollections(leaf, names)
        } while (names.size != before)
        var taken = false
        var handedBack = false
        for (move in leaves.filterIsInstance<MoveCollectionEffect>()) {
            if (move.from !in names) continue
            val to = move.destination as? CardDestination.ToZone ?: continue
            when {
                to.zone == Zone.GRAVEYARD || to.zone == Zone.EXILE -> taken = true
                to.zone == Zone.HAND && to.player == Player.You -> taken = true
                to.zone == Zone.HAND -> handedBack = true
            }
        }
        return when {
            taken && handedBack -> TargetPolarity.FLEXIBLE
            taken -> TargetPolarity.HARMFUL
            else -> null
        }
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
     * The pipeline collections of [effect] gathered straight from [CardSource.ChosenTargets] — every
     * chosen target of every slot, unnarrowed. (A filtered one, in [chosenTargetCollections], may
     * hold only some slots' targets, so it names no slot by itself.)
     */
    internal fun allChosenTargets(effect: Effect): Set<String> =
        EffectWalker.leaves(effect).filterIsInstance<GatherCardsEffect>()
            .filter { it.source == CardSource.ChosenTargets }.mapTo(HashSet()) { it.storeAs }

    /** The slots of [requirements] that choose an object rather than a player. */
    private fun objectSlots(requirements: List<TargetRequirement>): List<Int> = requirements.indices.filter {
        val requirement = (requirements[it] as? TargetOther)?.baseRequirement ?: requirements[it]
        requirement !is TargetPlayer && requirement !is TargetOpponent
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
     * Whether [move] puts [effect]'s own chosen targets into a graveyard or exile for good — no
     * owner recast ([ownerRecastsChosenTargets]) and no later step, now or in a delayed trigger,
     * returning what it moved to the battlefield (a pipeline blink).
     */
    internal fun removesChosenTargets(move: MoveCollectionEffect, effect: Effect): Boolean {
        if (move.from !in allChosenTargets(effect)) return false
        val zone = (move.destination as? CardDestination.ToZone)?.zone
        if (zone != Zone.GRAVEYARD && zone != Zone.EXILE) return false
        if (zone == Zone.EXILE && ownerRecastsChosenTargets(effect)) return false
        // The return may read the moved cards back by the original collection's name, and sit in a
        // delayed trigger or an "if any were moved" branch (Phelia, Exuberant Shepherd's end-step return).
        fun deep(e: Effect): List<Effect> = EffectWalker.payoffLeaves(e).flatMap { leaf ->
            if (leaf is CreateDelayedTriggerEffect) listOf(leaf) + deep(leaf.effect) else listOf(leaf)
        }
        val leaves = deep(effect)
        val moved = listOfNotNull(move.from, move.storeMovedAs).toHashSet()
        // An exile linked to the source comes back as its linked-exile pile (Illusionist's Stratagem).
        if (move.linkToSource) {
            leaves.filterIsInstance<GatherCardsEffect>()
                .filter { it.source is CardSource.FromLinkedExile }.mapTo(moved) { it.storeAs }
        }
        do {
            val before = moved.size
            for (leaf in leaves) moved += CardIntentAnalyzer.derivedCollections(leaf, moved)
        } while (moved.size != before)
        return leaves.none {
            it is MoveCollectionEffect && it.from in moved &&
                (it.destination as? CardDestination.ToZone)?.zone == Zone.BATTLEFIELD
        }
    }

    /** The slots of [requirements] that choose a permanent: no player, and no card in another zone. */
    internal fun permanentSlots(requirements: List<TargetRequirement>): List<Int> = requirements.indices.filter { slot ->
        val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
        when (requirement) {
            is TargetPlayer, is TargetOpponent -> false
            is TargetObject -> requirement.filter.zone.let { it == null || it == Zone.BATTLEFIELD }
            else -> true
        }
    }

    /**
     * The slots an [ownerRecastsChosenTargets] pipeline acts on: every permanent slot, read
     * [TargetPolarity.FLEXIBLE] as a bounce is ([classify]'s move to hand). A slot whose card sits in
     * another zone (a graveyard card exiled beside it) is not bounced, and a player slot is no card.
     */
    private fun ownerRecastSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        if (!ownerRecastsChosenTargets(effect)) return emptyList()
        return permanentSlots(requirements)
    }

    /**
     * The permanent slots a pipeline blinks: it exiles its own chosen targets and, in the same
     * resolution, moves them — the exiled collection, or the source's linked-exile pile it filled —
     * back onto the battlefield. A move behind a condition is still a leaf here, so this reads the
     * card's plan, as [EffectWalker.blinkMoves] does for "exile it, then return it".
     */
    private fun pipelineBlinkSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        val leaves = EffectWalker.leaves(effect)
        val chosen = chosenTargetCollections(effect)
        if (chosen.isEmpty()) return emptyList()
        val exiles = leaves.filterIsInstance<MoveCollectionEffect>()
            .filter { it.from in chosen && (it.destination as? CardDestination.ToZone)?.zone == Zone.EXILE }
        if (exiles.isEmpty()) return emptyList()
        val exiled = exiles.flatMapTo(HashSet()) { listOfNotNull(it.from, it.storeMovedAs) }
        if (exiles.any { it.linkToSource }) {
            leaves.filterIsInstance<GatherCardsEffect>()
                .filter { it.source is CardSource.FromLinkedExile }.mapTo(exiled) { it.storeAs }
        }
        val returned = leaves.any {
            it is MoveCollectionEffect && it.from in exiled &&
                (it.destination as? CardDestination.ToZone)?.zone == Zone.BATTLEFIELD
        }
        if (!returned) return emptyList()
        return requirements.indices.filter { slot ->
            val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
            requirement is TargetObject && requirement.filter.zone.let { it == null || it == Zone.BATTLEFIELD }
        }
    }

    /**
     * The own-graveyard slots of [requirements] whose card's name [effect] notes
     * ([StoreCardNameEffect], neutral bookkeeping on its own) to search our own library for cards
     * of that name ([CardPredicate.NameEqualsChosen]) and put them into our hand: the slot names
     * what we tutor for. Searching an opponent's library for the name (Lobotomy, Cranial
     * Extraction) is disruption and reads nothing here.
     */
    private fun namedTutorSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        val leaves = EffectWalker.leaves(effect)
        val chosen = allChosenTargets(effect)
        val names = leaves.filterIsInstance<StoreCardNameEffect>().filter { it.from in chosen }.mapTo(HashSet()) { it.storeAs }
        if (names.isEmpty()) return emptyList()
        val found = leaves.filterIsInstance<GatherCardsEffect>().filter { gather ->
            val source = gather.source as? CardSource.FromZone ?: return@filter false
            source.zone == Zone.LIBRARY && source.player == Player.You &&
                source.filter.cardPredicates.any { it is CardPredicate.NameEqualsChosen && it.variableName in names }
        }.mapTo(HashSet()) { it.storeAs }
        if (found.isEmpty()) return emptyList()
        do {
            val before = found.size
            for (leaf in leaves) found += CardIntentAnalyzer.derivedCollections(leaf, found)
        } while (found.size != before)
        val toHand = leaves.any { leaf ->
            val to = (leaf as? MoveCollectionEffect)?.destination as? CardDestination.ToZone
            leaf is MoveCollectionEffect && leaf.from in found && to?.zone == Zone.HAND && to.player == Player.You
        }
        if (!toHand) return emptyList()
        return requirements.indices.filter { slot ->
            val filter = objectFilter(requirements[slot])
            filter?.zone == Zone.GRAVEYARD && ownGraveyardOnly(filter)
        }
    }

    /**
     * The leaves that act on their target without naming it: the counter, the spell-exile and the
     * redirect on "target spell", and damage divided among the chosen targets.
     * Only "counter *target* spell" — one that counters the triggering spell aims at nothing chosen.
     */
    private fun implicitTargetPolarity(leaf: Effect): TargetPolarity? = when (leaf) {
        // "Deals 2 damage divided as you choose among one or two targets" (Samut, the Tested,
        // Electrolyze, Arc Lightning): damage to each target the requirement chose, like the
        // named [DealDamageEffect] — the leaf carries no target of its own, so the slot is read here.
        is DividedDamageEffect -> TargetPolarity.HARMFUL
        is CounterEffect ->
            if (leaf.targetSource == CounterTargetSource.Chosen) TargetPolarity.HARMFUL else null
        is ExileTargetSpellEffect -> TargetPolarity.HARMFUL
        // "Return target spell to its owner's hand" (Failure, Reprieve, Hullbreaker Horror): the
        // spell is undone and its mana spent for nothing — a tempo counter, only worth aiming at
        // an opposing spell.
        is ReturnSpellToOwnersHandEffect -> TargetPolarity.HARMFUL
        // A redirect takes the spell or ability away from what its controller aimed it at. Pulled
        // onto this creature (Spellskite, Hydroelectric Specimen) or off ourselves (Reflecting
        // Mirror), it is only ever worth doing to an opposing object. An unrestricted "change the
        // target" (Bolt Bend, Misdirection) can also re-aim one of ours whose target went bad, so
        // its slot stays open to both sides; where it points is the decision's question.
        // A redirect of a spell named by reference (the triggering spell) aims at nothing chosen.
        is ChangeTargetEffect -> if (leaf.spell != EffectTarget.ContextTarget(0)) null else
            if (leaf.newTarget != null || leaf.onlyIfCurrentTargetIsController) TargetPolarity.HARMFUL
            else TargetPolarity.FLEXIBLE
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
        val move = when (leaf) {
            is MoveAllCountersEffect -> leaf.source to leaf.destination
            is MoveCountersEffect -> leaf.source to leaf.destination
            is MoveChosenCountersToTargetEffect -> leaf.source to leaf.destination
            is MoveCountersEachKindMissingEffect -> leaf.source to leaf.destination
            else -> null
        }
        if (move != null) {
            return listOf(move.first to TargetPolarity.FLEXIBLE, move.second to TargetPolarity.FLEXIBLE)
        }
        // "Copy target spell" / "copy target spell or ability" (Twincast, Return the Favor,
        // Lithoform Engine): the copy is ours
        // whichever side the original belongs to, and it is worth what that object does, which
        // only the stack shows — so both sides stay open, as a copied permanent's source does.
        if (leaf is CopyTargetSpellOrAbilityEffect) return listOf(leaf.target to TargetPolarity.FLEXIBLE)
        if (leaf is CopyTargetSpellEffect) return listOf(leaf.target to TargetPolarity.FLEXIBLE)
        if (leaf is CopyTargetTriggeredAbilityEffect) return listOf(leaf.target to TargetPolarity.FLEXIBLE)
        // "Create a token that's a copy of target artifact" (Saheeli's Artistry, Cackling
        // Counterpart's any-side kin): the token is ours whichever side the original sits on, as a
        // copied spell is. A copy of our own permanent is [ownCopySlot]'s gift; one any side can
        // fill is the best permanent on the board, which only the simulation sees. A token handed
        // to another player ([CreateTokenCopyOfTargetEffect.controller]) stays unread.
        if (leaf is CreateTokenCopyOfTargetEffect && leaf.controller == null) {
            return listOf(leaf.target to TargetPolarity.FLEXIBLE)
        }
        // "You may choose new targets for target spell" (Insidious Will, Wild Ricochet): the
        // all-targets redirect, read as the unrestricted single-target [ChangeTargetEffect] is — an
        // opposing spell turned away from what it aimed at, or one of ours re-aimed after its
        // target went bad. Where the targets go is the decision's question.
        if (leaf is ChangeTriggeringObjectTargetsEffect) return listOf(leaf.spell to TargetPolarity.FLEXIBLE)
        punisherReading(leaf, definitions, source)?.let { return it }
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
            // "Creatures target opponent controls don't untap during their next untap step"
            // (Imaginary Threats, Exhaustion, Mana Vapors): the target player's permanents stay
            // tapped through a turn — a tapper's lock on that player, never a gift.
            is SkipUntapEffect -> leaf.target to TargetPolarity.HARMFUL
            // Forcing a block can trade the target into a blocker or draw blockers away from its
            // teammates. Neither the one-blocker nor the all-blockers form fixes a target side;
            // keep both sides available to simulation, including beside a pump (Emergent Growth).
            is MustBeBlockedEffect -> leaf.target to TargetPolarity.FLEXIBLE
            // "Target creature blocks this turn if able" (Peema Aether-Seer, Culvert Ambusher):
            // takes the block choice away from its controller, dragging the creature into a block
            // it would not have made — the opponent's creature, like "can't block" above.
            is MarkMustBlockThisTurnEffect -> leaf.target to TargetPolarity.HARMFUL
            is CantBeRegeneratedEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Target creature loses all abilities" strips its holder — the static LoseAllAbilities
            // an Aura grants reads the same. A drawback-only creature gains from it, but that is a
            // board fact the cards that aim it (Lizard, Connors's Curse; Azure Beastbinder) don't rely on.
            is RemoveAllAbilitiesEffect -> leaf.target to TargetPolarity.HARMFUL
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
            // "Double the number of +1/+1 counters on …" (Biogenic Upgrade, Deepglow Skate): more of
            // what it already holds, so the counter decides the side; every kind at once is open.
            is DoubleCountersEffect -> leaf.target to (leaf.counterType?.let(::counterPolarity) ?: TargetPolarity.FLEXIBLE)
            // "For each kind of counter on target permanent or player, give that permanent or player
            // another counter of that kind" (Skyship Plunderer, Powerful Broker): one more of
            // whatever it holds — a +1/+1, loyalty or energy for our side, a -1/-1, poison or time
            // counter for theirs. Which kinds are there only the board shows, as doubling every kind.
            is ProliferateEffect -> (leaf.target ?: return emptyList()) to TargetPolarity.FLEXIBLE
            // "Choose a counter on target permanent or player. Give that permanent or player another
            // counter of that kind" (Animation Module, Clockspinning): the one-kind form of the line
            // above — its controller picks the kind on resolution, so a +1/+1 or loyalty counter for
            // our side and a -1/-1, poison or time counter for theirs. "A kind it doesn't have, from
            // among …" (Crystalline Giant) reads by the listed kinds when they agree; listed kinds
            // that pull opposite ways (or that have no side) leave it open, so FLEXIBLE. The iterated kind belongs
            // to its loop ([readsPerCounterKind]).
            is AddCountersOfChosenKindEffect -> leaf.target to when (leaf.kindFrom) {
                CounterKindSource.RECIPIENT -> TargetPolarity.FLEXIBLE
                CounterKindSource.LISTED_ABSENT ->
                    leaf.kinds.map(::counterPolarity).distinct().singleOrNull() ?: TargetPolarity.FLEXIBLE
                CounterKindSource.ITERATION -> return emptyList()
            }
            is MoveAllLastKnownCountersEffect -> leaf.target to TargetPolarity.FLEXIBLE
            is RemoveAnyNumberOfCountersEffect -> leaf.target to TargetPolarity.FLEXIBLE
            // "Remove a -1/-1 counter from another target creature you control" (Defiant Greatmaw,
            // Hapatra's Mark): taking a counter away does to its holder the opposite of putting it on,
            // so a -1/-1 or stun counter removed is a gift and a +1/+1 or loyalty counter a blow.
            is RemoveCountersEffect -> leaf.target to (counterPolarity(leaf.counterType)?.let(::opposite) ?: return emptyList())
            is RemoveAllCountersOfTypeEffect -> leaf.target to (counterPolarity(leaf.counterType)?.let(::opposite) ?: return emptyList())
            // Equip, fortify, reconfigure: "attach this permanent to target creature" does to that
            // creature whatever this Equipment does to the creature it equips — the same reading an
            // Aura's enchant target gets. When that can't be read (no source card, see
            // [forRequirements], or an Equipment whose bonus is mixed) the attach names nothing, so
            // a sibling leaf can still read the slot — The Aetherspark's "+1/+1 counter on that creature".
            is AttachEquipmentEffect -> source?.let { attachmentPolarity(it, definitions) }
                ?.takeIf { it != TargetPolarity.UNKNOWN }
                ?.let { leaf.target to it } ?: return emptyList()
            // "Attach target Equipment you control to target creature you control" (Halvar, Kemba,
            // Weapons Vendor, Beatrix's "any number of Equipment"): the creature gets whatever the
            // Equipment does to the creature it equips, and Equipment is built to be worn by its
            // controller's best creature — so the creature slot is a gift. Which Equipment to move is
            // the board's question (it may already sit on the creature that wants it most), so that
            // slot stays open for the simulation. Attaching what a theft just took, or onto what it took,
            // is the theft's follow-up (see theftFollowUps), so the stolen slot reads as the theft.
            is AttachTargetEquipmentToCreatureEffect -> return listOf(
                leaf.creatureTarget to TargetPolarity.BENEFICIAL,
                leaf.equipmentTarget to TargetPolarity.FLEXIBLE,
            )
            // "This becomes a copy of target creature" (Vesuvan Doppelganger), "target artifact or
            // creature becomes a copy of another target artifact or creature" (True Polymorph): the
            // copy does nothing to what it copies, and the best thing to copy may sit on either side
            // of the table — as may the permanent that is best turned into it (ours into a bomb,
            // theirs into a 1/1). Both slots are open for the simulation, which sees the copied
            // characteristics on the receiving permanent. A copy source our filter keeps on our side
            // is [ownCopySlot]'s gift.
            is EachPermanentBecomesCopyOfTargetEffect -> return listOfNotNull(
                leaf.target to TargetPolarity.FLEXIBLE,
                leaf.affected?.let { it to TargetPolarity.FLEXIBLE },
            )
            // The Aura token does to its object what any Aura does to the one it enchants.
            is CreateAuraTokenEffect -> leaf.target to definitions(leaf.tokenName)
                ?.let { attachmentPolarity(it, definitions) }
                ?.takeIf { it != TargetPolarity.UNKNOWN }
            // "Target creature gains '<trigger>' until end of turn" (Sygg, Wanderwine Wisdom): the
            // granted ability is controlled by the creature's controller, so it reads exactly like
            // the same trigger granted by an Aura.
            is GrantTriggeredAbilityEffect -> leaf.target to grantedTriggerPolarity(leaf.ability)
            // "Target creature loses all creature types" (Nameless Inversion, Amoeboid Changeling): its
            // lords, its typal payoffs and the spells that name its type stop seeing it. What a
            // creature gains by losing its types is dodging a typal sweeper — rare enough that the
            // loss is the curse it reads as.
            is LoseAllCreatureTypesEffect -> leaf.target to TargetPolarity.HARMFUL
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
            // "Target land gains '{T}: Add {C}{C}'": the mana is its controller's to spend. Any
            // other granted ability is activated by the holder's controller too, so it reads as
            // that ability's effect would for them (Quicksmith Spy's "{T}: Draw a card").
            is GrantActivatedAbilityEffect -> leaf.target to grantedActivatedPolarity(leaf.ability)
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
            // "Target player's life total becomes 1" (Vraska, Relic Seeker's −10):
            // a total set low is a loss for whoever holds it, and set high (20 or more) a gain. A
            // total in between, or one that depends on the board, could go either way for either
            // player, so the slot stays open and the simulation prices the new total.
            is SetLifeTotalEffect -> if (leaf.target.isSlot()) leaf.target to when (val amount = leaf.amount) {
                is DynamicAmount.Fixed -> when {
                    amount.amount <= 10 -> TargetPolarity.HARMFUL
                    amount.amount >= 20 -> TargetPolarity.BENEFICIAL
                    else -> TargetPolarity.FLEXIBLE
                }
                else -> TargetPolarity.FLEXIBLE
            } else return emptyList()
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

    /** The side a counter's removal favours: the other one than its placement's. */
    private fun opposite(polarity: TargetPolarity): TargetPolarity? = when (polarity) {
        TargetPolarity.BENEFICIAL -> TargetPolarity.HARMFUL
        TargetPolarity.HARMFUL -> TargetPolarity.BENEFICIAL
        else -> null
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
        // "Gains protection from red" (Crimson Acolyte) is granted as `PROTECTION_FROM_RED`, the
        // scoped form of [Keyword.PROTECTION] — the same gift.
        if (name.uppercase().startsWith("PROTECTION_FROM_")) return TargetPolarity.BENEFICIAL
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
        // stays a gap rather than hiding behind the enchant restriction.
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
        // An ability granted to the host is its controller's to activate or ignore — a gift when it
        // is all the Aura does (Abundant Growth), but beside a curse only the consolation the curse
        // leaves them: Compulsory Rest's "{2}, Sacrifice this creature: You gain 2 life" on a
        // creature that can't attack or block, Imprisoned in the Moon's mana ability on a creature
        // that became a land. It never turns the curse into a gift, so a harm outranks it.
        val consolations = mutableListOf<TargetPolarity>()
        // A triggered ability granted to the host that only hurts it, beside a gift the Aura gives
        // outright — Consuming Fervor's "+3/+3" and "at the beginning of your upkeep, put a -1/-1
        // counter on this creature" — is the gift's price, paid a turn at a time after the bonus has
        // landed: a pump that fades, not a curse. With no gift beside it, the harm is the card.
        val prices = mutableListOf<TargetPolarity>()
        for (static in card.script.staticAbilities) {
            if (hostIs != null && static is ConditionalStaticAbility) {
                val holds = when (static.condition) {
                    is NotCondition -> !hostIs
                    else -> hostIs
                }
                if (!holds) continue
            }
            val reading = staticPolarity(static) ?: continue
            when {
                static is GrantActivatedAbility -> consolations += reading
                static is GrantTriggeredAbility && reading == TargetPolarity.HARMFUL -> prices += reading
                else -> readings += reading
            }
        }
        // "Enchanted artifact is a creature with base power and toughness 4/4" (Mightstone's
        // Animation, Zoetic Glyph, Puppet Crafting): the creature type and the body arrive together,
        // so the host's controller gains a creature. The base P/T alone stays unread above — on a
        // creature it can shrink as well as grow — and the type alone could leave a 0/0.
        if (animatesHost(card.script.staticAbilities)) readings += TargetPolarity.BENEFICIAL
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
            // "Enchant player … at the beginning of enchanted player's upkeep, that player loses 3
            // life unless they sacrifice a nonland permanent or discard a card" (Torment of Scarabs):
            // the host is the player, named by the leaves that act on them.
            readings += EffectWalker.payoffLeaves(effect).mapNotNull(::enchantedPlayerLeafPolarity)
            readings += hostControllerHarms(effect)
        }
        // A replacement effect on an Aura (Sandskin's "prevent all combat damage dealt to and by
        // enchanted creature") is read nowhere above, and is often the whole card. Without it the
        // statics alone could call an either-way Aura one-sided, so its presence is a veto — except
        // for an entry choice (Traveler's Cloak's land type), which only parameterises a static.
        // A self-only zone redirect (disturb's "if this would be put into a graveyard, exile it
        // instead") moves the Aura itself and never touches the creature, so it is no veto either.
        if (card.script.replacementEffects.any { !it.affectsOnlyTheAuraItself() }) readings += TargetPolarity.UNKNOWN
        // A price needs a gift the Aura gives outright, so judge it before any consolation joins.
        val payPrices = TargetPolarity.BENEFICIAL !in readings || TargetPolarity.HARMFUL in readings
        if (TargetPolarity.HARMFUL !in readings) readings += consolations
        if (payPrices) readings += prices
        return readings
    }

    /**
     * Whether [statics] make the attached permanent a creature with a real body: a "CREATURE"
     * [GrantCardType] on the host beside a [SetBasePowerToughnessStatic] of at least 2/2 on it.
     */
    private fun animatesHost(statics: List<StaticAbility>): Boolean =
        statics.any { it is GrantCardType && it.cardType == "CREATURE" && it.filter.scope == Scope.AttachedTo } &&
            statics.any {
                it is SetBasePowerToughnessStatic && it.filter.scope == Scope.AttachedTo &&
                    (it.power ?: 0) >= 2 && (it.toughness ?: 0) >= 2
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

    /**
     * How a granted activated ability reads for the permanent holding it: a mana ability is the
     * holder's controller's mana; any other does what its effect does for the player activating it.
     */
    internal fun grantedActivatedPolarity(ability: ActivatedAbility): TargetPolarity =
        if (ability.isManaAbility) TargetPolarity.BENEFICIAL
        else grantedEffectPolarity(ability.effect, ability.targetRequirements)

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
            // "When this creature dies, return it to its owner's hand" (Verdant Rebirth, Ashnod's
            // Intervention): a trigger that brings its own holder back out of the graveyard or
            // exile is recursion for the creature's side — the same gift an Aura's would be.
            if (returnsItselfFromAGraveyard(leaf)) {
                readings += TargetPolarity.BENEFICIAL
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
     * "Choose target creature. Whenever it deals combat damage to a player this turn, you draw that
     * many cards" (The Inner Light II, Captain Howler), "this turn, when target creature you control
     * attacks and isn't blocked, you may gain life equal to its power" (Delif's Cone): a delayed
     * trigger that watches a target and fires on that object's own offence — it attacks, goes
     * unblocked, deals damage to anything but us — pays its controller for the watched creature
     * connecting, so the creature to name is one that attacks for us. Not when what it fires harms
     * the creature it watched on a slot that can name theirs.
     */
    private fun offenceWatchSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        if (leaf !is CreateDelayedTriggerEffect) return null
        val watched = leaf.watchedTarget ?: return null
        val offence = when (val event = leaf.trigger?.event) {
            is EventPattern.AttackEvent, EventPattern.BecomesUnblockedEvent -> true
            is EventPattern.DealsDamageEvent -> event.recipient != Recipient.You
            else -> false
        }
        if (!offence) return null
        val harmsWatched = EffectWalker.firedLeaves(leaf).any { fired ->
            classify(fired).any { (target, polarity) ->
                polarity == TargetPolarity.HARMFUL && (target == EffectTarget.TriggeringEntity || target == watched)
            }
        }
        // Only an object watched attacks or deals the damage; a player slot names no attacker.
        val slot = slotOf(watched, requirements)?.takeIf { it in objectSlots(requirements) } ?: return null
        // On a slot kept to our own creatures, a harm the payoff sets on the watched one is its
        // price (Delif's Cone: "if you do, it assigns no combat damage"), and still ours to pick.
        return if (!harmsWatched || isOwnSide(requirements[slot])) slot else null
    }

    private fun returnsItselfFromAGraveyard(leaf: Effect): Boolean =
        leaf is MoveToZoneEffect && leaf.target == EffectTarget.Self &&
            (leaf.fromZone == Zone.GRAVEYARD || leaf.fromZone == Zone.EXILE) &&
            (leaf.destination == Zone.HAND || leaf.destination == Zone.BATTLEFIELD)

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

    /**
     * What a triggered or activated leaf of an "Enchant player" Aura does to the player it
     * enchants: life lost, damage dealt, or an unless-clause they must pay or suffer from is a
     * curse. Gifts (life, cards) stay unread: Tenuous Truce enchants an opponent yet gives both players
     * cards, and reading it as BENEFICIAL would aim it only at our own side. Null otherwise.
     */
    private fun enchantedPlayerLeafPolarity(leaf: Effect): TargetPolarity? {
        val enchanted = EffectTarget.PlayerRef(Player.EnchantedPlayer)
        return when (leaf) {
            is LoseLifeEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == enchanted }
            is DealDamageEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == enchanted }
            is PayOrSufferEffect -> TargetPolarity.HARMFUL.takeIf {
                leaf.player == enchanted &&
                    EffectWalker.payoffLeaves(leaf.suffer).all { enchantedPlayerLeafPolarity(it) == TargetPolarity.HARMFUL }
            }
            else -> null
        }
    }

    /**
     * "This Aura deals 2 damage to enchanted artifact's controller" (Gremlin Infestation): a
     * pipeline gathers the host, then damages or drains the player controlling it. That player is
     * whoever the Aura is cast onto the side of, so each such leaf is a harm to the host's slot.
     */
    private fun hostControllerHarms(effect: Effect): List<TargetPolarity> {
        val leaves = EffectWalker.leaves(effect)
        val hostKeys = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { gather -> (gather.source as? CardSource.Entity)?.entity?.let { it in HOST_TARGETS } == true }
            .mapTo(HashSet()) { it.storeAs }
        if (hostKeys.isEmpty()) return emptyList()
        fun hostController(target: EffectTarget) =
            target is EffectTarget.ControllerOfPipelineTarget && target.collectionName in hostKeys
        return leaves.mapNotNull { leaf ->
            when (leaf) {
                is DealDamageEffect -> TargetPolarity.HARMFUL.takeIf { hostController(leaf.target) }
                is LoseLifeEffect -> TargetPolarity.HARMFUL.takeIf { hostController(leaf.target) }
                else -> null
            }
        }
    }

    private fun staticPolarity(static: StaticAbility): TargetPolarity? =
        enchantedPlayerPolarity(static) ?: hostStaticPolarity(static)

    /**
     * What an "Enchant player" Aura's static does to the player it enchants, read through that
     * player's own actions ("enchanted player can't activate abilities …") or the permanents they
     * control ("creatures enchanted player controls lose all abilities and have base power and
     * toughness 1/1" — Overwhelming Splendor). Null for any static that doesn't name them.
     */
    private fun enchantedPlayerPolarity(static: StaticAbility): TargetPolarity? = when (static) {
        is PlayersCantActivateAbilities -> if (static.affected == Player.EnchantedPlayer) TargetPolarity.HARMFUL else null
        is PlayersCantCastSpells -> if (static.affected == Player.EnchantedPlayer) TargetPolarity.HARMFUL else null
        is LoseAllAbilities -> if (static.filter.reachesEnchantedPlayer()) TargetPolarity.HARMFUL else null
        is SetBasePowerToughnessStatic ->
            if (static.filter.reachesEnchantedPlayer() && static.shrinksToAtMostOneOne()) TargetPolarity.HARMFUL else null
        else -> null
    }

    private fun hostStaticPolarity(static: StaticAbility): TargetPolarity? = when (static) {
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
        // "Has base power and toughness 1/1" (Cursed Role) shrinks nearly anything it lands on, and
        // a toothless 0/2 (Illusory Wrappings) takes away all it does in combat but block — the
        // line [baseStatsPolarity] draws for the one-shot effect. A larger base is a gift or a
        // curse depending on the creature, so it stays unread.
        is SetBasePowerToughnessStatic -> if (static.filter.scope != Scope.AttachedTo) null
            else baseStatsPolarity(static.power?.let(DynamicAmount::Fixed), static.toughness?.let(DynamicAmount::Fixed))
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
        // Damage as -1/-1 counters or poison, a return from death, a copy attacking each other
        // opponent, every creature type for the tribal payoffs (Oko, Lorwyn Liege's +2): none of
        // them costs the creature that gains it anything.
        Keyword.WITHER, Keyword.INFECT, Keyword.PERSIST, Keyword.UNDYING, Keyword.MYRIAD,
        Keyword.CHANGELING,
    )
}
