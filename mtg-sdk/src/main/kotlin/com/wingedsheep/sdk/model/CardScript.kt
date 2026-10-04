package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.scripting.ClassLevelAbility
import com.wingedsheep.sdk.scripting.SagaChapterAbility
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
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
 * Marker carried by [CardScript.returnTransformedFromGraveyardOnResolve]: when a spell with this
 * set resolves after being cast from a graveyard, it is put onto the battlefield transformed with
 * [counters] instead of going to its owner's graveyard.
 *
 * @property counters counters the transformed permanent enters the battlefield with (e.g. a
 *   finality counter for Esper Origins). Empty means "enter with no extra counters".
 */
@Serializable
data class ReturnTransformedFromGraveyard(
    val counters: List<CounterType> = emptyList()
)

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
 *             targetRequirement = TargetObject(filter = TargetFilter.Creature)
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
 *             id = AbilityId.next(),
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
     * A spell-level **waterbend** additional cost (Avatar: The Last Airbender) —
     * *"As an additional cost to cast this spell, [you may] waterbend {N}."* Kept separate from
     * [additionalCosts] because waterbend is paid through the alternative-payment channel
     * ([com.wingedsheep.sdk.scripting.AlternativePaymentChoice.tapForGenericPermanents], tapping
     * artifacts/creatures), not the additional-cost payment continuation. See
     * [com.wingedsheep.sdk.scripting.SpellWaterbendCost].
     */
    val spellWaterbend: SpellWaterbendCost? = null,

    /**
     * For Aura spells, defines what the aura can enchant.
     * If set, this permanent is an Aura that attaches to valid targets.
     * Example: `TargetObject(filter = TargetFilter.Creature)` for "Enchant creature"
     */
    val auraTarget: TargetRequirement? = null,

    /**
     * A narrower requirement an Aura *spell's* target must meet **as it is cast** — and only then.
     * Dream Leash: "Enchant permanent / You can't choose an untapped permanent as this spell's
     * target as you cast it." The printed restriction applies to the choice only (its 2005-10-01
     * ruling), so it is not what the spell re-checks on resolution (CR 608.2b re-checks [auraTarget]),
     * not what the enchant state-based action reads, and not what an Aura put onto the battlefield
     * without being cast checks. Null for every Aura whose cast target is just its enchant
     * restriction. Read through [castAuraTarget].
     */
    val auraCastTarget: TargetRequirement? = null,

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
     * "If [condition], this spell can't be countered." Checked against the spell *on the stack*
     * whenever something tries to counter it — the condition reads the spell's own cast-time
     * values (its X, mana spent) and its caster as `Player.You` (Banefire: X is 5 or more).
     * Use [cantBeCountered] for the unconditional form.
     */
    val cantBeCounteredIf: @Serializable Condition? = null,

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
     * Alternate target requirements used when this spell declared an optional additional cost.
     * When non-empty and the cast declared *any* slot on that rail, these replace
     * [targetRequirements] (e.g., Fight with Fire: unkicked targets one creature, kicked divides
     * among any targets; Brave the Wilds: only the bargained cast chooses a target, CR 702.166d).
     *
     * Named for kicker because that's the mechanic that came first and because the field name is
     * serialized into pinned replay card definitions — renaming it would silently change how old
     * recordings resolve. It is not kicker-specific: bargain uses the same slot.
     */
    val kickerTargetRequirements: List<TargetRequirement> = emptyList(),

    /**
     * Alternate spell effect used when this spell declared an optional additional cost.
     * When non-null and the cast declared *any* slot on that rail, this replaces [spellEffect]
     * (e.g., Fight with Fire: unkicked deals 5 to one creature, kicked divides 10 among any
     * targets). Paired with [kickerTargetRequirements]; see there for why the kicker naming stays
     * even though bargain shares the slot.
     */
    val kickerSpellEffect: Effect? = null,

    /**
     * Alternate target requirements used when this spell is cast for its cleave cost
     * (CR 702.148, Innistrad: Crimson Vow). When non-empty and the spell was cleaved, these replace
     * [targetRequirements]. This is how the cleave text-change (removing all text in square
     * brackets) is modelled *structurally* rather than by parsing brackets — the card author writes
     * the brackets-removed targeting explicitly. Used when removing bracketed text broadens or drops
     * a targeting restriction (e.g. Fierce Retribution: "target [attacking] creature" → "target
     * creature"; Wash Away: "target spell [that wasn't cast from its owner's hand]" → "target spell").
     */
    val cleaveTargetRequirements: List<TargetRequirement> = emptyList(),

    /**
     * Alternate spell effect used when this spell is cast for its cleave cost (CR 702.148).
     * When non-null and the spell was cleaved, this replaces [spellEffect]. Removing bracketed text
     * can change which objects the effect touches (Path of Peril: "destroy all creatures [with mana
     * value 2 or less]" → "destroy all creatures"), drop a step (Dig Up: remove "[reveal it,]"), or
     * delete an entire clause including a delayed triggered ability that is then never created at all
     * (Alchemist's Gambit: remove "[At the beginning of that turn's end step, you lose the game.]").
     * The variant is applied at cast time so the resolving spell only ever carries the cleaved shape.
     */
    val cleaveSpellEffect: Effect? = null,

    /**
     * Spell effect used when this spell is cast for its overload cost (CR 702.96) — the printed
     * effect with every "target" read as "each", written out by the card author. An overloaded spell
     * has no target requirements at all (CR 702.96b), so this effect must not read chosen targets.
     */
    val overloadSpellEffect: Effect? = null,

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
     * Whether this spell shuffles itself into its owner's library on resolution instead of going to
     * the graveyard. Used for cards that say "Shuffle <card name> into its owner's library." — the
     * Mirrodin Besieged Zenith cycle (Green Sun's Zenith and its four siblings).
     *
     * The sibling of [selfExileOnResolve], and the same seam: both replace the destination of
     * CR 608.2n ("as the final part of an instant or sorcery spell's resolution, the spell is put
     * into its owner's graveyard"). The two are mutually exclusive — a card prints one clause or the
     * other, never both — and setting both is **rejected**, by the `init` block below and again in
     * the DSL, where the message can name the offending card.
     *
     * Three things this deliberately is **not**:
     *  - not a zone-change replacement — [com.wingedsheep.sdk.scripting.ReplacementEffect] shapes
     *    such as `RedirectZoneChange` apply to any card heading to a graveyard from anywhere, while
     *    this is one printed instruction about the spell's own resolution;
     *  - not the cast-this-way rider
     *    ([com.wingedsheep.sdk.scripting.effects.AfterResolveDestination]), which another effect
     *    stamps onto a spell *it* is casting — and which this **outranks**: those riders are
     *    written "if that spell *would be put into a graveyard*, [somewhere] instead" (Kylox's
     *    Voltstrider), and a spell that shuffles itself into its owner's library never would be, so
     *    the rider has nothing to replace. On the countered and fizzled paths, where the card really
     *    is put into a graveyard, the rider still wins;
     *  - not "put it on the bottom of its owner's library" — the card is shuffled in, so the
     *    library is randomized and a `LibraryShuffledEvent` is emitted (contrast
     *    [com.wingedsheep.sdk.scripting.effects.AfterResolveDestination.BOTTOM_OF_LIBRARY], which
     *    does not shuffle).
     *
     * It does **not** outrank flashback (CR 702.34a) or harmonize (CR 702.180a), printed or granted.
     * Those two are worded "exile this card instead of putting it anywhere else any time it would
     * leave the stack" rather than naming the graveyard, so unlike every other clause at this seam
     * they still apply to a spell that shuffles itself in: a flashbacked Blue Sun's Zenith is
     * exiled, not shuffled into its owner's library.
     *
     * Read at resolution-destination time, so — like [selfExileOnResolve] — it is correctly inert
     * when the spell is countered or fizzles: those paths never reach CR 608.2n, and the card goes
     * to its owner's graveyard as usual.
     */
    val selfShuffleIntoLibraryOnResolve: Boolean = false,

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
     * When set, a spell that resolves **after being cast from a graveyard** is exiled and then put
     * onto the battlefield transformed (its back face up) under its owner's control, entering with
     * [ReturnTransformedFromGraveyard.counters], instead of being put into its owner's graveyard.
     *
     * Models Esper Origins: "If this spell was cast from a graveyard, exile it, then put it onto the
     * battlefield transformed under its owner's control with a finality counter on it." The card must
     * be double-faced with a permanent back face (a non-DFC or a non-permanent back is a no-op per the
     * official ruling on putting a non-double-faced card onto the battlefield transformed).
     *
     * Evaluated at resolution-destination time — exactly like flashback's graveyard-cast exile
     * ([selfExileOnResolve] / [com.wingedsheep.sdk.scripting.KeywordAbility.Flashback]) — from the
     * spell's `castFromZone`, not from any effect run during resolution. This makes it immune to
     * mid-resolution pauses (e.g. a Surveil earlier in the same resolution) and correctly inert when
     * the spell is countered or fizzles. It takes precedence over the flashback exile: a card cast
     * from a graveyard that both has flashback and this flag returns transformed rather than exiling.
     */
    val returnTransformedFromGraveyardOnResolve: ReturnTransformedFromGraveyard? = null,

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
     * Wired via the `mayBeginGameOnBattlefield()` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    val mayStartOnBattlefield: Boolean = false,

    /**
     * "You may reveal this card from your opening hand. If you do, …" (CR 103.6b). When non-null
     * the card carries that opening-hand action: in the same post-mulligan walk as
     * [mayStartOnBattlefield], its owner is asked whether to reveal it, and a "yes" reveals the card
     * and runs this effect with the card as source and its owner as controller. The payoff is
     * almost always a delayed trigger (CR 603.7a — created "as a result of a static ability that
     * allows a player to take an action"), e.g. Devourer of Destiny's
     * `Effects.CreateDelayedTrigger(step = UPKEEP, fireOnPlayer = PlayerRef(You)) { … }` for
     * "at the beginning of your first upkeep" — the trigger is created before turn 1, so its next
     * matching step *is* the first one.
     *
     * Wired via the `revealFromOpeningHand(effect)` DSL helper on [com.wingedsheep.sdk.dsl.CardBuilder].
     */
    val openingHandReveal: Effect? = null,

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
    init {
        // "Exile <card name>." and "Shuffle <card name> into its owner's library." are two
        // spellings of one slot — the CR 608.2n destination — so a script that sets both has no
        // answer, only whichever clause `StackResolver` happens to test first. The DSL rejects it
        // too, with a message that names the card; this backstop covers the paths that build a
        // CardScript directly (test fixtures, the Assay compiler), where the DSL guard never runs.
        require(!(selfExileOnResolve && selfShuffleIntoLibraryOnResolve)) {
            "A CardScript sets both selfExileOnResolve and selfShuffleIntoLibraryOnResolve; a " +
                "spell has one CR 608.2n destination, so pick the clause the card actually prints"
        }
    }

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
     * The requirement an Aura spell's target is chosen against while casting: [auraCastTarget]
     * when the card narrows the choice, otherwise its [auraTarget]. Legal-action enumeration and
     * cast validation read this; the requirement captured on the stack for resolution stays
     * [auraTarget].
     */
    val castAuraTarget: TargetRequirement?
        get() = auraCastTarget ?: auraTarget

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
