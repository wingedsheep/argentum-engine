package com.wingedsheep.sdk.model

import com.wingedsheep.sdk.core.*
import com.wingedsheep.sdk.scripting.KeywordAbility
import kotlinx.serialization.Serializable

/**
 * An official ruling for a Magic: The Gathering card.
 * @param date The date of the ruling (e.g., "6/8/2016")
 * @param text The ruling text explaining card interactions or clarifications
 */
@Serializable
data class Ruling(
    val date: String,
    val text: String
)

/**
 * Metadata from Scryfall for web client features like booster drafts.
 * Contains information useful for display, pricing, and card organization.
 */
@Serializable
data class ScryfallMetadata(
    val collectorNumber: String? = null,
    val rarity: Rarity = Rarity.COMMON,
    val artist: String? = null,
    val flavorText: String? = null,
    val imageUri: String? = null,
    val scryfallId: String? = null,
    val releaseDate: String? = null,
    val rulings: List<Ruling> = emptyList()
)

/**
 * Card rarity levels.
 */
@Serializable
enum class Rarity {
    COMMON,
    UNCOMMON,
    RARE,
    MYTHIC,
    SPECIAL,
    BONUS
}

/**
 * Complete definition of a Magic: The Gathering card.
 *
 * CardDefinition combines static attributes (name, cost, types, stats) with
 * behavioral logic (script). This is the master object that fully describes
 * a card for both the rules engine and the client.
 *
 * ## Structure
 * - **Static attributes**: name, manaCost, typeLine, creatureStats, keywords
 * - **Behavioral logic**: script (contains effects, abilities, triggers)
 * - **Metadata**: Scryfall data for display, rarity, art
 *
 * ## Philosophy
 * Cards are defined as data, not code. The script property contains pure data
 * that describes what the card does. The engine interprets this data.
 *
 * @see CardScript for behavioral logic (effects, abilities, triggers)
 */
