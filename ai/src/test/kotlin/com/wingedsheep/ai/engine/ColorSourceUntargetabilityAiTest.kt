package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.engine.mechanics.targeting.UntargetableByRules
import com.wingedsheep.mtg.sets.definitions.m11.cards.GaeasRevenge
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.UntargetableBy
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "Can't be the target of nongreen spells or abilities from nongreen sources" (Gaea's Revenge) is
 * part of the creature's body, like hexproof: read as understood, and priced in play.
 */
class ColorSourceUntargetabilityAiTest : FunSpec({
    val nongreen = UntargetableByRules.keyword(UntargetableBy.SourcesOfColor(ProtectionScope.NonColor(Color.GREEN)))
    val white = UntargetableByRules.keyword(UntargetableBy.SourcesOfColor(ProtectionScope.Color(Color.WHITE)))

    test("Gaea's Revenge's untargetability is read as part of its body and priced in play") {
        val rating = AiCardRating.of(GaeasRevenge, listOf("M11", "ORI"))
        rating.intent.unrecognised shouldBe false
        rating.board.parts.single { it.label.startsWith("Can't be targeted by nongreen") }.value shouldBeGreaterThan 0.0
    }

    test("the board values non-colour untargetability above a single colour's, and both above none") {
        val none = BoardPresence.creatureBodyValue(8, 5, emptySet())
        val oneColor = BoardPresence.creatureBodyValue(8, 5, setOf(white))
        val allButOne = BoardPresence.creatureBodyValue(8, 5, setOf(nongreen))
        oneColor shouldBeGreaterThan none
        allButOne shouldBeGreaterThan oneColor
    }
})
