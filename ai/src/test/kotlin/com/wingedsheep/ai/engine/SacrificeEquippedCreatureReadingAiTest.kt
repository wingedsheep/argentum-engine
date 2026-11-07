package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Sacrifice it" aimed at an Equipment's host — Thirsting Axe's "at the beginning of your end step,
 * if equipped creature didn't deal combat damage to a creature this turn, sacrifice it". Its
 * controller sacrifices the creature they equipped, their own: the Axe's drawback. Read as a
 * sacrifice of any other permanent it was REMOVAL, and the end-step trigger made the Axe "repeatable
 * removal" worth four board points — an Equipment the AI would have valued for killing its own team.
 * The same holds for every "sacrifice it" aimed at the permanent a card is attached to.
 */
class SacrificeEquippedCreatureReadingAiTest : ScenarioTestBase() {

    private fun intent(name: String) = CardIntentAnalyzer.analyze(cardRegistry.getCard(name)!!)

    init {
        test("sacrificing the equipped creature is the Equipment's drawback, not removal") {
            val axe = intent("Thirsting Axe")
            withClue("tags: ${axe.tags}") {
                axe.tags shouldNotContain IntentTag.REMOVAL
                axe.tags shouldContain IntentTag.PUMP
            }
            withClue("the Axe earns no \"repeatable removal\" rung on the prior ladder") {
                CardIntentAnalyzer.priorLadder(axe).map { it.claim } shouldNotContain "repeatable removal"
            }
        }

        test("every attached-host sacrifice is the card's own price: Breath of Fury, Stitcher's Graft") {
            // "Enchant creature you control … sacrifice it", "whenever Stitcher's Graft becomes
            // unattached from a permanent, sacrifice that permanent": you can sacrifice only your own.
            listOf("Breath of Fury", "Stitcher's Graft").forEach { name ->
                withClue("$name: ${intent(name).tags}") {
                    intent(name).tags shouldNotContain IntentTag.REMOVAL
                }
            }
        }

        test("an edict still reads as removal") {
            intent("Diabolic Edict").tags shouldContain IntentTag.REMOVAL
        }
    }
}
