package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.text.TextReplaceable
import com.wingedsheep.sdk.scripting.text.TextReplacer
import kotlinx.serialization.Serializable

/**
 * Filter for targeting game objects, with zone context.
 * Wraps GameObjectFilter and adds zone-specific targeting behavior.
 *
 * Unified approach for targeting game objects across all zones.
 *
 * ## Usage Examples
 *
 * ```kotlin
 * // Target any creature
 * TargetFilter.Creature
 *
 * // Target tapped creature
 * TargetFilter.TappedCreature
 *
 * // Target creature in graveyard
 * TargetFilter.CreatureInGraveyard
 *
 * // Target spell on stack
 * TargetFilter.SpellOnStack
 *
 * // Custom filter: target black creature you control
 * TargetFilter(GameObjectFilter.Creature.withColor(Color.BLACK).youControl())
 * ```
 */
@Serializable
data class TargetFilter(
    val baseFilter: GameObjectFilter,
    val zone: Zone = Zone.BATTLEFIELD,
    val excludeSelf: Boolean = false,
    /**
     * If true, the entity referenced by the trigger's `triggeringEntityId` is excluded.
     * Models "other than that creature" phrasing where "that creature" = the trigger's
     * triggering entity (e.g., the creature that became the target of an opponent's spell),
     * not the source of the ability.
     */
    val excludeTriggeringEntity: Boolean = false,
    /**
     * Additional zone-scoped clauses unioned with this one, for a single target that may be
     * chosen from several zones each with its own predicate — the cross-zone "or" wording
     * (Sorceress's Schemes: "instant or sorcery card from your graveyard *or* exiled card with
     * flashback you own"). The legal-target set is the union over [clauses] (this filter plus all
     * [alternatives]), and a chosen target is legal iff it satisfies *any* clause. Each alternative
     * carries its own [zone]/[baseFilter], so the clauses can span graveyard, exile, the
     * battlefield, etc.
     *
     * This is *not* a multi-target requirement — it is still a single target. Build it through
     * [or] / [anyOf]; the common single-zone filter leaves this empty. `GameObjectFilter.anyOf`
     * is the same idea *within one zone*; this lifts it across zones, which the flat `baseFilter`
     * can't express because each zone needs its own predicate.
     */
    val alternatives: List<TargetFilter> = emptyList()
) : TextReplaceable<TargetFilter> {
    val description: String
        get() = buildDescription()

    /** True when this filter is a cross-zone union (has at least one [alternatives] clause). */
    val isUnion: Boolean get() = alternatives.isNotEmpty()

    /**
     * Flatten this filter into its single-zone clauses: this filter (with [alternatives] stripped)
     * followed by every alternative's own clauses. Each returned filter has an empty [alternatives],
     * so dispatch sites can branch on [zone] without re-checking for unions. A non-union filter
     * returns just itself.
     */
    fun clauses(): List<TargetFilter> =
        listOf(if (alternatives.isEmpty()) this else copy(alternatives = emptyList())) +
            alternatives.flatMap { it.clauses() }

    /** Union this filter with [other] — adds [other] as an alternative clause. */
    fun or(other: TargetFilter): TargetFilter = copy(alternatives = alternatives + other)

    private fun buildDescription(): String =
        if (alternatives.isEmpty()) describeClause()
        else clauses().joinToString(" or ") { it.describeClause() }

    private fun describeClause(): String = buildString {
        if (excludeSelf) append("other ")
        append(baseFilter.description)
        if (zone != Zone.BATTLEFIELD) {
            append(" in ")
            append(zone.displayName)
        }
    }

    // =============================================================================
    // Pre-built Creature Targets (Battlefield)
    // =============================================================================

    companion object {
        /**
         * A cross-zone union target: a single target chosen from any of the given clauses, each
         * with its own zone/predicate (Sorceress's Schemes). The first clause's zone is treated as
         * the primary one for display/zone purposes. See [alternatives].
         */
        fun anyOf(first: TargetFilter, vararg rest: TargetFilter): TargetFilter =
            first.copy(alternatives = first.alternatives + rest.toList())

        /** Target any creature */
        val Creature = TargetFilter(GameObjectFilter.Companion.Creature)

        /** Target creature you control */
        val CreatureYouControl = TargetFilter(GameObjectFilter.Companion.Creature.youControl())

        /** Target creature an opponent controls */
        val CreatureOpponentControls = TargetFilter(GameObjectFilter.Companion.Creature.opponentControls())

        /** Target other creature (excluding source) */
        val OtherCreature = TargetFilter(GameObjectFilter.Companion.Creature, excludeSelf = true)

        /** Target other creature you control */
        val OtherCreatureYouControl = TargetFilter(GameObjectFilter.Companion.Creature.youControl(), excludeSelf = true)

        /** Target tapped creature */
        val TappedCreature = TargetFilter(GameObjectFilter.Companion.Creature.tapped())

        /** Target untapped creature */
        val UntappedCreature = TargetFilter(GameObjectFilter.Companion.Creature.untapped())

        /** Target attacking creature */
        val AttackingCreature = TargetFilter(GameObjectFilter.Companion.Creature.attacking())

        /** Target blocking creature */
        val BlockingCreature = TargetFilter(GameObjectFilter.Companion.Creature.blocking())

        /** Target attacking or blocking creature */
        val AttackingOrBlockingCreature = TargetFilter(GameObjectFilter.Companion.Creature.attackingOrBlocking())

        /** Target nonlegendary creature */
        val NonlegendaryCreature = TargetFilter(GameObjectFilter.Companion.Creature.nonlegendary())

        // =============================================================================
        // Pre-built Permanent Targets (Battlefield)
        // =============================================================================

        /** Target any permanent */
        val Permanent = TargetFilter(GameObjectFilter.Companion.Permanent)

        /** Target nonland permanent */
        val NonlandPermanent = TargetFilter(GameObjectFilter.Companion.NonlandPermanent)

        /** Another target nonland permanent (excluding the source) */
        val OtherNonlandPermanent = TargetFilter(GameObjectFilter.Companion.NonlandPermanent, excludeSelf = true)

        /** Target permanent you control */
        val PermanentYouControl = TargetFilter(GameObjectFilter.Companion.Permanent.youControl())

        /** Target nonland permanent an opponent controls */
        val NonlandPermanentOpponentControls = TargetFilter(GameObjectFilter.Companion.NonlandPermanent.opponentControls())

        /** Target artifact */
        val Artifact = TargetFilter(GameObjectFilter.Companion.Artifact)

        /** Target enchantment */
        val Enchantment = TargetFilter(GameObjectFilter.Companion.Enchantment)

        /** Target creature or enchantment */
        val CreatureOrEnchantment = TargetFilter(GameObjectFilter.Companion.CreatureOrEnchantment)

        /** Target artifact or enchantment */
        val ArtifactOrEnchantment = TargetFilter(GameObjectFilter.Companion.ArtifactOrEnchantment)

        /** Target artifact, creature, or enchantment */
        val ArtifactCreatureOrEnchantment =
            TargetFilter(GameObjectFilter.Companion.ArtifactCreatureOrEnchantment)

        /** Target artifact, creature, or enchantment an opponent controls */
        val ArtifactCreatureOrEnchantmentOpponentControls =
            TargetFilter(GameObjectFilter.Companion.ArtifactCreatureOrEnchantment.opponentControls())

        /** Target creature or artifact */
        val CreatureOrArtifact = TargetFilter(GameObjectFilter.Companion.CreatureOrArtifact)

        /** Target artifact or land */
        val ArtifactOrLand = TargetFilter(GameObjectFilter.Companion.ArtifactOrLand)

        /** Target land */
        val Land = TargetFilter(GameObjectFilter.Companion.Land)

        /** Target nonbasic land (Rocket Volley, Shivan Harvest, Encroaching Wastes). */
        val NonbasicLand = TargetFilter(GameObjectFilter.Companion.NonbasicLand)

        /** Target planeswalker */
        val Planeswalker = TargetFilter(GameObjectFilter.Companion.Planeswalker)

        // =============================================================================
        // Pre-built Graveyard Targets
        // =============================================================================

        /** Target any card in a graveyard */
        val CardInGraveyard = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.GRAVEYARD)

        /** Target creature card in a graveyard */
        val CreatureInGraveyard = TargetFilter(GameObjectFilter.Companion.Creature, zone = Zone.GRAVEYARD)

        /** Target creature card in your graveyard */
        val CreatureInYourGraveyard = TargetFilter(GameObjectFilter.Companion.Creature.ownedByYou(), zone = Zone.GRAVEYARD)

        /** Target instant or sorcery card in a graveyard */
        val InstantOrSorceryInGraveyard = TargetFilter(GameObjectFilter.Companion.InstantOrSorcery, zone = Zone.GRAVEYARD)

        /** Target instant or sorcery card in your graveyard */
        val InstantOrSorceryInYourGraveyard = TargetFilter(GameObjectFilter.Companion.InstantOrSorcery.ownedByYou(), zone = Zone.GRAVEYARD)

        // =============================================================================
        // Pre-built Stack Targets
        // =============================================================================

        /** Target any spell on the stack */
        val SpellOnStack = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.STACK)

        /** Target creature spell on the stack */
        val CreatureSpellOnStack = TargetFilter(GameObjectFilter.Companion.Creature, zone = Zone.STACK)

        /** Target noncreature spell on the stack */
        val NoncreatureSpellOnStack = TargetFilter(GameObjectFilter.Companion.Noncreature, zone = Zone.STACK)

        /** Target instant or sorcery spell on the stack */
        val InstantOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.InstantOrSorcery, zone = Zone.STACK)

        // =============================================================================
        // Permanent Targeting
        // =============================================================================

        /** Target permanent an opponent controls */
        val PermanentOpponentControls = TargetFilter(GameObjectFilter.Companion.Permanent.opponentControls())

        /** Target creature or land permanent */
        val CreatureOrLandPermanent = TargetFilter(GameObjectFilter.Companion.CreatureOrLand)

        /** Target noncreature permanent */
        val NoncreaturePermanent = TargetFilter(GameObjectFilter.Companion.NoncreaturePermanent)

        // =============================================================================
        // Spell Targeting (additional)
        // =============================================================================

        /** Target sorcery spell on the stack */
        val SorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.Sorcery, zone = Zone.STACK)

        /** Target creature or sorcery spell on the stack */
        val CreatureOrSorcerySpellOnStack = TargetFilter(GameObjectFilter.Companion.CreatureOrSorcery, zone = Zone.STACK)

        /** Target instant spell on the stack */
        val InstantSpellOnStack = TargetFilter(GameObjectFilter.Companion.Instant, zone = Zone.STACK)

        /** Target activated or triggered ability on the stack */
        val ActivatedOrTriggeredAbilityOnStack = TargetFilter(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsActivatedOrTriggeredAbility)),
            zone = Zone.STACK
        )

        /** Target triggered ability on the stack (not activated, not spells) */
        val TriggeredAbilityOnStack = TargetFilter(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsTriggeredAbility)),
            zone = Zone.STACK
        )

        /** Target activated ability on the stack (not triggered, not spells; mana abilities never use the stack) */
        val ActivatedAbilityOnStack = TargetFilter(
            GameObjectFilter(cardPredicates = listOf(CardPredicate.IsActivatedAbility)),
            zone = Zone.STACK
        )

        /** Target any spell or ability on the stack (spells and activated/triggered abilities) */
        val SpellOrAbilityOnStack = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.STACK)

        /**
         * Target an instant spell, sorcery spell, activated ability, or triggered ability on the
         * stack — the four-way "copy target spell or ability" clause (Return the Favor, the
         * Fork/Twincast family generalized to abilities). Expressed as a single [CardPredicate.Or]
         * so the targeting enumeration matches whichever stack-object kind the chosen entity is.
         */
        val InstantSorcerySpellOrAbilityOnStack = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsInstant,
                            CardPredicate.IsSorcery,
                            CardPredicate.IsActivatedOrTriggeredAbility
                        )
                    )
                )
            ),
            zone = Zone.STACK
        )
    }

    // =============================================================================
    // Fluent Builder Methods (delegates to GameObjectFilter)
    // =============================================================================

    /** Add color requirement */
    fun withColor(color: Color) = copy(baseFilter = baseFilter.withColor(color))

    /** Match any of the specified colors (OR logic), e.g. "target white or black creature". */
    fun withAnyColor(vararg colors: Color) = copy(baseFilter = baseFilter.withAnyColor(*colors))

    /** Exclude color */
    fun notColor(color: Color) = copy(baseFilter = baseFilter.notColor(color))

    /** Restrict to nonartifact objects ("nonartifact creature", the Terror template). */
    fun nonartifact() = copy(baseFilter = baseFilter.nonartifact())

    /** Add subtype requirement */
    fun withSubtype(subtype: Subtype) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add subtype by string */
    fun withSubtype(subtype: String) = copy(baseFilter = baseFilter.withSubtype(subtype))

    /** Add keyword requirement */
    fun withKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withKeyword(keyword))

    /** Exclude keyword */
    fun withoutKeyword(keyword: Keyword) = copy(baseFilter = baseFilter.withoutKeyword(keyword))

    /** Mana value equals */
    fun manaValue(value: Int) = copy(baseFilter = baseFilter.manaValue(value))

    /** Mana value at most */
    fun manaValueAtMost(max: Int) = copy(baseFilter = baseFilter.manaValueAtMost(max))

    /** Mana value at most the X chosen for the source spell/ability */
    fun manaValueAtMostX() = copy(baseFilter = baseFilter.manaValueAtMostX())

    /** Mana value exactly equal to the X chosen for the source spell/ability (Repeal, Spell Blast). */
    fun manaValueEqualsX() = copy(baseFilter = baseFilter.manaValueEqualsX())

    /** Mana value at least */
    fun manaValueAtLeast(min: Int) = copy(baseFilter = baseFilter.manaValueAtLeast(min))

    /** Power exactly equal to the X chosen for the source spell/ability (Ent-Draught Basin). */
    fun powerEqualsX() = copy(baseFilter = baseFilter.powerEqualsX())

    /** Power at most */
    fun powerAtMost(max: Int) = copy(baseFilter = baseFilter.powerAtMost(max))

    /** Power at least */
    fun powerAtLeast(min: Int) = copy(baseFilter = baseFilter.powerAtLeast(min))

    /** Power strictly greater than the projected power of a referenced entity (source, triggering, etc.) */
    fun powerGreaterThanEntity(reference: com.wingedsheep.sdk.scripting.values.EntityReference) =
        copy(baseFilter = baseFilter.powerGreaterThanEntity(reference))

    /** Power strictly less than the projected power of a referenced entity (source, triggering, etc.) */
    fun powerLessThanEntity(reference: com.wingedsheep.sdk.scripting.values.EntityReference) =
        copy(baseFilter = baseFilter.powerLessThanEntity(reference))

    /** Projected power strictly greater than the object's own base (printed) power. */
    fun powerGreaterThanBase() = copy(baseFilter = baseFilter.powerGreaterThanBase())

    /** Power less than or equal to the projected power of a referenced entity (source, triggering, etc.) */
    fun powerAtMostEntity(reference: com.wingedsheep.sdk.scripting.values.EntityReference) =
        copy(baseFilter = baseFilter.powerAtMostEntity(reference))

    /** Toughness at most */
    fun toughnessAtMost(max: Int) = copy(baseFilter = baseFilter.toughnessAtMost(max))

    /** Toughness at least */
    fun toughnessAtLeast(min: Int) = copy(baseFilter = baseFilter.toughnessAtLeast(min))

    /** Power or toughness at least */
    fun powerOrToughnessAtLeast(min: Int) = copy(baseFilter = baseFilter.powerOrToughnessAtLeast(min))

    /** Must have no counters of any type ("with no counters on it" — Heartless Act). */
    fun withoutCounters() = copy(baseFilter = baseFilter.withoutCounters())

    /** Must be tapped */
    fun tapped() = copy(baseFilter = baseFilter.tapped())

    /** Must be untapped */
    fun untapped() = copy(baseFilter = baseFilter.untapped())

    /** Must be a Room with at least one locked door (CR 709.5c). */
    fun hasLockedDoor() = copy(baseFilter = baseFilter.hasLockedDoor())

    /** Must be attacking */
    fun attacking() = copy(baseFilter = baseFilter.attacking())

    /** Spell on the stack cast from [zone] (reads `SpellOnStackComponent.castFromZone`). */
    fun castFromZone(zone: Zone) = copy(baseFilter = baseFilter.castFromZone(zone))

    /**
     * Spell on the stack that was *not* cast from [zone] — Wash Away's "counter target spell that
     * wasn't cast from its owner's hand" (`Zone.HAND`).
     */
    fun notCastFromZone(zone: Zone) = copy(baseFilter = baseFilter.notCastFromZone(zone))

    /** Must have been dealt damage this turn ("...that was dealt damage this turn"). */
    fun dealtDamageThisTurn() = copy(baseFilter = baseFilter.dealtDamageThisTurn())

    /** Must be controlled by you */
    fun youControl() = copy(baseFilter = baseFilter.youControl())

    /** Must not be legendary */
    fun nonlegendary() = copy(baseFilter = baseFilter.nonlegendary())

    /** Must not be a basic land ("nonbasic land"). */
    fun nonbasic() = copy(baseFilter = baseFilter.nonbasic())

    /** Must be legendary */
    fun legendary() = copy(baseFilter = baseFilter.legendary())

    /** Must be controlled by opponent */
    fun opponentControls() = copy(baseFilter = baseFilter.opponentControls())

    /** Must be owned by you (for cards in graveyards/exile) */
    fun ownedByYou() = copy(baseFilter = baseFilter.ownedByYou())

    /** Must be owned by opponent (for cards in graveyards/exile) */
    fun ownedByOpponent() = copy(baseFilter = baseFilter.ownedByOpponent())

    /** Must have the greatest power among creatures its controller controls */
    fun hasGreatestPower() = copy(baseFilter = baseFilter.hasGreatestPower())

    /** Exclude the source permanent */
    fun other() = copy(excludeSelf = true)

    /**
     * Exclude the trigger's triggering entity (e.g., the creature that became the target
     * of an opponent's spell/ability). Use for "other than that creature" phrasing where
     * "that creature" refers to the event, not the ability source.
     */
    fun otherThanTriggeringEntity() = copy(excludeTriggeringEntity = true)

    /** Target in a different zone */
    fun inZone(zone: Zone) = copy(zone = zone)

    override fun applyTextReplacement(replacer: TextReplacer): TargetFilter {
        val newBase = baseFilter.applyTextReplacement(replacer)
        val newAlternatives = alternatives.map { it.applyTextReplacement(replacer) }
        val alternativesChanged = newAlternatives.indices.any { newAlternatives[it] !== alternatives[it] }
        return if (newBase !== baseFilter || alternativesChanged) {
            copy(baseFilter = newBase, alternatives = newAlternatives)
        } else this
    }
}
