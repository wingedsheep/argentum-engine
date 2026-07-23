package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityReference
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Predicates for matching card properties (static characteristics).
 * These predicates check inherent card properties that don't change based on game state.
 *
 * CardPredicates are composed into GameObjectFilter for use in effects, targeting, and counting.
 */
@Serializable
sealed interface CardPredicate : TextReplaceable<CardPredicate> {
    val description: String

    // =============================================================================
    // Type Predicates
    // =============================================================================

    @SerialName("IsCreature")
    @Serializable
    data object IsCreature : CardPredicate {
        override val description: String = "creature"
    }

    @SerialName("IsLand")
    @Serializable
    data object IsLand : CardPredicate {
        override val description: String = "land"
    }

    @SerialName("IsArtifact")
    @Serializable
    data object IsArtifact : CardPredicate {
        override val description: String = "artifact"
    }

    @SerialName("IsEnchantment")
    @Serializable
    data object IsEnchantment : CardPredicate {
        override val description: String = "enchantment"
    }

    @SerialName("IsPlaneswalker")
    @Serializable
    data object IsPlaneswalker : CardPredicate {
        override val description: String = "planeswalker"
    }

    @SerialName("IsInstant")
    @Serializable
    data object IsInstant : CardPredicate {
        override val description: String = "instant"
    }

    @SerialName("IsSorcery")
    @Serializable
    data object IsSorcery : CardPredicate {
        override val description: String = "sorcery"
    }

    /**
     * The card has an Adventure ([com.wingedsheep.sdk.model.CardLayout.ADVENTURE]) — i.e. it is an
     * adventurer card, regardless of which face it currently shows. Matches the whole card in any
     * zone (Frantic Firebolt counts adventurer cards in your graveyard). Tokens and non-adventure
     * cards never match.
     */
    @SerialName("HasAdventure")
    @Serializable
    data object HasAdventure : CardPredicate {
        override val description: String = "has an Adventure"
    }

    @SerialName("IsBasicLand")
    @Serializable
    data object IsBasicLand : CardPredicate {
        override val description: String = "basic land"
    }

    /** Matches creature, artifact, enchantment, planeswalker, land */
    @SerialName("IsPermanent")
    @Serializable
    data object IsPermanent : CardPredicate {
        override val description: String = "permanent"
    }

    @SerialName("IsNonland")
    @Serializable
    data object IsNonland : CardPredicate {
        override val description: String = "nonland"
    }

    @SerialName("IsNoncreature")
    @Serializable
    data object IsNoncreature : CardPredicate {
        override val description: String = "noncreature"
    }

    @SerialName("IsNonenchantment")
    @Serializable
    data object IsNonenchantment : CardPredicate {
        override val description: String = "nonenchantment"
    }

    @SerialName("IsNonartifact")
    @Serializable
    data object IsNonartifact : CardPredicate {
        override val description: String = "nonartifact"
    }

    @SerialName("IsToken")
    @Serializable
    data object IsToken : CardPredicate {
        override val description: String = "token"
    }

    @SerialName("IsNontoken")
    @Serializable
    data object IsNontoken : CardPredicate {
        override val description: String = "nontoken"
    }

    // =============================================================================
    // Supertype Predicates
    // =============================================================================

    @SerialName("IsLegendary")
    @Serializable
    data object IsLegendary : CardPredicate {
        override val description: String = "legendary"
    }

    @SerialName("IsNonlegendary")
    @Serializable
    data object IsNonlegendary : CardPredicate {
        override val description: String = "nonlegendary"
    }

    // =============================================================================
    // Color Predicates
    // =============================================================================

