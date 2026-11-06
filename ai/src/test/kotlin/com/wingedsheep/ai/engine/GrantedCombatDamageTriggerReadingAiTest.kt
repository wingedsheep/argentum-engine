package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Target creature gains 'whenever this creature deals combat damage to a player, draw a card'
 * until end of turn" (Sygg, Wanderwine Wisdom; Commando Raid): a payoff for this turn's attack
 * connecting, read as [IntentTag.COMBAT_ENHANCEMENT]. The granted draw waits on combat damage, so it
 * is not read as card draw a card-flow window could cash.
 */
class GrantedCombatDamageTriggerReadingAiTest : ScenarioTestBase() {

    private fun tags(name: String) = CardIntentAnalyzer.analyze(cardRegistry.requireCard(name)).tags

    init {
        test("an until-end-of-turn combat-damage trigger grant reads as a combat enhancement") {
            for (name in listOf("Sygg, Wanderwine Wisdom", "Commando Raid")) {
                withClue("$name rewards this turn's attacker for connecting") {
                    tags(name) shouldContain IntentTag.COMBAT_ENHANCEMENT
                }
            }
            withClue("Sygg's draw needs the creature to connect; it is no draw on its own") {
                tags("Sygg, Wanderwine Wisdom") shouldNotContain IntentTag.DRAW
            }
        }

        test("Sygg reads fully") {
            val reading = AiReadingAudit.read(cardRegistry.requireCard("Sygg, Wanderwine Wisdom"))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }
    }
}
