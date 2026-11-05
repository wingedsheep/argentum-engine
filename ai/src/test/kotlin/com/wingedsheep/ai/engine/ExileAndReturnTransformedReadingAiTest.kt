package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Exile this, then return it to the battlefield transformed" — a permanent flipping itself as a new
 * object (Conqueror's Galleon's end-of-combat flip into Conqueror's Foothold, the FIN eikons) — has
 * no target and nothing to time, so it needs no intent, like an in-place self-transform.
 */
class ExileAndReturnTransformedReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("a self exile-and-return-transformed reads fully and leaves the card recognised") {
        for ((name, sets) in listOf("Conqueror's Galleon" to listOf("XLN"), "Clive, Ifrit's Dominant" to listOf("FIN"))) {
            withClue("$name reads every checked ability fully") {
                AiReadingAudit.read(card(name)).abilities.filter { it.checked }.all { it.readFully } shouldBe true
            }
            withClue("$name is not flagged 'abilities not recognised'") {
                AiCardRating.of(card(name), sets).intent.unrecognised shouldBe false
            }
        }
    }
})
