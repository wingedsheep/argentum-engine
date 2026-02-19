package com.wingedsheep.sdk.serialization

import com.wingedsheep.sdk.model.CardDefinition
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
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
     *
     * Applies [CompactJsonTransformer.expand] to restore compacted singleton
     * strings (e.g., `"EntersBattlefield"` → `{"type": "EntersBattlefield"}`)
     * before deserialization.
     *
     * Assigns fresh AbilityIds after deserialization.
     */
    fun fromJson(jsonString: String): CardDefinition {
        val element = CardSerialization.json.parseToJsonElement(jsonString)
        val expanded = CompactJsonTransformer.expand(element)
        val card = CardSerialization.json.decodeFromJsonElement(CardDefinition.serializer(), expanded)
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
     * Load all card JSON files from a classpath resource directory.
     *
     * Use this when cards are bundled inside a JAR (e.g., in src/main/resources/).
     * The [resourcePath] should be the classpath path (e.g., "/cards/scourge/").
     * The [index] lists the JSON file names in the directory since classpath
     * directories inside JARs cannot be listed dynamically.
     *
     * @param clazz Class to use for resource loading
     * @param resourcePath Classpath resource directory (e.g., "/cards/scourge/")
     * @param index List of JSON file names (e.g., ["carrion-feeder.json", ...])
     * @param validate If true, run CardValidator on each card and collect errors
     */
    fun loadSetFromClasspath(
        clazz: Class<*>,
        resourcePath: String,
        index: List<String>,
        validate: Boolean = true
    ): List<CardDefinition> {
        val cards = mutableListOf<CardDefinition>()
        val prefix = if (resourcePath.endsWith("/")) resourcePath else "$resourcePath/"

        for (fileName in index.sorted()) {
            val jsonString = clazz.getResource("$prefix$fileName")?.readText()
                ?: error("Card resource not found: $prefix$fileName")
            val card = fromJson(jsonString)

            if (validate) {
                val validationErrors = CardValidator.validate(card)
                if (validationErrors.any { it is CardValidationError.MissingCreatureStats }) {
                    error("Validation failed for $fileName: ${validationErrors.joinToString("; ") { it.message }}")
                }
            }

            cards.add(card)
        }

        return cards
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
