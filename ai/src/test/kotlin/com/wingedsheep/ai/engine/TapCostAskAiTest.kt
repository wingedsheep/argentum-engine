package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.CardAsks
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.scripting.GameObjectFilter
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * An activation cost that taps a narrow kind of permanent you control — Clock of Omens' "tap two
 * untapped artifacts you control" — asks the deck for that kind, the way Atog's sacrifice cost
 * asks for artifacts. A broad tap cost (Springleaf Drum's "tap an untapped creature you control")
 * is fed by any deck and asks for nothing.
 */
class TapCostAskAiTest : ScenarioTestBase() {

    private fun card(name: String) = cardRegistry.getCard(name) ?: error("$name is not registered")

    init {
        test("tapping two artifacts asks the deck for artifacts") {
            val clock = card("Clock of Omens")
            CardAsks.of(clock).map { it.filter } shouldContain GameObjectFilter.Artifact
            SynergyAxes.of(clock).isPayoff shouldBe true
        }

        test("tapping a creature asks for nothing") {
            CardAsks.of(card("Springleaf Drum")).shouldBeEmpty()
        }
    }
}
