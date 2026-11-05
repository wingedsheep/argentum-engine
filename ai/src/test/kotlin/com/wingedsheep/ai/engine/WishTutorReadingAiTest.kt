package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A wish — a card fetched from outside the game (our sideboard) into our hand — reads as a tutor:
 * the same search for the card we need, from a different zone. Burning Wish picks the card itself;
 * Extrapolate the Impossible reveals two and an opponent picks which one we get, the second step
 * re-gathering the revealed pair by name.
 *
 * Unread, both cards carried no intent tag at all ("abilities not recognised").
 */
class WishTutorReadingAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    init {
        test("a wish into our hand reads as a tutor") {
            listOf("Burning Wish", "Extrapolate the Impossible").forEach { name ->
                withClue("$name fetches a card from outside the game into our hand") {
                    intents.forName(name)!!.tags shouldContain IntentTag.TUTOR
                }
            }
        }

        test("a library search that hands the opponent the card is not our tutor") {
            // Control: the re-gather step only carries a wished collection on; it doesn't turn
            // an opponent's hand into ours.
            withClue("Divination draws, it doesn't wish") {
                intents.forName("Divination")!!.tags shouldNotContain IntentTag.TUTOR
            }
        }
    }
}
