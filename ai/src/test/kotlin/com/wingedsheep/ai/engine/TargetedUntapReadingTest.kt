package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * An untap aimed at a target. "Untap target land" (Arbor Elf, Hope Tender's "untap two target
 * lands") is ramp: the land we aim it at taps for mana again. "Untap target creature you control"
 * (Dauntless Aven) needs no intent, as a permanent untapping itself doesn't: what the untap buys is
 * on the board for the simulation to see. An untap that can land on a creature of anyone's is
 * neither, and stays unread.
 */
class TargetedUntapReadingTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    private fun unread(name: String) =
        AiReadingAudit.read(card(name)).abilities.flatMap { it.findings }.filterNot { it.read }

    init {
        test("untapping a target land is ramp") {
            for (name in listOf("Arbor Elf", "Hope Tender")) {
                withClue(name) {
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.RAMP
                    unread(name) shouldBe emptyList()
                }
            }
        }

        test("untapping our own creature needs no intent") {
            unread("Dauntless Aven") shouldBe emptyList()
        }

        test("an untap that sets up another step on the land is not read as ramp") {
            // Wakeroot Elemental: "Untap target land you control. It becomes a 5/5 Elemental creature
            // with haste." The untap readies the land to attack, so the ability is the animation —
            // a combat enhancement — and never ramp.
            val tags = CardIntentAnalyzer.analyze(cardRegistry.requireCard("Wakeroot Elemental")).tags
            tags shouldNotContain IntentTag.RAMP
            tags shouldContain IntentTag.COMBAT_ENHANCEMENT
        }

        test("untapping any creature is not ramp") {
            val untap = TapUntapEffect(EffectTarget.ContextTarget(0), tap = false)
            CardIntentAnalyzer.analyzeEffect(untap, listOf(TargetObject(filter = TargetFilter.Creature))).tags shouldNotContain
                IntentTag.RAMP
            CardIntentAnalyzer.analyzeEffect(untap, listOf(TargetObject(filter = TargetFilter.Land))).tags shouldContain
                IntentTag.RAMP
        }
    }
}
