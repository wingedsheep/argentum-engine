package com.wingedsheep.ai.engine.knowledge

import com.wingedsheep.engine.mechanics.layers.ProjectedState
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.PreventDamage
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.AdditionalManaOnTap
import com.wingedsheep.sdk.scripting.AdditionalSourceTriggers
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.CompositeStaticAbility
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.conditions.EnchantedCreatureHasSubtype
import com.wingedsheep.sdk.scripting.conditions.EntityMatches
import com.wingedsheep.sdk.scripting.conditions.NotCondition
import com.wingedsheep.sdk.scripting.ControlEnchantedPermanent
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.GrantProtection
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.GrantProtectionFromCardType
import com.wingedsheep.sdk.scripting.GrantProtectionFromChosenColorToGroup
import com.wingedsheep.sdk.scripting.GrantProtectionFromControlledColors
import com.wingedsheep.sdk.scripting.GrantProtectionFromToGroup
import com.wingedsheep.sdk.scripting.LoseAllAbilities
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.CantAttackOrBlockUnlessPay
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GrantCardType
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RemoveKeywordStatic
import com.wingedsheep.sdk.scripting.GrantWard
import com.wingedsheep.sdk.scripting.SetBasePowerToughnessStatic
import com.wingedsheep.sdk.scripting.PlayersCantActivateAbilities
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.GrantStaticAbility
import com.wingedsheep.sdk.scripting.PreventActivatedAbilities
import com.wingedsheep.sdk.scripting.UntapSelfDuringOtherUntapSteps
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementEffect
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.BecomeCreatureEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTargetEffect
import com.wingedsheep.sdk.scripting.effects.ChangeTriggeringObjectTargetsEffect
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.ChoosePileEffect
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.DrainLifeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.ForEachEffect
import com.wingedsheep.sdk.scripting.effects.GrantCantBeBlockedByChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.IterationSpace
import com.wingedsheep.sdk.scripting.effects.CantCastSpellsEffect
import com.wingedsheep.sdk.scripting.effects.ExchangeLifeTotalsEffect
import com.wingedsheep.sdk.scripting.effects.SetLifeTotalEffect
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.effects.LoseAllCreatureTypesEffect
import com.wingedsheep.sdk.scripting.effects.SetCreatureSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.AddSubtypeEffect
import com.wingedsheep.sdk.scripting.effects.LoseGameEffect
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
import com.wingedsheep.sdk.scripting.effects.ReturnSelfToBattlefieldAttachedEffect
import com.wingedsheep.sdk.scripting.effects.AttachTargetEquipmentToCreatureEffect
import com.wingedsheep.sdk.scripting.effects.BecomePreparedEffect
import com.wingedsheep.sdk.scripting.effects.UnprepareEffect
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.CantAttackEffect
import com.wingedsheep.sdk.scripting.effects.CantBlockEffect
import com.wingedsheep.sdk.scripting.effects.CanAttackDespiteDefenderThisTurnEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetSpellOrAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CopyTargetTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.CounterEffect
import com.wingedsheep.sdk.scripting.effects.CreatePredefinedTokenEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.InvestigateEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CounterTargetSource
import com.wingedsheep.sdk.scripting.effects.CreateAuraTokenEffect
import com.wingedsheep.sdk.scripting.effects.ChangeWordInTextEffect
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
import com.wingedsheep.sdk.scripting.effects.GiveControlToTargetPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GrantEvasionKeywordEffect
import com.wingedsheep.sdk.scripting.effects.GrantHexproofFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.LockPlayerCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantProtectionFromChosenColorEffect
import com.wingedsheep.sdk.scripting.effects.HijackNextTurnEffect
import com.wingedsheep.sdk.scripting.effects.GrantStaticAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantFlashbackEffect
import com.wingedsheep.sdk.scripting.effects.GrantActivatedAbilityEffect
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.MarkExileOnDeathEffect
import com.wingedsheep.sdk.scripting.effects.MustBeBlockedEffect
import com.wingedsheep.sdk.scripting.effects.MarkMustAttackEffect
import com.wingedsheep.sdk.scripting.effects.MarkMustBlockEffect
import com.wingedsheep.sdk.scripting.effects.ForceBlockEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.effects.TurnFaceDownEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.effects.SetBaseStatsEffect
import com.wingedsheep.sdk.scripting.effects.FightEffect
import com.wingedsheep.sdk.scripting.effects.MutualDamageEffect
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
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PreventDamageEffect
import com.wingedsheep.sdk.scripting.effects.PreventionSourceFilter
import com.wingedsheep.sdk.scripting.effects.ProliferateEffect
import com.wingedsheep.sdk.scripting.effects.PreventionDirection
import com.wingedsheep.sdk.scripting.effects.HealDamageEffect
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapCollectionEffect
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CastFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SurveilEffect
import com.wingedsheep.sdk.scripting.effects.ScryEffect
import com.wingedsheep.sdk.scripting.effects.GatherUntilMatchEffect
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
 * An Aura whose gift or curse turns on what it enchants — the creature type (Sea Legs' "+0/+2 as
 * long as it's a Pirate. Otherwise, it gets -2/-0", Gift of Fangs' Vampire, Bonds of Faith's Human)
 * or the card type (Armament of Nyx's "double strike as long as it's an enchantment. Otherwise,
 * prevent all damage that would be dealt by enchanted creature").
 * Its enchant slot is [TargetPolarity.FLEXIBLE] as a whole, but each candidate has one side:
 * [whenIs] for a creature that has [trait], [otherwise] for one that doesn't.
 */
data class HostSplit(val trait: HostTrait, val whenIs: TargetPolarity, val otherwise: TargetPolarity) {
    fun polarityFor(hostHasTrait: Boolean): TargetPolarity = if (hostHasTrait) whenIs else otherwise
}

/** What a [HostSplit] asks of a candidate host, read off its projected characteristics. */
sealed interface HostTrait {
    fun of(projected: ProjectedState, entityId: EntityId): Boolean

    /** "As long as enchanted creature is a Pirate." */
    data class CreatureType(val subtype: Subtype) : HostTrait {
        override fun of(projected: ProjectedState, entityId: EntityId) = projected.hasSubtype(entityId, subtype.value)
    }

    /** "As long as it's an enchantment" — [type] as [ProjectedState.hasType] spells it ("ENCHANTMENT"). */
    data class CardType(val type: String) : HostTrait {
        override fun of(projected: ProjectedState, entityId: EntityId) = projected.hasType(entityId, type)
    }
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
 * Otherwise, …"), or on its card type ("double strike as long as it's an enchantment. Otherwise,
 * prevent all damage it would deal" — Armament of Nyx), is read twice, once for a host of that type and once for any other host. Two
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
     * because what it does turns on the host's creature type or card type — see [HostSplit]. Null for every
     * other card.
     */
    fun hostSplit(card: CardDefinition, definitions: CardLookup = NO_DEFINITIONS): HostSplit? =
        hostSplitCache.getOrPut(card.name to (definitions !== NO_DEFINITIONS)) {
            val script = card.script
            if (card.cardFaces.isNotEmpty() || script.auraTarget == null || script.targetRequirements.isNotEmpty()) {
                return@getOrPut emptyList()
            }
            val trait = hostTrait(card) ?: return@getOrPut emptyList()
            val (whenIs, otherwise) = hostWorlds(card, definitions)
            val oneSided = setOf(TargetPolarity.BENEFICIAL, TargetPolarity.HARMFUL)
            if (whenIs in oneSided && otherwise in oneSided && whenIs != otherwise) {
                listOf(HostSplit(trait, whenIs!!, otherwise!!))
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
    fun forMode(
        mode: Mode,
        definitions: CardLookup = NO_DEFINITIONS,
        source: CardDefinition? = null,
    ): List<TargetPolarity> =
        forRequirements(mode.effect, mode.targetRequirements, definitions, source = source)

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
     * on resolution ([CastFromCollectionWithoutPayingCostEffect], paid or free, or several of them
     * at once — Diluvian Primordial's [CastAnyNumberFromCollectionWithoutPayingCostEffect]) or grants the cast
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
                (leaf is CastAnyNumberFromCollectionWithoutPayingCostEffect && leaf.from in chosen) ||
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
                    // "Put a +1/+1 counter on it and untap it" (Mark of Mutiny): a counter that only
                    // grows the creature we took is ours to attack with, part of the theft.
                    is AddCountersEffect ->
                        if (part.target in stolen && counterPolarity(part.counterType) == TargetPolarity.BENEFICIAL) gifts += part
                        else stolen.clear()
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
        // into the block, not a gift to it. So is untapping it (Mark for Death: "blocks this turn if
        // able. Untap that creature") — a tapped creature can't be made to block.
        val forcedBlockers = targetLeaves(effect).filterIsInstance<MarkMustBlockEffect>().mapTo(HashSet()) { it.target }
        for (leaf in targetLeaves(effect)) {
            if (blinks.any { it === leaf }) continue
            if (leaf is GrantKeywordEffect && leaf.keyword == Keyword.REACH.name && leaf.target in forcedBlockers) continue
            if (leaf is TapUntapEffect && !leaf.tap && leaf.target in forcedBlockers) continue
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
            // `ContextTarget(0)` walks every target of every slot in turn, so each slot reads as the
            // body reads on that slot alone. "Return up to two target creatures your opponents
            // control … Target player draws two cards" (Roiling Waters) bounces each creature and
            // can't move the player: a body of object effects says nothing about a player slot
            // (Homesickness guards its tap with "if it's a creature"), so only object slots take its
            // reading and the rest are left to their other steps.
            if (leaf is ForEachEffect && leaf.space == IterationSpace.Targets) {
                if (requirements.size == 1) {
                    // A bounce can save our creature from removal even when the spell can
                    // choose several targets. Preserve the body's either-way reading so target
                    // simulation can compare rescue against offensive tempo.
                    perSlot[0] += forRequirements(leaf.body, requirements, definitions, source)
                } else {
                    for (slot in objectSlots(requirements)) {
                        val reading = forRequirements(leaf.body, listOf(requirements[slot]), definitions, source).single()
                        if (reading != TargetPolarity.UNKNOWN) perSlot[slot] += reading
                    }
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
                val everyTarget = space.collection in allChosenTargets(effect)
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        slotOf(target, requirements)?.let { perSlot[it] += polarity }
                    }
                    // "Any number of target creatures you control each become a copy of that
                    // creature until end of turn" (Polymorphous Rush): every chosen target becomes
                    // the copy. Which of ours is worth turning into it — a weak body made big, not
                    // a big one made small — is the board's question, as the copy reading below
                    // makes it for a declared `affected` slot: either way.
                    if (everyTarget && inner is EachPermanentBecomesCopyOfTargetEffect && inner.affected == EffectTarget.IterationEntity) {
                        for (slot in permanentSlots(requirements)) perSlot[slot] += TargetPolarity.FLEXIBLE
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
            // A group no target player owns ("each creature you control with a +1/+1 counter on it
            // deals damage equal to its power to that creature", Nissa's Judgment) names no slot
            // for `IterationEntity`, but the victim its body bites is still the slot it names.
            if (space is IterationSpace.Group) {
                val groupOwner = groupOwnerSlot(space, requirements)
                for (inner in EffectWalker.payoffLeaves(leaf.body)) {
                    for ((target, polarity) in classify(inner, definitions, source)) {
                        val slot = if (target == EffectTarget.IterationEntity) groupOwner else slotOf(target, requirements)
                        slot?.let { perSlot[it] += polarity }
                    }
                }
                continue
            }
            if (leaf is GatherCardsEffect) {
                attachmentRemovalSlot(leaf, effect, requirements)?.let { perSlot[it] += TargetPolarity.HARMFUL }
                val top = leaf.source as? CardSource.TopOfLibrary
                if (top != null) {
                    val slot = playerSlotOf(top.player, requirements)
                    val reading = if (top.isMill) TargetPolarity.HARMFUL else libraryTopPolarity(setOf(leaf.storeAs), effect)
                    if (reading != null) slot?.let { perSlot[it] += reading }
                }
            }
            // "Target player reveals cards from the top of their library until they reveal a land
            // card, then puts those cards into their graveyard" (Balustrade Spy): a dig of unknown
            // depth off that player's top, read as the fixed-count gather above — binned, it is a mill.
            if (leaf is GatherUntilMatchEffect) {
                val reading = libraryTopPolarity(setOf(leaf.storeMatch, leaf.storeRevealed), effect)
                if (reading != null) playerSlotOf(leaf.player, requirements)?.let { perSlot[it] += reading }
            }
            // The muzzled player's slot reads here; the shield's own recipient (if it targets
            // one) still reads through the classification below.
            preventionSourceOwnerSlot(leaf, requirements)?.let { perSlot[it] += TargetPolarity.HARMFUL }
            preventionSparedSlot(leaf, requirements)?.let { perSlot[it] += TargetPolarity.BENEFICIAL }
            val offenceWatch = offenceWatchSlot(leaf, requirements)
            if (offenceWatch != null) {
                perSlot[offenceWatch] += TargetPolarity.BENEFICIAL
                continue
            }
            val damageWatch = damageWatchSlot(leaf, requirements)
            if (damageWatch != null) {
                perSlot[damageWatch] += TargetPolarity.FLEXIBLE
                continue
            }
            val measured = measuredPlayerSlots(leaf, requirements)
            if (measured.isNotEmpty()) {
                measured.forEach { perSlot[it] += TargetPolarity.FLEXIBLE }
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
        // "Target opponent looks at the top four cards of your library and separates them into a
        // face-down pile and a face-up pile" (Fortune's Favor, Sauron's Ransom), "target opponent
        // chooses two of those cards" (Gifts Ungiven): the slot only names who decides a split of our
        // cards ([Chooser.TargetPlayer]). The decider works against us, so it is the opponent's side
        // — and an opponent slot can't name anyone else. Only when nothing else reads the slot.
        if (perSlot[0].isEmpty() && decidedByTargetOpponent(leaves, requirements)) perSlot[0] += TargetPolarity.HARMFUL
        // "Target opponent gets an emblem with 'Whenever a player draws a card, you lose 2 life'"
        // (Ob Nixilis Reignited's −8): inside an iteration over the targeted player, `you` is that
        // player, so the life the body costs them is harm aimed at the slot — as is an emblem that
        // pumps each creature attacking them (Garruk, Apex Predator's −8). Only when nothing else
        // reads it.
        for (each in leaves.filterIsInstance<ForEachEffect>()) {
            val players = (each.space as? IterationSpace.Players)?.players ?: continue
            if (players != Player.TargetPlayer && players != Player.TargetOpponent) continue
            val slot = requirements.indices.filter { requirements[it] is TargetPlayer || requirements[it] is TargetOpponent }.singleOrNull() ?: continue
            if (perSlot[slot].isEmpty() && (CardIntentAnalyzer.visitedPlayerLosesLife(each.body) ||
                    CardIntentAnalyzer.visitedPlayerArmsTheirAttackers(each.body))
            ) perSlot[slot] += TargetPolarity.HARMFUL
        }
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
        // Graveyard cards a pipeline returns to our hand (Wildest Dreams): the best card is the one to name.
        if (CardIntentAnalyzer.returnsChosenGraveyardTargetsToHand(effect, requirements)) recursions += requirements.indices
        // A single graveyard target a pipeline returns (Foul Renewal), or graveyard targets it puts
        // back on top of our library (Gravepurge): the single-card recursion as gather + move.
        recursions += pipelineGraveyardReturnSlots(effect, requirements)
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
        for (slot in pipelineRemovalSlots(effect, requirements)) perSlot[slot] += TargetPolarity.HARMFUL
        for (slot in lossSlots(leaves, requirements)) {
            if (TargetPolarity.BENEFICIAL !in perSlot[slot]) perSlot[slot] += TargetPolarity.HARMFUL
        }
        for (slot in refillSlots(leaves, requirements)) {
            if (perSlot[slot].isEmpty()) perSlot[slot] += TargetPolarity.BENEFICIAL
        }
        for (slot in measuredGainSlots(leaves, requirements)) {
            if (perSlot[slot].isEmpty()) {
                perSlot[slot] += if (isOwnSide(requirements[slot])) TargetPolarity.BENEFICIAL else TargetPolarity.FLEXIBLE
            }
        }
        for (slot in largeBaseSlots(leaves, requirements)) {
            val alone = perSlot[slot].isEmpty() &&
                leaves.none { it !is SetBaseStatsEffect && touchesOtherwise(it, requirements[slot], slot, definitions, source) }
            if (alone) perSlot[slot] += TargetPolarity.FLEXIBLE
        }
        for (slot in animateSlots(leaves, requirements)) {
            val alone = perSlot[slot].isEmpty() &&
                leaves.none { it !is BecomeCreatureEffect && touchesOtherwise(it, requirements[slot], slot, definitions, source) }
            if (alone) perSlot[slot] += if (animatesNoncreature(requirements[slot])) TargetPolarity.BENEFICIAL else TargetPolarity.FLEXIBLE
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
        // An Aura returned attached to a target whose effect on its host can't be read one-sidedly
        // — Radiant Restraints' "creatures enchanted player controls enter tapped" is a replacement
        // effect, a veto on the Aura reading — is still aimed by the side its slot is confined to:
        // "target opponent" or "target creature an opponent controls" is a curse's victim. On our own
        // side ("target creature you control") it is, like Equip above, a question of which of our
        // creatures wears it best: open for the simulation. Only when nothing else reads the slot.
        for (aura in leaves.filterIsInstance<ReturnSelfToBattlefieldAttachedEffect>()) {
            val slot = slotOf(aura.target, requirements) ?: continue
            if (perSlot[slot].isNotEmpty()) continue
            val requirement = requirements[slot]
            when {
                requirement is TargetOpponent || isOpponentSide(requirement) -> perSlot[slot] += TargetPolarity.HARMFUL
                isOwnSide(requirement) -> perSlot[slot] += TargetPolarity.FLEXIBLE
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
                // "Target player sacrifices a creature, then gains life equal to its toughness"
                // (Devour Flesh): an edict on an opponent, or a creature we no longer need cashed in
                // for life on ourselves — which side it serves is the board's to say.
                sacrificeRepaidInLife(slot, readings, leaves, requirements, definitions, source) -> TargetPolarity.FLEXIBLE
                drawsForItsLife(slot, readings, leaves, requirements, definitions, source) -> TargetPolarity.BENEFICIAL
                // "Prevent all damage that would be dealt to and dealt by target permanent an opponent
                // controls" (Kiora, the Crashing Wave; Dovin's −1): shielding a permanent and silencing
                // it takes it out of combat altogether. Which side that helps is the board's to say —
                // unless the slot is confined to one side: the opponent's threat disarmed, or our own
                // creature kept safe. Only where the steps otherwise disagree (Maze of Ith's untap
                // already reads the slot).
                combine(readings) == TargetPolarity.UNKNOWN && shieldsAndSilences(slot, leaves, requirements) -> when {
                    isOpponentSide(requirements[slot]) -> TargetPolarity.HARMFUL
                    isOwnSide(requirements[slot]) -> TargetPolarity.BENEFICIAL
                    else -> TargetPolarity.FLEXIBLE
                }
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
     * A player slot whose only harm is a forced sacrifice and whose only help is life the same
     * player gains (Devour Flesh): open, since the sacrificed creature is the whole price.
     */
    private fun sacrificeRepaidInLife(
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
        return beneficial.isNotEmpty() && beneficial.all { it is GainLifeEffect } &&
            harmful.isNotEmpty() && harmful.all { it is ForceSacrificeEffect }
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
            leaf is com.wingedsheep.sdk.scripting.effects.RepeatWhileEffect -> targetLeaves(leaf.body)
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
     * is a loss, so the slot is harmful whenever what they suffer for not paying is, or when the
     * suffer is only a gift to our controller (Indulgent Tormentor's draw); a branch choice
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
            leaf is PayOrSufferEffect && leaf.player.isSlot() -> readings(leaf.suffer).let { suffer ->
                when {
                    hurts(suffer, leaf.player) -> suffer
                    // "Draw a card unless target opponent sacrifices a creature or pays 3 life"
                    // (Indulgent Tormentor): the suffer never touches the payer — it is a gift to
                    // our controller — so either way out is theirs to lose: a payment, or our card.
                    suffer.none { it.first == leaf.player } && servesController(leaf.suffer) ->
                        suffer + (leaf.player to TargetPolarity.HARMFUL)
                    else -> null
                }
            }
            leaf is ChooseActionEffect && !EffectWalker.choosesOwnBranch(leaf) && leaf.player.isSlot() ->
                leaf.choices.flatMap { readings(it.effect) }.takeIf { hurts(it, leaf.player) }
            else -> null
        }
    }

    /** Whether every payoff leaf of [effect] is a known gift to the ability's controller ([controllerPolarity]). */
    private fun servesController(effect: Effect): Boolean =
        EffectWalker.payoffLeaves(effect).let { leaves ->
            leaves.isNotEmpty() && leaves.all { controllerPolarity(it) == TargetPolarity.BENEFICIAL }
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

    /**
     * Whether [slot] is both shielded from damage and stopped from dealing it: one
     * [PreventionDirection.Both] shield, or a [PreventionDirection.ToTarget] and a
     * [PreventionDirection.FromTarget] shield on the same target.
     */
    private fun shieldsAndSilences(slot: Int, leaves: List<Effect>, requirements: List<TargetRequirement>): Boolean {
        val directions = leaves.filterIsInstance<PreventDamageEffect>()
            .filter { it.preventDamage && slotOf(it.target, requirements) == slot }
            .map { it.direction }.toSet()
        return PreventionDirection.Both in directions ||
            (PreventionDirection.ToTarget in directions && PreventionDirection.FromTarget in directions)
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

    /** A fixed draw this large can empty a Limited library: a mill-out threat, not only card advantage. */
    private const val DECKING_DRAW = 10

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
     * The player slots a **token count sized by target players** reads — "choose any number of
     * target opponents. Create X 1/1 Soldiers, where X is the number of creatures those opponents
     * control" (Call the Coppercoats), "investigate X times, where X is the total number of
     * creatures those players control" (Officious Interrogation) — or none.
     *
     * The tokens are the controller's; the targeted players are only counted, not acted on, as a
     * draw sized by a target opponent's damage is (Knollspine Dragon). So each slot reads
     * [TargetPolarity.FLEXIBLE]: understood, and the simulation weighs who has the most to count.
     * A token handed *to* a target player is [tokenGiftSlot]'s, not this.
     */
    private fun measuredPlayerSlots(leaf: Effect, requirements: List<TargetRequirement>): List<Int> {
        val (controller, count) = when (leaf) {
            is CreateTokenEffect -> leaf.controller to leaf.count
            is InvestigateEffect -> leaf.controller to leaf.count
            else -> return emptyList()
        }
        if (controller?.isSlot() == true) return emptyList()
        val player = (count as? DynamicAmount.Count)?.player ?: return emptyList()
        return when (player) {
            Player.EachTargetedPlayer -> requirements.indices.filter {
                requirements[it] is TargetPlayer || requirements[it] is TargetOpponent
            }
            else -> listOfNotNull(playerSlotOf(player, requirements))
        }
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
        val slot = when (leaf) {
            is CreateTokenEffect -> leaf.controller?.takeIf { it.isSlot() }?.let { slotOf(it, requirements) }
            // A card put onto the battlefield under a target player's control is the same body on
            // their side: "Target opponent may choose one of the exiled cards and put it onto the
            // battlefield under their control" (Dubious Challenge).
            is MoveCollectionEffect -> (leaf.destination as? CardDestination.ToZone)
                ?.takeIf { it.zone == Zone.BATTLEFIELD }
                ?.player?.takeIf { it is Player.BoundVariable || it is Player.ContextPlayer }
                ?.let { playerSlotOf(it, requirements) }
            else -> null
        } ?: return null
        return slot to if (requirements[slot] is TargetOpponent) TargetPolarity.FLEXIBLE else TargetPolarity.BENEFICIAL
    }

    /** The player slot [player] names among [requirements] — a bound or indexed target player — or null. */
    private fun playerSlotOf(player: Player, requirements: List<TargetRequirement>): Int? = when (player) {
        Player.TargetPlayer, Player.TargetOpponent -> requirements.indices.filter {
            requirements[it] is TargetPlayer || requirements[it] is TargetOpponent
        }.singleOrNull()
        is Player.ContextPlayer -> player.index.takeIf { it in requirements.indices }
        is Player.BoundVariable -> requirements.indexOfFirst { it.id == player.name }.takeIf { it >= 0 }
        else -> null
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
        // Grim Contest lends its creature's toughness the same way a fighter lends its power.
        is MutualDamageEffect -> ownSideSlot(leaf.target1, requirements)
        else -> null
    }

    /**
     * The slots only *measured* by a gain for the ability's controller — "choose target creature you
     * control. You gain life equal to that creature's power plus its toughness" (Predator's Rapport),
     * "draw cards equal to target creature's power": the gain is the controller's, so the slot is
     * neither helped nor hurt, just read for its size. Named from our own side it is our biggest
     * object to pick, as a recursion slot is our best card; named from anyone's the simulation
     * weighs which board holds the biggest. Only when nothing else reads the slot.
     */
    private fun measuredGainSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> {
        fun isController(target: EffectTarget) =
            target == EffectTarget.Controller || target == EffectTarget.PlayerRef(Player.You)
        val amounts = leaves.mapNotNull { leaf ->
            when (leaf) {
                is GainLifeEffect -> leaf.amount.takeIf { isController(leaf.target) }
                is DrawCardsEffect -> leaf.count.takeIf { isController(leaf.target) }
                else -> null
            }
        }
        return amounts.flatMap(::measuredEntities).mapNotNullTo(HashSet()) { slotOf(it, requirements) }
    }

    /** The entities [amount] reads a numeric property of, through its arithmetic. */
    private fun measuredEntities(amount: DynamicAmount): List<EffectTarget> = when (amount) {
        is DynamicAmount.EntityProperty -> listOf(amount.entity)
        is DynamicAmount.Add -> measuredEntities(amount.left) + measuredEntities(amount.right)
        is DynamicAmount.Subtract -> measuredEntities(amount.left)
        is DynamicAmount.Multiply -> if (amount.multiplier > 0) measuredEntities(amount.amount) else emptyList()
        is DynamicAmount.Max -> measuredEntities(amount.left) + measuredEntities(amount.right)
        is DynamicAmount.Min -> measuredEntities(amount.left) + measuredEntities(amount.right)
        is DynamicAmount.IfPositive -> measuredEntities(amount.amount)
        is DynamicAmount.Divide -> measuredEntities(amount.numerator)
        else -> emptyList()
    }

    /**
     * The player slots whose **whole graveyard** a pipeline shuffles back into their own library —
     * "target player shuffles their graveyard into their library" (Clear the Mind, Learn from the
     * Past). It is the refill [lossSlots] leaves out, Feldon's Cane's and Elixir of Immortality's
     * own-graveyard shuffle given a target: spent cards to draw again and a deck-out staved off,
     * so the slot reads beneficial and our own graveyard is the one to name. Every-player shuffles
     * (Survive) name no slot and stay the graveyard hate the intent reader calls them. Only when
     * nothing else reads the slot.
     */
    private fun refillSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> {
        val graveyardOf = HashMap<String, Player>()
        for (leaf in leaves) {
            val source = (leaf as? GatherCardsEffect)?.source as? CardSource.FromZone ?: continue
            if (source.zone == Zone.GRAVEYARD && source.filter == GameObjectFilter.Any) graveyardOf[leaf.storeAs] = source.player
        }
        if (graveyardOf.isEmpty()) return emptySet()
        return leaves.mapNotNullTo(HashSet()) { leaf ->
            val move = leaf as? MoveCollectionEffect ?: return@mapNotNullTo null
            val player = graveyardOf[move.from] ?: return@mapNotNullTo null
            val to = move.destination as? CardDestination.ToZone ?: return@mapNotNullTo null
            if (to.zone == Zone.LIBRARY && to.player == player) playerSlotOf(player, requirements) else null
        }
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
            // "Exile target creature and all other creatures its controller controls with the same
            // name" (Declaration in Stone) as a pipeline: the stored target permanent, and the
            // permanents its controller controls, are that player's to lose. `ControllerOf` names the
            // controller of the effect's first chosen target.
            fun permanentSlot(slot: Int?) = slot?.takeIf { requirements.getOrNull(it)?.let(::objectFilter)?.zone == Zone.BATTLEFIELD }
            val entitySlot = when (val source = leaf.source) {
                is CardSource.Entity -> permanentSlot(slotOf(source.entity, requirements))
                is CardSource.ControlledPermanents -> permanentSlot(0.takeIf { source.player is Player.ControllerOf })
                else -> null
            }
            if (entitySlot != null) {
                slotOfHand[leaf.storeAs] = entitySlot
                continue
            }
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
    private fun groupOwnerSlot(space: IterationSpace.Group, requirements: List<TargetRequirement>): Int? =
        controllingPlayerSlot(space.filter.baseFilter, requirements)

    /**
     * "Prevent all combat damage that would be dealt this turn by creatures target opponent
     * controls" (Encircling Fissure): a shield over the sources a target player controls muzzles
     * that player's side, so the player slot it names reads harmful. "Prevent all damage that
     * would be dealt to this creature by target creature this turn" (Stonewise Fortifier,
     * `PreventionSourceFilter.Specific`) muzzles the targeted creature itself, so that object
     * slot reads harmful too. Null when the shield names no slot through its source filter.
     */
    private fun preventionSourceOwnerSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        if (leaf !is PreventDamageEffect || !leaf.preventDamage) return null
        return when (val sources = leaf.sourceFilter) {
            is PreventionSourceFilter.Matching -> controllingPlayerSlot(sources.filter, requirements)
            is PreventionSourceFilter.Specific -> slotOf(sources.source, requirements)
            PreventionSourceFilter.AnySource, is PreventionSourceFilter.Chosen,
            PreventionSourceFilter.TargetingRecipient -> null
        }
    }

    /**
     * "Prevent all combat damage that would be dealt by creatures other than target creature this
     * turn" (Terrifying Presence): the shield muzzles every source except the one the slot names,
     * so that creature alone keeps its damage — a slot meant for our own best attacker or blocker.
     * Null when the shield's source filter excludes no target slot.
     */
    private fun preventionSparedSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        if (leaf !is PreventDamageEffect || !leaf.preventDamage) return null
        val filter = (leaf.sourceFilter as? PreventionSourceFilter.Matching)?.filter ?: return null
        return filter.statePredicates.firstNotNullOfOrNull { predicate ->
            ((predicate as? StatePredicate.Not)?.predicate as? StatePredicate.IsEntity)
                ?.let { slotOf(it.reference, requirements) }
        }
    }

    /** The target-player slot whose permanents [filter] confines itself to, or null. */
    private fun controllingPlayerSlot(filter: GameObjectFilter, requirements: List<TargetRequirement>): Int? {
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
     * What the cards gathered off a target player's library top into [collections] do to that player,
     * when the gather is no mill of its own: "target player mills five cards, then puts each Goblin
     * card milled this way into their hand" (Grub's Command), "exile the top two cards of target
     * opponent's library" (Maralen, Fae Ascendant). Cards that only leave for a graveyard, exile or
     * our hand are their loss, [TargetPolarity.HARMFUL]; some of them handed back to their own hand
     * makes it [TargetPolarity.FLEXIBLE] — which library it is worth digging depends on the cards.
     * Null when nothing moves them out (a look, a reorder). The moves are read through gates
     * ([EffectWalker.payoffLeaves]): Tunnel Vision bins the pile only "if a card with the chosen
     * name is revealed this way".
     */
    private fun libraryTopPolarity(collections: Set<String>, effect: Effect): TargetPolarity? {
        val leaves = EffectWalker.payoffLeaves(effect)
        val names = HashSet(collections)
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
    internal fun removesChosenTargets(move: MoveCollectionEffect, effect: Effect): Boolean =
        move.from in allChosenTargets(effect) && removesForGood(move, effect)

    /**
     * "Destroy all Auras attached to target land" (Street Sweeper): [gather] collects what is
     * attached to a target ([CardSource.AttachedTo]) and a later step puts that collection into a
     * graveyard or exile for good. The target itself is untouched, but the Auras and Equipment on a
     * permanent are overwhelmingly its controller's own — Wild Growth on their land, a pump on
     * their creature — so stripping them is aimed at an opponent's permanent, the slot reading
     * [TargetPolarity.HARMFUL]. A slot only our own permanents fill is left alone (there the strip is
     * the price of something else), as is an attachment filter confined to our own.
     */
    private fun attachmentRemovalSlot(
        gather: GatherCardsEffect,
        effect: Effect,
        requirements: List<TargetRequirement>,
    ): Int? {
        val source = gather.source as? CardSource.AttachedTo ?: return null
        if (source.zone != Zone.BATTLEFIELD) return null
        if (source.filter.controllerPredicate == ControllerPredicate.ControlledByYou) return null
        val slot = slotOf(source.host, requirements) ?: return null
        if (isOwnSide(requirements[slot])) return null
        val removed = EffectWalker.leaves(effect).any {
            it is MoveCollectionEffect && it.from == gather.storeAs && removesForGood(it, effect)
        }
        return slot.takeIf { removed }
    }

    /**
     * Whether [move] puts what it moves into a graveyard or exile for good — [removesChosenTargets]
     * without asking where the moved collection came from.
     */
    private fun removesForGood(move: MoveCollectionEffect, effect: Effect): Boolean {
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
     * Whether [effect] is a pipeline blink: it exiles its own chosen targets and, in the same
     * resolution, moves them — the exiled collection, or the source's linked-exile pile it filled —
     * back onto the battlefield (Displace, Ghostly Flicker, Illusionist's Stratagem). A move behind a
     * condition is still a leaf here, so this reads the card's plan, as [EffectWalker.blinkMoves]
     * does for "exile it, then return it".
     */
    internal fun blinksChosenTargets(effect: Effect): Boolean {
        val leaves = EffectWalker.leaves(effect)
        val chosen = chosenTargetCollections(effect)
        if (chosen.isEmpty()) return false
        val exiles = leaves.filterIsInstance<MoveCollectionEffect>()
            .filter { it.from in chosen && (it.destination as? CardDestination.ToZone)?.zone == Zone.EXILE }
        if (exiles.isEmpty()) return false
        val exiled = exiles.flatMapTo(HashSet()) { listOfNotNull(it.from, it.storeMovedAs) }
        if (exiles.any { it.linkToSource }) {
            leaves.filterIsInstance<GatherCardsEffect>()
                .filter { it.source is CardSource.FromLinkedExile }.mapTo(exiled) { it.storeAs }
        }
        return leaves.any {
            it is MoveCollectionEffect && it.from in exiled &&
                (it.destination as? CardDestination.ToZone)?.zone == Zone.BATTLEFIELD
        }
    }

    /**
     * The graveyard slots a pipeline gathers and brings back — [recursionSlot]'s single-card move,
     * spelled as `gather` + `move`. "Return target creature card from your graveyard to your hand"
     * as `moveTracked(gather(CardSource.Entity(t)), ToZone(HAND))`, to read what came back (Foul
     * Renewal), and "put any number of target creature cards from your graveyard on top of your
     * library" as `toLibraryTop(gather(ChosenTargets))` (Gravepurge, Forever Young): the card comes
     * back to us, so the best card is the one to name. As with the single move, onto our own
     * battlefield or into our hand from any graveyard, into the library only out of our own — from
     * anyone's graveyard a tuck is graveyard hate. A `ChosenTargets` gather reads only when every
     * slot is a graveyard card, so a battlefield slot beside it keeps its own reading.
     */
    private fun pipelineGraveyardReturnSlots(effect: Effect, requirements: List<TargetRequirement>): Set<Int> {
        fun inGraveyard(slot: Int) = objectFilter(requirements[slot])?.zone == Zone.GRAVEYARD
        // The return may sit in a delayed trigger carrying the gathered card ("return that card …
        // at the beginning of your next upkeep", Rescue from the Underworld).
        val leaves = EffectWalker.leavesThroughDelayed(effect)
        val returned = mutableSetOf<Int>()
        for (gather in leaves.filterIsInstance<GatherCardsEffect>()) {
            val slots = when (val source = gather.source) {
                is CardSource.ChosenTargets ->
                    requirements.indices.toList().takeIf { it.isNotEmpty() && it.all(::inGraveyard) }
                is CardSource.Entity -> slotOf(source.entity, requirements)?.takeIf(::inGraveyard)?.let(::listOf)
                else -> null
            } ?: continue
            val collections = hashSetOf(gather.storeAs)
            do {
                val before = collections.size
                for (leaf in leaves) collections += CardIntentAnalyzer.derivedCollections(leaf, collections)
            } while (collections.size != before)
            val comesBack = leaves.any { leaf ->
                val move = leaf as? MoveCollectionEffect ?: return@any false
                val to = move.destination as? CardDestination.ToZone ?: return@any false
                move.from in collections && move.faceDown == null && to.player == Player.You && when (to.zone) {
                    Zone.HAND -> true
                    Zone.BATTLEFIELD -> !move.underOwnersControl
                    Zone.LIBRARY -> slots.all { ownGraveyardOnly(objectFilter(requirements[it])) }
                    else -> false
                }
            }
            if (comesBack) returned += slots
        }
        return returned
    }

    /**
     * The permanent slots a pipeline gathers one at a time and removes for good — "exile any number
     * of target creatures. For each creature exiled this way, its controller creates a 4/4 Sphinx"
     * (Hour of Need) as `IfYouDo(moveTracked(gather(CardSource.Entity(t)), ToZone(EXILE)), then = …)`
     * per target: the single-target exile it is, spelled as gather + tracked move so the payoff can
     * ask what left. The move usually sits in an "if you do" gate's action, which the payoff walk
     * leaves out as a price; here it is the card's removal, not a cost paid with our own things,
     * so the gate's interior is read too ([CardIntentAnalyzer.gateInteriorLeaves]). A slot that
     * can only hold our own permanents is skipped: exiling our own thing is a price, not removal.
     */
    private fun pipelineRemovalSlots(effect: Effect, requirements: List<TargetRequirement>): Set<Int> =
        pipelineRemovals(effect, requirements).mapTo(HashSet()) { it.first }

    /**
     * Each [pipelineRemovalSlots] slot with the move that removes it — what the intent reader
     * ([CardIntentAnalyzer]) reads as the single-target destroy or exile it amounts to.
     */
    internal fun pipelineRemovals(effect: Effect, requirements: List<TargetRequirement>): List<Pair<Int, MoveCollectionEffect>> {
        val permanent = permanentSlots(requirements).toSet()
        val leaves = CardIntentAnalyzer.gateInteriorLeaves(effect)
        val removed = mutableListOf<Pair<Int, MoveCollectionEffect>>()
        for (gather in leaves.filterIsInstance<GatherCardsEffect>()) {
            val entity = (gather.source as? CardSource.Entity)?.entity ?: continue
            val slot = slotOf(entity, requirements)?.takeIf { it in permanent && !isOwnSide(requirements[it]) } ?: continue
            leaves.firstOrNull { it is MoveCollectionEffect && it.from == gather.storeAs && removesForGood(it, effect) }
                ?.let { removed += slot to (it as MoveCollectionEffect) }
        }
        return removed
    }

    /** The permanent slots a pipeline blinks ([blinksChosenTargets]). */
    private fun pipelineBlinkSlots(effect: Effect, requirements: List<TargetRequirement>): List<Int> {
        if (!blinksChosenTargets(effect)) return emptyList()
        return requirements.indices.filter { slot ->
            val requirement = (requirements[slot] as? TargetOther)?.baseRequirement ?: requirements[slot]
            requirement is TargetObject && requirement.filter.zone.let { it == null || it == Zone.BATTLEFIELD }
        }
    }

    /**
     * Whether [requirements]' first slot is a target opponent and some step of [leaves] has that
     * player make the selection ([Chooser.TargetPlayer] resolves to the first target).
     */
    private fun decidedByTargetOpponent(leaves: List<Effect>, requirements: List<TargetRequirement>): Boolean {
        val first = requirements.firstOrNull()?.let { (it as? TargetOther)?.baseRequirement ?: it }
        if (first !is TargetOpponent) return false
        return leaves.any {
            (it is SelectFromCollectionEffect && it.chooser == Chooser.TargetPlayer) ||
                (it is ChoosePileEffect && it.chooser == Chooser.TargetPlayer)
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
        // "If damage is prevented this way, you may have Channel Harm deal that much damage to
        // target creature": the shield's reaction can name the spell's own targets, read as the
        // reaction's leaves are anywhere else — a damage reaction aims at their side.
        val reaction = (leaf as? PreventDamageEffect)?.onPrevented
        if (leaf is PreventDamageEffect && reaction != null) {
            return classify(leaf.copy(onPrevented = null), definitions, source) +
                EffectWalker.leaves(reaction).flatMap { classify(it, definitions, source) }
        }
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
        // "The controller of target spell copies it" (Meletis Charlatan) hands the copy to the
        // spell's own controller, so it is a gift to that side: aim it at our own spells.
        if (leaf is CopyTargetSpellEffect) {
            val polarity = if (leaf.copier is Player.ControllerOf) TargetPolarity.BENEFICIAL else TargetPolarity.FLEXIBLE
            return listOf(leaf.target to polarity)
        }
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
        // "Target opponent loses 1 life for each attacking creature you control. You gain that much
        // life" (Foul-Tongue Shriek, Bloodhunter Bat's "target player"): the single LoseLife and
        // GainLife above in one step — the player who loses is hurt, the one who gains is helped.
        if (leaf is DrainLifeEffect) return listOfNotNull(
            leaf.from.takeIf { it.isSlot() }?.let { it to TargetPolarity.HARMFUL },
            leaf.to.takeIf { it.isSlot() }?.let { it to TargetPolarity.BENEFICIAL },
        )
        // "Target opponent gains control of this Equipment" (Avarice Amulet, Jinxed Idol), "target
        // opponent gains control of target permanent you control" (Harmless Offering): a hand-off
        // whose worth to the receiver is the permanent's — a gift when it is an Equipment, a curse
        // when it is Jinxed Idol — so the player slot is the simulation's to price. The permanent
        // handed over is the price of it, read as an exchange's own side is: understood, pruning
        // nothing, and the removal ranking still gives away our worst.
        if (leaf is GiveControlToTargetPlayerEffect) return listOfNotNull(
            leaf.permanent.takeIf { it.isSlot() }?.let { it to TargetPolarity.FLEXIBLE },
            leaf.newController.takeIf { it.isSlot() }?.let { it to TargetPolarity.FLEXIBLE },
        )
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
            // "Target creature can't attack" (detain, Inaction Injunction): an attacker benched.
            is CantAttackEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Creatures target opponent controls don't untap during their next untap step"
            // (Imaginary Threats, Exhaustion, Mana Vapors): the target player's permanents stay
            // tapped through a turn — a tapper's lock on that player, never a gift.
            is SkipUntapEffect -> leaf.target to TargetPolarity.HARMFUL
            // "You control target player during that player's next turn" (Mindslaver, Worst Fears):
            // their turn is played against them; aimed at ourselves it does nothing.
            is HijackNextTurnEffect -> leaf.target to TargetPolarity.HARMFUL
            // Forcing a block can trade the target into a blocker or draw blockers away from its
            // teammates. Neither the one-blocker nor the all-blockers form fixes a target side;
            // keep both sides available to simulation, including beside a pump (Emergent Growth).
            is MustBeBlockedEffect -> leaf.target to TargetPolarity.FLEXIBLE
            // "Change the text of target spell or permanent by replacing all instances of one color
            // word with another" (Sleight of Mind, Trait Doctoring, Crystal Spray's basic land type):
            // its worth is the words the target carries — our creature's "protection from white"
            // turned to the opponent's colour, or their "islandwalk" turned to a land we don't
            // control — and the words are chosen on resolution. Neither side is fixed, so both stay
            // open and the simulation weighs what each candidate's rewritten text does.
            is ChangeWordInTextEffect -> leaf.target to TargetPolarity.FLEXIBLE
            // "Target land becomes a 3/3 Elemental creature with haste until end of turn" (Embodiment
            // of Fury): a body-sized animate names nothing here, like a large base above, and alone it
            // reads as a gift ([animateSlots]). Beside an effect that reads the slot one way (Lizard,
            // Connors's Curse strips abilities as it rebases to 4/4) that effect decides. A body too
            // small to fight with (a 0/1 or 1/1) is as often the setup for killing it: either side.
            is BecomeCreatureEffect -> if (baseStatsPolarity(leaf.power, leaf.toughness) != null)
                leaf.target to TargetPolarity.FLEXIBLE else return emptyList()
            // "Target creature blocks this turn if able" (Peema Aether-Seer, Culvert Ambusher):
            // takes the block choice away from its controller, dragging the creature into a block
            // it would not have made — the opponent's creature, like "can't block" above.
            is MarkMustBlockEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Target creature an opponent controls attacks … if able" (Alluring Siren, Gideon,
            // Battle-Forged): the attack choice taken away — the attacking mirror of the forced block.
            is MarkMustAttackEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Up to one target creature defending player controls blocks it this combat if able"
            // (Impetuous Devils, Avalanche Tusker): the same forced block, pinned to the attacker
            // that names it — a blocker dragged off the rest of the defence or into a bad trade.
            is ForceBlockEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Transform target Werewolf you control" (Waxing Moon), "transform target Incubator
            // token you control" (Elvish Vatkeeper, Seedpod Caretaker): the cards that aim a
            // transform aim it at their controller's own double-faced permanent, whose other face
            // is the one they want — a gift. A permanent transforming itself names no slot.
            is TransformEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is CantBeRegeneratedEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Target creature loses all abilities" strips its holder — the static LoseAllAbilities
            // an Aura grants reads the same. A drawback-only creature gains from it, but that is a
            // board fact the cards that aim it (Lizard, Connors's Curse; Azure Beastbinder) don't rely on.
            is RemoveAllAbilitiesEffect -> leaf.target to TargetPolarity.HARMFUL
            // "Turn target creature with a morph ability face down" (Backslide, Master of the Veil):
            // its holder is left a nameless 2/2 with no abilities until it pays the morph cost again —
            // the same strip as losing all abilities, aimed at an opponent's creature.
            is TurnFaceDownEffect -> leaf.target to TargetPolarity.HARMFUL
            // "That player can't get counters" (Suncleanser): a prohibition on the player — the
            // energy, experience and other counters they build toward stop coming. Locking out
            // poison would help them, but the cards aim it at an opponent to shut off their engine.
            is LockPlayerCountersEffect -> leaf.target to TargetPolarity.HARMFUL
            is MarkExileOnDeathEffect -> leaf.target to TargetPolarity.HARMFUL
            is TapUntapEffect -> leaf.target to
                if (leaf.tap) TargetPolarity.HARMFUL else TargetPolarity.BENEFICIAL
            is ModifyStatsEffect -> leaf.target to oneShotStatsPolarity(leaf.powerModifier, leaf.toughnessModifier)
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
            is MutualDamageEffect -> leaf.target2 to TargetPolarity.HARMFUL
            is RegenerateEffect -> leaf.target to TargetPolarity.BENEFICIAL
            // "Target creature with defender can attack this turn as though it didn't have defender"
            // (Assault Formation): lifting a wall's defender only ever turns our own blocker into an attacker.
            is CanAttackDespiteDefenderThisTurnEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is HealDamageEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is PreventDamageEffect -> leaf.target to when {
                !leaf.preventDamage -> null
                leaf.direction == PreventionDirection.ToTarget -> TargetPolarity.BENEFICIAL
                leaf.direction == PreventionDirection.FromTarget -> TargetPolarity.HARMFUL
                else -> null
            }
            is GrantKeywordEffect -> leaf.target to keywordPolarity(leaf.keyword)
            is GrantEvasionKeywordEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is GrantCantBeBlockedByChosenColorEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is GrantProtectionFromChosenColorEffect -> leaf.target to TargetPolarity.BENEFICIAL
            is GrantHexproofFromChosenColorEffect -> leaf.target to TargetPolarity.BENEFICIAL
            // A marker counter with no rule of its own (fate) means what the card that places it
            // does to what bears one — see [markerCounterPolarity].
            is AddCountersEffect -> leaf.target to
                (counterPolarity(leaf.counterType) ?: markerCounterPolarity(source, leaf.counterType, definitions))
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
            // "Return this card to the battlefield transformed attached to target opponent" (Accursed
            // Witch's Infectious Curse), "… attached to target creature" (Gryff's Boon): the target
            // gets whatever the returned Aura — the back face when it returns transformed — does to
            // what it enchants, the same reading the Aura's own enchant target gets. Unread (no
            // source card, or an Aura that reads mixed) it names nothing here, and
            // [forRequirements] falls back on the side the target is confined to.
            is ReturnSelfToBattlefieldAttachedEffect -> source
                ?.let { if (leaf.transformed) it.backFace else it }
                ?.let { attachmentPolarity(it, definitions) }
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
            // "Target creature becomes a Coward" / "becomes a Warrior" on a card whose own "Cowards
            // can't block Warriors" names the type: the blocker type takes the creature's blocks
            // away, the protected type makes it evade them — see [TypeEvasion]. A type change the
            // card's statics don't name stays unread: what it is for is the board's question.
            is SetCreatureSubtypesEffect -> TypeEvasion.of(source, leaf)?.let { leaf.target to it.polarity }
                ?: return emptyList()
            // "Target land becomes an Island in addition to its other types" on a card whose own
            // attack names Islands (Sealock Monster's "can't attack unless defending player controls
            // an Island", or islandwalk): the type is handed to the defending player's land to open
            // that attack, so the slot is aimed at an opponent's land — see [LandTypeAttackEnabler].
            // The mana ability the type brings them is the price. Any other added type stays unread.
            is AddSubtypeEffect -> if (LandTypeAttackEnabler.of(source, leaf)) leaf.target to TargetPolarity.HARMFUL
                else return emptyList()
            // "If a triggered ability of that creature triggers, it triggers an additional time"
            // (Lwaxana Troi, Charming Guest): a doubler only ever repeats its holder's controller's
            // own triggers, so it is aimed at a creature of yours.
            // "Its activated abilities can't be activated" handed to the target itself (detain's
            // second half): a lock on what it holds, aimed at the opponent's permanent.
            is GrantStaticAbilityEffect -> when {
                leaf.ability is AdditionalSourceTriggers -> leaf.target to TargetPolarity.BENEFICIAL
                (leaf.ability as? PreventActivatedAbilities)?.filter?.statePredicates?.contains(StatePredicate.IsSource) == true ->
                    leaf.target to TargetPolarity.HARMFUL
                else -> return emptyList()
            }
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
            // A draw that is *sized by* a slot's player rather than aimed at them — "draw cards equal
            // to the damage dealt to target opponent this turn" (Knollspine Dragon): the controller
            // draws, so the draw is no gift or curse to the player the amount measures. The slot is
            // read, not acted on, so both sides stay open and the simulation weighs the candidates.
            // "Any number of target players each draw twenty cards" (Jace, Memory Adept's −7): a draw
            // big enough to empty a library is a mill-out on an opponent as much as card advantage
            // for whoever can survive it, so the slot reads either way and the simulation weighs
            // each player's library. A loop over the targets names each one as
            // `PlayerRef(ContextPlayer(i))`, the player spelling of `ContextTarget(i)`.
            is DrawCardsEffect -> when {
                leaf.target.isSlot() || (leaf.target as? EffectTarget.PlayerRef)?.player is Player.ContextPlayer -> {
                    val slot = (((leaf.target as? EffectTarget.PlayerRef)?.player as? Player.ContextPlayer)
                        ?.let { EffectTarget.ContextTarget(it.index) }) ?: leaf.target
                    val decks = ((leaf.count as? DynamicAmount.Fixed)?.amount ?: 0) >= DECKING_DRAW
                    slot to if (decks) TargetPolarity.FLEXIBLE else TargetPolarity.BENEFICIAL
                }
                else -> ((leaf.count as? DynamicAmount.TurnTracking)?.player as? Player.ContextPlayer)
                    ?.let { EffectTarget.ContextTarget(it.index) to TargetPolarity.FLEXIBLE }
                    ?: return emptyList()
            }
            is GainLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.BENEFICIAL else return emptyList()
            is LoseLifeEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
            // "Target player loses the game" (Door to Nothingness): only ever aimed at an opponent.
            is LoseGameEffect -> if (leaf.target.isSlot()) leaf.target to TargetPolarity.HARMFUL else return emptyList()
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
     * The slots a body-sized "becomes an N/N creature" names (Embodiment of Fury's landfall
     * animate). Animating a land or noncreature artifact hands its controller an attacker or
     * blocker for the duration, so on its own that slot is a gift ([animatesNoncreature]); on a
     * slot that can already hold a creature (Suit Up, Startling Development) it is a base-P/T set
     * like [largeBaseSlots], growing a small creature and shrinking a big one, so it reads either
     * way. Beside an effect that reads the slot one way, that effect decides, which is why
     * [classify] leaves the leaf unattributed and this is applied last.
     */
    /** Whether [requirement] only reaches permanents that aren't creatures yet: "target land", "target noncreature artifact". */
    private fun animatesNoncreature(requirement: TargetRequirement): Boolean {
        val filter = (((requirement as? TargetOther)?.baseRequirement ?: requirement) as? TargetObject)?.filter
            ?: return false
        if (filter.alternatives.isNotEmpty()) return false
        val predicates = filter.baseFilter.cardPredicates
        return CardPredicate.IsCreature !in predicates &&
            (CardPredicate.IsNoncreature in predicates || CardPredicate.IsLand in predicates)
    }

    private fun animateSlots(leaves: List<Effect>, requirements: List<TargetRequirement>): Set<Int> =
        leaves.filterIsInstance<BecomeCreatureEffect>()
            .filter { baseStatsPolarity(it.power, it.toughness) == null }
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
     * A one-shot "+X/-X" whose toughness loss is a count that grows with the board — "where X is the
     * number of artifacts you control" (Tezzeret the Schemer), "sacrifice X Squirrels" (Chatterfang)
     * — is removal: X is sized to kill, and whatever the power side adds dies with the creature. A
     * fixed trade — "+3/-3 until end of turn" (Spatial Contortion), "+2/-1" — kills a small creature
     * of theirs as often as it turns a sturdy one of ours into a bigger attacker, so like the lasting
     * form ([lastingTradePolarity]) it reads [TargetPolarity.FLEXIBLE] and the simulation compares
     * the candidates.
     */
    private fun oneShotStatsPolarity(power: DynamicAmount, toughness: DynamicAmount): TargetPolarity? =
        statsPolarity(power, toughness)
            ?: TargetPolarity.HARMFUL.takeIf { toughness !is DynamicAmount.Fixed && AmountSign.of(toughness) == AmountSign.LOSS }
            ?: if (power is DynamicAmount.Fixed && toughness is DynamicAmount.Fixed) {
                lastingTradePolarity(power.amount, toughness.amount)
            } else null

    /**
     * A lasting "+N/-M" or "-N/+M" on the host (Boon of Emrakul's "+3/-3", Maggot Therapy's "+2/-2",
     * Twisted Experiment's "+3/-1") trades one stat for the other for as long as it stays attached:
     * it kills a small creature of theirs and turns a sturdy one of ours into a bigger attacker.
     * Which side it belongs on is the board's call, so it reads [TargetPolarity.FLEXIBLE] and the
     * simulation compares the candidates. Null unless the two signs really differ.
     */
    private fun lastingTradePolarity(power: Int, toughness: Int): TargetPolarity? =
        TargetPolarity.FLEXIBLE.takeIf { (power > 0 && toughness < 0) || (power < 0 && toughness > 0) }

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

    /**
     * A counter kind [counterPolarity] leaves unread, put by [source] — what the card's own other
     * abilities do to a target that must bear one. Triad of Fates' fate counter is the price of
     * its "exile target creature that has a fate counter on it": marking a creature is the
     * targeting half of that removal, so it goes on theirs. A blink beside the removal (its
     * other fate ability) is a choice either side can use and doesn't overturn it; a card whose
     * keyed abilities only help or only disagree reads that way. Null when no ability of
     * [source] keys a target on the counter. The keyed abilities are read without [source], so
     * a marker reading never recurses into another.
     */
    internal fun markerCounterPolarity(source: CardDefinition?, type: CounterType, definitions: CardLookup): TargetPolarity? {
        if (source == null) return null
        val readings = source.script.activatedAbilities.flatMap { ability ->
            val reqs = ability.targetRequirements
            val keyed = reqs.indices.filter { slot ->
                objectFilter(reqs[slot])?.baseFilter?.statePredicates
                    ?.any { it is StatePredicate.HasCounter && it.counterType == type } == true
            }
            if (keyed.isEmpty()) return@flatMap emptyList()
            val slots = forRequirements(ability.effect, reqs, definitions)
            keyed.mapNotNull { slots.getOrNull(it) }
        }.filter { it != TargetPolarity.UNKNOWN }.toSet()
        return when {
            readings.isEmpty() -> null
            TargetPolarity.HARMFUL in readings && TargetPolarity.BENEFICIAL !in readings -> TargetPolarity.HARMFUL
            readings.size == 1 -> readings.single()
            else -> TargetPolarity.FLEXIBLE
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
        // Petrification marks a creature Xathrid Gorgon has locked down (defender, no activations):
        // the counter itself does nothing, but it is only ever placed as part of that lock.
        CounterType.STUN, CounterType.FINALITY, CounterType.PETRIFICATION,
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
        // "Gains toxic 1" (Skrelv, Defector Mite) is granted as `TOXIC_<n>`, the counted form of
        // [Keyword.TOXIC]: its combat damage also poisons — good for the attacker holding it.
        if (name.uppercase().startsWith("TOXIC_")) return TargetPolarity.BENEFICIAL
        runCatching { Keyword.valueOf(name.uppercase()) }.getOrNull()?.let { keyword ->
            return when (keyword) {
                in BENEFICIAL_KEYWORDS -> TargetPolarity.BENEFICIAL
                Keyword.DEFENDER -> TargetPolarity.HARMFUL
                else -> null
            }
        }
        return when (runCatching { AbilityFlag.valueOf(name.uppercase()) }.getOrNull()) {
            AbilityFlag.CANT_BE_BLOCKED -> TargetPolarity.BENEFICIAL
            // "Assigns combat damage equal to its toughness rather than its power" (Plagon, Bill the
            // Pony): granted to our own high-toughness creatures to turn them into attackers.
            AbilityFlag.ASSIGNS_COMBAT_DAMAGE_AS_TOUGHNESS -> TargetPolarity.BENEFICIAL
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
        // The mirror, "Enchant creature you control" (Journey to Eternity, whose whole card is a
        // dies trigger that brings the creature back): only our own creature can wear it, so the
        // slot is a gift on our side — the best creature is the one to name.
        if (reading == TargetPolarity.UNKNOWN && aura != null && isOwnSide(aura) &&
            attachmentReadings(card, definitions, hostIs = null).isEmpty()
        ) return TargetPolarity.BENEFICIAL
        return reading
    }

    private fun hostReading(card: CardDefinition, definitions: CardLookup): TargetPolarity {
        if (hostTrait(card) == null) {
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
     * [card]'s attachment reading for a host with its [hostTrait], and for any other host — null
     * for a world in which none of its abilities applies.
     */
    private fun hostWorlds(card: CardDefinition, definitions: CardLookup): Pair<TargetPolarity?, TargetPolarity?> {
        fun world(hostIs: Boolean) =
            attachmentReadings(card, definitions, hostIs).takeIf { it.isNotEmpty() }?.let(::combine)
        return world(hostIs = true) to world(hostIs = false)
    }

    /**
     * The one host trait every conditional static of [card] turns on — "as long as enchanted
     * creature is a Pirate" (its creature type), "as long as it's an enchantment" (Nyx Infusion's
     * card type, an `EntityMatches` on the enchanted permanent), or their "otherwise" — or null when
     * it has none, or any conditional static reads something else.
     */
    private fun conditionalHostTrait(card: CardDefinition): HostTrait? {
        val conditional = card.script.staticAbilities.filterIsInstance<ConditionalStaticAbility>()
        if (conditional.isEmpty()) return null
        val traits = conditional.map { hostTraitOf(it.condition) ?: return null }
        return traits.distinct().singleOrNull()
    }

    /**
     * What [card]'s abilities turn on about the host: the trait its conditional statics name ([conditionalHostTrait]), or the one
     * card type every host-type gate of its statics and replacements names ([hostTypeGate]) — Armament
     * of Nyx's double strike "as long as it's an enchantment" and its prevention "otherwise". Null when
     * neither, or when its gates name more than one card type.
     */
    private fun hostTrait(card: CardDefinition): HostTrait? {
        conditionalHostTrait(card)?.let { return it }
        if (card.script.staticAbilities.any { it is ConditionalStaticAbility }) return null
        val gates = card.script.staticAbilities.mapNotNull { hostTypeGate(it) } +
            card.script.replacementEffects.mapNotNull { hostTypeGate(it) }
        return gates.map { it.first }.distinct().singleOrNull()?.let(HostTrait::CardType)
    }

    /**
     * The card type a host-scoped static is gated on, and whether it applies to a host of that type
     * (true) or to any other (false): "enchanted creature has double strike as long as it's an
     * enchantment" is a grant over `Enchantment.attachedToBySource()`. Null when ungated.
     */
    private fun hostTypeGate(static: StaticAbility): Pair<String, Boolean>? = when (static) {
        is GrantKeyword -> static.filter.takeIf(::reachesHost)?.baseFilter?.let(::typeGate)
        else -> null
    }

    /** [hostTypeGate] for a [hostDamagePrevention] replacement, gated by the damage source's type. */
    private fun hostTypeGate(replacement: ReplacementEffect): Pair<String, Boolean>? =
        if (!hostDamagePrevention(replacement)) null
        else typeGate(((replacement as PreventDamage).appliesTo as EventPattern.DamageEvent).source)

    private fun typeGate(filter: GameObjectFilter): Pair<String, Boolean>? =
        filter.cardPredicates.firstNotNullOfOrNull { predicate ->
            HOST_GATE_TYPES[predicate]?.let { it to true }
                ?: (predicate as? CardPredicate.Not)?.predicate?.let { HOST_GATE_TYPES[it] }?.let { it to false }
        }

    /** The card-type predicates a [hostTypeGate] reads, as [ProjectedState.hasType] spells the type. */
    private val HOST_GATE_TYPES: Map<CardPredicate, String> = mapOf(
        CardPredicate.IsEnchantment to "ENCHANTMENT",
        CardPredicate.IsArtifact to "ARTIFACT",
    )

    /**
     * Whether a group filter names the permanent its source is attached to — [Scope.AttachedTo], or
     * a battlefield group narrowed to it with `attachedToBySource()` (the form a type-gated grant
     * takes, so the layer system re-checks the gate as the host's types change).
     */
    private fun reachesHost(filter: GroupFilter): Boolean =
        filter.scope == Scope.AttachedTo ||
            (filter.scope == Scope.Battlefield && StatePredicate.IsAttachedToBySource in filter.baseFilter.statePredicates)

    /**
     * "Prevent all damage that would be dealt by enchanted creature" (Armament of Nyx's otherwise
     * half): every point the host deals — in a fight or by its abilities — comes to nothing, so it
     * attacks, blocks and burns for no effect. A curse on the host, as "can't attack or block" is.
     */
    private fun hostDamagePrevention(replacement: ReplacementEffect): Boolean {
        if (replacement !is PreventDamage || replacement.amount != null || replacement.half || replacement.allBut != null) return false
        val event = replacement.appliesTo as? EventPattern.DamageEvent ?: return false
        return StatePredicate.IsAttachedToBySource in event.source.statePredicates && event.recipient == Recipient.Any &&
            event.source.controllerPredicate == null
    }

    /**
     * The host trait a conditional static's condition (or its "otherwise", a [NotCondition]) asks
     * about: the enchanted creature's subtype, or the enchanted permanent being exactly one of the
     * [HOST_GATE_TYPES] card types ("as long as it's an enchantment").
     */
    private fun hostTraitOf(condition: Condition): HostTrait? = when (val inner = (condition as? NotCondition)?.condition ?: condition) {
        is EnchantedCreatureHasSubtype -> HostTrait.CreatureType(inner.subtype)
        is EntityMatches -> inner.filter
            .takeIf {
                inner.entity == EffectTarget.EnchantedPermanent && it.cardPredicates.size == 1 &&
                    it.statePredicates.isEmpty() && it.controllerPredicate == null && it.anyOf.isEmpty()
            }
            ?.let { HOST_GATE_TYPES[it.cardPredicates.single()] }
            ?.let(HostTrait::CardType)
        else -> null
    }

    /**
     * Every reading [attachmentPolarity] combines. With [hostIs] set, a static gated on the host's
     * creature or card type ([conditionalHostTrait]) or card type ([hostTypeGate]) counts only in the world where its
     * condition holds.
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
            // "Enchanted creature attacks each combat if able" (Skin Invasion): its controller loses
            // the choice to hold it back, so it runs into the blockers of whoever cast it — a curse
            // on its own. Beside a gift ("gets +2/+2 and attacks each combat if able", Furor of the
            // Bitten) it is the gift's price, as a harmful granted trigger is, not a veto on it.
            if (static is MustAttack && static.filter.scope == Scope.AttachedTo) {
                prices += TargetPolarity.HARMFUL
                continue
            }
            if (hostIs != null && static is ConditionalStaticAbility) {
                val holds = when (static.condition) {
                    is NotCondition -> !hostIs
                    else -> hostIs
                }
                if (!holds) continue
            }
            if (hostIs != null && hostTypeGate(static)?.second?.let { it != hostIs } == true) continue
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
        for (ability in card.script.triggeredAbilities) readings += hostDamageTriggerReadings(ability)
        for (ability in card.script.triggeredAbilities) readings += hostStepTriggerReadings(ability)
        for (ability in card.script.triggeredAbilities) readings += hostTapTriggerReadings(ability)
        for (ability in card.script.triggeredAbilities) readings += hostAttackCopyReadings(ability)
        for (ability in card.script.triggeredAbilities) readings += hostCopySourceReadings(ability)
        for (ability in card.script.triggeredAbilities) readings += hostCombatTriggerReadings(ability, definitions)
        // "{1}{G}{U}, Return this Aura to its owner's hand: Put two +1/+1 counters on enchanted
        // creature" (Krasis Incubation): an activation whose cost takes the attachment off the
        // board ends whatever else it does to the host, and only its controller chooses to pay
        // it — a parting gift that lifts the curse, a consolation as a granted ability is. Beside
        // an Arrest lock it never makes the Aura a gift, so a harm outranks it.
        val (releases, keeps) = card.script.activatedAbilities.partition { removesAttachment(it.cost) }
        val abilityEffects = card.script.triggeredAbilities.map { it.effect } + keeps.map { it.effect }
        for (effect in releases.map { it.effect }) {
            consolations += EffectWalker.payoffLeaves(effect).flatMap { leaf ->
                classify(leaf, definitions).filter { (target, _) -> target in HOST_TARGETS }.map { it.second }
            }
        }
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
            readings += hostCarrierHarms(effect)
            // "That player sacrifices it unless they pay {X}" (Soul Tithe), "destroy that land unless
            // that player pays {1} or 1 life" (Erosion): the host's controller pays every upkeep or
            // loses it — a curse on the host either way ([CardIntentAnalyzer.hostTaxSuffer] only
            // answers for a suffer that takes the host off the battlefield).
            if (EffectWalker.payoffLeaves(effect).any { CardIntentAnalyzer.hostTaxSuffer(it) != null }) {
                readings += TargetPolarity.HARMFUL
            }
        }
        // A replacement effect on an Aura (Sandskin's "prevent all combat damage dealt to and by
        // enchanted creature") is read nowhere above, and is often the whole card. Without it the
        // statics alone could call an either-way Aura one-sided, so its presence is a veto — except
        // for an entry choice (Traveler's Cloak's land type), which only parameterises a static.
        // A self-only zone redirect (disturb's "if this would be put into a graveyard, exile it
        // instead") moves the Aura itself and never touches the creature, so it is no veto either.
        // Preventing all damage the host would deal ([hostDamagePrevention]) reads as a curse, and
        // when its source filter is gated on the host's card type (Armament of Nyx's "otherwise")
        // it counts only for the side of the HostSplit whose host it can apply to.
        for (replacement in card.script.replacementEffects) {
            when {
                replacement.affectsOnlyTheAuraItself() -> Unit
                // "Nontoken creatures you control enter as a copy of enchanted creature" (Infinite
                // Reflection): the host is only the copy source — untouched, and worth copying
                // whichever side it is on, so the simulation picks the best creature to name.
                replacement is EntersAsCopy && replacement.copyOf in HOST_TARGETS ->
                    readings += TargetPolarity.FLEXIBLE
                hostDamagePrevention(replacement) ->
                    if (hostIs == null || hostTypeGate(replacement)?.second?.let { it != hostIs } != true) {
                        readings += TargetPolarity.HARMFUL
                    }
                else -> readings += TargetPolarity.UNKNOWN
            }
        }
        // A price needs a gift the Aura gives outright, so judge it before any consolation joins.
        val payPrices = TargetPolarity.BENEFICIAL !in readings || TargetPolarity.HARMFUL in readings
        if (TargetPolarity.HARMFUL !in readings) readings += consolations
        // A payoff off the host's death (Fruit of the First Tree) is the same kind of consolation
        // beside a curse — Gremlin Infestation's Gremlin when the artifact it burns dies — and the
        // whole point of an Aura that does nothing else. Only then: a static no reader handles
        // leaves no trace in [readings] (Public Enemy's "all creatures attack enchanted creature's
        // controller" beside its death draw), so an Aura with statics or activations keeps its gap.
        if (TargetPolarity.HARMFUL !in readings && card.script.auraTarget != null &&
            card.script.staticAbilities.isEmpty() && card.script.activatedAbilities.isEmpty()
        ) {
            for (ability in card.script.triggeredAbilities) readings += hostDeathPayoffReadings(ability)
        }
        for (ability in card.script.triggeredAbilities) readings += hostDeathReturnReadings(ability)
        if (payPrices) readings += prices
        return readings
    }

    /** Whether paying [cost] takes the attachment itself off the battlefield (return, sacrifice, exile it). */
    private fun removesAttachment(cost: AbilityCost): Boolean = when (cost) {
        AbilityCost.ReturnSelfToHand, AbilityCost.SacrificeSelf, AbilityCost.ExileSelf -> true
        is AbilityCost.Composite -> cost.costs.any(::removesAttachment)
        else -> false
    }

    /**
     * Whether [statics] make the attached permanent a creature with a real body: a "CREATURE"
     * [GrantCardType] on the host beside a [SetBasePowerToughnessStatic] of at least 2/2 on it.
     */
    internal fun animatesHost(statics: List<StaticAbility>): Boolean =
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
     * Whether an attachment's own statics curse its host — a harmful static beside any ability it
     * grants (Imprisoned in the Moon's lost abilities, Compulsory Rest's "can't attack or block").
     * What the curse hands its host is then the opponent's consolation, never the Aura's payoff.
     */
    internal fun cursesHost(statics: List<StaticAbility>): Boolean = statics.any { static ->
        static !is GrantActivatedAbility && static !is GrantTriggeredAbility &&
            staticPolarity(static) == TargetPolarity.HARMFUL
    }

    /**
     * How a granted activated ability reads for the permanent holding it: a mana ability is the
     * holder's controller's mana; any other does what its effect does for the player activating it.
     */
    internal fun grantedActivatedPolarity(ability: ActivatedAbility): TargetPolarity =
        if (ability.isManaAbility) TargetPolarity.BENEFICIAL
        else grantedEffectPolarity(ability.effect, ability.targetRequirements)

    private fun grantedEffectPolarity(effect: Effect, requirements: List<TargetRequirement>): TargetPolarity {
        val readings = mutableListOf<TargetPolarity>()
        val payoff = EffectWalker.payoffLeaves(effect)
        val milled = HashSet<String>()
        for (leaf in payoff) {
            // "{1}{U}, {T}: Target player mills three cards" (Evanescent Intellect's grant): a mill
            // of a chosen player is aimed by whoever activates it — at an opponent, as the printed
            // ability's harmful slot is — and its move to the graveyard is the same mill.
            val top = (leaf as? GatherCardsEffect)?.source as? CardSource.TopOfLibrary
            if (top != null && top.isMill && playerSlotOf(top.player, requirements) != null) {
                milled += leaf.storeAs
                readings += TargetPolarity.BENEFICIAL
                continue
            }
            if (leaf is MoveCollectionEffect && leaf.from in milled) continue
            // "Create a 4/4 Angel that's tapped and attacking. Exile that token at end of combat"
            // (Invocation of Saint Traft): a delayed step that only takes back what the trigger made
            // is its price; what else it does when it fires reads like any other step.
            if (leaf is CreateDelayedTriggerEffect) {
                readings += EffectWalker.firedLeaves(leaf, payoff).map { grantedEffectPolarity(it, requirements) }
                continue
            }
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

    /**
     * "Choose target creature. Whenever that creature is dealt damage this turn, it deals that much
     * damage to each other creature and each player" (Arcbond): a delayed trigger that watches a
     * target and fires on that object being dealt damage, firing nothing that helps or harms the
     * watched object itself. The object only says *when* it fires — whichever creature is about to
     * take the most damage, an attacker of ours walking into a block or their creature our burn is
     * aimed at — so either side's can be the one to name: open for the simulation to compare.
     */
    private fun damageWatchSlot(leaf: Effect, requirements: List<TargetRequirement>): Int? {
        if (leaf !is CreateDelayedTriggerEffect) return null
        val watched = leaf.watchedTarget ?: return null
        if (leaf.trigger?.event !is EventPattern.DamageReceivedEvent) return null
        val touchesWatched = EffectWalker.firedLeaves(leaf).any { fired ->
            classify(fired).any { (target, _) -> target == EffectTarget.TriggeringEntity || target == watched }
        }
        if (touchesWatched) return null
        return slotOf(watched, requirements)?.takeIf { it in objectSlots(requirements) }
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
            // "{T}: Scry 1, then draw a card" (Oracle's Insight's grant): the compact scry and
            // surveil nodes always look at their controller's own library — card selection for them.
            is ScryEffect, is SurveilEffect -> TargetPolarity.BENEFICIAL
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

    /**
     * Whether [filter] names the permanent the source's own host is attached to —
     * `hasAttached(attachedToBySource())`, "the creature enchanted Equipment is attached to"
     * (Artificer's Hex). At most one permanent ever matches.
     */
    internal fun carriesSourceHost(filter: GameObjectFilter): Boolean =
        filter.statePredicates.any {
            it is StatePredicate.HasAttached && StatePredicate.IsAttachedToBySource in it.filter.statePredicates
        }

    /**
     * "If enchanted Equipment is attached to a creature, destroy that creature" (Artificer's Hex):
     * a pipeline gathers the permanent carrying the host ([carriesSourceHost]) and moves it off the
     * battlefield — destroyed, exiled, bounced or tucked. Whoever controls the host loses the
     * creature wearing it, so each such move is a harm to the host's slot.
     */
    private fun hostCarrierHarms(effect: Effect): List<TargetPolarity> {
        val leaves = EffectWalker.leaves(effect)
        val carrierKeys = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { gather -> (gather.source as? CardSource.BattlefieldMatching)?.filter?.let(::carriesSourceHost) == true }
            .mapTo(HashSet()) { it.storeAs }
        if (carrierKeys.isEmpty()) return emptyList()
        return leaves.filterIsInstance<MoveCollectionEffect>()
            .filter { move ->
                move.from in carrierKeys &&
                    (move.destination as? CardDestination.ToZone)?.zone in CARRIER_REMOVAL_ZONES
            }
            .map { TargetPolarity.HARMFUL }
    }

    private val CARRIER_REMOVAL_ZONES = setOf(Zone.GRAVEYARD, Zone.EXILE, Zone.HAND, Zone.LIBRARY)

    /**
     * What an attachment's "whenever enchanted creature deals / is dealt damage" trigger does to
     * the host's side. Its controller there is named off the damage event rather than as
     * "enchanted creature": [EffectTarget.ControllerOfDamageSource] when the host deals the damage
     * (Visions of Brutality's "its controller loses that much life"), the damaged permanent's
     * controller — [Player.TriggeringPlayer] or [EffectTarget.ControllerOfTriggeringEntity] — when
     * it is dealt it (Ragged Veins). Draining or burning that player is a curse on the host. A
     * gain for the attachment's own controller off its host's damage (Spirit Link's "you gain that
     * much life") pays off only while the host keeps dealing damage, so it belongs on one of ours.
     */
    private fun hostDamageTriggerReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED) return emptyList()
        val dealsDamage = ability.trigger is EventPattern.DealsDamageEvent
        if (!dealsDamage && ability.trigger !is EventPattern.DamageReceivedEvent) return emptyList()
        fun hostController(target: EffectTarget) = if (dealsDamage) {
            target == EffectTarget.ControllerOfDamageSource
        } else {
            target == EffectTarget.PlayerRef(Player.TriggeringPlayer) || target == EffectTarget.ControllerOfTriggeringEntity
        }
        return EffectWalker.payoffLeaves(ability.effect).mapNotNull { leaf ->
            when (leaf) {
                is LoseLifeEffect -> TargetPolarity.HARMFUL.takeIf { hostController(leaf.target) }
                is DealDamageEffect -> TargetPolarity.HARMFUL.takeIf { hostController(leaf.target) }
                is GainLifeEffect -> TargetPolarity.BENEFICIAL.takeIf {
                    dealsDamage && leaf.target == EffectTarget.Controller
                }
                else -> null
            }
        }
    }

    /**
     * What an attachment's "at the beginning of the upkeep of enchanted permanent's controller"
     * trigger does to that player (`Triggers.attached.beginningOf`): the step is the host
     * controller's, bound as [Player.TriggeringPlayer], so burning or draining "that player" is a
     * curse on the host's side — Feedback, Wanderlust, Cursed Land, Power Leak ("this Aura deals 2
     * damage to that player", whatever X they pay to prevent).
     */
    private fun hostStepTriggerReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED || ability.trigger !is EventPattern.StepEvent) return emptyList()
        val thatPlayer = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        return EffectWalker.payoffLeaves(ability.effect).mapNotNull { leaf ->
            when (leaf) {
                is LoseLifeEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == thatPlayer }
                is DealDamageEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == thatPlayer }
                else -> null
            }
        }
    }

    /**
     * What an attachment's "whenever enchanted land becomes tapped" trigger does to the host's
     * side (`Triggers.attached.becomesTapped`): the tapped host is the triggering entity, so
     * draining or burning its controller is a tax on using it — Psychic Venom, Contaminated Ground.
     * Milling that player is the same tax (Chronic Flooding's "that land's controller mills three cards").
     */
    private fun hostTapTriggerReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED || ability.trigger !is EventPattern.TapEvent) return emptyList()
        val hostController = EffectTarget.ControllerOfTriggeringEntity
        return EffectWalker.payoffLeaves(ability.effect).mapNotNull { leaf ->
            when (leaf) {
                is LoseLifeEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == hostController }
                is DealDamageEffect -> TargetPolarity.HARMFUL.takeIf { leaf.target == hostController }
                is GatherCardsEffect -> TargetPolarity.HARMFUL.takeIf {
                    val top = leaf.source as? CardSource.TopOfLibrary
                    top != null && top.isMill && top.player == Player.ControllerOfTriggeringEntity
                }
                else -> null
            }
        }
    }

    /**
     * What an Aura's "when enchanted creature dies, return that card to the battlefield under its
     * owner's control" trigger (Gift of Immortality) does to its host: the creature comes straight
     * back to whoever owns it, a death undone — a gift on our own creature, wasted on an opponent's.
     * Read off the pipeline that gathers the dying card itself and moves it onto the battlefield
     * under its owner's control.
     *
     * A return under the Aura controller's own control (Unhallowed Pact) serves them on either
     * side: on our creature it undoes the death, on theirs it steals the card when it dies. That
     * reads [TargetPolarity.FLEXIBLE] — both sides stay eligible and simulation picks the host.
     */
    private fun hostDeathReturnReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED) return emptyList()
        val dies = ability.trigger as? EventPattern.ZoneChangeEvent ?: return emptyList()
        if (dies.from != Zone.BATTLEFIELD || dies.to != Zone.GRAVEYARD) return emptyList()
        val leaves = EffectWalker.payoffLeaves(ability.effect)
        val dyingCard = leaves.filterIsInstance<GatherCardsEffect>()
            .filter { (it.source as? CardSource.Entity)?.entity == EffectTarget.TriggeringEntity }
            .map { it.storeAs }.toSet()
        val returns = leaves.filterIsInstance<MoveCollectionEffect>().filter { move ->
            move.from in dyingCard && (move.destination as? CardDestination.ToZone)?.zone == Zone.BATTLEFIELD
        }
        return when {
            returns.any { it.underOwnersControl } -> listOf(TargetPolarity.BENEFICIAL)
            returns.any { (it.destination as CardDestination.ToZone).player == Player.You } ->
                listOf(TargetPolarity.FLEXIBLE)
            else -> emptyList()
        }
    }

    /**
     * What an Aura's "when enchanted creature dies, you gain X life and draw X cards" trigger (Fruit
     * of the First Tree) does to the host's side: every payoff leaf is a gift to the Aura's own
     * controller, collected when its host dies. On our own creature that death is ours to time — a
     * chump block, a trade, a sacrifice — while an opponent's creature is kept alive by the player
     * who doesn't want us paid, so the slot belongs on one of ours. A death trigger that hurts the
     * host's controller (Creature Bond) or moves the Aura (Angelic Destiny) isn't this shape.
     */
    private fun hostDeathPayoffReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED) return emptyList()
        val dies = ability.trigger as? EventPattern.ZoneChangeEvent ?: return emptyList()
        if (dies.from != Zone.BATTLEFIELD || dies.to != Zone.GRAVEYARD) return emptyList()
        if (!servesController(ability.effect) && !servesDeadHostController(ability.effect)) return emptyList()
        return listOf(TargetPolarity.BENEFICIAL)
    }

    /**
     * Whether every payoff of an attached dies trigger is a gift to the dead host's own controller
     * — "when enchanted creature dies, its controller draws a card" (Fate Foretold), named
     * [EffectTarget.ControllerOfTriggeringEntity]. The card goes to whoever controlled the host, so
     * the Aura is a gift to the host's side and belongs on one of ours, like a payoff for the Aura's
     * controller ([hostDeathPayoffReadings]).
     */
    private fun servesDeadHostController(effect: Effect): Boolean =
        EffectWalker.payoffLeaves(effect).let { leaves ->
            leaves.isNotEmpty() && leaves.all { leaf ->
                when (leaf) {
                    is DrawCardsEffect -> leaf.target == EffectTarget.ControllerOfTriggeringEntity
                    is GainLifeEffect -> leaf.target == EffectTarget.ControllerOfTriggeringEntity
                    else -> false
                }
            }
        }

    /**
     * An attachment's trigger that makes other permanents copies of its host — "when this Aura enters
     * attached to a creature, each other nontoken creature you control becomes a copy of that
     * creature" (Infinite Reflection): the host is the copy source, left as it is, and the best
     * creature on either side is the one to copy, so the reading is [TargetPolarity.FLEXIBLE].
     */
    private fun hostCopySourceReadings(ability: TriggeredAbility): List<TargetPolarity> =
        EffectWalker.payoffLeaves(ability.effect).mapNotNull { leaf ->
            TargetPolarity.FLEXIBLE.takeIf {
                leaf is EachPermanentBecomesCopyOfTargetEffect && leaf.affected == null &&
                    (leaf.target in HOST_TARGETS || leaf.target == EffectTarget.AttachedToTriggeringPermanent)
            }
        }

    /**
     * What an attachment's "whenever enchanted creature attacks, you may create a token that's a
     * copy of that creature" trigger (Mirror Mockery) does: the copy arrives on the Aura
     * controller's side, untapped and not attacking, in the middle of the host's attack. On an
     * opponent's attacker it is a blocker made of the very creature coming at us — a deterrent
     * that curses the host's attacks; on our own attacker it only stands beside it. So the slot is
     * aimed at an opponent's creature. A copy made attacking, or for someone else, isn't this shape.
     */
    private fun hostAttackCopyReadings(ability: TriggeredAbility): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED || ability.trigger !is EventPattern.AttackEvent) return emptyList()
        return EffectWalker.payoffLeaves(ability.effect).mapNotNull { leaf ->
            TargetPolarity.HARMFUL.takeIf {
                leaf is CreateTokenCopyOfTargetEffect && leaf.target == EffectTarget.TriggeringEntity &&
                    leaf.controller == null && !leaf.attacking
            }
        }
    }

    /**
     * What an attachment's own combat trigger does to its host — "whenever enchanted creature
     * attacks, it gets +X/+0 until end of turn" (Thunderous Might). An attached attack, unblocked or
     * unfiltered becomes-blocked trigger binds the host itself as the triggering entity, so a leaf aimed at
     * "it" lands on the creature the Aura enchants and reads as the same leaf aimed at the enchanted
     * creature would. The token copy of the attacker (Mirror Mockery) is [hostAttackCopyReadings]'
     * reading, not this one's.
     */
    private fun hostCombatTriggerReadings(ability: TriggeredAbility, definitions: CardLookup): List<TargetPolarity> {
        if (ability.binding != TriggerBinding.ATTACHED) return emptyList()
        val trigger = ability.trigger
        // A blocker-filtered becomes-blocked trigger ("blocked by a Wall") binds the blocker, not the host.
        val hostBound = trigger is EventPattern.AttackEvent || trigger == EventPattern.BecomesUnblockedEvent ||
            (trigger is EventPattern.BecomesBlockedEvent && trigger.filter == null)
        if (!hostBound) return emptyList()
        return EffectWalker.payoffLeaves(ability.effect)
            .filterNot { it is CreateTokenCopyOfTargetEffect }
            .flatMap { leaf ->
                classify(leaf, definitions).filter { (target, _) -> target == EffectTarget.TriggeringEntity }.map { it.second }
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
            if (static.filter.reachesEnchantedPlayer() && static.shrinksToSmallBase()) TargetPolarity.HARMFUL else null
        else -> null
    }

    private fun hostStaticPolarity(static: StaticAbility): TargetPolarity? = when (static) {
        is ModifyStats -> if (static.filter.scope != Scope.AttachedTo) null
            else statsPolarity(DynamicAmount.Fixed(static.powerBonus), DynamicAmount.Fixed(static.toughnessBonus))
                ?: lastingTradePolarity(static.powerBonus, static.toughnessBonus)
        is GrantKeyword -> if (!reachesHost(static.filter)) null else keywordPolarity(static.keyword)
        // "Enchanted creature loses flying" (Grounded): the grant turned around — taking away a
        // keyword that helps its holder is a curse, taking away defender a gift.
        is RemoveKeywordStatic -> if (!reachesHost(static.filter)) null else keywordPolarity(static.keyword)?.let(::opposite)
        is GrantLandwalkOfChosenType -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        // "Enchanted land has 'Untap this land during each other player's untap step'" (Urban
        // Burgeoning): the host untaps for its controller on every other turn — a gift to it.
        is GrantStaticAbility -> if (!reachesHost(static.filter)) null else when (static.ability) {
            is UntapSelfDuringOtherUntapSteps -> TargetPolarity.BENEFICIAL
            else -> null
        }
        is CantAttack -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        is CantBlock -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.HARMFUL
        // "Enchanted creature can't attack unless its controller pays {3}" (Brainwash): a price on
        // the host's every attack. No filter: on an Aura it only ever reads the host. On a creature's
        // own script (Qal Sisma Behemoth) it reads HARMFUL too, which only matters to [cursesHost],
        // and that is consulted for AttachedTo grants — which a bare creature never carries.
        is CantAttackOrBlockUnlessPay -> TargetPolarity.HARMFUL
        is GrantTriggeredAbility -> if (static.filter.scope != Scope.AttachedTo) null else grantedTriggerPolarity(static.ability)
        is GrantWard -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtection -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtectionFromCardType -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtectionFromToGroup -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        // Protection from the chosen color (Flickering Ward), or from the colors of permanents you
        // control (Pledge of Loyalty): a shield for the host, whatever the quality turns out to be.
        is GrantProtectionFromChosenColorToGroup -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
        is GrantProtectionFromControlledColors -> if (static.filter.scope != Scope.AttachedTo) null else TargetPolarity.BENEFICIAL
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
        // "Enchanted land is every basic land type in addition to its other types" (Nylea's
        // Presence): each added basic land type brings its mana ability with it, so the host's
        // controller can tap it for more colours. Additive only — a land-type change that
        // replaces the land's types is a different static and stays unread here.
        is GrantSubtype -> if (static.filter.scope != Scope.AttachedTo || static.subtype !in Subtype.ALL_BASIC_LAND_TYPES) null
            else TargetPolarity.BENEFICIAL
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
        Keyword.HORSEMANSHIP, Keyword.SKULK, Keyword.SWAMPWALK, Keyword.FORESTWALK, Keyword.ISLANDWALK,
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
