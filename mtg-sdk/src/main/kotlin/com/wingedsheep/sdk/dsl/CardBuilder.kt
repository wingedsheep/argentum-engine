package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.targeting.TargetRequirement

/**
 * DSL entry point for defining cards.
 *
 * Usage:
 * ```kotlin
 * val bolt = card("Lightning Bolt") {
 *     manaCost = "{R}"
 *     typeLine = "Instant"
 *     spell {
 *         effect = Effects.DealDamage(3, EffectTarget.ContextTarget(0))
 *         target = Targets.Any
 *     }
 * }
 * ```
 */
fun card(name: String, init: CardBuilder.() -> Unit): CardDefinition {
    val builder = CardBuilder(name)
    builder.init()
    return builder.build()
}

/**
 * DSL entry point for defining basic land cards with art variants.
 *
 * Basic lands have an intrinsic mana ability that taps for one mana of their color.
 * This helper creates a basic land with the specified metadata for art variants.
 *
 * Usage:
 * ```kotlin
 * val plains196 = basicLand("Plains") {
 *     collectorNumber = "196"
 *     artist = "Douglas Shuler"
 *     imageUri = "https://cards.scryfall.io/..."
 * }
 * ```
 *
 * @param landType The basic land type: "Plains", "Island", "Swamp", "Mountain", or "Forest"
 * @param init Metadata configuration for this art variant
 */
fun basicLand(landType: String, init: BasicLandBuilder.() -> Unit): CardDefinition {
    val builder = BasicLandBuilder(landType)
    builder.init()
    return builder.build()
}

/**
 * Builder class for constructing basic land CardDefinition instances.
 */
@CardDsl
class BasicLandBuilder(private val landType: String) {
    var collectorNumber: String? = null
    var artist: String? = null
    var flavorText: String? = null
    var imageUri: String? = null
    var rarity: Rarity = Rarity.COMMON

    private val manaColor: Color = when (landType) {
        "Plains" -> Color.WHITE
        "Island" -> Color.BLUE
        "Swamp" -> Color.BLACK
        "Mountain" -> Color.RED
        "Forest" -> Color.GREEN
        else -> throw IllegalArgumentException("Unknown basic land type: $landType")
    }

    fun build(): CardDefinition {
        val typeLine = TypeLine.parse("Basic Land — $landType")

        // Basic lands have an intrinsic mana ability: "{T}: Add {color}."
        // Mana abilities don't use the stack and resolve immediately.
        val manaAbility = ActivatedAbility(
            id = AbilityId.generate(),
            cost = AbilityCost.Tap,
            effect = AddManaEffect(manaColor),
            isManaAbility = true,
            timing = TimingRule.ManaAbility
        )

        val script = CardScript(
            activatedAbilities = listOf(manaAbility)
        )

        val metadata = ScryfallMetadata(
            collectorNumber = collectorNumber,
            rarity = rarity,
            artist = artist,
            flavorText = flavorText,
            imageUri = imageUri
        )

        return CardDefinition(
            name = landType,
            manaCost = ManaCost.ZERO,
            typeLine = typeLine,
            oracleText = "({T}: Add {${manaColor.symbol}}.)",
            script = script,
            metadata = metadata
        )
    }
}

/**
 * Builder class for constructing CardDefinition instances using DSL syntax.
 */
@DslMarker
annotation class CardDsl

@CardDsl
class CardBuilder(private val name: String) {

    // =========================================================================
    // Basic Properties
    // =========================================================================

    /**
     * Mana cost as a string (e.g., "{2}{U}{U}").
     */
    var manaCost: String = ""

    /**
     * Type line as a string (e.g., "Creature — Human Wizard").
     */
    var typeLine: String = ""

    /**
     * Power (for creatures). Can be set as Int for fixed stats.
     */
    var power: Int? = null

    /**
     * Toughness (for creatures). Can be set as Int for fixed stats.
     */
    var toughness: Int? = null

