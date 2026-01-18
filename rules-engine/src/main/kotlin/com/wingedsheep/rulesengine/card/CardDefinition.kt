package com.wingedsheep.rulesengine.card

import com.wingedsheep.rulesengine.core.*
import kotlinx.serialization.Serializable

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
    val setCode: String? = null
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
            supertypes: Set<Supertype> = emptySet()
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
            keywords = keywords
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
            supertypes: Set<Supertype> = emptySet()
        ): CardDefinition = CardDefinition(
            name = name,
            manaCost = manaCost,
            typeLine = TypeLine(
                supertypes = supertypes,
                cardTypes = setOf(CardType.ARTIFACT),
                subtypes = setOf(Subtype.EQUIPMENT)
            ),
            oracleText = oracleText,
            equipCost = equipCost
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
    }
}
