package com.wingedsheep.ai.engine

import com.wingedsheep.ai.audit.AiReadingAudit
import com.wingedsheep.ai.engine.deck.CommanderDeckRoles
import com.wingedsheep.ai.engine.deck.DeckRole
import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * "Exile the top card of each player's library, then you may cast any number of spells from among
 * those cards without paying their mana costs" (Etali, Primal Storm): spells cast off a library
 * top on resolution are cards on top of the ones in hand — the impulse draw with its window closed
 * at resolution. It reads as card draw, so a repeatable one fills a Commander deck's draw slot.
 */
class FreeCastFromLibraryTopReadingAiTest : ScenarioTestBase() {
    init {
        test("casting spells exiled off a library top reads as card draw") {
            for (name in listOf("Etali, Primal Storm", "Kotis, the Fangkeeper", "Captain Benjamin Sisko")) {
                val card = cardRegistry.getCard(name)!!
                withClue(name) {
                    CardIntentAnalyzer.analyze(card).tags shouldContain IntentTag.DRAW
                    AiReadingAudit.audit(card).map { it.key }
                        .filter { "CastAnyNumberFromCollection" in it || "TopOfLibrary" in it } shouldBe emptyList()
                }
            }
        }

        test("Etali attacking every turn fills a Commander deck's card-draw role") {
            CommanderDeckRoles.rolesOf(cardRegistry.getCard("Etali, Primal Storm")!!) shouldContain DeckRole.DRAW
        }

        test("a free cast from a graveyard is no draw off the library") {
            // Kefka casts from graveyards: recursion's question, not this reading's.
            val kefka = cardRegistry.getCard("Kefka, Dancing Mad")!!
            AiReadingAudit.audit(kefka).map { it.key } shouldContain "intent: CastAnyNumberFromCollectionWithoutPayingCostEffect"
            CommanderDeckRoles.rolesOf(kefka) shouldNotContain DeckRole.DRAW
        }
    }
}
