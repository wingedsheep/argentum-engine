package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.doubles.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

/**
 * [com.wingedsheep.sdk.scripting.MayCastSelfFromZones] — a card's own "you may cast this card from
 * your graveyard / from exile" (Squee, the Immortal; Eternal Scourge) reads as recursion: the card
 * comes back to be cast again. Unread, the static left both cards "abilities not recognised" and
 * the draft heuristic priced them as their bare bodies.
 */
class SelfRecastReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("a self cast-from-zone permission reads as recursion and earns a heuristic term") {
            listOf("Squee, the Immortal", "Eternal Scourge").forEach { name ->
                withClue(name) {
                    CardIntentAnalyzer.analyze(card(name)).tags shouldContain IntentTag.RECURSION
                    val terms = LimitedCardRater.explain(card(name)).heuristic
                    withClue("terms: $terms") {
                        terms.single { it.part == "Static ability 1" }.value shouldBeGreaterThan 0.0
                    }
                }
            }
        }

        test("the draft heuristic prices Eternal Scourge above the same body without the permission") {
            val scourge = card("Eternal Scourge")
            val withoutRecast = scourge.copy(name = "Eternal Scourge (no recast)", script = scourge.script.copy(staticAbilities = emptyList()))
            val rated = LimitedCardRater.explain(scourge).heuristicRating
            val bare = LimitedCardRater.explain(withoutRecast).heuristicRating
            withClue("with the permission $rated vs without $bare") {
                (rated > bare) shouldBe true
            }
        }
    }
}
