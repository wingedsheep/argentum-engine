package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A move ([com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect]) whose target requirement is a
 * card in a graveyard and whose `fromZone` is left unsaid — `Effects.Move(target, Zone.BATTLEFIELD)`
 * over "target creature card from your graveyard" — is recursion. The intent reader only knew it
 * when the move spelled the zone out, so Reaping Willow, Dread Return and the Lorwyn Eclipsed
 * reflexive reanimators ("you may tap another Merfolk. When you do, return target creature card …")
 * read as doing nothing at all. The zone comes from the requirement, as the polarity reader's
 * recursion slot already reads it.
 *
 * No decision in today's AI turns on these cards' RECURSION tag (they are sorcery-speed, and
 * AmbushWindow's "hands us something" floor only reads flash cards), so the reading is pinned here.
 */
class GraveyardTargetMoveReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun tags(card: String) = intents.forName(card)!!.tags

    init {
        test("a move out of a graveyard target, onto the battlefield or into a hand, reads as recursion") {
            withClue("Reaping Willow: return target creature card from your graveyard to the battlefield") {
                tags("Reaping Willow") shouldContain IntentTag.RECURSION
            }
            withClue("Dread Return: return target creature card from your graveyard to the battlefield") {
                tags("Dread Return") shouldContain IntentTag.RECURSION
            }
            withClue("Meanders Guide: a reflexive trigger's own graveyard target") {
                tags("Meanders Guide") shouldContain IntentTag.RECURSION
            }
            withClue("Slumbering Walker: a reflexive trigger's own graveyard target") {
                tags("Slumbering Walker") shouldContain IntentTag.RECURSION
            }
        }

        test("exiling a graveyard card is not recursion") {
            withClue("Cling to Dust exiles target card from a graveyard: graveyard hate, not a re-buy") {
                tags("Cling to Dust") shouldNotContain IntentTag.RECURSION
            }
        }
    }
}
