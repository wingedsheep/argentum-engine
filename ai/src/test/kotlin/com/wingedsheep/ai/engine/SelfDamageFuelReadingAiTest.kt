package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.definitions.tsp.cards.StuffyDoll
import io.kotest.matchers.collections.shouldContain
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Damage a permanent deals to itself is a price — unless the card watches its own damage. Stuffy
 * Doll's "{T}: This creature deals 1 damage to itself" fires "whenever this creature is dealt
 * damage, it deals that much damage to the chosen player", so the tap is a 1-point drain.
 */
class SelfDamageFuelReadingAiTest : FunSpec({
    test("self-damage that fires the card's own dealt-damage trigger reads as that trigger's purpose") {
        val tap = StuffyDoll.script.activatedAbilities.single()
        CardIntentAnalyzer.analyzeEffect(tap.effect, source = StuffyDoll).tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
        AiReadingAudit.read(StuffyDoll).abilities.flatMap { it.findings }.filter { !it.read } shouldBe emptyList()
    }

    test("without such a trigger, self-damage still reads as no purpose of its own") {
        val tap = StuffyDoll.script.activatedAbilities.single()
        CardIntentAnalyzer.analyzeEffect(tap.effect).tags shouldBe emptySet()
    }
})
