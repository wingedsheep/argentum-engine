package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.ogw.cards.PyromancersAssault
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain

/**
 * "Whenever you cast your second spell each turn" (Pyromancer's Assault) pays off a turn of two
 * spells — what the deck's cheap instants and sorceries make possible — so it rewards the spells axis
 * they feed, and the deckbuilder sees it beside them.
 */
class NthSpellCastSynergyAxesTest : FunSpec({

    test("a second-spell trigger rewards the spells axis") {
        SynergyAxes.of(PyromancersAssault).rewards shouldContain SynergyAxis.SPELLS
    }
})
