package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.aer.cards.CallForUnity
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * Revolt — "if a permanent you controlled left the battlefield this turn" as a trigger's
 * intervening-if — pays off what the death axis feeds (a sac outlet, a Servo or Clue cashed in), so
 * the deckbuilder and the in-game evaluator see a revolt card's fuel beside it.
 */
class RevoltSynergyAxesTest : FunSpec({

    test("a revolt trigger rewards the death axis") {
        SynergyAxes.of(CallForUnity).rewards shouldContain SynergyAxis.DEATH
    }
})
