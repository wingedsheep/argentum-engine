package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * `CantAttackOrBlockUnlessPay` — a mana price on a creature's own attacks and blocks. Printed on
 * the creature itself (Qal Sisma Behemoth's "can't attack or block unless you pay {2}", Myr
 * Prototype) it is the drawback of an oversized body, a [IntentTag.COMBAT_RESTRICTION]. On an
 * Aura (Brainwash's "enchanted creature can't attack unless its controller pays {3}") it lands on
 * the host instead: a curse to aim at the opponent's creature, never the Aura's own drawback.
 */
class PayToAttackReadingAiTest : ScenarioTestBase() {

    private fun registered(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a creature's own pay-to-attack static reads as a combat restriction") {
            listOf("Qal Sisma Behemoth", "Myr Prototype").forEach { name ->
                withClue(name) { CardIntentAnalyzer.analyze(registered(name)).tags shouldContain IntentTag.COMBAT_RESTRICTION }
            }
        }

        test("the drawback earns no static-effect rung and isn't aimed at the opponent") {
            val intent = CardIntentAnalyzer.analyze(registered("Qal Sisma Behemoth"))
            intent.affectsOpponent shouldBe false
            intent.staticPriorValue shouldBe CardIntent.UNKNOWN.staticPriorValue
        }

        test("on an Aura the tax curses what it enchants, and is not the Aura's own drawback") {
            val brainwash = registered("Brainwash")
            TargetPolarityAnalyzer.forSpell(brainwash) shouldBe listOf(TargetPolarity.HARMFUL)
            CardIntentAnalyzer.analyze(brainwash).tags shouldNotContain IntentTag.COMBAT_RESTRICTION
        }
    }
}
