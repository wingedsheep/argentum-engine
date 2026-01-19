package com.wingedsheep.rulesengine.card

import com.wingedsheep.rulesengine.core.*
import kotlinx.serialization.Serializable

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
    val releaseDate: String? = null
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

@Serializable
data class CardDefinition(
    val name: String,
    val manaCost: ManaCost,
    val typeLine: TypeLine,
    val oracleText: String = "",
    val creatureStats: CreatureStats? = null,
    val keywords: Set<Keyword> = emptySet(),
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
            metadata = metadata
        )

        fun sorcery(
            name: String,
            manaCost: ManaCost,
            oracleText: String
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.sorcery(),
            oracleText = oracleText
        )

        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText
        )

        fun basicLand(
            name: String,
            subtype: Subtype
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.basicLand(subtype)
        )

        fun enchantment(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = subtypes
            ),
            oracleText = oracleText
        )

        fun aura(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ENCHANTMENT),
                subtypes = setOf(Subtype.AURA)
            ),
            oracleText = oracleText
        )

        fun artifact(
            name: String,
            manaCost: ManaCost,
            oracleText: String = "",
            subtypes: Set<Subtype> = emptySet(),
            supertypes: Set<Supertype> = emptySet()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = subtypes
            ),
            oracleText = oracleText
        )

        fun equipment(
            name: String,
            manaCost: ManaCost,
            equipCost: ManaCost,
            oracleText: String = "",
            supertypes: Set<Supertype> = emptySet(),
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
            supertypes: Set<Supertype> = emptySet()
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
            keywords = keywords
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
            startingLoyalty = startingLoyalty,
            metadata = metadata
        )

        /**
         * Creates an instant card with metadata.
         */
        fun instant(
            name: String,
            manaCost: ManaCost,
            oracleText: String,
            keywords: Set<Keyword> = emptySet(),
            metadata: ScryfallMetadata = ScryfallMetadata()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine.instant(),
            oracleText = oracleText,
            keywords = keywords,
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
            metadata = metadata
        )
    }
}
