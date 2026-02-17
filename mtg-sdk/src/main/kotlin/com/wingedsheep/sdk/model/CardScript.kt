package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetRequirement
import kotlinx.serialization.Serializable

// Type alias for clarity - replacement effects are in the scripting package

/**
 * Source zone for cast-time creature type choice.
 * Determines where to scan for available creature types during casting.
 */
@Serializable
enum class CastTimeCreatureTypeSource {
    /** Scan the caster's graveyard for creature subtypes */
    GRAVEYARD
}

/**
 * CardScript contains the behavioral logic of a card - what happens when it's cast,
 * what abilities it has, and what targets it requires.
 *
 * This is the "script" that defines card behavior while CardDefinition holds the
 * static attributes (name, cost, types, stats). Together they form a complete card.
 *
 * ## Philosophy
 * CardScript is pure data - it describes WHAT should happen, not HOW.
 * The engine (mtg-engine) interprets this data and executes the logic.
 * This separation keeps the SDK content-neutral and the engine card-agnostic.
 *
 * ## Usage Examples
 *
 * ### Simple Instant (Lightning Bolt)
 * ```kotlin
 * CardScript(
 *     spellEffect = DealDamageEffect(3, EffectTarget.ContextTarget(0)),
 *     targetRequirements = listOf(AnyTarget())
 * )
 * ```
 *
 * ### Creature with ETB (Flametongue Kavu)
 * ```kotlin
 * CardScript(
 *     triggeredAbilities = listOf(
 *         TriggeredAbility.create(
 *             trigger = OnEnterBattlefield(),
 *             effect = DealDamageEffect(4, EffectTarget.ContextTarget(0)),
 *             targetRequirement = TargetCreature()
 *         )
 *     )
 * )
 * ```
 *
 * ### Mana Rock (Sol Ring)
 * ```kotlin
 * CardScript(
 *     activatedAbilities = listOf(
 *         ActivatedAbility(
 *             id = AbilityId.generate(),
 *             cost = AbilityCost.Tap,
 *             effect = AddColorlessManaEffect(2),
 *             isManaAbility = true
 *         )
 *     )
 * )
 * ```
 *
 * ### Static Enchantment (Glorious Anthem)
 * ```kotlin
 * CardScript(
 *     staticAbilities = listOf(
 *         ModifyStats(1, 1, StaticTarget.AllControlledCreatures)
 *     )
 * )
 * ```
 */
