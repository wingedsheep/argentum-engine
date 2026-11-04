package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiCardRating
import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Two readings the Rivals of Ixalan multicolour batch surfaced:
 *
 *  - A +1/+1 counter a creature puts on **itself** (Resplendent Griffin's "whenever this attacks,
 *    if you have the city's blessing, put a +1/+1 counter on it") needs no intent and every reader
 *    understands it, yet the card was flagged "abilities not recognised" because no tag was placed.
 *    An ability that reads fully no longer counts as unplaced.
 *  - "Put a creature card exiled with this permanent onto the battlefield under your control"
 *    (Tomb of the Dusk Rose) returns a card from the source's linked-exile pile — the same stock a
 *    graveyard return draws on, so it reads as recursion instead of "no intent". Only onto our
 *    battlefield: a hand is its owner's, so returning that pile to hand can give an opponent their
 *    card back (Ashiok's Erasure).
 */
class SelfGrowthAndExilePileReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.distinctBy { it.name }.associateBy { it.name } }

    fun card(name: String) = byName[name] ?: error("'$name' is not in the corpus")

    test("an ability that needs no intent leaves the card recognised") {
        withClue("Resplendent Griffin's +1/+1 counter on itself reads fully as needing no intent") {
            AiCardRating.of(card("Resplendent Griffin"), listOf("RIX")).intent.unrecognised shouldBe false
        }
    }

    test("returning a card from the source's linked-exile pile to our battlefield is recursion") {
        val procession = card("Profane Procession")
        val tomb = procession.backFace ?: error("Profane Procession has no back face")
        CardIntentAnalyzer.analyze(tomb).tags shouldContain IntentTag.RECURSION
        withClue("the Tomb's put-onto-the-battlefield ability now reads fully") {
            AiReadingAudit.read(procession).abilities.filter { it.checked }.all { it.readFully } shouldBe true
        }
        withClue("Ashiok's Erasure hands the exiled spell back to its owner: a release, not a recursion") {
            CardIntentAnalyzer.analyze(card("Ashiok's Erasure")).tags shouldNotContain IntentTag.RECURSION
        }
    }
})
