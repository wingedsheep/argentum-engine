package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.encodeToString
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.extension
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.streams.toList

/**
 * Loads CardDefinition objects from JSON files.
 *
 * Usage:
 * ```kotlin
 * val card = CardLoader.fromJson(jsonString)
 * val cards = CardLoader.loadSet(Path.of("cards/onslaught/"))
 * ```
 *
 * The loader:
 * 1. Deserializes JSON using [CardSerialization.json]
 * 2. Assigns fresh AbilityIds via [withGeneratedIds]
 * 3. Optionally validates via [CardValidator]
 */
object CardLoader {

    /**
     * Deserialize a CardDefinition from a JSON string.
     * Assigns fresh AbilityIds after deserialization.
     */
    fun fromJson(jsonString: String): CardDefinition {
        val card = CardSerialization.json.decodeFromString<CardDefinition>(jsonString)
        return card.withGeneratedIds()
    }

    /**
     * Serialize a CardDefinition to a pretty-printed JSON string.
     */
    fun toJson(card: CardDefinition): String {
        return CardSerialization.json.encodeToString(card)
    }

    /**
     * Load a single card from a JSON file.
     */
    fun loadFromFile(path: Path): CardDefinition {
        val jsonString = path.readText()
        return fromJson(jsonString)
    }

    /**
     * Load all card JSON files from a directory.
     *
     * @param directory Directory containing .json card files
     * @param validate If true, run CardValidator on each card and collect errors
     * @return Result containing loaded cards and any validation errors
     */
    fun loadSet(directory: Path, validate: Boolean = true): CardSetLoadResult {
        val cards = mutableListOf<CardDefinition>()
        val errors = mutableListOf<CardLoadError>()

        val jsonFiles = Files.walk(directory, 1)
            .filter { it.isRegularFile() && it.extension == "json" }
            .toList()
            .sortedBy { it.name }

        for (file in jsonFiles) {
            try {
                val card = loadFromFile(file)

                if (validate) {
                    val validationErrors = CardValidator.validate(card)
                    if (validationErrors.any { it is CardValidationError.MissingCreatureStats }) {
                        errors.add(
                            CardLoadError(
                                file = file,
                                cardName = null,
                                message = "Validation failed: ${validationErrors.joinToString("; ") { it.message }}"
                            )
                        )
                        continue
                    }
                    // Non-fatal validation errors are reported but card is still loaded
                    validationErrors.forEach { error ->
                        errors.add(
                            CardLoadError(
                                file = file,
                                cardName = card.name,
                                message = "Warning: ${error.message}"
                            )
                        )
                    }
                }

                cards.add(card)
            } catch (e: Exception) {
                errors.add(
                    CardLoadError(
                        file = file,
                        cardName = null,
                        message = "Failed to load: ${e.message}"
                    )
                )
            }
        }

        return CardSetLoadResult(cards, errors)
    }
}

/**
 * Result of loading a set of card JSON files.
 */
data class CardSetLoadResult(
    val cards: List<CardDefinition>,
    val errors: List<CardLoadError>
) {
    val hasErrors: Boolean get() = errors.isNotEmpty()
}

/**
 * Error encountered while loading a card JSON file.
 */
data class CardLoadError(
    val file: Path,
    val cardName: String?,
    val message: String
)
