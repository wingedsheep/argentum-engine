package com.wingedsheep.ai.engine

import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.doubles.shouldBeGreaterThanOrEqual

/**
 * "Exile up to one target instant or sorcery card from your graveyard. You may cast that card until
 * the end of your next turn" (Scholar of Combustion): the intent readers called the pipeline
 * recursion, but the draft heuristic priced it at zero — a gather, an exile and a cast grant are
 * each worth nothing alone — so the card rated as its body. The exiled-and-recast graveyard card is
 * now priced as the recursion it is, the way a reanimation pipeline (Boneyard Parley) already was.
 */
class GraveyardRecastRatingAiTest : ScenarioTestBase() {

    private fun triggerTerm(name: String) =
        LimitedCardRater.explain(cardRegistry.getCard(name)!!).heuristic

    init {
        test("Scholar of Combustion's exile-and-recast trigger is priced as recursion") {
            val terms = triggerTerm("Scholar of Combustion")
            withClue("$terms") {
                terms.single { it.part == "Triggered ability 1" }.value shouldBeGreaterThanOrEqual 0.55
            }
        }

        test("Practiced Scrollsmith's exile-and-recast trigger is priced as recursion") {
            val terms = triggerTerm("Practiced Scrollsmith")
            withClue("$terms") {
                terms.single { it.part == "Triggered ability 1" }.value shouldBeGreaterThanOrEqual 0.55
            }
        }
    }
}
