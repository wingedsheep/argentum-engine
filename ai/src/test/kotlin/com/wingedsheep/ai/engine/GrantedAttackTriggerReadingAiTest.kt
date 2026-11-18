package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * A trigger only a fight fires, granted until end of turn — Launch the Fleet's "any number of target
 * creatures each gain 'Whenever this creature attacks, create a 1/1 white Soldier creature token
 * that's tapped and attacking'". Like the granted combat-damage draw (Commando Raid) it pays off in
 * this turn's attack: a combat enhancement. Unread, the spell had no intent at all and the draft
 * heuristic gave it no term for what it does.
 */
class GrantedAttackTriggerReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name)!!

    init {
        test("a granted attack trigger until end of turn reads as a combat enhancement, through the per-target loop") {
            CardIntentAnalyzer.analyze(card(LAUNCH)).tags shouldContain IntentTag.COMBAT_ENHANCEMENT
            val reading = AiReadingAudit.read(card(LAUNCH))
            withClue(reading.toString()) { reading.readFully shouldBe true }
        }

        test("the draft heuristic prices Launch the Fleet's spell") {
            val terms = LimitedCardRater.explain(card(LAUNCH)).heuristic
            withClue("terms: $terms") {
                terms.single { it.part == "Spell" }.value shouldBeGreaterThan 0.0
            }
        }
    }

    private companion object {
        const val LAUNCH = "Launch the Fleet"
    }
}