@Serializable
data class CardScript(
    /**
     * The effect that happens when this spell resolves.
     * Used for instants and sorceries.
     * For permanents, this is typically null (they just enter the battlefield).
     */
    val spellEffect: Effect? = null,

    /**
     * Target requirements that must be declared when casting this spell.
     * The engine validates targets and prompts the player for selection.
     *
     * Effects reference these via EffectTarget.ContextTarget(index) where
     * index corresponds to the position in this list.
     */
    val targetRequirements: List<TargetRequirement> = emptyList(),

    /**
     * Triggered abilities that fire when specific game events occur.
     * Examples: ETB triggers, death triggers, combat triggers.
     */
    val triggeredAbilities: List<TriggeredAbility> = emptyList(),

    /**
     * Activated abilities that can be activated by paying costs.
     * Examples: Tap abilities, loyalty abilities, equip.
     */
    val activatedAbilities: List<ActivatedAbility> = emptyList(),

    /**
     * Static abilities that provide continuous effects.
     * Applied via the layer system (Rule 613).
     * Examples: +1/+1 buffs, keyword grants, restrictions.
     */
    val staticAbilities: List<StaticAbility> = emptyList(),

    /**
     * Replacement effects that modify game events before they happen.
     * Unlike triggered abilities, replacement effects don't use the stack.
     * Examples: Doubling Season (doubles tokens/counters), Rest in Peace (exile instead of graveyard).
     */
    val replacementEffects: List<ReplacementEffect> = emptyList(),

    /**
     * Additional costs that must be paid when casting this spell.
     * Separate from mana costs.
     * Examples: Sacrifice a creature, discard a card, pay life.
     */
    val additionalCosts: List<AdditionalCost> = emptyList(),

    /**
     * For Aura spells, defines what the aura can enchant.
     * If set, this permanent is an Aura that attaches to valid targets.
     * Example: TargetCreature() for "Enchant creature"
     */
    val auraTarget: TargetRequirement? = null,

    /**
     * Timing and conditional restrictions on when this spell can be cast.
     * Used for cards like "Cast only during the declare attackers step."
     * The engine enforces these during legal action calculation.
     */
    val castRestrictions: List<CastRestriction> = emptyList(),

    /**
     * If set, the caster must choose a creature type during casting (not resolution).
     * The chosen type is stored on the stack and available via EffectContext.chosenCreatureType
     * at resolution time.
     *
     * The source determines where to look for available creature types:
     * - GRAVEYARD: Scan the caster's graveyard for creature subtypes
     */
    val castTimeCreatureTypeChoice: CastTimeCreatureTypeSource? = null
) {
    /**
     * Whether this card has any scripted behavior.
     * Vanilla creatures and basic lands return false.
     */
    val hasBehavior: Boolean
        get() = spellEffect != null ||
                targetRequirements.isNotEmpty() ||
                triggeredAbilities.isNotEmpty() ||
                activatedAbilities.isNotEmpty() ||
                staticAbilities.isNotEmpty() ||
                replacementEffects.isNotEmpty() ||
                additionalCosts.isNotEmpty() ||
                auraTarget != null ||
                castRestrictions.isNotEmpty()

    /**
     * Whether this spell has timing/conditional restrictions on casting.
     */
    val hasCastRestrictions: Boolean
        get() = castRestrictions.isNotEmpty()

    /**
     * Whether this is an Aura that requires an enchant target.
     */
    val isAura: Boolean
        get() = auraTarget != null

    /**
     * Whether this spell requires targets when cast.
     */
    val requiresTargets: Boolean
        get() = targetRequirements.isNotEmpty() || auraTarget != null

    /**
     * Whether this spell has additional costs beyond mana.
     */
    val hasAdditionalCosts: Boolean
        get() = additionalCosts.isNotEmpty()

    /**
     * All abilities (triggered + activated + static + replacement) for iteration.
     */
    val allAbilities: List<Any>
        get() = triggeredAbilities + activatedAbilities + staticAbilities + replacementEffects

    /**
     * Whether this card has replacement effects.
     */
    val hasReplacementEffects: Boolean
        get() = replacementEffects.isNotEmpty()

    companion object {
        /**
         * Empty script for vanilla cards with no special behavior.
         */
        val EMPTY = CardScript()

        /**
         * Create a simple spell script (for instants/sorceries).
         */
        fun spell(
            effect: Effect,
            vararg targets: TargetRequirement,
            additionalCosts: List<AdditionalCost> = emptyList()
        ): CardScript = CardScript(
            spellEffect = effect,
            targetRequirements = targets.toList(),
            additionalCosts = additionalCosts
        )

        /**
         * Create a creature script with triggered abilities.
         */
        fun creature(
            vararg triggeredAbilities: TriggeredAbility,
            staticAbilities: List<StaticAbility> = emptyList(),
            activatedAbilities: List<ActivatedAbility> = emptyList()
        ): CardScript = CardScript(
            triggeredAbilities = triggeredAbilities.toList(),
            staticAbilities = staticAbilities,
            activatedAbilities = activatedAbilities
        )

        /**
         * Create an aura script.
         */
        fun aura(
            enchantTarget: TargetRequirement,
            staticAbilities: List<StaticAbility>,
            triggeredAbilities: List<TriggeredAbility> = emptyList()
        ): CardScript = CardScript(
            auraTarget = enchantTarget,
            staticAbilities = staticAbilities,
            triggeredAbilities = triggeredAbilities
        )

        /**
         * Create a permanent script with activated abilities (artifacts, lands, etc.).
         */
        fun permanent(
            vararg activatedAbilities: ActivatedAbility,
            staticAbilities: List<StaticAbility> = emptyList(),
            triggeredAbilities: List<TriggeredAbility> = emptyList(),
            replacementEffects: List<ReplacementEffect> = emptyList()
        ): CardScript = CardScript(
            activatedAbilities = activatedAbilities.toList(),
            staticAbilities = staticAbilities,
            triggeredAbilities = triggeredAbilities,
            replacementEffects = replacementEffects
        )

        /**
         * Create a script with replacement effects (like Doubling Season, Rest in Peace).
         */
        fun withReplacementEffects(
            vararg replacementEffects: ReplacementEffect,
            staticAbilities: List<StaticAbility> = emptyList(),
            triggeredAbilities: List<TriggeredAbility> = emptyList(),
            activatedAbilities: List<ActivatedAbility> = emptyList()
        ): CardScript = CardScript(
            replacementEffects = replacementEffects.toList(),
            staticAbilities = staticAbilities,
            triggeredAbilities = triggeredAbilities,
            activatedAbilities = activatedAbilities
        )
    }
}
