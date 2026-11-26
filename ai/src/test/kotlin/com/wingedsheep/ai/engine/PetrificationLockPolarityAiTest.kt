package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.m13.cards.XathridGorgon
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Xathrid Gorgon's "put a petrification counter on target creature. It gains defender and becomes
 * a colorless artifact … Its activated abilities can't be activated." The petrification counter is
 * a pure marker, but it only ever marks that lock, so it reads as harm and the slot aims at an
 * opponent's creature rather than being a coin toss.
 */
class PetrificationLockPolarityAiTest : FunSpec({

    test("Xathrid Gorgon's petrify ability targets the opponent's side") {
        val petrify = XathridGorgon.script.activatedAbilities.single()
        TargetPolarityAnalyzer.forAbility(XathridGorgon, petrify) shouldBe listOf(TargetPolarity.HARMFUL)
    }

    test("Xathrid Gorgon reads fully") {
        val reading = AiReadingAudit.read(XathridGorgon)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
