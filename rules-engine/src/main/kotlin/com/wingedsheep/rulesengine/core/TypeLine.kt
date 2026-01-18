package com.wingedsheep.rulesengine.core

import kotlinx.serialization.Serializable

@Serializable
data class TypeLine(
    val supertypes: Set<Supertype> = emptySet(),
    val cardTypes: Set<CardType>,
    val subtypes: Set<Subtype> = emptySet()
) {
    val isCreature: Boolean get() = CardType.CREATURE in cardTypes
    val isLand: Boolean get() = CardType.LAND in cardTypes
    val isSorcery: Boolean get() = CardType.SORCERY in cardTypes
    val isInstant: Boolean get() = CardType.INSTANT in cardTypes
    val isEnchantment: Boolean get() = CardType.ENCHANTMENT in cardTypes
    val isArtifact: Boolean get() = CardType.ARTIFACT in cardTypes
    val isPermanent: Boolean get() = cardTypes.any { it.isPermanent }

    val isAura: Boolean get() = isEnchantment && hasSubtype(Subtype.AURA)
    val isEquipment: Boolean get() = isArtifact && hasSubtype(Subtype.EQUIPMENT)
    val isArtifactCreature: Boolean get() = isArtifact && isCreature

    val isBasicLand: Boolean get() = isLand && Supertype.BASIC in supertypes
    val isLegendary: Boolean get() = Supertype.LEGENDARY in supertypes

    fun hasSubtype(subtype: Subtype): Boolean = subtype in subtypes

    override fun toString(): String = buildString {
        if (supertypes.isNotEmpty()) {
            append(supertypes.joinToString(" ") { it.displayName })
            append(" ")
        }
        append(cardTypes.joinToString(" ") { it.displayName })
        if (subtypes.isNotEmpty()) {
            append(" — ")
            append(subtypes.joinToString(" ") { it.value })
        }
    }

    companion object {
        fun parse(typeLineString: String): TypeLine {
            val parts = typeLineString.split("—", "–", "-").map { it.trim() }
            val typesPart = parts[0]
            val subtypesPart = parts.getOrNull(1)

            val typeWords = typesPart.split(" ").filter { it.isNotBlank() }

            val supertypes = mutableSetOf<Supertype>()
            val cardTypes = mutableSetOf<CardType>()

            typeWords.forEach { word ->
                Supertype.fromString(word)?.let { supertypes.add(it) }
                    ?: CardType.fromString(word)?.let { cardTypes.add(it) }
            }

            val subtypes = subtypesPart
                ?.split(" ")
                ?.filter { it.isNotBlank() }
                ?.map { Subtype(it) }
                ?.toSet()
                ?: emptySet()

            return TypeLine(supertypes, cardTypes, subtypes)
        }

        fun creature(subtypes: Set<Subtype> = emptySet()): TypeLine =
            TypeLine(cardTypes = setOf(CardType.CREATURE), subtypes = subtypes)

        fun sorcery(): TypeLine =
            TypeLine(cardTypes = setOf(CardType.SORCERY))

        fun instant(): TypeLine =
            TypeLine(cardTypes = setOf(CardType.INSTANT))

        fun basicLand(subtype: Subtype): TypeLine =
            TypeLine(
                supertypes = setOf(Supertype.BASIC),
                cardTypes = setOf(CardType.LAND),
                subtypes = setOf(subtype)
            )

        fun enchantment(subtypes: Set<Subtype> = emptySet()): TypeLine =
            TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = subtypes)

        fun aura(): TypeLine =
            TypeLine(cardTypes = setOf(CardType.ENCHANTMENT), subtypes = setOf(Subtype.AURA))

        fun artifact(subtypes: Set<Subtype> = emptySet()): TypeLine =
            TypeLine(cardTypes = setOf(CardType.ARTIFACT), subtypes = subtypes)

        fun equipment(): TypeLine =
            TypeLine(cardTypes = setOf(CardType.ARTIFACT), subtypes = setOf(Subtype.EQUIPMENT))

        fun artifactCreature(subtypes: Set<Subtype> = emptySet()): TypeLine =
            TypeLine(cardTypes = setOf(CardType.ARTIFACT, CardType.CREATURE), subtypes = subtypes)
    }
}
