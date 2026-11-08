package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.j25.cards.FumulusTheInfestation
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Whenever a player sacrifices a nontoken creature" watches our own sacrifices as well as the
 * opponents', so it pays off the death axis the deck's sac outlets feed — like "whenever you
 * sacrifice" does — and the deckbuilder sees Fumulus beside those outlets.
 */
class AnyPlayerSacrificeSynergyAxesTest : FunSpec({

    test("a trigger on any player's sacrifice rewards the death axis") {
        SynergyAxes.of(FumulusTheInfestation).rewards shouldContain SynergyAxis.DEATH
    }
})
