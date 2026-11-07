package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.aer.cards.SecretSalvage
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Exile target nonland card from your graveyard. Search your library for any number of cards with
 * the same name as that card … put them into your hand" (Secret Salvage): the pipeline notes the
 * target's name and tutors same-named cards from our own library to our hand. The graveyard card is
 * the name we tutor for — our own pick, not graveyard hate — and the name-noting step is bookkeeping.
 */
class NamedTutorPolarityAiTest : FunSpec({

    test("a tutor keyed on the name of a card in our graveyard reads its slot as our own pick") {
        TargetPolarityAnalyzer.forSpell(SecretSalvage) shouldBe listOf(TargetPolarity.BENEFICIAL)
    }

    test("Secret Salvage reads fully") {
        val reading = AiReadingAudit.read(SecretSalvage)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
