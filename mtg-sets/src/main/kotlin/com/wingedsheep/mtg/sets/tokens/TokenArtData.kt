package com.wingedsheep.mtg.sets.tokens

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.TokenPrinting
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Bundled per-set token printings, synced from Scryfall's token sets (`t<code>`).
 *
 * This is the bulk layer behind `MtgSet.tokenArt`: rather than hand-authoring a row for every
 * token every set prints, `tokens.json` carries all of them and the wiring registers
 * `set.tokenArt + TokenArtData.forSet(set.code)` — hand-authored rows first, so a set that wants
 * to override the synced art (or supply art Scryfall has none of) simply declares it.
 *
 * Refresh with `./gradlew :mtg-sets:syncTokenArt` (see [SyncTokenArtKt]).
 *
 * ## What isn't in here
 * Roughly a third of the sets we implement are old enough that Wizards never printed token cards
 * for them — Alpha through Invasion, Tempest, Odyssey, Onslaught. Scryfall has no `t<code>` set to
 * sync, so their tokens fall through to the engine-wide generic art unless a set declares its own
 * (Invasion self-hosts Saproling and Reflection art under `web-client/public/images/tokens/`).
 */
object TokenArtData {

    private const val RESOURCE = "/tokens.json"

    /** Parsed once on first touch; empty if the resource is missing. */
    private val bySet: Map<String, List<TokenPrinting>> by lazy { load() }

    /** Token printings this set contributes, or empty when the set has none on Scryfall. */
    fun forSet(setCode: String): List<TokenPrinting> = bySet[setCode].orEmpty()

    /**
     * The whole art sheet to register for [set]: its hand-authored rows, plus the synced rows for
     * tokens it doesn't declare itself.
     *
     * The precedence — a set's own declaration beats the Scryfall sync — used to be implicit in
     * writing `set.tokenArt + forSet(set.code)` and letting the first match win. It cannot be, now
     * that a set may declare *several* arts for one token and the engine deals out every match: a
     * plain concatenation would tack the synced row onto the end of the set's own run. Foundations
     * borrows Jumpstart's four Dog arts for the Release the Dogs reprint, and `tfdn`'s single Dog
     * must not become a fifth.
     *
     * Overriding is per token *identity*, not per name, so a set declaring art for one Cat doesn't
     * silently swallow the synced art of a differently-statted Cat it also prints.
     */
    fun forSet(set: MtgSet): List<TokenPrinting> =
        set.tokenArt + forSet(set.code).filterNot { synced ->
            set.tokenArt.any {
                it.matches(synced.name, synced.power, synced.toughness, synced.colors.orEmpty())
            }
        }

    /**
     * [donor]'s synced sheet minus every token name [setCode] prints itself — the `tokenArt` for a
     * companion product that shares another set's token sheet.
     *
     * Bonus sheets and Commander decks mint tokens their own `t<code>` never printed, because the
     * physical token came in the main set's boosters: The Big Score's Clue and Treasure are OTJ
     * tokens, Bloomburrow Commander's Treasure is a Bloomburrow token. Those render with
     * engine-wide generic art unless the companion set claims the main set's printings.
     *
     * Names the companion prints itself are dropped rather than shadowed: the wiring registers
     * `set.tokenArt` *ahead* of `forSet(set.code)`, so a token on both sheets would otherwise show
     * the donor's art on the companion's card.
     */
    fun borrowedFrom(donor: String, setCode: String): List<TokenPrinting> {
        val own = forSet(setCode)
        return forSet(donor).filterNot { borrowed -> own.any { it.matchesName(borrowed.name) } }
    }

    /** Set codes carrying synced token art. */
    val setCodes: Set<String> get() = bySet.keys

    /** Total synced printings, for diagnostics. */
    val size: Int get() = bySet.values.sumOf { it.size }

    // ---------------------------------------------------------------------------------------
    // Wire format
    // ---------------------------------------------------------------------------------------

    /**
     * On-disk row. Separate from [TokenPrinting] so the SDK model stays free of serialization
     * concerns and the file can carry provenance ([scryfallId]) the engine doesn't need.
     *
     * [power] / [toughness] are null for noncreature tokens (Treasure, Clue, Role). [colors] is
     * an empty list for a colorless token — distinct from "unspecified", which this format has no
     * way to express and doesn't need: every synced row comes from a real printing.
     */
    @Serializable
    private data class Row(
        val name: String,
        val imageUri: String,
        val power: Int? = null,
        val toughness: Int? = null,
        val colors: List<String> = emptyList(),
        val scryfallId: String? = null,
    )

    private val parser = Json { ignoreUnknownKeys = true }
    private val serializer = MapSerializer(String.serializer(), ListSerializer(Row.serializer()))

    private fun load(): Map<String, List<TokenPrinting>> {
        val text = TokenArtData::class.java.getResource(RESOURCE)?.readText() ?: return emptyMap()
        return parser.decodeFromString(serializer, text).mapValues { (_, rows) ->
            rows.map { row ->
                TokenPrinting(
                    name = row.name,
                    imageUri = row.imageUri,
                    power = row.power,
                    toughness = row.toughness,
                    colors = row.colors.mapNotNullTo(mutableSetOf()) { c ->
                        runCatching { Color.valueOf(c) }.getOrNull()
                    },
                )
            }
        }
    }
}
