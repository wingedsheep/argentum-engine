package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.emn.cards.NahirisWrath
import com.wingedsheep.mtg.sets.definitions.iko.cards.ChanneledForce
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * A spell's "as an additional cost, discard X cards" puts our cards into the graveyard just as a
 * discard effect or an activated ability's discard cost does, so it feeds the graveyard axis — the
 * deckbuilder sees Nahiri's Wrath as fuel beside delirium and flashback payoffs.
 */
class DiscardAdditionalCostSynergyAxesTest : FunSpec({

    test("a discard-X additional cost feeds the graveyard axis") {
        SynergyAxes.of(NahirisWrath).feeds shouldContain SynergyAxis.GRAVEYARD
        SynergyAxes.of(ChanneledForce).feeds shouldContain SynergyAxis.GRAVEYARD
    }
})
