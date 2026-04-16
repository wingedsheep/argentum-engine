package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.model.*
import com.wingedsheep.sdk.scripting.ClassLevelAbility
import com.wingedsheep.sdk.scripting.SagaChapterAbility
import com.wingedsheep.sdk.scripting.*
import com.wingedsheep.sdk.scripting.conditions.Condition
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.AddManaEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import com.wingedsheep.sdk.scripting.targets.withId

/**
 * DSL entry point for defining cards.
 *
 * Usage:
 * ```kotlin
 * val bolt = card("Lightning Bolt") {
 *     manaCost = "{R}"
 *     typeLine = "Instant"
 *     spell {
 *         val any = target("any", Targets.Any)
 *         effect = Effects.DealDamage(3, any)
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
     * Example: `dynamicStats(DynamicAmount.AggregateZone(Player.Each, Zone.GRAVEYARD, aggregation = Aggregation.DISTINCT_TYPES), toughnessOffset = 1)` for Tarmogoyf.
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
     * Morph cost as a mana cost string (e.g., "{2}{U}").
     * When set, the card gains the Morph keyword ability with a mana cost.
     * For non-mana morph costs (e.g., pay life), use [morphCost] instead.
     */
    var morph: String? = null

    /**
     * Morph cost as a [PayCost] for non-mana morph costs (e.g., pay life).
     * When set, the card gains the Morph keyword ability.
     * For mana-based morph, the simpler [morph] string property is preferred.
     */
    var morphCost: PayCost? = null

    /**
     * Effect to execute as a replacement effect when this morph creature is turned face up.
     * Used for morph creatures with "as this is turned face up, put N counters on it" or similar.
     */
    var morphFaceUpEffect: Effect? = null

    /**
     * Warp cost as a mana cost string (e.g., "{1}{R}").
     * When set, the card gains the Warp keyword ability.
     * Warp allows casting for an alternative cost; the permanent is exiled at end of turn
     * and can be re-cast from exile using the warp cost on future turns.
     */
    var warp: String? = null

    /**
     * If set, the caster must choose a creature type during casting.
     * The source determines where to look for available creature types.
     */
    var castTimeCreatureTypeChoice: CastTimeCreatureTypeSource? = null

    /**
     * Whether this spell can't be countered by spells or abilities.
     */
    var cantBeCountered: Boolean = false

    /**
     * A condition under which this spell can be cast as though it had flash.
     * Used for Ferocious-style conditional flash abilities.
     */
    var conditionalFlash: Condition? = null

    /**
     * An alternative cost the caster may pay instead of the spell's mana cost.
     * Used for cards like Zahid, Djinn of the Lamp.
     */
    var selfAlternativeCost: SelfAlternativeCost? = null

    /**
     * Evoke cost. If set, the creature can be cast for this cost as an alternative.
     * When evoked, the creature is sacrificed when it enters the battlefield.
     * The evoke cost is specified as a mana string, e.g., "{R/W}{R/W}".
     */
    var evoke: String? = null

    // =========================================================================
    // Internal State
    // =========================================================================

    private var keywordSet: MutableSet<Keyword> = mutableSetOf()
    private var flagSet: MutableSet<AbilityFlag> = mutableSetOf()
    private var keywordAbilityList: MutableList<KeywordAbility> = mutableListOf()
    private var spellBuilder: SpellBuilder? = null
    private val triggeredAbilities: MutableList<TriggeredAbility> = mutableListOf()
    private val activatedAbilities: MutableList<ActivatedAbility> = mutableListOf()
    private val staticAbilities: MutableList<StaticAbility> = mutableListOf()
    private val additionalCosts: MutableList<AdditionalCost> = mutableListOf()
    private val replacementEffects: MutableList<ReplacementEffect> = mutableListOf()
    private val classLevelsList: MutableList<ClassLevelAbility> = mutableListOf()
    private val sagaChaptersList: MutableList<SagaChapterAbility> = mutableListOf()
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
     * Add ability flags to this card.
     * Ability flags are non-keyword static abilities like "can't be blocked"
     * or "doesn't untap during your untap step".
     */
    fun flags(vararg flags: AbilityFlag) {
        flagSet.addAll(flags)
    }

    /**
     * Add Prowess — keyword + triggered ability (+1/+1 on noncreature spell cast).
     */
    fun prowess() {
        keywordSet.add(Keyword.PROWESS)
        triggeredAbilities.add(
            TriggeredAbility.create(
                trigger = Triggers.YouCastNoncreature.event,
                binding = Triggers.YouCastNoncreature.binding,
                effect = ModifyStatsEffect(
                    powerModifier = 1,
                    toughnessModifier = 1,
                    target = EffectTarget.Self
                )
            )
        )
    }

    /**
     * Add the Vivid ability-word tag for display and attach an ETB triggered ability
     * whose effect scales with the number of distinct colors among permanents you
     * control (Lorwyn Eclipsed, effect-scaling half).
     *
     * The factory receives a [DynamicAmount] representing that colour count so the
     * effect composes with it naturally, e.g.:
     *
     * ```kotlin
     * vividEtb { colors ->
     *     CompositeEffect(listOf(
     *         GatherUntilMatchEffect(filter = GameObjectFilter.Permanent, count = colors, ...),
     *         ...
     *     ))
     * }
     * ```
     *
     * This is a DSL convenience — it emits an ordinary [TriggeredAbility] and a
     * [Keyword.VIVID] tag, so the resulting [CardDefinition] serializes/deserializes
     * exactly like a hand-written equivalent.
     */
    fun vividEtb(effectFactory: (DynamicAmount) -> Effect) {
        keywordSet.add(Keyword.VIVID)
        triggeredAbilities.add(
            TriggeredAbility.create(
                trigger = Triggers.EntersBattlefield.event,
                binding = Triggers.EntersBattlefield.binding,
                effect = effectFactory(DynamicAmounts.colorsAmongPermanents())
            )
        )
    }

    /**
     * Add the Vivid ability-word tag for display and the "This spell costs {1} less
     * to cast for each colour among permanents you control" cost reduction
     * (Lorwyn Eclipsed, cost-reduction half).
     *
     * Like [vividEtb], this emits normal serializable data — a [Keyword.VIVID] tag
     * plus a [SpellCostReduction] static ability sourced from
     * [CostReductionSource.ColorsAmongPermanentsYouControl].
     */
    fun vividCostReduction() {
        keywordSet.add(Keyword.VIVID)
        staticAbilities.add(SpellCostReduction(CostReductionSource.ColorsAmongPermanentsYouControl))
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
     * This sets the equipCost metadata and generates the activated ability
     * (Equip: attach to target creature you control, sorcery speed).
     */
    fun equipAbility(cost: String) {
        equipCost = ManaCost.parse(cost)
        activatedAbilities.add(
            ActivatedAbility(
                id = AbilityId.generate(),
                cost = AbilityCost.Mana(ManaCost.parse(cost)),
                effect = AttachEquipmentEffect(EffectTarget.BoundVariable("creature you control")),
                targetRequirements = listOf(
                    TargetCreature(filter = TargetFilter.CreatureYouControl, id = "creature you control")
                ),
                isManaAbility = false,
                timing = TimingRule.SorcerySpeed,
            )
        )
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
    // Class Levels
    // =========================================================================

    /**
     * Add a class level upgrade ability.
     * Level 1 abilities use the normal CardBuilder methods (triggeredAbility, staticAbility, etc.).
     * Use this for levels 2 and 3 to define the cost and abilities gained at that level.
     *
     * @param level The class level (2 or 3)
     * @param cost The mana cost to level up (e.g., "{1}{G}")
     */
    fun classLevel(level: Int, cost: String, init: ClassLevelBuilder.() -> Unit) {
        val builder = ClassLevelBuilder(level, cost)
        builder.init()
        classLevelsList.add(builder.build())
    }

    // =========================================================================
    // Saga Chapters
    // =========================================================================

    /**
     * Add a saga chapter ability.
     * Chapter abilities trigger when lore counters reach or exceed the chapter number.
     */
    fun sagaChapter(chapter: Int, init: SagaChapterBuilder.() -> Unit) {
        val builder = SagaChapterBuilder(chapter)
        builder.init()
        sagaChaptersList.add(builder.build())
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
            castTimeCreatureTypeChoice = castTimeCreatureTypeChoice,
            cantBeCountered = cantBeCountered,
            conditionalFlash = conditionalFlash,
            kickerTargetRequirements = spellBuilder?.kickerTargetRequirements ?: emptyList(),
            kickerSpellEffect = spellBuilder?.kickerEffect,
            classLevels = classLevelsList.toList(),
            sagaChapters = sagaChaptersList.toList(),
            selfExileOnResolve = spellBuilder?.exilesOnResolve ?: false,
            selfAlternativeCost = selfAlternativeCost
        )

        // Build metadata
        val metadata = metadataBuilder?.build() ?: ScryfallMetadata()

        // Add morph keyword ability if morph cost is specified
        val finalKeywordAbilities = buildList {
            addAll(keywordAbilityList)
            when {
                morph != null -> add(KeywordAbility.Morph(PayCost.Mana(ManaCost.parse(morph!!)), morphFaceUpEffect))
                morphCost != null -> add(KeywordAbility.Morph(morphCost!!, morphFaceUpEffect))
            }
            if (warp != null) add(KeywordAbility.Warp(ManaCost.parse(warp!!)))
            if (evoke != null) add(KeywordAbility.Evoke(ManaCost.parse(evoke!!)))
        }

        // Derive simple keywords from parameterized keyword abilities
        val derivedKeywords = finalKeywordAbilities.mapNotNull { it.keyword }.toSet()
        val finalKeywords = keywordSet + derivedKeywords

        return CardDefinition(
            name = name,
            manaCost = parsedManaCost,
            typeLine = parsedTypeLine,
            oracleText = oracleText,
            creatureStats = creatureStats,
            keywords = finalKeywords,
            flags = flagSet.toSet(),
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
 * 1. Simple (single target, legacy):
 * ```kotlin
 * spell {
 *     target = Targets.Creature
 *     effect = Effects.Destroy(EffectTarget.ContextTarget(0))
 * }
 * ```
 *
 * 2. Named binding (preferred):
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
    private var selfExileOnResolve: Boolean = false

    /**
     * Mark this spell to exile itself on resolution instead of going to the graveyard.
     * Used for cards that say "Exile <card name>."
     */
    fun selfExile() {
        selfExileOnResolve = true
    }

    internal val exilesOnResolve: Boolean get() = selfExileOnResolve

    /**
     * Alternate effect used when kicker is paid. When set along with [kickerTarget],
     * the kicked version uses completely different targeting and effect resolution.
     */
    var kickerEffect: Effect? = null

    /**
     * Alternate target used when kicker is paid. When set, the kicked version
     * uses this target requirement instead of the normal [target].
     */
    var kickerTarget: TargetRequirement? = null

    // Named kicker target bindings (for kicker spells with multiple alternate targets)
    private val namedKickerTargets: MutableList<Pair<String, TargetRequirement>> = mutableListOf()

    /**
     * Declare a named kicker target and get an EffectTarget reference to use in kickerEffect.
     * Use this when the kicked version needs multiple targets (e.g., Goblin Barrage).
     *
     * @param name A descriptive name for the target
     * @param requirement The target requirement specification
     * @return An EffectTarget.BoundVariable that references this kicker target by name
     */
    fun kickerTarget(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedKickerTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    internal val kickerTargetRequirements: List<TargetRequirement>
        get() = if (namedKickerTargets.isNotEmpty()) {
            namedKickerTargets.map { it.second }
        } else {
            listOfNotNull(kickerTarget)
        }

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
     * @return An EffectTarget.BoundVariable that references this target by name
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    /**
     * Declare a multi-target requirement and get indexed BoundVariable references.
     *
     * @param name A descriptive name for the targets
     * @param requirement The target requirement with count > 1
     * @return A list of BoundVariable references: name[0], name[1], ...
     */
    fun targets(name: String, requirement: TargetRequirement): List<EffectTarget.BoundVariable> {
        namedTargets.add(name to requirement.withId(name))
        return (0 until requirement.count).map { i -> EffectTarget.BoundVariable("$name[$i]") }
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
     *             effect = EffectPatterns.tapAll(CreatureGroupFilter.OpponentsControl)
     *         }
     *         mode("Draw a card") {
     *             effect = Effects.DrawCards(1)
     *         }
     *     }
     * }
     * ```
     */
    fun modal(
        chooseCount: Int = 1,
        minChooseCount: Int = chooseCount,
        allowRepeat: Boolean = false,
        init: ModalBuilder.() -> Unit
    ) {
        val builder = ModalBuilder(chooseCount, minChooseCount, allowRepeat)
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
class ModalBuilder(
    private val chooseCount: Int,
    private val minChooseCount: Int = chooseCount,
    private val allowRepeat: Boolean = false
) {
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

    internal fun build(): ModalEffect =
        ModalEffect(modes.toList(), chooseCount, minChooseCount, allowRepeat)
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
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        targets.add(requirement.withId(name))
        return EffectTarget.BoundVariable(name)
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
    /**
     * The trigger specification. Assign a [TriggerSpec] from the [Triggers] facade
     * (e.g., `trigger = Triggers.EntersBattlefield`).
     */
    var trigger: TriggerSpec = Triggers.EntersBattlefield

    var effect: Effect? = null
    var target: TargetRequirement? = null
    var optional: Boolean = false
    var elseEffect: Effect? = null
    var triggerZone: Zone = Zone.BATTLEFIELD
    /** Intervening-if condition (Rule 603.4): checked when trigger would fire AND at resolution. */
    var triggerCondition: Condition? = null
    /** When true, the triggered ability is controlled by the triggering entity's controller. */
    var controlledByTriggeringEntityController: Boolean = false
    /** When true, this triggered ability triggers at most once each turn. */
    var oncePerTurn: Boolean = false
    /** Optional human-readable description that overrides the auto-generated one. */
    var description: String? = null

    private val namedTargets = mutableListOf<Pair<String, TargetRequirement>>()

    /**
     * Declare a named target for this triggered ability and get an EffectTarget reference.
     * Can be called multiple times for multi-target triggered abilities.
     *
     * @param name A descriptive name for the target
     * @param requirement The target requirement specification
     * @return An EffectTarget.BoundVariable that references this target by name
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    fun build(): TriggeredAbility {
        requireNotNull(effect) { "Triggered ability must have an effect" }
        val allTargets = if (namedTargets.isNotEmpty()) {
            namedTargets.map { it.second }
        } else {
            listOfNotNull(target)
        }
        val primaryTarget = allTargets.firstOrNull()
        val additionalTargets = if (allTargets.size > 1) allTargets.drop(1) else emptyList()
        return TriggeredAbility.create(
            trigger = trigger.event,
            binding = trigger.binding,
            effect = effect!!,
            optional = optional,
            targetRequirement = primaryTarget,
            additionalTargetRequirements = additionalTargets,
            elseEffect = elseEffect,
            activeZone = triggerZone,
            triggerCondition = triggerCondition,
            controlledByTriggeringEntityController = controlledByTriggeringEntityController,
            oncePerTurn = oncePerTurn,
            descriptionOverride = description
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
    var description: String? = null
    var hasConvoke: Boolean = false
    var holdPriority: Boolean = false

    // Named target bindings (for multi-target abilities)
    private val namedTargets: MutableList<Pair<String, TargetRequirement>> = mutableListOf()

    /**
     * Declare a named target and get an EffectTarget reference to use in effects.
     * Same pattern as SpellBuilder.target().
     *
     * @param name A descriptive name for the target (for debugging/documentation)
     * @param requirement The target requirement specification
     * @return An EffectTarget.BoundVariable that references this target by name
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
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
            promptOnDraw = promptOnDraw,
            descriptionOverride = description,
            hasConvoke = hasConvoke,
            holdPriority = holdPriority
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
                (e.powerModifier as? DynamicAmount.Fixed)?.amount ?: 0,
                (e.toughnessModifier as? DynamicAmount.Fixed)?.amount ?: 0,
                filter as? StaticTarget ?: StaticTarget.AttachedCreature
            )
            is GrantKeywordEffect -> GrantKeyword(
                e.keyword,
                filter as? StaticTarget ?: StaticTarget.AttachedCreature
            )
            is CompositeEffect -> {
                // For composite, we create a ModifyStats with the first stat mod found
                // This is a simplification - real implementation would handle this better
                val statMod = e.effects.filterIsInstance<ModifyStatsEffect>().firstOrNull()
                if (statMod != null) {
                    ModifyStats(
                        (statMod.powerModifier as? DynamicAmount.Fixed)?.amount ?: 0,
                        (statMod.toughnessModifier as? DynamicAmount.Fixed)?.amount ?: 0,
                        filter as? StaticTarget ?: StaticTarget.AttachedCreature
                    )
                } else {
                    // Fallback for keyword grants in composites
                    val keyword = e.effects.filterIsInstance<GrantKeywordEffect>().firstOrNull()
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
    var description: String? = null
    private val namedTargets: MutableList<Pair<String, TargetRequirement>> = mutableListOf()

    /**
     * Add a named target for this loyalty ability and get an EffectTarget reference.
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    fun build(): ActivatedAbility {
        requireNotNull(effect) { "Loyalty ability must have an effect" }
        val targetReqs = if (namedTargets.isNotEmpty()) {
            namedTargets.map { it.second }
        } else {
            listOfNotNull(target)
        }
        return ActivatedAbility(
            id = AbilityId.generate(),
            cost = AbilityCost.Loyalty(loyaltyChange),
            effect = effect!!,
            targetRequirements = targetReqs,
            isPlaneswalkerAbility = true,
            timing = TimingRule.SorcerySpeed,
            descriptionOverride = description
        )
    }
}

// =============================================================================
// Class Level Builder
// =============================================================================

/**
 * Builder for a Class enchantment level (2 or 3).
 * Supports adding triggered, static, and activated abilities gained at this level.
 */
@CardDsl
class ClassLevelBuilder(private val level: Int, private val costString: String) {
    private val triggeredAbilities: MutableList<TriggeredAbility> = mutableListOf()
    private val staticAbilities: MutableList<StaticAbility> = mutableListOf()
    private val activatedAbilities: MutableList<ActivatedAbility> = mutableListOf()
    private val replacementEffects: MutableList<ReplacementEffect> = mutableListOf()

    /**
     * Add a triggered ability gained at this class level.
     */
    fun triggeredAbility(init: TriggeredAbilityBuilder.() -> Unit) {
        val builder = TriggeredAbilityBuilder()
        builder.init()
        triggeredAbilities.add(builder.build())
    }

    /**
     * Add a static ability gained at this class level.
     */
    fun staticAbility(init: StaticAbilityBuilder.() -> Unit) {
        val builder = StaticAbilityBuilder()
        builder.init()
        staticAbilities.add(builder.build())
    }

    /**
     * Add an activated ability gained at this class level.
     */
    fun activatedAbility(init: ActivatedAbilityBuilder.() -> Unit) {
        val builder = ActivatedAbilityBuilder()
        builder.init()
        activatedAbilities.add(builder.build())
    }

    /**
     * Add a replacement effect gained at this class level.
     */
    fun replacementEffect(effect: ReplacementEffect) {
        replacementEffects.add(effect)
    }

    fun build(): ClassLevelAbility {
        return ClassLevelAbility(
            level = level,
            cost = ManaCost.parse(costString),
            triggeredAbilities = triggeredAbilities.toList(),
            staticAbilities = staticAbilities.toList(),
            activatedAbilities = activatedAbilities.toList(),
            replacementEffects = replacementEffects.toList()
        )
    }
}

// =============================================================================
// Saga Chapter Builder
// =============================================================================

@CardDsl
class SagaChapterBuilder(private val chapter: Int) {
    var effect: Effect? = null
    var target: TargetRequirement? = null

    private val namedTargets = mutableListOf<Pair<String, TargetRequirement>>()

    /**
     * Declare a named target for this chapter ability and get an EffectTarget reference.
     */
    fun target(name: String, requirement: TargetRequirement): EffectTarget.BoundVariable {
        namedTargets.add(name to requirement.withId(name))
        return EffectTarget.BoundVariable(name)
    }

    fun build(): SagaChapterAbility {
        requireNotNull(effect) { "Saga chapter $chapter must have an effect" }
        val allTargets = if (namedTargets.isNotEmpty()) {
            namedTargets.map { it.second }
        } else {
            listOfNotNull(target)
        }
        val primaryTarget = allTargets.firstOrNull()
        val additionalTargets = if (allTargets.size > 1) allTargets.drop(1) else emptyList()
        return SagaChapterAbility(
            chapter = chapter,
            effect = effect!!,
            targetRequirement = primaryTarget,
            additionalTargetRequirements = additionalTargets
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
