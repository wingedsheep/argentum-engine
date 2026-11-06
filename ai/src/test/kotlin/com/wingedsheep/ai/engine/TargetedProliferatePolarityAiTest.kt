package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.mtg.sets.definitions.aer.cards.SkyshipPlunderer
import com.wingedsheep.mtg.sets.definitions.msh.cards.PowerfulBroker
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * "For each kind of counter on target permanent or player, give that permanent or player another
 * counter of that kind" — `Effects.Proliferate(target)`. One more of whatever the target holds: a
 * +1/+1, loyalty or energy counter for our side, a -1/-1 or poison counter for theirs, so the slot
 * stays open for the simulation to weigh each candidate's counters, and needs no intent tag.
 */
class TargetedProliferatePolarityAiTest : FunSpec({

    test("targeted proliferate is read either way") {
        val broker = PowerfulBroker.script.activatedAbilities.single()
        TargetPolarityAnalyzer.forAbility(PowerfulBroker, broker) shouldBe listOf(TargetPolarity.FLEXIBLE)
    }

    test("Skyship Plunderer and Powerful Broker read fully") {
        listOf(SkyshipPlunderer, PowerfulBroker).forEach { card ->
            val reading = AiReadingAudit.read(card)
            withClue("$reading") { reading.readFully shouldBe true }
        }
    }
})
