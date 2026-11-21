package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.engine.support.ScenarioTestBase
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Whenever an instant or sorcery spell you control deals damage" (Blaze Commando's
 * per-instruction observer, Satyr Firedancer) is paid off by the deck's burn, so it rewards the
 * SPELLS axis that Lightning Bolt feeds. A creature-sourced damage observer doesn't.
 */
class SpellDamageObserverSynergyAiTest : ScenarioTestBase() {

    private fun axes(card: String) = SynergyAxes.of(cardRegistry.requireCard(card))

    init {
        test("spell-damage observers reward instants and sorceries") {
            listOf("Blaze Commando", "Satyr Firedancer").forEach { card ->
                withClue(card) { axes(card).rewards shouldContain SynergyAxis.SPELLS }
            }
            axes("Lightning Bolt").feeds shouldContain SynergyAxis.SPELLS
        }

        test("a creature-damage observer is not a spells payoff") {
            axes("Grizzly Bears").rewards shouldNotContain SynergyAxis.SPELLS
        }
    }
}
