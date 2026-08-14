package com.wingedsheep.mtg.sets

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * Guards [com.wingedsheep.sdk.model.CardDefinition.meldResult] against the way it rots: someone
 * authors a meld result (a new set's meld pair, or a reprint whose canonical lands here) and it
 * silently joins the booster / draft / constructed pools as an uncastable card — the Chittering
 * Host bug.
 *
 * The expectation is derived, not listed: a meld *part* names its result in its oracle text
 * ("… exile them, then meld them into Chittering Host."), so every result the corpus already knows
 * about is discoverable from the parts. A meld result that isn't authored yet simply isn't checked;
 * one that is must carry the flag.
 */
class MeldResultFlagTest : FunSpec({

    val meldsInto = Regex("""meld them into ([^.]+)\.""")

    val allCards = MtgSetCatalog.all.flatMap { it.cards }
    val byName = allCards.associateBy { it.name }

    val namedResults = allCards
        .flatMap { meldsInto.findAll(it.oracleText).map { m -> m.groupValues[1] } }
        .distinct()
        .sorted()

    test("meld parts name their results (the derivation this test rests on)") {
        namedResults.shouldNotBeEmpty()
    }

    test("every authored meld result is flagged meldResult") {
        assertSoftly {
            for (name in namedResults) {
                val result = byName[name] ?: continue  // result not implemented yet — nothing to flag
                withClue("$name is the result of a meld pair, so it must set meldResult = true") {
                    result.meldResult shouldBe true
                }
            }
        }
    }

    // The inverse: nothing that *is* a real card gets flagged. A flagged card drops out of every
    // pool a player can draw a deck from, so a stray flag would silently delete a draftable card.
    test("only meld results are flagged") {
        assertSoftly {
            for (card in allCards.filter { it.meldResult }) {
                withClue("${card.name} is flagged meldResult but no meld part melds into it") {
                    (card.name in namedResults) shouldBe true
                }
            }
        }
    }
})
