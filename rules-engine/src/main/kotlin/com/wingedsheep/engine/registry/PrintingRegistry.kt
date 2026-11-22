package com.wingedsheep.engine.registry

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.PrintingRef

/**
 * Registry for looking up specific *printings* of cards.
 *
 * Sits alongside [CardRegistry], not on top of it. `CardRegistry` owns the canonical
 * `CardDefinition` per oracle (script, types, P/T) — what the rules engine cares about.
 * `PrintingRegistry` owns the per-print presentation/metadata (set, collector number, art,
 * artist, scryfall id) that the deckbuilder shows and the in-game UI renders.
 *
 * ## Usage
 * ```kotlin
 * val printings = PrintingRegistry()
 * printings.register(Printing(oracleId = "abc", name = "Lightning Bolt", setCode = "M10", collectorNumber = "146"))
 * printings.getPrinting(PrintingRef("M10", "146")) // -> the printing above
 * printings.printingsOf("Lightning Bolt")           // -> all printings of that name
 * printings.defaultPrinting("Lightning Bolt")       // -> newest by release date
 * ```
 *
 * ## Default-printing synthesis
 * Until per-set Scryfall printing data lands (see Phase 5 of the multi-printing plan), the
 * registry can synthesize a "default printing" for a `CardDefinition` directly from its
 * existing `setCode` + `metadata`. That keeps the rest of the system migrate-able in
 * isolation: deckbuilder code can ask for a printing and always get one even though the
 * full Scryfall index isn't loaded yet.
 */
class PrintingRegistry {
    private val byRef = mutableMapOf<PrintingRef, Printing>()
    private val byName = mutableMapOf<String, MutableList<Printing>>()
    private val byOracle = mutableMapOf<String, MutableList<Printing>>()

    /**
     * Register a single printing. Idempotent on `PrintingRef` — re-registering the same
     * `(setCode, collectorNumber)` overwrites the prior entry. Useful when reloading
     * printing data from a refreshed Scryfall dump.
     */
    fun register(printing: Printing) {
        val ref = printing.ref
        // If we're overwriting, evict from the secondary indexes first to avoid duplicates.
        byRef[ref]?.let { existing ->
            byName[existing.name]?.removeAll { it.ref == ref }
            byOracle[existing.oracleId]?.removeAll { it.ref == ref }
        }
        byRef[ref] = printing
        byName.getOrPut(printing.name) { mutableListOf() }.add(printing)
        byOracle.getOrPut(printing.oracleId) { mutableListOf() }.add(printing)
    }

    fun register(printings: Iterable<Printing>) {
        printings.forEach(::register)
    }

    /**
     * Synthesise and register a default printing for a [CardDefinition] from its
     * `setCode` + `metadata.collectorNumber`. No-op if either is missing, or if a
     * printing for that `(setCode, collectorNumber)` already exists. Returns the
     * registered (or pre-existing) printing, or null if synthesis isn't possible.
     */
    fun registerSynthesizedDefault(card: CardDefinition): Printing? {
        val ref = card.defaultPrintingRef ?: return null
        byRef[ref]?.let { return it }
        val synth = Printing(
            oracleId = card.oracleId ?: card.name,
            name = card.name,
            setCode = ref.setCode,
            collectorNumber = ref.collectorNumber,
            scryfallId = card.metadata.scryfallId,
            artist = card.metadata.artist,
            imageUri = card.metadata.imageUri,
            backFaceImageUri = card.backFace?.metadata?.imageUri,
            releaseDate = card.metadata.releaseDate,
            rarity = card.metadata.rarity,
        )
        register(synth)
        return synth
    }

    fun getPrinting(ref: PrintingRef): Printing? = byRef[ref]

    fun getPrinting(setCode: String, collectorNumber: String): Printing? =
        byRef[PrintingRef(setCode, collectorNumber)]

    fun printingsOf(name: String): List<Printing> = byName[name].orEmpty().toList()

    fun printingsOfOracle(oracleId: String): List<Printing> =
        byOracle[oracleId].orEmpty().toList()

    /**
     * Default printing for a card name. Newest by `releaseDate` (lexicographic ISO date),
     * with printings missing a release date sorted last. Within the same release date, the
     * plain (non-alternate-frame) printing wins so the deckbuilder catalog never defaults to
     * a showcase/borderless variant — players still reach those through the printing picker.
     * When all candidates lack a date, the first-registered one wins. Returns null if no
     * printings are known for that name.
     */
    fun defaultPrinting(name: String): Printing? {
        val candidates = byName[name] ?: return null
        if (candidates.isEmpty()) return null
        // Sort: dated entries first by descending date, plain frames before alternate frames,
        // undated last in registration order.
        return candidates
            .withIndex()
            .sortedWith(
                compareBy<IndexedValue<Printing>> { it.value.releaseDate == null }
                    .thenByDescending { it.value.releaseDate ?: "" }
                    .thenBy { it.value.isAlternateFrame }
                    .thenBy { it.index }
            )
            .first()
            .value
    }

    fun hasPrinting(ref: PrintingRef): Boolean = ref in byRef

    /** Every registered printing, in no particular order — for indexes built over the whole corpus. */
    fun allPrintings(): Collection<Printing> = byRef.values.toList()

    val size: Int get() = byRef.size

    fun clear() {
        byRef.clear()
        byName.clear()
        byOracle.clear()
    }
}
