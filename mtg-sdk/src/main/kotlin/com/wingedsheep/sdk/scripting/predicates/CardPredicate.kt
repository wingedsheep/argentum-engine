package com.wingedsheep.sdk.scripting.predicates

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
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

    // =============================================================================
    // Power/Toughness Predicates
    // =============================================================================

    @SerialName("PowerEquals")
    @Serializable
    data class PowerEquals(val value: Int) : CardPredicate {
        override val description: String = "with power $value"
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

    /** Matches creatures that have the subtype chosen on the source permanent (ChosenCreatureTypeComponent) */
    @SerialName("HasChosenSubtype")
    @Serializable
    data object HasChosenSubtype : CardPredicate {
        override val description: String = "of the chosen type"
    }

    /**
     * Matches objects whose color set includes the color chosen on the source
     * permanent (read from its ChosenColorComponent). Composable color analogue of
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
