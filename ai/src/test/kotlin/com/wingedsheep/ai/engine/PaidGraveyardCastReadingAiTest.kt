package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * A pipeline that gathers its own graveyard target and casts it on resolution, still paying its
 * costs — "you may cast target card with the same name as that spell from your graveyard"
 * (Harness the Storm), "you may cast target instant or sorcery card from your graveyard"
 * (Chandra, Flame's Catalyst) — gets the card back through the stack: [IntentTag.RECURSION], as
 * returning it to hand does — and so does granting that cast for the turn instead
 * (Vohar, Vodalian Desecrator; Jace, Telepath Unbound's −3). Unread, the ability read as "no intent": a paid
 * `CastFromCollectionWithoutPayingCostEffect` earns no tag of its own (only the free cast is a
 * [IntentTag.FREE_SPELL]).
 */
class PaidGraveyardCastReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("casting a target graveyard card, paying its costs, reads as recursion") {
            for (name in listOf("Harness the Storm", "Chandra, Flame's Catalyst")) {
                withClue(name) {
                    intents.forName(name)!!.tags shouldContain IntentTag.RECURSION
                    AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true
                }
            }
        }

        test("granting a this-turn cast of a target graveyard card, paying its costs, reads as recursion") {
            // "You may cast target instant or sorcery card from your graveyard this turn. If that
            // spell would be put into your graveyard, exile it instead": a GrantMayPlayFromExile over
            // the chosen graveyard card rather than a cast on resolution — Vohar's sacrifice ability,
            // and Jace, Telepath Unbound's −3 on the back of Jace, Vryn's Prodigy.
            intents.forName("Vohar, Vodalian Desecrator")!!.tags shouldContain IntentTag.RECURSION
            for (name in listOf("Vohar, Vodalian Desecrator", "Jace, Vryn's Prodigy")) {
                withClue(name) {
                    AiReadingAudit.read(cardRegistry.getCard(name)!!).readFully shouldBe true
                }
            }
        }
    }
}
