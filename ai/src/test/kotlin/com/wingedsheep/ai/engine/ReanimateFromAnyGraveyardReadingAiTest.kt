package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Put a creature card from a graveyard onto the battlefield under your control" (Entering, the right
 * half of Breaking // Entering) is a pipeline gather over **every** graveyard. It can bring back our own
 * creature as well as take an opponent's, so it reads as recursion — the same reading the single-card
 * "target creature card from a graveyard" return (Rise from the Grave) already gets. The pipeline
 * regrowth reader used to accept only a gather from our own graveyard, which left Entering reading as
 * mill plus a haste grant. Every graveyard returned under its owners' control (Living Death) stays a
 * symmetric reset, not a recursion.
 */
class ReanimateFromAnyGraveyardReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("reanimating a creature card from any graveyard onto our battlefield is recursion") {
        withClue("Entering's any-graveyard gather moved onto our battlefield reads as recursion") {
            CardIntentAnalyzer.analyze(card("Breaking // Entering")).tags shouldContain IntentTag.RECURSION
        }
        withClue("the single-card spelling of the same return already read as recursion") {
            CardIntentAnalyzer.analyze(card("Rise from the Grave")).tags shouldContain IntentTag.RECURSION
        }
        withClue("Living Death returns every graveyard to its owners: a reset, not a recursion") {
            CardIntentAnalyzer.analyze(card("Living Death")).tags shouldNotContain IntentTag.RECURSION
        }
    }
})
