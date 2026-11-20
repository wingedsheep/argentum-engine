package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.IntentCatalog
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * A card that only works through our creatures carrying a +1/+1 counter cashes in the deck's
 * counters: a trigger watching such a creature (Bred for the Hunt) and a spell that can only target
 * one (Mutant's Prey) both read as [SynergyAxis.PLUS_COUNTERS] payoffs, so the drafter and deck
 * builder pair them with the cards that put counters down.
 */
class PlusCounterCarrierSynergyAiTest : ScenarioTestBase() {

    private val intents by lazy { IntentCatalog.of(cardRegistry) }

    private fun axes(name: String) = SynergyAxes.of(intents.definition(name)!!)

    init {
        test("a trigger watching our creatures with a +1/+1 counter rewards the counters axis") {
            axes("Bred for the Hunt").rewards shouldContain SynergyAxis.PLUS_COUNTERS
            axes("Bred for the Hunt").sharedWith(axes("Battlegrowth")) shouldContain SynergyAxis.PLUS_COUNTERS
        }

        test("a spell that can only target our creature with a +1/+1 counter rewards the counters axis") {
            axes("Mutant's Prey").rewards shouldContain SynergyAxis.PLUS_COUNTERS
        }

        test("the same trigger over any creature we control is no counters payoff") {
            axes("Starwinder").rewards shouldNotContain SynergyAxis.PLUS_COUNTERS
        }
    }
}