    @SerialName("HasColor")
    @Serializable
    data class HasColor(val color: Color) : CardPredicate {
        override val description: String = color.displayName.lowercase()
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val replaced = replacer.replaceColor(color)
            return if (replaced != color) copy(color = replaced) else this
        }
    }

    @SerialName("NotColor")
    @Serializable
    data class NotColor(val color: Color) : CardPredicate {
        override val description: String = "non${color.displayName.lowercase()}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val replaced = replacer.replaceColor(color)
            return if (replaced != color) copy(color = replaced) else this
        }
    }

    /**
     * Matches cards whose colors include the color chosen during this effect's resolution
     * (e.g. via [ChooseColorThenEffect][com.wingedsheep.sdk.scripting.effects.ChooseColorThenEffect]).
     * Reads the chosen color from the resolution context, so it only matches inside an effect that
     * has stored one. Used by the Coalition Dragon cycle ("for each permanent of that color").
     */
    @SerialName("HasChosenColor")
    @Serializable
    data object HasChosenColor : CardPredicate {
        override val description: String = "of the chosen color"
    }

    @SerialName("IsColorless")
    @Serializable
    data object IsColorless : CardPredicate {
        override val description: String = "colorless"
    }

    /** One or more colors (the complement of [IsColorless]). "a permanent that's one or more colors." */
    @SerialName("IsColored")
    @Serializable
    data object IsColored : CardPredicate {
        override val description: String = "one or more colors"
    }

    @SerialName("IsMulticolored")
    @Serializable
    data object IsMulticolored : CardPredicate {
        override val description: String = "multicolored"
    }

    @SerialName("IsMonocolored")
    @Serializable
    data object IsMonocolored : CardPredicate {
        override val description: String = "monocolored"
    }

    // =============================================================================
    // Subtype Predicates
    // =============================================================================

    @SerialName("HasSubtype")
    @Serializable
    data class HasSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = subtype.value
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else HasSubtype(new)
        }
    }

    /**
     * Matches cards with any one of the given subtypes (OR logic).
     * Used for "Rabbits, Bats, Birds, and/or Mice" patterns.
     */
    @SerialName("HasAnyOfSubtypes")
    @Serializable
    data class HasAnyOfSubtypes(val subtypes: List<Subtype>) : CardPredicate {
        override val description: String = subtypes.joinToString(", ") { it.value }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newSubtypes = subtypes.map { replacer.replaceSubtype(it) }
            return if (newSubtypes == subtypes) this else HasAnyOfSubtypes(newSubtypes)
        }
    }

    @SerialName("NotSubtype")
    @Serializable
    data class NotSubtype(val subtype: Subtype) : CardPredicate {
        override val description: String = "non-${subtype.value}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = replacer.replaceSubtype(subtype)
            return if (new == subtype) this else NotSubtype(new)
        }
    }

    /** Matches basic land types: Plains, Island, Swamp, Mountain, Forest */
    @SerialName("HasBasicLandType")
    @Serializable
    data class HasBasicLandType(val landType: String) : CardPredicate {
        override val description: String = landType
    }

    // =============================================================================
    // Name Predicates
    // =============================================================================

    @SerialName("NameEquals")
    @Serializable
    data class NameEquals(val name: String) : CardPredicate {
        override val description: String = "named $name"
    }

    /**
     * Matches cards whose name equals the value stored in `chosenValues[variableName]` —
     * a card name chosen earlier in the pipeline (via [com.wingedsheep.sdk.scripting.effects.OptionType.CARD_NAME]
     * or [com.wingedsheep.sdk.scripting.effects.StoreCardNameEffect]). Matching is
     * case-insensitive. Fails closed (no match) when nothing has been stored yet, or in
     * static/projection contexts that have no pipeline. Used by "name a card … cards with
     * that name" effects (Desperate Research, Lobotomy).
     */
    @SerialName("NameEqualsChosen")
    @Serializable
    data class NameEqualsChosen(val variableName: String) : CardPredicate {
        override val description: String = "with the chosen name"
    }

    /**
     * Matches cards whose name equals a name **durably chosen by the source permanent** as it
     * entered — read from that permanent's [com.wingedsheep.engine.state.components.battlefield.CastChoicesComponent]
     * under [slot]. Matching is case-insensitive.
     *
     * Unlike [NameEqualsChosen] (which reads a transient pipeline variable and fails closed in
     * projection), this predicate is **static-projection / activation-legality safe**: the source
     * permanent's id is in scope wherever a static ability's filter is evaluated (the granter
     * supplies it as the predicate-context source). Used by name-keyed static abilities such as
     * Petrified Hamlet ("sources with the chosen name … / Lands with the chosen name …").
     *
     * Fails closed (no match) when the source has made no such choice.
     */
    @SerialName("NameEqualsChosenComponent")
    @Serializable
    data class NameEqualsChosenComponent(val slot: ChoiceSlot = ChoiceSlot.CARD_NAME) : CardPredicate {
        override val description: String = "with the chosen name"
    }

    /**
     * Matches a card whose name is **not** shared with any Room the evaluating player controls
     * (CR 709). Per the Central Elevator ruling, only the names of a Room's *unlocked* doors
     * count: a Room with no unlocked doors contributes neither of its names, and a split Room
     * card shares a name if **either** of its door names matches a controlled Room's unlocked
     * door name. Models "a Room card that doesn't have the same name as a Room you control".
     *
     * Evaluated against the evaluating player ([com.wingedsheep.engine.handlers.PredicateContext.controllerId]).
     * Fails open (matches) when no controller is in scope, and matches every Room card when the
     * controller has no unlocked doors. Pair with a `Room` subtype filter at the search site.
     */
    @SerialName("NameNotSharedWithControlledRoom")
    @Serializable
    data object NameNotSharedWithControlledRoom : CardPredicate {
        override val description: String = "that doesn't have the same name as a Room you control"
    }

    /**
     * Matches cards *originally printed* in the given set — i.e. whose canonical
     * [com.wingedsheep.sdk.model.CardDefinition.setCode] equals [setCode] (case-insensitive),
     * regardless of which printing is actually in play. This is the card's first/canonical set, so
     * a later reprint still matches its original set. Models "a name originally printed in the
     * [set] expansion" (Golgothian Sylex → ATQ; ARN City in a Bottle → ARN).
     *
     * The engine reads the entity's `CardComponent.originalSetCode`, populated from the canonical
     * definition at entity creation; tokens (no set) never match.
     */
    @SerialName("OriginallyPrintedInSet")
    @Serializable
    data class OriginallyPrintedInSet(val setCode: String) : CardPredicate {
        override val description: String = "originally printed in $setCode"
    }

    // =============================================================================
    // Keyword Predicates
    // =============================================================================

    @SerialName("HasKeyword")
    @Serializable
    data class HasKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "with ${keyword.displayName.lowercase()}"
    }

    @SerialName("NotKeyword")
    @Serializable
    data class NotKeyword(val keyword: Keyword) : CardPredicate {
        override val description: String = "without ${keyword.displayName.lowercase()}"
    }

    // =============================================================================
    // Mana Value Predicates
    // =============================================================================

    @SerialName("ManaValueEquals")
    @Serializable
    data class ManaValueEquals(val value: Int) : CardPredicate {
        override val description: String = "with mana value $value"
    }

    @SerialName("ManaValueAtMost")
    @Serializable
    data class ManaValueAtMost(val max: Int) : CardPredicate {
        override val description: String = "with mana value $max or less"
    }

    /**
     * Mana value exactly equal to the number chosen for the source spell/ability.
     * Resolves against [PredicateContext.xValue] at evaluation time. Used by effects
     * that "choose a number" and then act on objects with that mana value (Void). When
     * the chosen number is unbound, nothing matches.
     */
    @SerialName("ManaValueEqualsX")
    @Serializable
    data object ManaValueEqualsX : CardPredicate {
        override val description: String = "with mana value equal to the chosen number"
    }

    /**
     * Mana value at most the X chosen for the source spell/ability.
     * Resolves against [PredicateContext.xValue] at evaluation time, so it works
     * both at cast-time target validation and at resolution-time legality re-check.
     */
    @SerialName("ManaValueAtMostX")
    @Serializable
    data object ManaValueAtMostX : CardPredicate {
        override val description: String = "with mana value X or less"
    }

    @SerialName("ManaValueAtLeast")
    @Serializable
    data class ManaValueAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with mana value $min or greater"
    }

    /**
     * Mana value at most that of a referenced entity. Resolves the entity from
     * [PredicateContext] (triggering entity, source permanent, etc.) and compares against
     * its [com.wingedsheep.sdk.model.CardComponent]'s mana value.
     *
     * Used by cards like Kodama of the East Tree: "a permanent card with equal or lesser
     * mana value [than the permanent that just entered]".
     */
    @SerialName("ManaValueAtMostEntity")
    @Serializable
    data class ManaValueAtMostEntity(val reference: EntityReference) : CardPredicate {
        override val description: String = "with mana value less than or equal to ${reference.description}"
    }

    /**
     * Mana value at most the amount of mana actually spent to cast a referenced entity.
     *
     * Resolves the reference (typically [EntityReference.Source]) and reads its mana-spent
     * record: the live `SpellOnStackComponent` buckets while the source is still a spell, or
     * the `CastRecordComponent` snapshot once it has resolved into a permanent. Returns no
     * match when neither is present (e.g., the source was put onto the battlefield without
     * being cast, so 0 mana was spent — CR-faithful for "X is the amount of mana spent").
     *
     * Used by Edge of Eternities warp payoffs like Astelli Reclaimer: "return target ...
     * card with mana value X or less ..., where X is the amount of mana spent to cast this
     * creature." X is 5 cast for {3}{W}{W}, 3 cast with warp for {2}{W}, 0 cast for free.
     */
    @SerialName("ManaValueAtMostEntityManaSpent")
    @Serializable
    data class ManaValueAtMostEntityManaSpent(val reference: EntityReference) : CardPredicate {
        override val description: String =
            "with mana value less than or equal to the mana spent to cast ${reference.description}"
    }

    /**
     * Mana value at most the number of distinct *colors* of mana spent to cast a referenced
     * entity (0–5). Sibling of [ManaValueAtMostEntityManaSpent], but compares against the
     * color *count* rather than the total mana — the **Converge** exile-by-color-count variant
     * ("… mana value less than or equal to the number of colors of mana spent to cast this
     * creature"). Resolves the reference and reads its recorded payment (live spell buckets or
     * the resolved permanent's cast record); colorless is not a color and never contributes.
     */
    @SerialName("ManaValueAtMostColorsSpent")
    @Serializable
    data class ManaValueAtMostColorsSpent(val reference: EntityReference) : CardPredicate {
        override val description: String =
            "with mana value less than or equal to the number of colors of mana spent to cast ${reference.description}"
    }

    /**
     * Mana value at most a resolved [DynamicAmount]. The general "mana value X or less, where X is
     * <some game value>" cap: feed it any [DynamicAmount] (a turn-tracking total, a count over a
     * filter, a life total, an arithmetic composition, …) and the engine evaluates it at the moment
     * the predicate is checked, comparing against the card's mana value.
     *
     * The sibling fixed/entity-derived caps cover their narrow cases ([ManaValueAtMost] = a constant,
     * [ManaValueAtMostX] = the cast {X}, [ManaValueAtMostEntity] / [ManaValueAtMostEntityManaSpent] /
     * [ManaValueAtMostColorsSpent] = values read off a referenced entity). This variant is the
     * open-ended one for any other dynamic source — e.g. Moseo, Vein's New Dean: "return … a creature
     * card with mana value X or less …, where X is the amount of life you gained this turn"
     * ([DynamicAmount.TurnTracking] of `LIFE_GAINED`).
     */
    @SerialName("ManaValueAtMostDynamic")
    @Serializable
    data class ManaValueAtMostDynamic(val amount: DynamicAmount) : CardPredicate {
        override val description: String = "with mana value ${amount.description} or less"

        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newAmount = amount.applyTextReplacement(replacer)
            return if (newAmount === amount) this else copy(amount = newAmount)
        }
    }

    @SerialName("ManaValueIsEven")
    @Serializable
    data object ManaValueIsEven : CardPredicate {
        override val description: String = "with even mana value"
    }

    @SerialName("ManaValueIsOdd")
    @Serializable
    data object ManaValueIsOdd : CardPredicate {
        override val description: String = "with odd mana value"
    }

    /**
     * Matches a card whose printed mana cost contains an {X} symbol (e.g. "a card with {X} in its
     * mana cost", Paradox Surveyor). This inspects the printed cost's `hasX` flag, not the computed
     * mana value — so a card on the stack cast with X=0 still matches by its printed cost, and a
     * face-down object (no mana cost) never matches.
     */
    @SerialName("HasXInManaCost")
    @Serializable
    data object HasXInManaCost : CardPredicate {
        override val description: String = "with {X} in its mana cost"
    }

    // =============================================================================
    // Power/Toughness Predicates
    // =============================================================================

    @SerialName("PowerEquals")
    @Serializable
    data class PowerEquals(val value: Int) : CardPredicate {
        override val description: String = "with power $value"
    }

    /**
     * Power exactly equal to the X chosen for the source spell/ability. Resolves against
     * `PredicateContext.xValue` at evaluation time — the power analogue of [ManaValueEqualsX].
     * Used by an X-cost activated ability that targets "a creature with power X"
     * (Ent-Draught Basin). When X is unbound (legal-action enumeration runs before the player
     * chooses X) it matches permissively so the ability is still offered; the chosen X is then
     * enforced at activation-time validation and resolution-time re-check.
     */
    @SerialName("PowerEqualsX")
    @Serializable
    data object PowerEqualsX : CardPredicate {
        override val description: String = "with power X"
    }

    @SerialName("PowerAtMost")
    @Serializable
    data class PowerAtMost(val max: Int) : CardPredicate {
        override val description: String = "with power $max or less"
    }

    @SerialName("PowerAtLeast")
    @Serializable
    data class PowerAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with power $min or greater"
    }

    @SerialName("ToughnessEquals")
    @Serializable
    data class ToughnessEquals(val value: Int) : CardPredicate {
        override val description: String = "with toughness $value"
    }

    @SerialName("ToughnessAtMost")
    @Serializable
    data class ToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with toughness $max or less"
    }

    /**
     * Toughness at most the X chosen for the source spell/ability.
     * Resolves against [PredicateContext.xValue] at evaluation time, so it works at the
     * spell's resolution-time filter pass (e.g., Zero Point Ballad's mass destruction).
     */
    @SerialName("ToughnessAtMostX")
    @Serializable
    data object ToughnessAtMostX : CardPredicate {
        override val description: String = "with toughness X or less"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    @SerialName("ToughnessAtLeast")
    @Serializable
    data class ToughnessAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with toughness $min or greater"
    }

    /** Power or toughness is at least the given value (OR logic) */
    @SerialName("PowerOrToughnessAtLeast")
    @Serializable
    data class PowerOrToughnessAtLeast(val min: Int) : CardPredicate {
        override val description: String = "with power or toughness $min or greater"
    }

    /** Power or toughness is at most the given value (OR logic) */
    @SerialName("PowerOrToughnessAtMost")
    @Serializable
    data class PowerOrToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with power or toughness $max or less"
    }

    /** Total power and toughness (sum) is at most the given value */
    @SerialName("TotalPowerAndToughnessAtMost")
    @Serializable
    data class TotalPowerAndToughnessAtMost(val max: Int) : CardPredicate {
        override val description: String = "with total power and toughness $max or less"
    }

    /** Toughness is strictly greater than power */
    @SerialName("ToughnessGreaterThanPower")
    @Serializable
    data object ToughnessGreaterThanPower : CardPredicate {
        override val description: String = "with toughness greater than its power"
    }

    /**
     * Power strictly greater than the projected power of a referenced entity. Mirrors
     * [ManaValueAtMostEntity] in shape and resolution: the entity is looked up via
     * [PredicateContext] (Source / Triggering / AffectedEntity), and the comparison reads
     * its [com.wingedsheep.sdk.model.CardComponent] power (projected when available).
     *
     * Used by Éowyn, Fearless Knight: "exile target creature an opponent controls with
     * greater power" — the candidate's power is compared against Éowyn's
     * ([EntityReference.Source]) at target choice and at resolution-time legality re-check.
     */
    @SerialName("PowerGreaterThanEntity")
    @Serializable
    data class PowerGreaterThanEntity(val reference: EntityReference) : CardPredicate {
        override val description: String = "with power greater than ${reference.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /**
     * Power less than or equal to the projected power of a referenced entity. Mirror of
     * [PowerGreaterThanEntity]; the entity is looked up via [PredicateContext]
     * (Source / Triggering / AffectedEntity) and the comparison reads its
     * [com.wingedsheep.sdk.model.CardComponent] power (projected when available).
     *
     * Used by Old Man of the Sea: "target creature with power less than or equal to this
     * creature's power" — the candidate's power is compared against the Old Man's
     * ([EntityReference.Source]) at target choice and at resolution-time legality re-check.
     */
    @SerialName("PowerAtMostEntity")
    @Serializable
    data class PowerAtMostEntity(val reference: EntityReference) : CardPredicate {
        override val description: String = "with power less than or equal to ${reference.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /**
     * Power strictly less than the projected power of [reference] (e.g. "a creature with lesser
     * power" than the source — Rangers of Ithilien). Strict counterpart of [PowerAtMostEntity].
     */
    @SerialName("PowerLessThanEntity")
    @Serializable
    data class PowerLessThanEntity(val reference: EntityReference) : CardPredicate {
        override val description: String = "with power less than ${reference.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate = this
    }

    /**
     * Projected power strictly greater than the object's own base power — "a creature with power
     * greater than its base power" (Kutzil, Malamet Exemplar; the Malamet cycle). Self-relative:
     * the comparison reads the object's current (projected) power against its printed base power
     * ([com.wingedsheep.sdk.model.CardComponent.baseStats] `.basePower`), so any pump that raises
     * power above base — a +1/+1 counter, an anthem, a temporary boost — qualifies, while a
     * shrunk or unmodified creature does not. Off-battlefield objects (no projected power) never
     * match; `*`/CDA power (no fixed printed base) never matches.
     */
    @SerialName("PowerGreaterThanBase")
    @Serializable
    data object PowerGreaterThanBase : CardPredicate {
        override val description: String = "with power greater than its base power"
    }

    // =============================================================================
    // Context-relative Predicates (Pipeline Variable References)
    // =============================================================================

    /** Matches cards that have a subtype matching a value stored in chosenValues[variableName] */
    @SerialName("HasSubtypeFromVariable")
    @Serializable
    data class HasSubtypeFromVariable(val variableName: String) : CardPredicate {
        override val description: String = "of the chosen type"
    }

    /** Matches cards that have a subtype matching any string in storedStringLists[listName] */
    @SerialName("HasSubtypeInStoredList")
    @Serializable
    data class HasSubtypeInStoredList(val listName: String) : CardPredicate {
        override val description: String = "of a type chosen this way"
    }

    /**
     * Matches cards that share at least one subtype with **each** subtype group stored
     * under the named key in the pipeline's `storedSubtypeGroups` map. A group is a
     * `Set<String>` of subtypes — typically produced by
     * [com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect] which reads an
     * entity collection from `storedCollections` and extracts each entity's projected
     * subtypes into a `List<Set<String>>`.
     *
     * Semantics: for every group `G_i` in the stored list, the card's subtype set `C`
     * must satisfy `C ∩ G_i ≠ ∅`. This is stricter than "shares a subtype with any
     * one group" but looser than "has a subtype in the intersection of groups" — e.g.,
     * tap {Elf, Warrior} + {Goblin, Rogue}, a Warrior-Rogue card matches (shares
     * Warrior with the first group and Rogue with the second) even though the
     * intersection is empty.
     *
     * Used by Cryptic Gateway: "creature card that shares a creature type with each
     * creature tapped this way".
     */
    @SerialName("HasSubtypeInEachStoredGroup")
    @Serializable
    data class HasSubtypeInEachStoredGroup(val groupName: String) : CardPredicate {
        override val description: String = "that shares a creature type with each of them"
    }

    // =============================================================================
    // Source-relative Predicates
    // =============================================================================

    /** Matches creatures that are NOT of the type chosen on the source permanent */
    @SerialName("NotOfSourceChosenType")
    @Serializable
    data object NotOfSourceChosenType : CardPredicate {
        override val description: String = "that isn't of the chosen type"
    }

    /** Matches spells that share a creature subtype with the source permanent's projected types */
    @SerialName("SharesCreatureTypeWithSource")
    @Serializable
    data object SharesCreatureTypeWithSource : CardPredicate {
        override val description: String = "that shares a creature type with this creature"
    }

    /** Matches creatures that share a creature subtype with the triggering entity */
    @SerialName("SharesCreatureTypeWithTriggeringEntity")
    @Serializable
    data object SharesCreatureTypeWithTriggeringEntity : CardPredicate {
        override val description: String = "that shares a creature type with it"
    }

    /** Matches creatures that have the subtype chosen on the source permanent (CastChoicesComponent) */
    @SerialName("HasChosenSubtype")
    @Serializable
    data object HasChosenSubtype : CardPredicate {
        override val description: String = "of the chosen type"
    }

    /**
     * Matches objects whose color set includes the color chosen on the source
     * permanent (read from its CastChoicesComponent). Composable color analogue of
     * [HasChosenSubtype] — combine with a type predicate to express e.g. "an instant
     * or sorcery spell of the chosen color" (Harsh Judgment). Colorless objects never
     * match; if the source has no chosen color, nothing matches.
     */
    @SerialName("SharesChosenColorWithSource")
    @Serializable
    data object SharesChosenColorWithSource : CardPredicate {
        override val description: String = "of the chosen color"
    }

    /** Matches creatures that share a creature subtype with the referenced entity */
    @SerialName("SharesCreatureTypeWith")
    @Serializable
    data class SharesCreatureTypeWith(val entity: EntityReference) : CardPredicate {
        override val description: String = when (entity) {
            is EntityReference.Source -> "that shares a creature type with this creature"
            is EntityReference.Triggering -> "that shares a creature type with it"
            else -> "that shares a creature type with ${entity.description}"
        }
    }

    /** Matches objects that share a color with the referenced entity */
    @SerialName("SharesColorWith")
    @Serializable
    data class SharesColorWith(val entity: EntityReference) : CardPredicate {
        override val description: String = when (entity) {
            is EntityReference.Source -> "that shares a color with this permanent"
            is EntityReference.Triggering -> "that shares a color with it"
            else -> "that shares a color with ${entity.description}"
        }
    }

    /**
     * Matches objects that share a color with the recipient of the in-flight damage,
     * and are not that recipient. Only meaningful inside a damage replacement's source
     * filter, where the engine supplies the recipient. Combine with a type predicate to
     * express "another creature that shares a color" — Well-Laid Plans uses
     * `GameObjectFilter.Creature` + this predicate. Colorless on either side never matches.
     */
    @SerialName("SharesColorWithRecipient")
    @Serializable
    data object SharesColorWithRecipient : CardPredicate {
        override val description: String = "that shares a color with it"
    }

    /**
     * Matches objects that share a color with at least one permanent the evaluating player
     * controls matching [filter]. Used by Ringsight ("a card that shares a color with a legendary
     * creature you control") with `filter = GameObjectFilter.Creature.legendary()`. The colors of
     * the controlled permanents are read from projected state (so anthem/devotion-style color
     * grants are honored). Colorless candidates never match.
     */
    @SerialName("SharesColorWithPermanentYouControl")
    @Serializable
    data class SharesColorWithPermanentYouControl(val filter: GameObjectFilter) : CardPredicate {
        override val description: String = "that shares a color with ${filter.description} you control"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Matches creature cards that share **no** creature type with any permanent the evaluating
     * player controls matching [filter]. Used by Radagast the Brown ("a creature card that doesn't
     * share a creature type with a creature you control") with `filter = GameObjectFilter.Creature`.
     * The creature types of the controlled permanents are read from projected state, so granted
     * types (changelings, type-changing effects) are honored. A candidate with no creature types of
     * its own shares none, so it matches.
     */
    @SerialName("DoesNotShareCreatureTypeWithPermanentYouControl")
    @Serializable
    data class DoesNotShareCreatureTypeWithPermanentYouControl(val filter: GameObjectFilter) : CardPredicate {
        override val description: String = "that doesn't share a creature type with ${filter.description} you control"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    /**
     * Matches cards that share **no** land type with any permanent the evaluating player
     * controls matching [filter]. Used by Hiveheart Shaman ("a basic land card that doesn't
     * share a land type with a land you control") with `filter = GameObjectFilter.Land`. The
     * land types of the controlled permanents are read from projected state, so type-changing
     * effects are honored. A candidate with no land types of its own shares none, so it matches.
     */
    @SerialName("DoesNotShareLandTypeWithPermanentYouControl")
    @Serializable
    data class DoesNotShareLandTypeWithPermanentYouControl(val filter: GameObjectFilter) : CardPredicate {
        override val description: String = "that doesn't share a land type with ${filter.description} you control"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newFilter = filter.applyTextReplacement(replacer)
            return if (newFilter !== filter) copy(filter = newFilter) else this
        }
    }

    // =============================================================================
    // Stack Item Type Predicates
    // =============================================================================

    /**
     * Matches activated or triggered abilities on the stack (not spells).
     * Used by cards like Stifle: "Counter target activated or triggered ability."
     */
    @SerialName("IsActivatedOrTriggeredAbility")
    @Serializable
    data object IsActivatedOrTriggeredAbility : CardPredicate {
        override val description: String = "activated or triggered ability"
    }

    /**
     * Matches only triggered abilities on the stack (not activated abilities or spells).
     * Used by cards like Kirol, Attentive First-Year: "Copy target triggered ability you control."
     */
    @SerialName("IsTriggeredAbility")
    @Serializable
    data object IsTriggeredAbility : CardPredicate {
        override val description: String = "triggered ability"
    }

    /**
     * Matches only activated abilities on the stack (not triggered abilities or spells).
     * Mana abilities never use the stack, so they can't be matched. Used by cards like
     * Bind: "Counter target activated ability." (Stifle counters either kind and uses the
     * broader [IsActivatedOrTriggeredAbility].)
     */
    @SerialName("IsActivatedAbility")
    @Serializable
    data object IsActivatedAbility : CardPredicate {
        override val description: String = "activated ability"
    }

    /**
     * Matches a permanent that has at least one intrinsic activated ability that isn't a
     * mana ability (and isn't a loyalty ability). Backed by the precomputed
     * `CardComponent.hasNonManaActivatedAbility` flag, so abilities granted by other
     * continuous effects are not counted. Used by Tsabo's Web: "Each land with an activated
     * ability that isn't a mana ability doesn't untap during its controller's untap step."
     */
    @SerialName("HasNonManaActivatedAbility")
    @Serializable
    data object HasNonManaActivatedAbility : CardPredicate {
        override val description: String = "with an activated ability that isn't a mana ability"
    }

    /**
     * Matches a permanent or graveyard card that has at least one intrinsic activated ability of
     * any kind — mana, loyalty, or otherwise — activatable from the battlefield. Backed by the
     * precomputed `CardComponent.hasActivatedAbility` flag, so abilities granted by other
     * continuous effects are not counted. Unlike [HasNonManaActivatedAbility] this DOES count mana
     * abilities. Used by the craft material clause on The Enigma Jewel: "Craft with four or more
     * nonlands with activated abilities."
     */
    @SerialName("HasActivatedAbility")
    @Serializable
    data object HasActivatedAbility : CardPredicate {
        override val description: String = "with an activated ability"
    }

    /**
     * Matches a spell or ability on the stack at least one of whose chosen targets
     * matches [subfilter]. Player targets are skipped (they aren't card-like and have
     * no game-object filter to match against). Used by cards like Teferi's Response
     * ("target spell or ability ... that targets a land you control").
     *
     * The subfilter is evaluated in the same [com.wingedsheep.engine.handlers.PredicateContext]
     * as the outer match — so "you control" inside the subfilter resolves against the
     * outer chooser, exactly as for any other filter.
     */
    @SerialName("TargetsMatching")
    @Serializable
    data class TargetsMatching(val subfilter: GameObjectFilter) : CardPredicate {
        override val description: String = "that targets ${subfilter.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val newSubfilter = subfilter.applyTextReplacement(replacer)
            return if (newSubfilter !== subfilter) copy(subfilter = newSubfilter) else this
        }
    }

    // =============================================================================
    // Composite Predicates
    // =============================================================================

    @SerialName("And")
    @Serializable
    data class And(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicates.map { it.applyTextReplacement(replacer) }
            return if (new == predicates) this else And(new)
        }
    }

    @SerialName("Or")
    @Serializable
    data class Or(val predicates: List<CardPredicate>) : CardPredicate {
        override val description: String = predicates.joinToString(" or ") { it.description }
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicates.map { it.applyTextReplacement(replacer) }
            return if (new == predicates) this else Or(new)
        }
    }

    @SerialName("Not")
    @Serializable
    data class Not(val predicate: CardPredicate) : CardPredicate {
        override val description: String = "non-${predicate.description}"
        override fun applyTextReplacement(replacer: TextReplacer): CardPredicate {
            val new = predicate.applyTextReplacement(replacer)
            return if (new === predicate) this else Not(new)
        }
    }
}
