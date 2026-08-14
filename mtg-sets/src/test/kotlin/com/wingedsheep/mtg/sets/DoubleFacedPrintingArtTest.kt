package com.wingedsheep.mtg.sets

import com.wingedsheep.sdk.model.CardDefinition
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty

/**
 * Corpus-wide gate: **every `Printing` of a two-faced card declares `backFaceImageUri`.**
 *
 * A `Printing` row is the only place a *reprint's* art can live — the `CardDefinition` carries the
 * card's earliest printing and nothing else. Omit the back-face URL and nothing breaks loudly:
 * `CardEntityFactory` falls through to `cardDef.backFace.metadata.imageUri`, so the reprint quietly
 * wears the original set's back face. That is how an Innistrad Remastered Garruk Relentless turned
 * into an *Innistrad* Garruk, the Veil-Cursed the moment he flipped — six INR rows were missing the
 * field, and only a player noticing the wrong illustration ever surfaced it.
 *
 * Front art is not checked here: a row with no `imageUri` at all falls back to the canonical art
 * for *both* faces, which is a visibly incomplete row rather than the half-right state this catches.
 */
class DoubleFacedPrintingArtTest : FunSpec({

    /** Canonical definition per card name — the printing rows only carry the name. */
    val definitionsByName: Map<String, CardDefinition> =
        MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name }

    test("every printing of a two-faced card carries back-face art") {
        val gaps = MtgSetCatalog.all.flatMap { set ->
            set.printings
                .filter { printing ->
                    printing.backFaceImageUri == null &&
                        definitionsByName[printing.name]?.isDoubleFaced == true
                }
                .map { "[${set.code}] ${it.name} (#${it.collectorNumber})" }
        }

        if (gaps.isNotEmpty()) {
            println("=== printings of two-faced cards with no backFaceImageUri: ${gaps.size} ===")
            gaps.sorted().forEach { println("  $it") }
            println("--- add `backFaceImageUri` (Scryfall's `card_faces[1].image_uris.normal`) ---")
        }
        gaps.shouldBeEmpty()
    }
})
