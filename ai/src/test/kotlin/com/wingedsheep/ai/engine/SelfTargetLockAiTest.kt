package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.engine.evaluation.BoardPresence
import com.wingedsheep.mtg.sets.definitions.m14.cards.FiendslayerPaladin
import com.wingedsheep.mtg.sets.definitions.war.cards.TomikDistinguishedAdvokist
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * "This creature can't be the target of black or red spells your opponents control" (Fiendslayer
 * Paladin) is a battlefield targeting lock on the creature itself — part of its body like hexproof:
 * read as understood and priced in play. A lock over other permanents (Tomik's lands) is not.
 */
class SelfTargetLockAiTest : FunSpec({
    test("Fiendslayer Paladin's untargetability is read as part of its body and priced in play") {
        val rating = AiCardRating.of(FiendslayerPaladin, listOf("M14"))
        rating.intent.unrecognised shouldBe false
        rating.board.parts.single { it.label.startsWith("Can't be targeted by") }.value shouldBeGreaterThan 0.0
    }

    test("only a lock on the permanent itself counts as a body lock") {
        FiendslayerPaladin.script.staticAbilities.any(BoardPresence::isSelfTargetLock) shouldBe true
        TomikDistinguishedAdvokist.script.staticAbilities.any(BoardPresence::isSelfTargetLock) shouldBe false
    }
})