    /**
     * Dynamic power (for creatures with characteristic-defining abilities).
     * Takes precedence over `power` if set.
     */
    var dynamicPower: CharacteristicValue? = null

    /**
     * Dynamic toughness (for creatures with characteristic-defining abilities).
     * Takes precedence over `toughness` if set.
     */
    var dynamicToughness: CharacteristicValue? = null

    /**
     * Set dynamic stats from the same source with optional offsets.
     * Example: `dynamicStats(DynamicAmount.CardTypesInAllGraveyards, toughnessOffset = 1)` for Tarmogoyf.
     */
    fun dynamicStats(source: DynamicAmount, powerOffset: Int = 0, toughnessOffset: Int = 0) {
        dynamicPower = CharacteristicValue.dynamic(source, powerOffset)
        dynamicToughness = CharacteristicValue.dynamic(source, toughnessOffset)
    }

    /**
     * Starting loyalty (for planeswalkers).
     */
    var startingLoyalty: Int? = null

    /**
     * Oracle text (rules text).
     */
    var oracleText: String = ""

    /**
     * Aura target requirement (for Auras).
     */
    var auraTarget: TargetRequirement? = null

    /**
     * Morph cost as a string (e.g., "{2}{U}").
     * When set, the card gains the Morph keyword ability.
     */
    var morph: String? = null

    /**
     * If set, the caster must choose a creature type during casting.
     * The source determines where to look for available creature types.
     */
    var castTimeCreatureTypeChoice: CastTimeCreatureTypeSource? = null

    // =========================================================================
    // Internal State
    // =========================================================================

    private var keywordSet: MutableSet<Keyword> = mutableSetOf()
    private var keywordAbilityList: MutableList<KeywordAbility> = mutableListOf()
    private var spellBuilder: SpellBuilder? = null
    private val triggeredAbilities: MutableList<TriggeredAbility> = mutableListOf()
    private val activatedAbilities: MutableList<ActivatedAbility> = mutableListOf()
    private val staticAbilities: MutableList<StaticAbility> = mutableListOf()
    private val additionalCosts: MutableList<AdditionalCost> = mutableListOf()
    private val replacementEffects: MutableList<ReplacementEffect> = mutableListOf()
    private var equipCost: ManaCost? = null
    private var metadataBuilder: MetadataBuilder? = null

    // =========================================================================
    // Keywords
    // =========================================================================

    /**
     * Add keywords to this card.
     */
    fun keywords(vararg keywords: Keyword) {
        keywordSet.addAll(keywords)
    }

    /**
     * Add a parameterized keyword ability.
     * Examples: ward {2}, protection from blue, annihilator 2
     */
    fun keywordAbility(ability: KeywordAbility) {
        keywordAbilityList.add(ability)
    }

    /**
     * Add multiple parameterized keyword abilities.
     */
    fun keywordAbilities(vararg abilities: KeywordAbility) {
        keywordAbilityList.addAll(abilities)
    }

    // =========================================================================
    // Spell Effect (for Instants/Sorceries)
    // =========================================================================

    /**
     * Define the spell effect for instants and sorceries.
     */
    fun spell(init: SpellBuilder.() -> Unit) {
        val builder = SpellBuilder()
        builder.init()
        spellBuilder = builder
    }

    // =========================================================================
    // Triggered Abilities
    // =========================================================================

    /**
     * Add a triggered ability.
     */
    fun triggeredAbility(init: TriggeredAbilityBuilder.() -> Unit) {
        val builder = TriggeredAbilityBuilder()
        builder.init()
        triggeredAbilities.add(builder.build())
    }

    // =========================================================================
    // Activated Abilities
    // =========================================================================

    /**
     * Add an activated ability.
     */
    fun activatedAbility(init: ActivatedAbilityBuilder.() -> Unit) {
        val builder = ActivatedAbilityBuilder()
        builder.init()
        activatedAbilities.add(builder.build())
    }

    // =========================================================================
    // Static Abilities
    // =========================================================================

