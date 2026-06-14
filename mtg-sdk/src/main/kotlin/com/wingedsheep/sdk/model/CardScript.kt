package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.ClassLevelAbility
import com.wingedsheep.sdk.scripting.SagaChapterAbility
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable
import com.wingedsheep.sdk.dsl.leyline

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
 *         ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
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
     * State-triggered abilities (CR 603.8) that fire when a game-state condition
     * becomes true (rather than in response to an event). The engine polls these at
     * priority pass points and latches per (entityId, abilityId) to prevent re-firing
     * while the condition stays true. Examples: "When you control no Islands,
     * sacrifice this creature."
     */
    val stateTriggeredAbilities: List<StateTriggeredAbility> = emptyList(),

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
    val castTimeCreatureTypeChoice: CastTimeCreatureTypeSource? = null,

    /**
     * Whether this spell can't be countered by spells or abilities.
     * When true, attempts to counter this spell simply fail.
     */
    val cantBeCountered: Boolean = false,

    /**
     * Whether this spell can't be copied (CR 707.10). When true, any effect that would
     * copy this spell on the stack creates no copy.
     */
    val cantBeCopied: Boolean = false,

    /**
     * A condition under which this spell can be cast as though it had flash.
     * Used for Ferocious-style "if you control a creature with power 4 or greater,
     * you may cast this spell as though it had flash" abilities.
     */
    val conditionalFlash: @Serializable Condition? = null,

    /**
     * Alternate target requirements used when this spell is kicked.
     * When non-empty and the spell is kicked, these replace [targetRequirements].
     * Used for kicker spells where kicked mode has completely different targeting
     * (e.g., Fight with Fire: unkicked targets one creature, kicked divides among any targets).
     */
    val kickerTargetRequirements: List<TargetRequirement> = emptyList(),

    /**
     * Alternate spell effect used when this spell is kicked.
     * When non-null and the spell is kicked, this replaces [spellEffect].
     * Used for kicker spells where kicked mode has a completely different effect type
     * (e.g., Fight with Fire: unkicked deals 5 to one creature, kicked divides 10 among any targets).
     */
    val kickerSpellEffect: Effect? = null,

    /**
     * Class level abilities (for Class enchantments).
     * Level 1 abilities use the base CardScript fields (triggeredAbilities, staticAbilities, etc.).
     * Levels 2+ are stored here with their level-up costs.
     * Players pay the cost as a sorcery-speed activated ability to advance to the next level.
     * Abilities are cumulative — gaining a higher level doesn't remove lower-level abilities.
     */
    val classLevels: List<ClassLevelAbility> = emptyList(),

    /**
     * Saga chapter abilities.
     * Each chapter triggers when lore counters reach or exceed the chapter number.
     * Sagas add a lore counter on ETB and at the beginning of each precombat main phase.
     */
    val sagaChapters: List<SagaChapterAbility> = emptyList(),

    /**
     * Whether this spell exiles itself on resolution instead of going to the graveyard.
     * Used for cards like Karn's Temporal Sundering that say "Exile <card name>."
     */
    val selfExileOnResolve: Boolean = false,

    /**
     * Paradigm (Secrets of Strixhaven). When true, this spell exiles itself on resolution
     * (implies [selfExileOnResolve]) and is tagged with the paradigm marker as it lands in
     * exile, so the engine synthesizes the recurring free-recast triggered ability
     * ([com.wingedsheep.sdk.scripting.Paradigm.recastAbility]): "At the beginning of each of
     * your first main phases, you may cast a copy of this card from exile without paying its
     * mana cost." The original stays in exile; each recast is a phantom copy (CR 707.10a).
     */
    val paradigm: Boolean = false,

    /**
     * An alternative cost that the caster may pay instead of the spell's mana cost.
     * Used for cards like Zahid, Djinn of the Lamp: "You may pay {3}{U} and tap an
     * untapped artifact you control rather than pay this spell's mana cost."
     *
     * When present, the legal actions calculator offers a "CastWithAlternativeCost"
     * option alongside the normal cast option, provided the player can afford
     * the alternative mana cost and pay any required additional costs.
     */
    val selfAlternativeCost: SelfAlternativeCost? = null,

    /**
     * Colors that may be spent on the `{X}` portion of this spell's mana cost.
     * Empty means no restriction (X can be paid with any mana, the default).
     *
     * Used for cards like Soul Burn ("Spend only black and/or red mana on X"). The
     * restriction applies only to the variable `{X}` symbols — the fixed colored/generic
     * portion of the cost is unaffected. Honored by the mana solver and cast payment path,
     * and the per-color amount actually spent on X is exposed via
     * [com.wingedsheep.sdk.scripting.values.DynamicAmount.ManaSpentOnX].
     */
    val xManaRestriction: Set<Color> = emptySet(),

    /**
     * Leyline mechanic. "If this card is in your opening hand, you may begin the game
     * with it on the battlefield." After all mulligans and bottoming resolve, the engine
     * walks each player in turn order (starting with the active player) and presents a
     * yes/no choice per Leyline card still in that player's opening hand. A "yes" puts the
     * card onto the battlefield under its owner's control through the standard zone-change
     * pipeline before the first turn begins; a "no" leaves it in hand.
     *
     * Wired via the `leyline()` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    val mayStartOnBattlefield: Boolean = false,

    /**
     * "As you cast this spell" condition captures (CR 601.2i). Each is a named condition the engine
     * evaluates the moment this spell finishes being cast; the names whose condition was true are
     * frozen onto the spell on the stack and read back at resolution via
     * [com.wingedsheep.sdk.scripting.conditions.CastTimeFlagSet]. Lets a spell branch on the
     * cast-time board state rather than the (possibly changed) resolution-time board — e.g. Steer
     * Clear "deals 4 damage instead if you controlled a Mount as you cast this spell".
     *
     * Declared with the `captureAtCast(flag, condition)` DSL on a spell. Empty for the vast
     * majority of spells.
     */
    val castTimeCaptures: List<CastTimeCapture> = emptyList()
) {
    /**
     * Whether this card has any scripted behavior.
     * Vanilla creatures and basic lands return false.
     */
    val hasBehavior: Boolean
        get() = spellEffect != null ||
                targetRequirements.isNotEmpty() ||
                triggeredAbilities.isNotEmpty() ||
                stateTriggeredAbilities.isNotEmpty() ||
                activatedAbilities.isNotEmpty() ||
                staticAbilities.isNotEmpty() ||
                replacementEffects.isNotEmpty() ||
                additionalCosts.isNotEmpty() ||
                auraTarget != null ||
                castRestrictions.isNotEmpty() ||
                classLevels.isNotEmpty() ||
                sagaChapters.isNotEmpty()

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
        get() = triggeredAbilities + stateTriggeredAbilities + activatedAbilities + staticAbilities + replacementEffects

    /**
     * Whether this card has replacement effects.
     */
    val hasReplacementEffects: Boolean
        get() = replacementEffects.isNotEmpty()

    /**
     * The maximum class level for Class enchantments, or null if not a Class.
     */
    val maxClassLevel: Int?
        get() = classLevels.maxOfOrNull { it.level }

    /**
     * Get all triggered abilities active at the given class level.
     * Includes base triggered abilities (always active) plus class-level-gated ones.
     * When [currentClassLevel] is null, returns only base abilities (for non-Class cards).
     */
    fun effectiveTriggeredAbilities(currentClassLevel: Int? = null): List<TriggeredAbility> {
        if (currentClassLevel == null || classLevels.isEmpty()) return triggeredAbilities
        val classAbilities = classLevels
            .filter { it.level <= currentClassLevel }
            .flatMap { it.triggeredAbilities }
        return triggeredAbilities + classAbilities
    }

    /**
     * Get all static abilities active at the given class level.
     * Includes base static abilities (always active) plus class-level-gated ones.
     * When [currentClassLevel] is null, returns only base abilities (for non-Class cards).
     */
    fun effectiveStaticAbilities(currentClassLevel: Int? = null): List<StaticAbility> {
        if (currentClassLevel == null || classLevels.isEmpty()) return staticAbilities
        val classAbilities = classLevels
            .filter { it.level <= currentClassLevel }
            .flatMap { it.staticAbilities }
        return staticAbilities + classAbilities
    }

    /**
     * Get all activated abilities active at the given class level.
     * Includes base activated abilities (always active) plus class-level-gated ones.
     * When [currentClassLevel] is null, returns only base abilities (for non-Class cards).
     */
    fun effectiveActivatedAbilities(currentClassLevel: Int? = null): List<ActivatedAbility> {
        if (currentClassLevel == null || classLevels.isEmpty()) return activatedAbilities
        val classAbilities = classLevels
            .filter { it.level <= currentClassLevel }
            .flatMap { it.activatedAbilities }
        return activatedAbilities + classAbilities
    }

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
