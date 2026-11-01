package com.wingedsheep.sdk.scripting

import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.protectionDescription
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Grants protection from a color to the target creature.
 * Used for auras like Crown of Awe: "Enchanted creature has protection from black and from red."
 *
 * @property color The color to grant protection from
 * @property filter What this ability applies to (typically AttachedCreature for auras)
 */
@SerialName("GrantProtection")
@Serializable
data class GrantProtection(
    val color: Color,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String = "${filter.description} have protection from ${color.displayName.lowercase()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants protection from a card type (e.g. "from instants", "from sorceries") to a filtered set
 * of creatures. Rule 702.16 protection-from-a-quality where the quality is a card type.
 *
 * Projected onto the affected creature(s) as the keyword `PROTECTION_FROM_CARDTYPE_<TYPE>`, mirroring
 * the `PROTECTION_FROM_SUBTYPE_<X>` / `PROTECTION_FROM_SUPERTYPE_<X>` keyword conventions. The engine
 * enforces the *targeting* leg of protection (a spell/permanent of that card type can't target the
 * creature) — which is the only DEBT leg that matters for instants and sorceries, since they never
 * deal combat damage to, block, or enchant a creature.
 *
 * Used by Sword of Wealth and Power ("Equipped creature ... has protection from instants and from
 * sorceries.") — one such ability per card type. Pair two of these for the two-type wording.
 *
 * @property cardType The card type protection is from (e.g. [CardType.INSTANT]).
 * @property filter What this applies to — defaults to the attached/equipped creature for Equipment.
 */
@SerialName("GrantProtectionFromCardType")
@Serializable
data class GrantProtectionFromCardType(
    val cardType: CardType,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String =
        "${filter.description} have protection from ${cardType.displayName.lowercase()}s"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants protection from the chosen color to a group of creatures.
 * Used for "As this enters, choose a color. [Group] have protection from the chosen color."
 * The chosen color is stored on the permanent via CastChoicesComponent and resolved dynamically.
 * Example: Ward Sliver (all Slivers have protection from the chosen color)
 *
 * @property filter The group of creatures that gain protection
 */
@SerialName("GrantProtectionFromChosenColorToGroup")
@Serializable
data class GrantProtectionFromChosenColorToGroup(
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Companion.Creature.youControl())
) : StaticAbility {
    override val description: String = "Creatures of the chosen group have protection from the chosen color"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants each affected creature "hexproof from each of its colors" — i.e., for every color
 * the creature currently has (after Layer 5), it also has hexproof from that color.
 *
 * Used by Tam, Mindful First-Year ("Each other creature you control has hexproof from each of
 * its colors.") — the protection set varies per creature and changes if a creature's colors
 * change. Colorless is not a color, so a colorless creature gains no hexproof.
 *
 * The grant is applied in Layer 6 (ABILITY) after colors are finalized in Layer 5, so the
 * keyword set reflects the projected colors of each affected creature.
 *
 * @property filter The group of creatures that gain the dynamic hexproof
 */
@SerialName("GrantHexproofFromOwnColorsToGroup")
@Serializable
data class GrantHexproofFromOwnColorsToGroup(
    val filter: GroupFilter = GroupFilter(GameObjectFilter.Companion.Creature.youControl(), excludeSelf = true)
) : StaticAbility {
    override val description: String = "${filter.description} have hexproof from each of their colors"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants each affected permanent "hexproof from [scope]" (CR 702.11d) — it can't be the target of
 * spells or abilities with that quality that its controller's opponents control.
 *
 * - `ProtectionScope.Monocolored` — Dragonfire Blade ("Equipped creature ... has hexproof from
 *   monocolored"; exactly one color, CR 105.2a).
 *
 * A permanent's own printed "hexproof from [quality]" is [KeywordAbility.Hexproof] instead (Niv-Mizzet,
 * Guildpact: `keywordAbility(KeywordAbility.Hexproof(ProtectionScope.Multicolored))`).
 *
 * Realized as the same projected `HEXPROOF_FROM_*` keyword a printed
 * [KeywordAbility.Hexproof] uses, so every targeting site that reads printed hexproof-from reads
 * this too. Only the scopes with such a keyword are accepted ([isSupported]): a color or
 * colors, a non-color, monocolored, multicolored, a card type, and the source kinds (spells,
 * permanents cast this turn, activated/triggered abilities). "Hexproof from each of its colors"
 * varies per creature and is [GrantHexproofFromOwnColorsToGroup].
 *
 * @property scope The quality the hexproof is from
 * @property filter The group of permanents that gain the hexproof (default: the equipped or
 *   enchanted creature)
 */
@SerialName("GrantHexproofFromToGroup")
@Serializable
data class GrantHexproofFromToGroup(
    val scope: ProtectionScope,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    init {
        require(isSupported(scope)) { "GrantHexproofFromToGroup has no hexproof keyword for $scope" }
    }

    override val description: String = "${filter.description} have hexproof from ${scope.protectionDescription()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }

    companion object {
        /** Whether the engine projects a `HEXPROOF_FROM_*` keyword for [scope]. */
        fun isSupported(scope: ProtectionScope): Boolean = when (scope) {
            is ProtectionScope.Color, is ProtectionScope.Colors, is ProtectionScope.NonColor,
            ProtectionScope.Monocolored, ProtectionScope.Multicolored, is ProtectionScope.CardType,
            ProtectionScope.Spells, ProtectionScope.PermanentsCastThisTurn, ProtectionScope.RingBearers,
            ProtectionScope.ActivatedAbilities, ProtectionScope.TriggeredAbilities,
            is ProtectionScope.ManaValueParity -> true
            is ProtectionScope.Subtype, is ProtectionScope.Supertype, is ProtectionScope.ChosenCardType,
            is ProtectionScope.ChosenCardName, ProtectionScope.Everything, ProtectionScope.EachOpponent,
            is ProtectionScope.ManaValueOtherThan -> false
        }
    }
}

/**
 * Grants each affected permanent "protection from [scope]" (CR 702.16) — the protection twin of
 * [GrantHexproofFromToGroup], for lords whose quality isn't a single color or card type.
 *
 * - `ProtectionScope.RingBearers` — Lord of the Nazgûl ("Wraiths you control have protection from
 *   Ring-bearers").
 *
 * Realized as the same projected `PROTECTION_FROM_*` keyword a printed [KeywordAbility.Protection]
 * with that scope projects, so every leg that enforces printed protection (targeting, damage,
 * enchanting/equipping, blocking) honors the granted one unchanged. Only the scopes with such a
 * keyword are accepted ([isSupported]); a single color or card type is rejected, since it keeps its
 * older spellings, [GrantProtection] and [GrantProtectionFromCardType].
 *
 * @property scope The quality the protection is from
 * @property filter The group of permanents that gain the protection (default: the equipped or
 *   enchanted creature)
 */
@SerialName("GrantProtectionFromToGroup")
@Serializable
data class GrantProtectionFromToGroup(
    val scope: ProtectionScope,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    init {
        require(isSupported(scope)) { "GrantProtectionFromToGroup has no protection keyword for $scope" }
    }

    override val description: String = "${filter.description} have protection from ${scope.protectionDescription()}"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }

    companion object {
        /**
         * Whether the engine projects a `PROTECTION_FROM_*` keyword for [scope] and no older spelling
         * already says it — a single color is [GrantProtection], a card type is
         * [GrantProtectionFromCardType].
         */
        fun isSupported(scope: ProtectionScope): Boolean = when (scope) {
            is ProtectionScope.Colors, ProtectionScope.Multicolored,
            is ProtectionScope.Subtype, is ProtectionScope.Supertype,
            ProtectionScope.EachOpponent, is ProtectionScope.ChosenCardType,
            ProtectionScope.Spells, ProtectionScope.PermanentsCastThisTurn, ProtectionScope.RingBearers,
            ProtectionScope.ActivatedAbilities, ProtectionScope.TriggeredAbilities,
            is ProtectionScope.ManaValueParity, is ProtectionScope.ManaValueOtherThan -> true
            is ProtectionScope.Color, is ProtectionScope.CardType,
            is ProtectionScope.NonColor, ProtectionScope.Monocolored, ProtectionScope.Everything,
            is ProtectionScope.ChosenCardName -> false
        }
    }
}

/**
 * Grants each affected creature protection from the colors of permanents the source's
 * controller currently controls. The protection set is board-derived and re-evaluated at
 * projection (after Layer 5), so it tracks the controller's permanents in real time; a
 * colorless permanent contributes no color.
 *
 * Used by Pledge of Loyalty ("Enchanted creature has protection from the colors of permanents
 * you control."). The "you" is the controller of the permanent holding this ability; the
 * default filter applies it to the enchanted creature.
 *
 * @property filter The group of creatures that gain the dynamic protection
 */
@SerialName("GrantProtectionFromControlledColors")
@Serializable
data class GrantProtectionFromControlledColors(
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String =
        "${filter.description} have protection from the colors of permanents you control"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Grants protection from **each card type of the cards exiled with this permanent** — the Imprint
 * payoff (CR 702.15) behind Mirror Golem ("Mirror Golem has protection from each of the exiled
 * card's card types").
 *
 * The card-type sibling of [GrantProtectionFromControlledColors]: the *set* of qualities is derived
 * at projection time rather than printed, here from the source's linked-exile pile
 * (`LinkedExileComponent`) instead of the controller's board. Each card type found is projected as
 * the usual `PROTECTION_FROM_CARDTYPE_<TYPE>` keyword, so every leg the engine already enforces for
 * printed card-type protection — targeting, blocking, and damage — honors it unchanged.
 *
 * An empty pile grants nothing, which is exactly right for a declined imprint: a Mirror Golem that
 * exiled no card has no protection.
 *
 * @property filter Which permanents gain the protection — defaults to the source itself, the only
 *   shape Imprint uses.
 */
@SerialName("GrantProtectionFromLinkedExiledCardTypes")
@Serializable
data class GrantProtectionFromLinkedExiledCardTypes(
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "${filter.description} has protection from each of the exiled card's card types"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Prevents matching permanents from having counters put on them — every kind (Blossombind,
 * Solemnity), or only [counterType] when it is set ("Creatures your opponents control can't have
 * +1/+1 counters put on them" — Blightbeetle; Melira's "-1/-1 counters").
 *
 * A typed prohibition stops only placements of that kind: a creature that would enter with
 * +1/+1 counters enters without them, other kinds still go on, and counters it already has stay
 * (unlike [MaxCounters] at zero, there is no state-based removal). A cost that would put that kind
 * on it can't be paid (CR 614.17b).
 */
@SerialName("CantReceiveCounters")
@Serializable
data class CantReceiveCounters(
    val filter: GroupFilter = GroupFilter.attachedCreature(),
    val counterType: CounterType? = null
) : StaticAbility {
    override val description: String =
        if (counterType == null) "${filter.description} can't have counters put on it"
        else "${filter.description} can't have ${counterType.printed} counters put on it"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * "This permanent can't have more than [max] [counterType] counters on it" (Rasputin Dreamweaver).
 *
 * A rule on the holder, not a replacement: a placement that would take it past [max] puts only as
 * many counters as fit, after every counter-placement replacement has applied (Doubling Season can't
 * push it past the limit), and one on a holder already at the limit doesn't happen at all — no
 * counters, no event. That includes the counters it enters with, so a doubled entry is still capped.
 * Other kinds are unaffected. A permanent that holds more than [max] anyway (it gained the limit
 * later) has the excess removed as a state-based action (CR 704.5r).
 *
 * A [ProjectedStaticAbility]: the engine's one counter-placement chokepoint
 * (`ReplacementEffectUtils.applyCounterPlacementModifiers`) reads it off the holder's projected
 * statics, so a granted copy counts and one lost with the holder's abilities doesn't.
 *
 * Closest types: [CantReceiveCounters] is an all-kinds prohibition over a [GroupFilter], projected as
 * a flag that cost-legality checks read (CR 614.17b) — it has no kind and no number to carry a limit;
 * `CapCounterPlacementThisTurn` is a replacement that caps one placement and then locks the recipient
 * for the turn, with no notion of how many counters it already has.
 */
@SerialName("MaxCounters")
@Serializable
data class MaxCounters(
    val counterType: CounterType,
    val max: Int,
) : ProjectedStaticAbility {
    override val description: String =
        "This permanent can't have more than $max ${counterType.printed} counters on it"
}

/**
 * Permanents matching [filter] can't be sacrificed — at all, or only to pay a cost ([scope]).
 *
 * [Scope.ANY] projects the [com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_SACRIFICED] flag, which
 * every sacrifice path honors: the sacrifice executor refuses such a permanent, and a cost of
 * casting a spell or activating an ability can't sacrifice it. Wrap in a
 * [ConditionalStaticAbility] for time-restricted forms (e.g. Zurgo, Thunder's Decree:
 * "During your end step, Warrior tokens you control have 'This token can't be sacrificed.'").
 *
 * [Scope.CASTING_AND_ACTIVATION_COSTS] projects
 * [com.wingedsheep.sdk.core.AbilityFlag.CANT_BE_SACRIFICED_FOR_COSTS] instead: "Players can't
 * sacrifice creatures to cast spells or activate abilities" (Angel of Jubilation) is
 * `CantBeSacrificed(GroupFilter.AllCreatures, Scope.CASTING_AND_ACTIVATION_COSTS)`. The prohibition
 * is per permanent, as the ruling reads it — an artifact creature can't pay "Sacrifice an artifact",
 * a noncreature artifact still can — and it reaches additional and alternative costs (emerge,
 * casualty) and mana abilities alike. A sacrifice a resolving spell or ability performs (an edict,
 * exploit, "sacrifice it unless you pay") is untouched.
 */
@SerialName("CantBeSacrificed")
@Serializable
data class CantBeSacrificed(
    val filter: GroupFilter = GroupFilter.attachedCreature(),
    val scope: Scope = Scope.ANY,
) : StaticAbility {
    /** When the matching permanents can't be sacrificed. */
    @Serializable
    enum class Scope {
        /** Never — by an effect or to pay a cost. */
        ANY,

        /** Not to pay a cost of casting a spell or activating an ability (mana abilities included). */
        CASTING_AND_ACTIVATION_COSTS,
    }

    override val description: String = when (scope) {
        Scope.ANY -> "${filter.description} can't be sacrificed"
        Scope.CASTING_AND_ACTIVATION_COSTS ->
            "${filter.description} can't be sacrificed to cast spells or activate abilities"
    }
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * You have shroud. (You can't be the target of spells or abilities.)
 * Grants shroud to the permanent's controller (player-level shroud).
 * Used for True Believer.
 */
@SerialName("GrantShroudToController")
@Serializable
data object GrantShroudToController : StaticAbility {
    override val description: String = "You have shroud"
}

/**
 * You have hexproof. (You can't be the target of spells or abilities your opponents control.)
 * Grants hexproof to the permanent's controller (player-level hexproof).
 * Unlike shroud, the controller can still target themselves.
 * Used for Shalai, Voice of Plenty.
 */
@SerialName("GrantHexproofToController")
@Serializable
data object GrantHexproofToController : StaticAbility {
    override val description: String = "You have hexproof"
}

/**
 * What a resolving spell or ability can tell a player to do to their own cards — the actions
 * [OpponentsCantCauseYouTo] forbids an opponent's source from compelling.
 */
@Serializable
enum class CompelledAction(val phrase: String) {
    /** Discard a card from hand (CR 701.9). */
    DISCARD("discard cards"),

    /** Sacrifice a permanent (CR 701.21). */
    SACRIFICE("sacrifice permanents"),
}

/**
 * Spells and abilities your opponents control can't cause you to discard cards and/or sacrifice
 * permanents, per [actions] — Sigarda, Host of Herons (sacrifice), Tamiyo, Collector of Tales
 * (both).
 *
 * A player-scoped "can't" that outranks the instruction (CR 101.2): as an opponent's spell or
 * ability resolves, the discard or sacrifice it would impose on this permanent's controller simply
 * doesn't happen, and an *optional* one offered by such a source can't be chosen — including
 * paying a discard or sacrifice cost the opponent's spell or ability imposes (a ward cost, or an
 * "unless you discard / sacrifice …" clause). Effects the controller themselves controls are
 * untouched, and so are the game rules: cleanup's hand-size discard, lethal damage, 0 toughness,
 * the legend rule and destruction aren't a spell or ability making you discard or sacrifice.
 *
 * Stamped as one marker per action (`GrantsDiscardImmunityComponent`,
 * `GrantsSacrificeImmunityComponent`) by the static-ability handler and read by
 * `CompelledActionImmunity`, which every discard and sacrifice site consults (including a
 * `ChooseAction` option that would discard or sacrifice); it is not a Rule 613
 * continuous effect.
 */
@SerialName("OpponentsCantCauseYouTo")
@Serializable
data class OpponentsCantCauseYouTo(val actions: Set<CompelledAction>) : StaticAbility {
    init {
        require(actions.isNotEmpty()) { "OpponentsCantCauseYouTo needs at least one action" }
    }

    override val description: String =
        "Spells and abilities your opponents control can't cause you to " +
            CompelledAction.entries.filter { it in actions }.joinToString(" or ") { it.phrase }
}

/**
 * You have protection from [scope] (CR 702.16).
 *
 * Grants player-level protection to the permanent's controller — the continuous, static
 * counterpart of the one-shot [com.wingedsheep.sdk.scripting.effects.GrantPlayerProtectionEffect]
 * (The One Ring). For a player only the **D**amage and **T**argeting legs of DEBT apply: while
 * this permanent is on the battlefield its controller can't be dealt damage by, nor be the target
 * of spells/abilities from, a source matching [scope]. When the permanent leaves the battlefield
 * the protection goes with it — no cleanup needed.
 *
 * General-purpose over any [ProtectionScope]: `EachOpponent` (Absolute Virtue —
 * "You have protection from each of your opponents."), a single color, `Everything`, etc., and the
 * choices the permanent made as it entered — `ChosenCardType` (Serra's Emissary) and
 * `ChosenCardName` (Runed Halo — "You have protection from the chosen card name.").
 *
 * @property scope The quality the controller is protected from.
 */
@SerialName("GrantProtectionToController")
@Serializable
data class GrantProtectionToController(
    val scope: ProtectionScope = ProtectionScope.EachOpponent
) : StaticAbility {
    override val description: String =
        "You have protection from " + when (val s = scope) {
            is ProtectionScope.Color -> s.color.displayName.lowercase()
            is ProtectionScope.Colors -> s.colors.joinToString(" and ") { it.displayName.lowercase() }
            is ProtectionScope.NonColor -> "non" + s.color.displayName.lowercase()
            ProtectionScope.Multicolored -> "multicolored"
            ProtectionScope.Monocolored -> "monocolored"
            is ProtectionScope.CardType -> s.cardType.lowercase() + "s"
            is ProtectionScope.ChosenCardType -> "the chosen card type"
            is ProtectionScope.ChosenCardName -> "the chosen card name"
            is ProtectionScope.Subtype -> s.subtype + "s"
            is ProtectionScope.Supertype -> s.supertype.lowercase() + " permanents"
            ProtectionScope.Everything -> "everything"
            ProtectionScope.EachOpponent -> "each of your opponents"
            ProtectionScope.Spells -> "spells"
            ProtectionScope.PermanentsCastThisTurn -> "permanents that were cast this turn"
            ProtectionScope.RingBearers -> "Ring-bearers"
            ProtectionScope.ActivatedAbilities -> "activated abilities"
            ProtectionScope.TriggeredAbilities -> "triggered abilities"
            is ProtectionScope.ManaValueParity -> "each ${s.parity.displayName} mana value"
            is ProtectionScope.ManaValueOtherThan -> "each mana value other than ${s.value}"
        }
}

/**
 * This permanent can't be the target of abilities your opponents control.
 * Unlike hexproof, this does NOT prevent targeting by spells — only by activated
 * and triggered abilities.
 * Used for Shanna, Sisay's Legacy.
 */
@SerialName("CantBeTargetedByOpponentAbilities")
@Serializable
data object CantBeTargetedByOpponentAbilities : StaticAbility {
    override val description: String = "Can't be the target of abilities your opponents control"
}

/**
 * Creatures matching [filter] can't be the target of *abilities from sources of a given card type*
 * ([sourceType]) — hexproof keyed to a source *category* (the source's card type) rather than a
 * controller or color. This blocks only abilities (activated/triggered), not spells, and applies
 * regardless of who controls the source (the ability's source per CR 113.7).
 *
 * Projected as the keyword `CANT_BE_TARGETED_BY_CARDTYPE_<TYPE>_SOURCE_ABILITIES`; enforced at
 * targeting by the engine (`TargetFinder`/`StackResolver`), which checks the targeting source's
 * projected/base card types. Used by Artifact Ward with [CardType.ARTIFACT] ("Enchanted creature
 * can't be the target of abilities from artifact sources").
 *
 * Deliberately *not* protection-from-[type]: protection would also stop the creature from being
 * enchanted/equipped/blocked/damaged by such sources, which this clause does not.
 *
 * @property sourceType The card type the restricted source must have (e.g. [CardType.ARTIFACT]).
 * @property filter What this applies to — defaults to the attached/enchanted creature for Auras.
 */
@SerialName("CantBeTargetedBySourceTypeAbilities")
@Serializable
data class CantBeTargetedBySourceTypeAbilities(
    val sourceType: CardType,
    val filter: GroupFilter = GroupFilter.attachedCreature()
) : StaticAbility {
    override val description: String =
        "${filter.description} can't be the target of abilities from ${sourceType.displayName.lowercase()} sources"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * The *kind* of spell or ability a [CantBeTargetedBy] permanent is untargetable by — a kind named
 * by something other than the source's color, card type or controller (those are protection,
 * hexproof-from and [CantBeTargetedBySourceTypeAbilities]).
 */
@Serializable
sealed interface UntargetableBy {
    val description: String
    fun applyTextReplacement(replacer: TextReplacer): UntargetableBy = this

    /**
     * Spells with [subtype] — "can't be the target of Aura spells" (Tetsuo Umezawa, Bartel
     * Runeaxe). Only spells: an ability of an Aura may still target it, and an Aura put onto the
     * battlefield without being cast doesn't target, so it may still be attached.
     */
    @SerialName("UntargetableBy.SpellsWithSubtype")
    @Serializable
    data class SpellsWithSubtype(val subtype: Subtype) : UntargetableBy {
        override val description: String = "${subtype.value} spells"
        override fun applyTextReplacement(replacer: TextReplacer): UntargetableBy {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else copy(subtype = new)
        }
    }

    /**
     * Spells and abilities that can target only objects with [subtype] — "spells that can target
     * only Walls or … abilities that can target only Walls" (Wall of Shadows). A target requirement
     * counts when its filter structurally demands [subtype] ("target Wall", "enchant Wall"); a
     * generic "target creature" can also reach a non-Wall, so it doesn't.
     */
    @SerialName("UntargetableBy.TargetingOnly")
    @Serializable
    data class TargetingOnly(val subtype: Subtype) : UntargetableBy {
        override val description: String
            get() = "spells that can target only ${subtype.value}s or of abilities that can target only ${subtype.value}s"

        override fun applyTextReplacement(replacer: TextReplacer): UntargetableBy {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else copy(subtype = new)
        }
    }
}

/**
 * Permanents matching [filter] (default: this one) can't be the target of spells or abilities of
 * the [quality] kind — "Tetsuo Umezawa can't be the target of Aura spells", "This creature can't be
 * the target of spells that can target only Walls or of abilities that can target only Walls".
 *
 * Closest neighbours: [CantBeTargetedBySourceTypeAbilities] keys on the source's *card type* and
 * only blocks abilities; [CantBeTargetedByOpponentAbilities] is controller-gated. Neither can say
 * "Aura spells" (a subtype, spells only) or "can target only Walls" (a property of the targeting
 * requirement, not the source). Like protection and unlike hexproof, this is not controller-gated:
 * it applies to its own controller's spells and abilities too.
 *
 * Projected as a keyword per [quality] (so losing all abilities drops it); enforced at every
 * targeting site — legal-target enumeration, cast/activation validation, and the resolution
 * re-check — by one shared engine helper.
 */
@SerialName("CantBeTargetedBy")
@Serializable
data class CantBeTargetedBy(
    val quality: UntargetableBy,
    val filter: GroupFilter = GroupFilter.source()
) : StaticAbility {
    override val description: String =
        "${filter.description.replaceFirstChar { it.uppercase() }} can't be the target of ${quality.description}"

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newQuality = quality.applyTextReplacement(replacer)
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newQuality !== quality || newFilter !== filter) copy(quality = newQuality, filter = newFilter) else this
    }
}

/**
 * Objects matching [filter] in [zones] can't be the targets of spells or abilities [sourceController]
 * controls — "Cards in graveyards can't be the targets of spells or abilities" (Ground Seal; Dennick,
 * Pious Apprentice), "Lands on the battlefield and land cards in graveyards can't be the targets of
 * spells or abilities your opponents control" (Tomik, Distinguished Advokist).
 *
 * Closest neighbours: [CantBeTargetedBy] is projected as a keyword onto *battlefield permanents*
 * (a [GroupFilter] scope) and scoped by a [UntargetableBy] quality, and [CantEnterTheBattlefield]
 * is the same zone-scoped, printer-owned shape for a different action (entering the battlefield).
 * Hexproof is relative to each permanent's *own* controller, so it can't say "opponents of this
 * lock's controller" about permanents anyone controls. A card in a graveyard has no projection entry
 * to carry a keyword, so this lock is read off the printing permanent's projected statics at each
 * targeting site instead — which is also why it stops working when that permanent loses its
 * abilities. [Zone.BATTLEFIELD] in [zones] reaches permanents too, whoever controls them. Choosing a
 * card without targeting it ("return a card from your graveyard") is untouched.
 *
 * @property zones The zones whose objects are untargetable.
 * @property filter Which objects in those zones, matched where they are.
 * @property sourceController Whose spells and abilities can't target them, relative to the lock's
 *   controller: [Player.Each] (anyone's, the lock's own controller included) or
 *   [Player.EachOpponent] ("spells or abilities your opponents control").
 */
@SerialName("CardsInZonesCantBeTargeted")
@Serializable
data class CardsInZonesCantBeTargeted(
    val zones: Set<Zone> = setOf(Zone.GRAVEYARD),
    val filter: GameObjectFilter = GameObjectFilter.Any,
    val sourceController: Player = Player.Each
) : StaticAbility {
    init {
        require(sourceController == Player.Each || sourceController == Player.EachOpponent) {
            "CardsInZonesCantBeTargeted.sourceController must be Player.Each or Player.EachOpponent, was $sourceController"
        }
    }

    override val description: String = buildString {
        val noun = if (filter == GameObjectFilter.Any) "card" else "${filter.description} card"
        val offBattlefield = zones - Zone.BATTLEFIELD
        val parts = buildList {
            if (Zone.BATTLEFIELD in zones) {
                add((if (filter == GameObjectFilter.Any) "permanents" else "${filter.description}s") + " on the battlefield")
            }
            if (offBattlefield.isNotEmpty()) add("${noun}s in ${zonePhrase(offBattlefield, "and")}")
        }
        append(parts.joinToString(" and ").replaceFirstChar { it.uppercase() })
        append(" can't be the targets of spells or abilities")
        if (sourceController == Player.EachOpponent) append(" your opponents control")
    }

    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * You can't lose the game.
 * Grants the "can't lose the game" effect to the permanent's controller.
 * Used for Lich's Mastery, Platinum Angel, etc.
 *
 * When active, prevents all game loss conditions: life at 0 or less,
 * 10+ poison counters, drawing from empty library, and card effects.
 * Note: opponents can still WIN the game via effects that say so.
 *
 * A [ProjectedStaticAbility], so an Equipment or Aura can grant it to the creature it's attached
 * to through [GrantStaticAbility] ("Equipped creature has 'You can't lose the game …'", Cloudsteel
 * Kirin) — the creature's controller is then "you".
 */
@SerialName("GrantCantLoseGame")
@Serializable
data object GrantCantLoseGame : ProjectedStaticAbility {
    override val description: String = "You can't lose the game"
}

/**
 * Your opponents can't win the game.
 * Grants the "opponents can't win the game" effect from the permanent's controller.
 * Used for Herald of Eternal Dawn, etc.
 *
 * When active, any effect that would cause an opponent of the controller to win the game
 * (e.g. an alternate "you win the game" win condition) does nothing at all — so it also
 * doesn't collaterally eliminate the winner's *other* opponents. This is distinct from, and
 * complementary to, [GrantCantLoseGame] on the same permanent: can't-lose protects the
 * controller from being made to lose, while this clause fizzles the opponent's win outright.
 * Grantable through [GrantStaticAbility] like [GrantCantLoseGame].
 */
@SerialName("GrantOpponentsCantWinGame")
@Serializable
data object GrantOpponentsCantWinGame : ProjectedStaticAbility {
    override val description: String = "Your opponents can't win the game"
}

/**
 * Grants the controller "you don't lose the game for having 0 or less life" — the *narrow*
 * sibling of [GrantCantLoseGame].
 *
 * Unlike [GrantCantLoseGame], this only suppresses the 0-or-less-life state-based action
 * (CR 704.5a). The controller can still lose to poison (704.5c), drawing from an empty library
 * (704.5b/c), and card effects. Marina Vendrell's Grimoire (DSK): "You have no maximum hand size
 * and don't lose the game for having 0 or less life." — its own "if you have no cards in hand, you
 * lose the game" clause therefore still functions.
 */
@SerialName("GrantCantLoseGameFromLife")
@Serializable
data object GrantCantLoseGameFromLife : StaticAbility {
    override val description: String = "You don't lose the game for having 0 or less life"
}

/**
 * Permanents matching [filter], and the [players] named, can be the targets of spells and abilities
 * you control as though they didn't have hexproof.
 *
 * Used for Nowhere to Run: "Creatures your opponents control can be the targets of
 * spells and abilities as though they didn't have hexproof."
 *
 * The filter is evaluated with the controller of this permanent as the "you" context,
 * so [GroupFilter.AllCreaturesOpponentsControl] means "creatures opponents of the
 * controller of this permanent control" — which is what "your opponents" means.
 *
 * [players] widens it to players: the players it names (read relative to this permanent's
 * controller) can be targeted as though they didn't have hexproof too. Kaya, Bane of the Dead —
 * "Your opponents and permanents your opponents control with hexproof can be the targets of
 * spells and abilities you control as though they didn't have hexproof" — is
 * `players = Player.EachOpponent` over a permanents-opponents-control [filter]. Null (the
 * default) leaves players' hexproof alone.
 *
 * Only spells and abilities this permanent's controller controls get the bypass, and it reaches
 * "hexproof from [quality]" as well (CR 702.11e). Does NOT suppress shroud (shroud still prevents
 * targeting by all players).
 */
@SerialName("SuppressHexproofForGroup")
@Serializable
data class SuppressHexproofForGroup(
    val filter: GroupFilter = GroupFilter.AllCreaturesOpponentsControl,
    val players: Player? = null
) : StaticAbility {
    override val description: String =
        (players?.let { "${it.description.replaceFirstChar(Char::uppercase)} and " } ?: "") +
            "${filter.description} can be targeted as though they didn't have hexproof"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}

/**
 * Ward abilities of creatures matching [filter] don't trigger.
 *
 * Used for Nowhere to Run: "Ward abilities of those creatures don't trigger."
 *
 * The filter is evaluated with the controller of this permanent as the "you" context.
 * Suppresses both intrinsic ward (from the creature's own keywords) and granted ward
 * (from GrantWard static abilities) for affected creatures.
 */
@SerialName("SuppressWardForGroup")
@Serializable
data class SuppressWardForGroup(
    val filter: GroupFilter = GroupFilter.AllCreaturesOpponentsControl
) : StaticAbility {
    override val description: String = "Ward abilities of ${filter.description} don't trigger"
    override fun applyTextReplacement(replacer: TextReplacer): StaticAbility {
        val newFilter = filter.applyTextReplacement(replacer)
        return if (newFilter !== filter) copy(filter = newFilter) else this
    }
}