    /**
     * Add a static ability.
     */
    fun staticAbility(init: StaticAbilityBuilder.() -> Unit) {
        val builder = StaticAbilityBuilder()
        builder.init()
        staticAbilities.add(builder.build())
    }

    // =========================================================================
    // Planeswalker Abilities
    // =========================================================================

    /**
     * Add a loyalty ability (for planeswalkers).
     */
    fun loyaltyAbility(loyaltyChange: Int, init: LoyaltyAbilityBuilder.() -> Unit) {
        val builder = LoyaltyAbilityBuilder(loyaltyChange)
        builder.init()
        activatedAbilities.add(builder.build())
    }

    // =========================================================================
    // Equipment
    // =========================================================================

    /**
     * Add an equip ability with the specified cost.
     */
    fun equipAbility(cost: String) {
        equipCost = ManaCost.parse(cost)
    }

    // =========================================================================
    // Additional Costs
    // =========================================================================

    /**
     * Add an additional cost to cast this spell.
     * Used for cards like Final Strike: "As an additional cost to cast this spell, sacrifice a creature."
     */
    fun additionalCost(cost: AdditionalCost) {
        additionalCosts.add(cost)
    }

    // =========================================================================
    // Replacement Effects
    // =========================================================================

    /**
     * Add a replacement effect to this card.
     * Used for cards like cycling lands that enter the battlefield tapped.
     */
    fun replacementEffect(effect: ReplacementEffect) {
        replacementEffects.add(effect)
    }

    // =========================================================================
    // Metadata
    // =========================================================================

    /**
     * Define metadata for this card.
     */
    fun metadata(init: MetadataBuilder.() -> Unit) {
        val builder = MetadataBuilder()
        builder.init()
        metadataBuilder = builder
    }

    // =========================================================================
    // Build
    // =========================================================================

    fun build(): CardDefinition {
        val parsedTypeLine = TypeLine.parse(typeLine)
        val parsedManaCost = if (manaCost.isNotEmpty()) ManaCost.parse(manaCost) else ManaCost.ZERO

        // Build creature stats if power/toughness provided
        // Dynamic values take precedence over fixed Int values
        val creatureStats = when {
            dynamicPower != null || dynamicToughness != null -> {
                val finalPower = dynamicPower ?: power?.let { CharacteristicValue.Fixed(it) }
                    ?: CharacteristicValue.Fixed(0)
                val finalToughness = dynamicToughness ?: toughness?.let { CharacteristicValue.Fixed(it) }
                    ?: CharacteristicValue.Fixed(0)
                CreatureStats(finalPower, finalToughness)
            }
            power != null && toughness != null -> CreatureStats(power!!, toughness!!)
            else -> null
        }

        // Build the script — wrap spell effect in ConditionalEffect if condition is set
        val rawSpellEffect = spellBuilder?.effect
        val spellEffect = if (spellBuilder?.condition != null && rawSpellEffect != null) {
            ConditionalEffect(spellBuilder!!.condition!!, rawSpellEffect)
        } else {
            rawSpellEffect
        }
        val script = CardScript(
            spellEffect = spellEffect,
            targetRequirements = spellBuilder?.targetRequirements ?: emptyList(),
            triggeredAbilities = triggeredAbilities.toList(),
            activatedAbilities = activatedAbilities.toList(),
            staticAbilities = staticAbilities.toList(),
            replacementEffects = replacementEffects.toList(),
            additionalCosts = additionalCosts.toList(),
            auraTarget = auraTarget,
            castRestrictions = spellBuilder?.restrictions ?: emptyList(),
            castTimeCreatureTypeChoice = castTimeCreatureTypeChoice
        )

        // Build metadata
        val metadata = metadataBuilder?.build() ?: ScryfallMetadata()

        // Add morph keyword ability if morph cost is specified
        val finalKeywordAbilities = if (morph != null) {
            keywordAbilityList + KeywordAbility.Morph(ManaCost.parse(morph!!))
        } else {
            keywordAbilityList.toList()
        }

        return CardDefinition(
            name = name,
            manaCost = parsedManaCost,
            typeLine = parsedTypeLine,
            oracleText = oracleText,
            creatureStats = creatureStats,
            keywords = keywordSet.toSet(),
            keywordAbilities = finalKeywordAbilities,
            script = script,
            equipCost = equipCost,
            startingLoyalty = startingLoyalty,
            metadata = metadata
        )
    }
}

