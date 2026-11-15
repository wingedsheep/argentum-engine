package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntent
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.effects.GrantNextSpellResolveDestinationEffect] with a `HAND`
 * destination — "the next time you cast an instant or sorcery spell from your hand this turn, put
 * that card into your hand instead of into your graveyard as it resolves" (Soulfire Grand Master) —
 * gets the spell card back, so it reads as [IntentTag.RECURSION], the single-card return to hand.
 * Before, the activated ability read as "no intent". A card back each activation is a repeatable
 * engine, so the creature's board value carries an ability term, as Archivist's draw does.
 */
class NextSpellResolveDestinationReadingAiTest : ScenarioTestBase() {
    init {
        test("Soulfire Grand Master's return-to-hand rider reads as recursion, and the card reads fully") {
            val card = cardRegistry.requireCard("Soulfire Grand Master")
            CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.RECURSION
            val reading = AiReadingAudit.read(card)
            withClue("$reading") { reading.readFully shouldBe true }
        }

        test("the repeatable return-to-hand prices the creature above its bare body") {
            val card = cardRegistry.requireCard("Soulfire Grand Master")
            CardIntentAnalyzer.analyzeInPlay(card).staticPriorValue shouldBeGreaterThan CardIntent.UNKNOWN.staticPriorValue
        }
    }
}
