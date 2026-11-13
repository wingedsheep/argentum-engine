package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardIntentAnalyzer
import com.wingedsheep.ai.engine.knowledge.IntentTag
import com.wingedsheep.ai.engine.knowledge.TargetPolarity
import com.wingedsheep.ai.engine.knowledge.TargetPolarityAnalyzer
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Inside an iteration over opponents `you` is the opponent visited, so an emblem made there is
 * theirs and its "you lose 2 life" drains them: Ob Nixilis Reignited's −8 ("target opponent gets
 * an emblem with 'Whenever a player draws a card, you lose 2 life'") is a clock on the opponent,
 * not an ability the AI has no intent for. A gain under the same iteration stays a gift.
 */
class OpponentEmblemLossReadingAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("an emblem handed to an opponent that costs them life reads as damage to players") {
            val minusEight = card("Ob Nixilis Reignited").script.activatedAbilities
                .single { it.description.startsWith("Target opponent gets an emblem") }
            CardIntentAnalyzer.analyzeEffect(minusEight.effect).tags shouldContain IntentTag.DAMAGE_TO_PLAYERS
        }

        test("the targeted opponent who gets the draining emblem is a harmful slot") {
            val minusEight = card("Ob Nixilis Reignited").script.activatedAbilities
                .single { it.description.startsWith("Target opponent gets an emblem") }
            TargetPolarityAnalyzer.forRequirements(minusEight.effect, minusEight.targetRequirements) shouldBe
                listOf(TargetPolarity.HARMFUL)
        }

        test("an opponent's own gain under 'each opponent' is still not ours") {
            CardIntentAnalyzer.analyze(card("Old-Growth Dryads")).tags shouldNotContain IntentTag.DAMAGE_TO_PLAYERS
        }
    }
}