// =============================================================================
// Spell Builder
// =============================================================================

/**
 * Builder for spell effects (instants and sorceries).
 *
 * Supports two targeting styles:
 *
 * 1. Simple (single target):
 * ```kotlin
 * spell {
 *     target = Targets.Creature
 *     effect = Effects.Destroy(EffectTarget.ContextTarget(0))
 * }
 * ```
 *
 * 2. Named binding (multiple targets):
 * ```kotlin
 * spell {
 *     val creature = target("creature", TargetCreature())
 *     val player = target("player", TargetPlayer())
 *     effect = Effects.Composite(
 *         Effects.Destroy(creature),
 *         Effects.DealDamage(3, player)
 *     )
 * }
 * ```
 */
@CardDsl
class SpellBuilder {
    var effect: Effect? = null
    var target: TargetRequirement? = null
    var condition: Condition? = null

    // Named target bindings
    private val namedTargets: MutableList<Pair<String, TargetRequirement>> = mutableListOf()

    // Cast restrictions
    private val castRestrictions: MutableList<CastRestriction> = mutableListOf()

    /**
     * Add a timing restriction: "Cast only during the [step]."
     */
    fun castOnlyDuring(step: Step) {
        castRestrictions.add(CastRestriction.OnlyDuringStep(step))
    }

    /**
     * Add a timing restriction: "Cast only during the [phase]."
     */
    fun castOnlyDuring(phase: Phase) {
        castRestrictions.add(CastRestriction.OnlyDuringPhase(phase))
    }

    /**
     * Add a conditional restriction: "Cast only if [condition]."
     */
    fun castOnlyIf(condition: Condition) {
        castRestrictions.add(CastRestriction.OnlyIfCondition(condition))
    }

    internal val restrictions: List<CastRestriction>
        get() = castRestrictions.toList()

    /**
     * Declare a named target and get an EffectTarget reference to use in effects.
     *
     * @param name A descriptive name for the target (for debugging/documentation)
     * @param requirement The target requirement specification
     * @return An EffectTarget.ContextTarget that references this target by index
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.ContextTarget {
        val index = namedTargets.size
        namedTargets.add(name to requirement)
        return EffectTarget.ContextTarget(index)
    }

    internal val targetRequirements: List<TargetRequirement>
        get() = if (namedTargets.isNotEmpty()) {
            namedTargets.map { it.second }
        } else {
            listOfNotNull(target)
        }

    /**
     * Define a modal spell effect.
     *
     * Example (Cryptic Command):
     * ```kotlin
     * spell {
     *     modal(chooseCount = 2) {
     *         mode("Counter target spell") {
     *             target = TargetSpell()
     *             effect = Effects.CounterSpell()
     *         }
     *         mode("Return target permanent to its owner's hand") {
     *             target = TargetPermanent()
     *             effect = Effects.ReturnToHand(EffectTarget.ContextTarget(0))
     *         }
     *         mode("Tap all creatures your opponents control") {
     *             effect = Effects.TapAll(CreatureGroupFilter.OpponentsControl)
     *         }
     *         mode("Draw a card") {
     *             effect = Effects.DrawCards(1)
     *         }
     *     }
     * }
     * ```
     */
    fun modal(chooseCount: Int = 1, init: ModalBuilder.() -> Unit) {
        val builder = ModalBuilder(chooseCount)
        builder.init()
        effect = builder.build()
    }
}

// =============================================================================
// Modal Builder
// =============================================================================

/**
 * Builder for modal spells with per-mode targeting.
 */
@CardDsl
class ModalBuilder(private val chooseCount: Int) {
    private val modes: MutableList<Mode> = mutableListOf()

