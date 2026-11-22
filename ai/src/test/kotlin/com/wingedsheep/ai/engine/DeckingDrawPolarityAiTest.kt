package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.m12.cards.JaceMemoryAdept
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Any number of target players each draw twenty cards" (Jace, Memory Adept's −7) — a draw over
 * each target (`ForEachTarget` naming each as `PlayerRef(ContextPlayer(0))`) big enough to empty a
 * library: a mill-out on an opponent as much as card advantage, so the slot reads either way and
 * the simulation weighs each player's library.
 */
class DeckingDrawPolarityAiTest : FunSpec({

    test("a twenty-card draw for each target player is read either way") {
        val ultimate = JaceMemoryAdept.script.activatedAbilities.last()
        TargetPolarityAnalyzer.forAbility(JaceMemoryAdept, ultimate) shouldBe listOf(TargetPolarity.FLEXIBLE)
    }

    test("Jace, Memory Adept reads fully") {
        val reading = AiReadingAudit.read(JaceMemoryAdept)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