@Serializable
data class CardDefinition(
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val creatureStats: CreatureStats? = null,
    val keywords: Set<Keyword> = emptySet(),
    val keywordAbilities: List<KeywordAbility> = emptyList(),  // Parameterized keywords (Ward {2}, Protection from blue)
    val script: CardScript = CardScript.EMPTY,  // Behavioral logic (abilities, effects)
    val equipCost: ManaCost? = null,  // For Equipment cards
    val oracleId: String? = null,
    val setCode: String? = null,
    val backFace: CardDefinition? = null,  // For double-faced cards
    val metadata: ScryfallMetadata = ScryfallMetadata(),  // Scryfall metadata for web client
    val startingLoyalty: Int? = null  // For planeswalkers
) {
    init {
        if (typeLine.isCreature) {
            requireNotNull(creatureStats) { "Creature cards must have power/toughness: $name" }
        }
    }

    val cmc: Int get() = manaCost.cmc

    val colors: Set<Color> get() = manaCost.colors

    val colorIdentity: Set<Color>
        get() {
            val identity = manaCost.colors.toMutableSet()
            // Color identity also includes colors in rules text (e.g., activation costs)
            // For now, we just use mana cost colors
            return identity
        }

    val isCreature: Boolean get() = typeLine.isCreature
    val isLand: Boolean get() = typeLine.isLand
    val isSorcery: Boolean get() = typeLine.isSorcery
    val isInstant: Boolean get() = typeLine.isInstant
    val isEnchantment: Boolean get() = typeLine.isEnchantment
    val isArtifact: Boolean get() = typeLine.isArtifact
    val isAura: Boolean get() = typeLine.isAura
    val isEquipment: Boolean get() = typeLine.isEquipment
    val isPermanent: Boolean get() = typeLine.isPermanent
    val isDoubleFaced: Boolean get() = backFace != null
    val isPlaneswalker: Boolean get() = CardType.PLANESWALKER in typeLine.cardTypes

    fun hasKeyword(keyword: Keyword): Boolean = keyword in keywords

    // =========================================================================
    // Script Convenience Properties
    // =========================================================================

    /** Whether this card has any scripted behavior beyond being a vanilla permanent */
    val hasBehavior: Boolean get() = script.hasBehavior

    /** The effect when this spell resolves (for instants/sorceries) */
    val spellEffect get() = script.spellEffect

    /** Triggered abilities on this card */
    val triggeredAbilities get() = script.triggeredAbilities

    /** Activated abilities on this card */
    val activatedAbilities get() = script.activatedAbilities

    /** Static abilities (continuous effects) on this card */
    val staticAbilities get() = script.staticAbilities

    /** Target requirements when casting this spell */
    val targetRequirements get() = script.targetRequirements

    /** Whether this spell requires targets when cast */
    val requiresTargets: Boolean get() = script.requiresTargets

    /** Whether this card has additional costs beyond mana */
    val hasAdditionalCosts: Boolean get() = script.hasAdditionalCosts

    override fun toString(): String = buildString {
        append(name)
        if (manaCost.symbols.isNotEmpty()) {
            append(" ")
            append(manaCost)
        }
        append("\n")
        append(typeLine)
        if (oracleText.isNotBlank()) {
            append("\n")
            append(oracleText)
        }
        creatureStats?.let {
            append("\n")
            append(it)
        }
    }

    companion object {
        fun creature(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            power: Int,
            toughness: Int,
            oracleText: String = "",
            keywords: Set<Keyword> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.CREATURE),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            creatureStats = CreatureStats(power, toughness),
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        fun sorcery(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.sorcery(),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            script: CardScript = CardScript.EMPTY
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText,
            script = script
        )

        fun basicLand(
            name: String,
            subtype: Subtype,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.basicLand(subtype),
            metadata = metadata
            // Note: Basic lands have implicit mana abilities inferred from subtypes
        )

        fun enchantment(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun aura(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = setOf(Subtype.AURA)
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun artifact(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            metadata = metadata
        )

        fun equipment(
            name: String,
            manaCost: ManaCost,
            equipCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = setOf(Subtype.EQUIPMENT)
            ),
            oracleText = oracleText,
            script = script,
            equipCost = equipCost,
            metadata = metadata
        )

        fun artifactCreature(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            power: Int,
            toughness: Int,
            oracleText: String = "",
            keywords: Set<Keyword> = emptySet(),
            supertypes: Set<Supertype> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            creatureStats = CreatureStats(power, toughness),
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a double-faced transforming creature card.
         * @param frontFace The front face definition (must be a creature)
         * @param backFace The back face definition (must be a creature)
         */
        fun doubleFacedCreature(
            frontFace: CardDefinition,
            backFace: CardDefinition
        ): CardDefinition {
            require(frontFace.isCreature) { "Front face must be a creature" }
            require(backFace.isCreature) { "Back face must be a creature" }
            return frontFace.copy(backFace = backFace)
        }

        /**
         * Creates a planeswalker card.
         * @param name Card name
         * @param manaCost Mana cost
         * @param subtypes Planeswalker subtypes (e.g., Ajani, Jace)
         * @param startingLoyalty Starting loyalty counters
         * @param oracleText Oracle text describing abilities
         * @param supertypes Supertypes (typically Legendary)
         * @param metadata Scryfall metadata
         */
        fun planeswalker(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            startingLoyalty: Int,
            oracleText: String = "",
            supertypes: Set<Supertype> = setOf(Supertype.LEGENDARY),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.PLANESWALKER),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            script = script,
            startingLoyalty = startingLoyalty,
            metadata = metadata
        )

        /**
         * Creates an instant card with metadata and optional script.
         */
        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a Kindred Instant (instant with creature subtypes).
         * Used for spells like Crib Swap that have Changeling.
         */
        fun kindredInstant(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.KINDRED, CardType.INSTANT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )

        /**
         * Creates a Kindred Enchantment (enchantment with creature subtypes).
         */
        fun kindredEnchantment(
            name: String,
            manaCost: ManaCost,
            subtypes: Set<Subtype>,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            script: CardScript = CardScript.EMPTY,
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                cardTypes = setOf(CardType.KINDRED, CardType.ENCHANTMENT),
                subtypes = subtypes
            ),
            oracleText = oracleText,
            keywords = keywords,
            script = script,
            metadata = metadata
        )
    }
}