    /**
     * Add a mode with the given description.
     */
    fun mode(description: String, init: ModeBuilder.() -> Unit) {
        val builder = ModeBuilder(description)
        builder.init()
        modes.add(builder.build())
    }

    /**
     * Add a mode with no target and a simple effect.
     */
    fun mode(description: String, effect: Effect) {
        modes.add(Mode.noTarget(effect, description))
    }

    internal fun build(): ModalEffect = ModalEffect(modes.toList(), chooseCount)
}

/**
 * Builder for a single mode within a modal spell.
 */
@CardDsl
class ModeBuilder(private val description: String) {
    var effect: Effect? = null
    var target: TargetRequirement? = null
    private val targets: MutableList<TargetRequirement> = mutableListOf()

    /**
     * Add a named target for this mode and get an EffectTarget reference.
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.ContextTarget {
        val index = targets.size
        targets.add(requirement)
        return EffectTarget.ContextTarget(index)
    }

    internal fun build(): Mode {
        requireNotNull(effect) { "Mode '$description' must have an effect" }
        val allTargets = if (targets.isNotEmpty()) {
            targets.toList()
        } else {
            listOfNotNull(target)
        }
        return Mode(effect!!, allTargets, description)
    }
}

// =============================================================================
// Triggered Ability Builder
// =============================================================================

@CardDsl
class TriggeredAbilityBuilder {
    var trigger: Trigger = OnEnterBattlefield()
    var effect: Effect? = null
    var target: TargetRequirement? = null
    var optional: Boolean = false
    var elseEffect: Effect? = null
    var triggerZone: Zone = Zone.BATTLEFIELD
    /** Intervening-if condition (Rule 603.4): checked when trigger would fire AND at resolution. */
    var triggerCondition: Condition? = null
    /** When true, the triggered ability is controlled by the triggering entity's controller. */
    var controlledByTriggeringEntityController: Boolean = false

    fun build(): TriggeredAbility {
        requireNotNull(effect) { "Triggered ability must have an effect" }
        return TriggeredAbility.create(
            trigger = trigger,
            effect = effect!!,
            optional = optional,
            targetRequirement = target,
            elseEffect = elseEffect,
            activeZone = triggerZone,
            triggerCondition = triggerCondition,
            controlledByTriggeringEntityController = controlledByTriggeringEntityController
        )
    }
}

// =============================================================================
// Activated Ability Builder
// =============================================================================

@CardDsl
class ActivatedAbilityBuilder {
    var cost: AbilityCost = AbilityCost.Tap
    var effect: Effect? = null
    var target: TargetRequirement? = null
    var manaAbility: Boolean = false
    var timing: TimingRule = TimingRule.InstantSpeed
    var restrictions: List<ActivationRestriction> = emptyList()
    var activateFromZone: Zone = Zone.BATTLEFIELD
    var promptOnDraw: Boolean = false

    // Named target bindings (for multi-target abilities)
    private val namedTargets: MutableList<Pair<String, TargetRequirement>> = mutableListOf()

    /**
     * Declare a named target and get an EffectTarget reference to use in effects.
     * Same pattern as SpellBuilder.target().
     *
     * @param name A descriptive name for the target (for debugging/documentation)
     * @param requirement The target requirement specification
     * @return An EffectTarget.ContextTarget that references this target by index
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.ContextTarget {
        val index = namedTargets.size
        namedTargets.add(name to requirement)
        return EffectTarget.ContextTarget(index)
    }

    internal val targetRequirements: List<TargetRequirement>
        get() = if (namedTargets.isNotEmpty()) {
            namedTargets.map { it.second }
        } else {
            listOfNotNull(target)
        }

    fun build(): ActivatedAbility {
        requireNotNull(effect) { "Activated ability must have an effect" }
        return ActivatedAbility(
            id = AbilityId.generate(),
            cost = cost,
            effect = effect!!,
            targetRequirements = targetRequirements,
            isManaAbility = manaAbility,
            timing = timing,
            restrictions = restrictions,
            activateFromZone = activateFromZone,
            promptOnDraw = promptOnDraw
        )
    }
}

// =============================================================================
// Static Ability Builder
// =============================================================================

@CardDsl
class StaticAbilityBuilder {
    var effect: Effect? = null
    var filter: Any? = null  // Can be GroupFilter or StaticTarget

    /**
     * Condition that must be met for this static ability to apply.
     * Used for cards like Karakyk Guardian: "hexproof if it hasn't dealt damage yet"
     */
    var condition: Condition? = null

