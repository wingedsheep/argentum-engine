package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.doubles.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * "Sacrifice it" aimed at an Equipment's host — Thirsting Axe's "at the beginning of your end step,
 * if equipped creature didn't deal combat damage to a creature this turn, sacrifice it". Its
 * controller sacrifices the creature they equipped, their own: the Axe's drawback. Read as a
 * sacrifice of any other permanent it was REMOVAL, and the end-step trigger made the Axe "repeatable
 * removal" worth four board points — an Equipment the AI would have valued for killing its own team.
 * The same holds for every "sacrifice it" aimed at the permanent a card is attached to, and for a
 * card sacrificing itself (Phantasmal Shieldback's "when this creature becomes the target of a spell
 * or ability, sacrifice it"), which drafted a {U} 1/3 Illusion like removal.
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

        test("a creature sacrificing itself is its drawback, not removal, and earns no removal pick") {
            val shieldback = cardRegistry.getCard("Phantasmal Shieldback")!!
            withClue("tags: ${intent("Phantasmal Shieldback").tags}") {
                intent("Phantasmal Shieldback").tags shouldNotContain IntentTag.REMOVAL
            }
            // A term worth nothing is left out of the explanation altogether.
            val trigger = LimitedCardRater.explain(shieldback).heuristic.firstOrNull { it.part == "Triggered ability 1" }
            withClue("the sacrifice trigger: $trigger") { (trigger?.value ?: 0.0) shouldBeLessThan 0.5 }
            for (name in listOf("Phantasmal Shieldback", "Skulking Ghost")) {
                val reading = AiReadingAudit.read(cardRegistry.getCard(name)!!)
                withClue("$name: $reading") { reading.readFully shouldBe true }
            }
        }
    }
}
