package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.dtk.cards.AssaultFormation
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "Target creature with defender can attack this turn as though it didn't have defender" —
 * `Effects.CanAttackDespiteDefenderThisTurn(target)`. Lifting defender only turns a wall into an
 * attacker, so the slot is aimed at our own creature.
 */
class DefenderLiftPolarityAiTest : FunSpec({

    test("a targeted defender lift is beneficial") {
        val lift = AssaultFormation.script.activatedAbilities.first()
        TargetPolarityAnalyzer.forAbility(AssaultFormation, lift) shouldBe listOf(TargetPolarity.BENEFICIAL)
    }

    test("Assault Formation reads fully") {
        val reading = AiReadingAudit.read(AssaultFormation)
        withClue("$reading") { reading.readFully shouldBe true }
    }
})
