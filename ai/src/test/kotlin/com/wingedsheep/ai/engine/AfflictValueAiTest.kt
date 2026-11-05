package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.mtg.sets.definitions.hou.cards.KhenraEternal
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan

/**
 * Afflict N (CR 702.130) taxes every block of its creature, so the AI prices it above the bare
 * body in play — Khenra Eternal is a 2/2 with afflict 1 and nothing else.
 */
class AfflictValueAiTest : FunSpec({
    test("afflict adds board value beyond the creature's body") {
        val rating = AiCardRating.of(KhenraEternal, listOf("HOU"))
        rating.board.parts.single { it.label == "Afflict" }.value shouldBeGreaterThan 0.0
    }
})