    /**
     * Direct static ability assignment (bypasses effect conversion).
     * Takes precedence over effect if set.
     */
    var ability: StaticAbility? = null

    fun build(): StaticAbility {
        // Build the base ability
        val baseAbility = ability ?: buildFromEffect()

        // Wrap in conditional if condition is set
        return if (condition != null) {
            ConditionalStaticAbility(baseAbility, condition!!)
        } else {
            baseAbility
        }
    }

    private fun buildFromEffect(): StaticAbility {
        // Convert effect to appropriate StaticAbility type
        return when (val e = effect) {
            is ModifyStatsEffect -> ModifyStats(
                e.powerModifier,
                e.toughnessModifier,
                filter as? StaticTarget ?: StaticTarget.AttachedCreature
            )
            is GrantKeywordUntilEndOfTurnEffect -> GrantKeyword(
                e.keyword,
                filter as? StaticTarget ?: StaticTarget.AttachedCreature
            )
            is CompositeEffect -> {
                // For composite, we create a ModifyStats with the first stat mod found
                // This is a simplification - real implementation would handle this better
                val statMod = e.effects.filterIsInstance<ModifyStatsEffect>().firstOrNull()
                if (statMod != null) {
                    ModifyStats(
                        statMod.powerModifier,
                        statMod.toughnessModifier,
                        filter as? StaticTarget ?: StaticTarget.AttachedCreature
                    )
                } else {
                    // Fallback for keyword grants in composites
                    val keyword = e.effects.filterIsInstance<GrantKeywordUntilEndOfTurnEffect>().firstOrNull()
                    if (keyword != null) {
                        GrantKeyword(keyword.keyword, filter as? StaticTarget ?: StaticTarget.AttachedCreature)
                    } else {
                        ModifyStats(0, 0, StaticTarget.AttachedCreature)
                    }
                }
            }
            else -> ModifyStats(0, 0, StaticTarget.AttachedCreature)
        }
    }
}

// =============================================================================
// Loyalty Ability Builder
// =============================================================================

@CardDsl
class LoyaltyAbilityBuilder(private val loyaltyChange: Int) {
    var effect: Effect? = null
    var target: TargetRequirement? = null

    fun build(): ActivatedAbility {
        requireNotNull(effect) { "Loyalty ability must have an effect" }
        return ActivatedAbility(
            id = AbilityId.generate(),
            cost = AbilityCost.Loyalty(loyaltyChange),
            effect = effect!!,
            targetRequirements = listOfNotNull(target),
            isPlaneswalkerAbility = true,
            timing = TimingRule.SorcerySpeed
        )
    }
}

// =============================================================================
// Metadata Builder
// =============================================================================

@CardDsl
class MetadataBuilder {
    var rarity: Rarity = Rarity.COMMON
    var collectorNumber: String? = null
    var artist: String? = null
    var flavorText: String? = null
    var imageUri: String? = null
    private val _rulings = mutableListOf<Ruling>()

    /**
     * Add an official ruling for this card.
     * @param date The date of the ruling (e.g., "6/8/2016")
     * @param text The ruling text
     */
    fun ruling(date: String, text: String) {
        _rulings.add(Ruling(date, text))
    }

    fun build(): ScryfallMetadata = ScryfallMetadata(
        collectorNumber = collectorNumber,
        rarity = rarity,
        artist = artist,
        flavorText = flavorText,
        imageUri = imageUri,
        rulings = _rulings.toList()
    )
}
