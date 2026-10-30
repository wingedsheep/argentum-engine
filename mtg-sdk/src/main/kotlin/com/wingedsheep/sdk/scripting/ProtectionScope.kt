package com.wingedsheep.sdk.scripting

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * The quality a Protection or Hexproof keyword ability is from.
 *
 * Mirrors the cases of MTG's "from X" qualifier so [KeywordAbility.Protection] and
 * [KeywordAbility.Hexproof] share a single sealed cost shape (Rule 702.16, 702.11b).
 */
@Serializable
sealed interface ProtectionScope {
    /** Protection / hexproof from a single color. */
    @SerialName("ProtectionScope.Color")
    @Serializable
    data class Color(val color: com.wingedsheep.sdk.core.Color) : ProtectionScope

    /** Protection from multiple colors — "from white and from blue". */
    @SerialName("ProtectionScope.Colors")
    @Serializable
    data class Colors(val colors: Set<com.wingedsheep.sdk.core.Color>) : ProtectionScope

    /**
     * The complement of a single color — "from nongreen". Matches every source that is **not**
     * [color], colorless sources included (CR 105.2c: a colorless object has no color, so it is
     * "nongreen"). A green-white source is green, so it does not match.
     *
     * Engine-wired for *hexproof* (Thrun, Breaker of Silence: "can't be the target of nongreen
     * spells your opponents control or abilities from nongreen sources your opponents control")
     * and for player protection; creature *protection* from a non-color is not projected.
     */
    @SerialName("ProtectionScope.NonColor")
    @Serializable
    data class NonColor(val color: com.wingedsheep.sdk.core.Color) : ProtectionScope

    /**
     * From multicolored — matches a source with two or more colors (CR 105.2b: a multicolored
     * object has two or more colors). Enforced on the same colour axis as [Color], so it covers
     * damage, enchanting/equipping, blocking and targeting (Argentum Masticore).
     */
    @SerialName("ProtectionScope.Multicolored")
    @Serializable
    data object Multicolored : ProtectionScope

    /**
     * From monocolored — matches a source with exactly one color (CR 105.2a), the twin of
     * [Multicolored]. Engine-wired for *hexproof*, printed ([KeywordAbility.Hexproof]) or granted
     * (Dragonfire Blade, via [GrantHexproofFromToGroup]), and for player protection; creature
     * *protection* from monocolored is not projected, and [KeywordAbility.Protection] rejects it.
     */
    @SerialName("ProtectionScope.Monocolored")
    @Serializable
    data object Monocolored : ProtectionScope

    /** Protection from a card type — "from creatures". */
    @SerialName("ProtectionScope.CardType")
    @Serializable
    data class CardType(val cardType: String) : ProtectionScope

    /**
     * Protection from the card type the **granting permanent chose as it entered** — "As Serra's
     * Emissary enters, choose a card type. You and creatures you control have protection from the
     * chosen card type." Late-bound: the engine reads the source's durable choice under [slot]
     * (a card-type display name such as "Artifact" or "Instant") wherever the protection is
     * applied, so it behaves exactly like [CardType] of that type, and protects from nothing
     * until a type is chosen. Only meaningful on statics a permanent grants —
     * `GrantProtectionFromToGroup` and `GrantProtectionToController`; it has no fixed keyword, so
     * a printed `Protection` keyword or a one-shot grant of it protects from nothing.
     *
     * @property slot The source's cast-choices slot holding the chosen type. An as-enters
     *   `EntersWithChoice(ChoiceType.MODE)` over card-type ids writes [ChoiceSlot.MODE] (Serra's
     *   Emissary); `Effects.ChooseCardTypeForSource` writes [ChoiceSlot.CARD_TYPE].
     */
    @SerialName("ProtectionScope.ChosenCardType")
    @Serializable
    data class ChosenCardType(val slot: ChoiceSlot = ChoiceSlot.CARD_TYPE) : ProtectionScope

    /**
     * Protection from the card name the **granting permanent chose as it entered** — "As Runed
     * Halo enters, choose a card name. You have protection from the chosen card name." The name
     * twin of [ChosenCardType]: late-bound, the engine reads the source's durable choice under
     * [slot] wherever the protection is applied and matches a source whose (projected) name equals
     * it — a token only when it carries that name, a face-down permanent never (it has no name).
     * With no name chosen it protects from nothing.
     *
     * Engine-wired for player protection only — [GrantProtectionToController]; there is no
     * creature-protection keyword for a name, so the printed keywords, the one-shot grants and the
     * group grants reject it.
     *
     * @property slot The source's cast-choices slot holding the chosen name;
     *   `EntersWithChoice(ChoiceType.CARD_NAME)` writes [ChoiceSlot.CARD_NAME].
     */
    @SerialName("ProtectionScope.ChosenCardName")
    @Serializable
    data class ChosenCardName(val slot: ChoiceSlot = ChoiceSlot.CARD_NAME) : ProtectionScope

