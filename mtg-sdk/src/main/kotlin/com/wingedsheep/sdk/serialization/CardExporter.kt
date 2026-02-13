package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path

/**
 * Exports Kotlin-defined CardDefinitions to JSON files.
 *
 * Used for migrating existing card sets from Kotlin DSL to JSON format.
 *
 * Usage:
 * ```kotlin
 * // Export a single card
 * val json = CardExporter.exportToJson(LightningBolt)
 *
 * // Export an entire set to a directory
 * CardExporter.exportSet(PortalSet.cards, Path.of("cards/portal/"))
 * ```
 */
object CardExporter {

    /**
     * Serialize a CardDefinition to pretty-printed JSON.
     */
    fun exportToJson(card: CardDefinition): String {
        return CardSerialization.json.encodeToString(card)
    }

    /**
     * Export a list of cards to individual JSON files in a directory.
     *
     * File naming convention: kebab-case.json matching the card name.
     * E.g., "Lightning Bolt" → "lightning-bolt.json"
     *
     * @param cards List of card definitions to export
     * @param outputDir Directory to write JSON files to (created if it doesn't exist)
     */
    fun exportSet(cards: List<CardDefinition>, outputDir: Path) {
        Files.createDirectories(outputDir)

        cards.forEach { card ->
            val fileName = cardNameToFileName(card.name)
            val filePath = outputDir.resolve(fileName)
            val json = exportToJson(card)
            Files.writeString(filePath, json)
        }
    }

    /**
     * Convert a card name to a kebab-case file name.
     * "Lightning Bolt" → "lightning-bolt.json"
     * "Serra's Blessing" → "serras-blessing.json"
     */
    internal fun cardNameToFileName(cardName: String): String {
        return cardName
            .lowercase()
            .replace("'", "")
            .replace(",", "")
            .replace(" ", "-")
            .replace("--", "-") + ".json"
    }
}
