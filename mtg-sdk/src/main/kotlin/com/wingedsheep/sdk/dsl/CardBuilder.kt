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
 *         effect = Effects.DealDamage(3)
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
     * Power (for creatures).
     */
    var power: Int? = null

    /**
     * Toughness (for creatures).
     */
    var toughness: Int? = null

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
        val creatureStats = if (power != null && toughness != null) {
            CreatureStats(power!!, toughness!!)
        } else {
            null
        }

        // Build the script
        val script = CardScript(
            spellEffect = spellBuilder?.effect,
            targetRequirements = spellBuilder?.targetRequirements ?: emptyList(),
            triggeredAbilities = triggeredAbilities.toList(),
            activatedAbilities = activatedAbilities.toList(),
            staticAbilities = staticAbilities.toList(),
            additionalCosts = additionalCosts.toList(),
            auraTarget = auraTarget
        )

        // Build metadata
        val metadata = metadataBuilder?.build() ?: ScryfallMetadata()

        return CardDefinition(
            name = name,
            manaCost = parsedManaCost,
            typeLine = parsedTypeLine,
            oracleText = oracleText,
            creatureStats = creatureStats,
            keywords = keywordSet.toSet(),
            keywordAbilities = keywordAbilityList.toList(),
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

@CardDsl
class SpellBuilder {
    var effect: Effect? = null
    var target: TargetRequirement? = null
    var condition: Condition? = null

    internal val targetRequirements: List<TargetRequirement>
        get() = listOfNotNull(target)
}

// =============================================================================
// Triggered Ability Builder
// =============================================================================

@CardDsl
class TriggeredAbilityBuilder {
    var trigger: Trigger = OnEnterBattlefield()
    var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
    var target: TargetRequirement? = null
    var optional: Boolean = false

    fun build(): TriggeredAbility = TriggeredAbility.create(
        trigger = trigger,
        effect = effect,
        optional = optional,
        targetRequirement = target
    )
}

// =============================================================================
// Activated Ability Builder
// =============================================================================

@CardDsl
class ActivatedAbilityBuilder {
    var cost: AbilityCost = AbilityCost.Tap
    var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
    var manaAbility: Boolean = false
    var timingRestriction: TimingRestriction = TimingRestriction.INSTANT

    fun build(): ActivatedAbility = ActivatedAbility(
        id = AbilityId.generate(),
        cost = cost,
        effect = effect,
        isManaAbility = manaAbility,
        timingRestriction = timingRestriction
    )
}

// =============================================================================
// Static Ability Builder
// =============================================================================

@CardDsl
class StaticAbilityBuilder {
    var effect: Effect? = null
    var filter: Any? = null  // Can be CreatureFilter or StaticTarget

    fun build(): StaticAbility {
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
    var effect: Effect = DrawCardsEffect(0, EffectTarget.Controller)
    var target: TargetRequirement? = null

    fun build(): ActivatedAbility = ActivatedAbility(
        id = AbilityId.generate(),
        cost = AbilityCost.Loyalty(loyaltyChange),
        effect = effect,
        isPlaneswalkerAbility = true,
        timingRestriction = TimingRestriction.SORCERY
    )
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

    fun build(): ScryfallMetadata = ScryfallMetadata(
        collectorNumber = collectorNumber,
        rarity = rarity,
        artist = artist,
        flavorText = flavorText,
        imageUri = imageUri
    )
}
