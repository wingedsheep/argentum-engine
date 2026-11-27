package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.avr.cards.DualCasting
import com.wingedsheep.mtg.sets.definitions.avr.cards.GuiseOfFire
import com.wingedsheep.mtg.sets.definitions.avr.cards.WildDefiance
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * Two spells-axis payoffs the deckbuilder couldn't see: a trigger on our creatures becoming the
 * target of an instant or sorcery (Wild Defiance), and a spell-copy ability an Aura hands to our
 * creature (Dual Casting). Both do nothing until the deck casts instants and sorceries.
 */
class SpellTargetAndGrantedCopySynergyAxesTest : FunSpec({

    test("a trigger on our creatures becoming the target of an instant or sorcery rewards spells") {
        SynergyAxes.of(WildDefiance).rewards shouldContain SynergyAxis.SPELLS
    }

    test("a spell-copy ability granted to the enchanted creature rewards spells") {
        SynergyAxes.of(DualCasting).rewards shouldContain SynergyAxis.SPELLS
    }

    test("an Aura granting no spell-copy ability rewards no spells") {
        SynergyAxes.of(GuiseOfFire).rewards shouldNotContain SynergyAxis.SPELLS
    }
})
