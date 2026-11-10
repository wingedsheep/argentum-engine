package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.soi.cards.NahiriTheHarbinger
import com.wingedsheep.mtg.sets.definitions.soi.cards.OliviaMobilizedForWar
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "You may discard a card. If you do, …" is a gate we decide: its discard bins our cards the way
 * an additional-cost discard does, and its payoff feeds what it makes — so the deckbuilder sees
 * Nahiri's rummage beside graveyard payoffs and Olivia's counters beside +1/+1-counter payoffs.
 */
class GatedPayoffSynergyAxesTest : FunSpec({

    test("a gated discard feeds the graveyard axis") {
        SynergyAxes.of(NahiriTheHarbinger).feeds shouldContain SynergyAxis.GRAVEYARD
    }

    test("a gated payoff feeds what it makes, and its discard the graveyard") {
        val feeds = SynergyAxes.of(OliviaMobilizedForWar).feeds
        feeds shouldContain SynergyAxis.PLUS_COUNTERS
        feeds shouldContain SynergyAxis.GRAVEYARD
    }
})
