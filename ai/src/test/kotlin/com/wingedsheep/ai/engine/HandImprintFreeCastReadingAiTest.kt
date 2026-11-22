package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.m14.cards.EliteArcanist
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "When this enters, you may exile an instant card from your hand" beside "copy the exiled card.
 * You may cast the copy without paying its mana cost" (Elite Arcanist, Isochron Scepter): the
 * imprint banks one of our own cards for a free cast later — [IntentTag.FREE_SPELL] — and, being
 * no new card, never [IntentTag.DRAW].
 */
class HandImprintFreeCastReadingAiTest : FunSpec({

    test("exiling an instant from our own hand into a pile cast for free reads as a banked free spell") {
        val scripts = listOf(EliteArcanist.script)
        val pileCast = CardIntentAnalyzer.pileCastTags(scripts)
        pileCast shouldContain IntentTag.FREE_SPELL
        val imprint = EliteArcanist.script.triggeredAbilities.single()
        val tags = CardIntentAnalyzer.analyzeEffect(imprint.effect, emptyList(), pileCast = pileCast).tags
        tags shouldContain IntentTag.FREE_SPELL
        tags shouldNotContain IntentTag.DRAW
    }

    test("Elite Arcanist reads fully") {
        val reading = AiReadingAudit.read(EliteArcanist)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
