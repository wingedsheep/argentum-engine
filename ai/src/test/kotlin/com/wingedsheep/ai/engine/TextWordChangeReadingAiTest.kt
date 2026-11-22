package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * "Change the text of target spell or permanent by replacing all instances of one color word with
 * another" ([com.wingedsheep.sdk.scripting.effects.ChangeWordInTextEffect]): worth whatever words
 * the target carries — our creature's protection turned toward the opponent's colour, their
 * landwalk turned toward a land we don't control — so its slot is [TargetPolarity.FLEXIBLE], both
 * sides open for the simulation to weigh. The rewrite has no purpose of its own beyond the text it
 * changes, and an until-end-of-turn one (Crystal Spray) is on the board the moment it resolves, as
 * a colour change is.
 *
 * Unread, the slot was UNKNOWN and the audit asked what the effect was for.
 */
class TextWordChangeReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a word rewrite keeps both sides of its target open") {
            for (name in listOf("Trait Doctoring", "Sleight of Mind", "Crystal Spray", "Magical Hack")) {
                withClue(name) { TargetPolarityAnalyzer.forSpell(card(name)) shouldBe listOf(TargetPolarity.FLEXIBLE) }
            }
        }

        test("the rewrite needs no intent or hold window of its own") {
            for (name in listOf("Trait Doctoring", "Sleight of Mind", "Crystal Spray", "Magical Hack")) {
                withClue(name) { AiReadingAudit.read(card(name)).readFully shouldBe true }
            }
        }
    }
}
