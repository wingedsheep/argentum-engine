package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.bng.cards.EpharaGodOfThePolis
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "If you had another creature enter the battlefield under your control last turn" as a trigger's
 * intervening-if (Ephara, God of the Polis) pays off what the creatures-enter axis feeds, so the
 * deckbuilder sees the creatures beside it as it does for a "whenever a creature you control
 * enters" trigger.
 */
class CreaturesEnteredGateSynergyAxesTest : FunSpec({

    test("a trigger gated on creatures entering under your control last turn rewards creatures entering") {
        SynergyAxes.of(EpharaGodOfThePolis).rewards shouldContain SynergyAxis.CREATURES_ENTER
    }
})