    /**
     * Protection from a subtype — "from Goblins". [cardType] narrows the quality to objects that
     * are also of that card type — "from God creatures" (Sigrid, God-Favored): a God that isn't
     * currently a creature (a Theros God below its devotion) doesn't match. Null matches the
     * subtype alone.
     */
    @SerialName("ProtectionScope.Subtype")
    @Serializable
    data class Subtype(val subtype: String, val cardType: String? = null) : ProtectionScope

    /**
     * Protection from a supertype, optionally narrowed to a [cardType] — "from legendary
     * creatures" (Tsabo Tavoc) is `Supertype("Legendary", "Creature")`, which a legendary
     * artifact or land doesn't match.
     */
    @SerialName("ProtectionScope.Supertype")
    @Serializable
    data class Supertype(val supertype: String, val cardType: String? = null) : ProtectionScope

    /** Protection from everything (Rule 702.16i). */
    @SerialName("ProtectionScope.Everything")
    @Serializable
    data object Everything : ProtectionScope

    /** Protection from each of the controller's opponents (Rule 702.16e). */
    @SerialName("ProtectionScope.EachOpponent")
    @Serializable
    data object EachOpponent : ProtectionScope

    /**
     * From spells — the quality of *being a spell* (CR 702.16a: the quality "can be any
     * characteristic value or information"). Matches a spell source: a spell being cast or on the
     * stack, including a copy of a spell. Once a permanent spell resolves it is a permanent, not a
     * spell, so its Aura/Equipment/creature half no longer matches (Emrakul, the World Anew).
     */
    @SerialName("ProtectionScope.Spells")
    @Serializable
    data object Spells : ProtectionScope

    /**
     * From permanents that were cast this turn — a battlefield source that entered this turn by
     * resolving as a cast spell (not a copy, a token, or a permanent put onto the battlefield),
     * and hasn't left since. Emrakul, the World Anew.
     */
    @SerialName("ProtectionScope.PermanentsCastThisTurn")
    @Serializable
    data object PermanentsCastThisTurn : ProtectionScope

    /**
     * From Ring-bearers — a battlefield source that is its controller's Ring-bearer (CR 701.54).
     * The designation is information about the object, so it is a quality protection can name
     * (CR 702.16a). Enforced like [PermanentsCastThisTurn]: damage, enchanting/equipping,
     * blocking, and targeting by abilities whose source is a Ring-bearer (CR 702.16b). Lord of
     * the Nazgûl ("Wraiths you control have protection from Ring-bearers").
     */
    @SerialName("ProtectionScope.RingBearers")
    @Serializable
    data object RingBearers : ProtectionScope

    /**
     * From activated abilities — only the targeting leg applies, since an ability never deals
     * damage, blocks, or enchants on its own (its *source* does). Volatile Stormdrake's "hexproof
     * from activated and triggered abilities" is this plus [TriggeredAbilities] (CR 702.11f).
     */
    @SerialName("ProtectionScope.ActivatedAbilities")
    @Serializable
    data object ActivatedAbilities : ProtectionScope

    /** From triggered abilities — the triggered twin of [ActivatedAbilities]. */
    @SerialName("ProtectionScope.TriggeredAbilities")
    @Serializable
    data object TriggeredAbilities : ProtectionScope

    /**
     * From each mana value of a [parity] — "protection from each mana value of the chosen
     * quality" (Lavabrink Venturer, with one conditional grant per choice). Mana value is a
     * characteristic (CR 202.3), so this covers every leg of protection (CR 702.16b–f): spells
     * whose mana value has the quality (a spell's {X} counts as the number chosen for it, CR
     * 202.3e), abilities from sources whose mana value does, and damage, enchanting/equipping and
     * blocking by such objects. Zero is even, so tokens that aren't copies and face-down
     * permanents (mana value 0, CR 708.2a and 202.3a) are even. A source with no mana value at all — an
     * emblem — never matches.
     *
     * Its own type rather than an axis on [CardType] or [Subtype], which name type-line words,
     * or on [Monocolored]/[Multicolored], which are colour counts.
     */
    @SerialName("ProtectionScope.ManaValueParity")
    @Serializable
    data class ManaValueParity(val parity: Parity) : ProtectionScope
}

/** Odd or even — "choose odd or even. (Zero is even.)" */
@Serializable
enum class Parity(val displayName: String) {
    ODD("odd"),
    EVEN("even");

    fun matches(value: Int): Boolean = (value % 2 == 0) == (this == EVEN)
}
