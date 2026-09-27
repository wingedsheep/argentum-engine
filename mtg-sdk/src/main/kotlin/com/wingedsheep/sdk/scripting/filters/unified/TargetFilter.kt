package com.wingedsheep.sdk.scripting.filters.unified

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ObjectFilterBuilder
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
) : TextReplaceable<TargetFilter>, ObjectFilterBuilder<TargetFilter> {
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

    /**
     * The object noun phrase this filter names, in Oracle word order — "creature you control",
     * "creature card in your graveyard", "noncreature spell" — without the "other" of [excludeSelf],
     * which belongs to the quantifier ("another target …"). The targeting prompt is built from it.
     */
    fun targetPhrase(plural: Boolean = false): String = TargetPhrase.describe(this, plural)

    private fun buildDescription(): String = (if (excludeSelf) "other " else "") + targetPhrase()

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

        /** Target blocked creature — an attacker that has become blocked (CR 509.1h) */
        val BlockedCreature = TargetFilter(GameObjectFilter.Companion.Creature.blocked())

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

        /**
         * Target token you control — any token permanent, not just a creature one. "Target token
         * you control becomes a copy of it" (Kaya, Spirits' Justice) is deliberately wide enough to
         * turn a Clue or a Treasure into a creature.
         */
        val TokenYouControl = TargetFilter(GameObjectFilter.Companion.Permanent.token().youControl())

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

        /** Target artifact, enchantment, or land (Creeping Mold) */
        val ArtifactEnchantmentOrLand =
            TargetFilter(GameObjectFilter.Companion.ArtifactEnchantmentOrLand)

        /** Target land */
        val Land = TargetFilter(GameObjectFilter.Companion.Land)

        /** Target nonbasic land (Rocket Volley, Shivan Harvest, Encroaching Wastes). */
        val NonbasicLand = TargetFilter(GameObjectFilter.Companion.NonbasicLand)

        /** Target planeswalker */
        val Planeswalker = TargetFilter(GameObjectFilter.Companion.Planeswalker)

        /** Target battle (CR 310). */
        val Battle = TargetFilter(GameObjectFilter.Companion.Battle)

        /** Target creature, planeswalker, or battle (Volcanic Spite, Shatter the Source). */
        val CreaturePlaneswalkerOrBattle = TargetFilter(GameObjectFilter.Companion.CreaturePlaneswalkerOrBattle)

        // =============================================================================
        // Pre-built Graveyard Targets
        // =============================================================================

        /** Target any card in a graveyard */
        val CardInGraveyard = TargetFilter(GameObjectFilter.Companion.Any, zone = Zone.GRAVEYARD)

        /** Target creature card in a graveyard */
        val CreatureInGraveyard = TargetFilter(GameObjectFilter.Companion.Creature, zone = Zone.GRAVEYARD)

        /** Target creature card in your graveyard */
        val CreatureInYourGraveyard = TargetFilter(GameObjectFilter.Companion.Creature.ownedByYou(), zone = Zone.GRAVEYARD)

        /**
         * Target permanent card in your graveyard — for wordings that print the word "permanent".
         *
         * **Not what a bare tribal noun names.** "Return target **Zombie card** from your graveyard"
         * names any card with the subtype, and a card with a creature type need not be a permanent
         * card: Kindred (formerly Tribal) carries creature types onto instants and sorceries, so
         * Tarfire and Boggart Birth Rite are Goblin cards, Murderous Rider is a Zombie card, and the
         * corpus holds 31 non-permanent Dragon cards, 12 Elf and 10 Cleric. An earlier version of
         * this KDoc asserted the opposite and nine hand-written cards were built on it; the
         * differential caught them once Argentum Assay learned to read card position correctly.
         *
         * For a bare tribal noun use `CardInGraveyard.withSubtype(…).ownedByYou()`.
         * [CreatureInYourGraveyard] is the counterpart for the adjectival "target Zombie creature
         * card", which *does* narrow to creature cards.
         */
        val PermanentInYourGraveyard = TargetFilter(GameObjectFilter.Companion.Permanent.ownedByYou(), zone = Zone.GRAVEYARD)

        /**
         * Target artifact card in your graveyard — the "return target artifact card from your
         * graveyard" family (Ritual of Restoration, Myr Retriever, Refurbish, Fortuitous Find).
         *
         * `GameObjectFilter.Artifact` is a lone `IsArtifact` predicate, so this is **inclusive**, not
         * exclusive: an artifact creature card in your graveyard satisfies this filter *and*
         * [CreatureInYourGraveyard]. A card printing both as separate modes still can't recur one
         * such card twice — the two targets are chosen at the same time and must be different
         * objects — but either mode alone will happily take it.
         *
         * Ownership, not control, is the axis: a card in a graveyard represents neither a permanent
         * nor a spell and so has no controller, only the owner whose graveyard it sits in. Use
         * `TargetFilter.Artifact` (battlefield, `youControl()`) when the wording means a permanent.
         */
        val ArtifactInYourGraveyard = TargetFilter(GameObjectFilter.Companion.Artifact.ownedByYou(), zone = Zone.GRAVEYARD)

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

        /**
         * Target an instant spell, sorcery spell, or triggered ability on the stack — the Spider-Sense
         * counter template. Narrower than [InstantSorcerySpellOrAbilityOnStack]: activated abilities
         * are **not** included (only triggered ones), matching "counter target instant spell, sorcery
         * spell, or triggered ability".
         */
        val InstantSorcerySpellOrTriggeredAbilityOnStack = TargetFilter(
            GameObjectFilter(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.IsInstant,
                            CardPredicate.IsSorcery,
                            CardPredicate.IsTriggeredAbility
                        )
                    )
                )
            ),
            zone = Zone.STACK
        )
    }

    // =============================================================================
    // Builders — the predicate builders come from [ObjectFilterBuilder]; these are TargetFilter's own
    // =============================================================================

    override fun mapObjectFilter(transform: (GameObjectFilter) -> GameObjectFilter) =
        copy(baseFilter = transform(baseFilter))

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
