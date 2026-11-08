package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.emn.cards.CoaxFromTheBlindEternities
import com.wingedsheep.mtg.sets.definitions.war.cards.KarnTheGreatCreator
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe

/**
 * "You may reveal an Eldrazi card you own from outside the game or choose a face-up Eldrazi card
 * you own in exile. Put that card into your hand" — one pick across the sideboard and exile pooled
 * into a single gather — is a wish like Burning Wish's sideboard-only gather: a tutor, and a wish
 * whose filter the dead-wish check reads.
 *
 * Unread, Coax from the Blind Eternities carried no intent tag at all ("abilities not recognised"),
 * and Karn, the Great Creator's −2 had none either.
 */
class MultiZoneWishTutorReadingAiTest : FunSpec({

    test("a wish pooling outside the game with exile reads as a tutor") {
        val intent = CardIntentAnalyzer.analyze(CoaxFromTheBlindEternities)
        intent.tags shouldContain IntentTag.TUTOR
        intent.wishFilters.shouldNotBeEmpty()
    }

    test("Coax from the Blind Eternities reads fully") {
        val reading = AiReadingAudit.read(CoaxFromTheBlindEternities)
        withClue("$reading") { reading.readFully shouldBe true }
    }

    test("Karn, the Great Creator's -2 knows what it is for") {
        val reading = AiReadingAudit.read(KarnTheGreatCreator)
        withClue("$reading") {
            reading.flags.none { it.check == AiReadingAudit.Check.INTENT } shouldBe true
        }
    }
})
