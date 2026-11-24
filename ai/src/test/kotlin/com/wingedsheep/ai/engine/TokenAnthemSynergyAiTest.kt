package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.definitions.isd.cards.IntangibleVirtue
import com.wingedsheep.mtg.sets.definitions.rtr.cards.PhantomGeneral
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain

/**
 * "Creature tokens you control get +1/+1" (Phantom General, Intangible Virtue): the anthem reaches
 * only the tokens the deck makes, so it reads as a payoff for the tokens axis.
 */
class TokenAnthemSynergyAiTest : FunSpec({

    test("an anthem for your creature tokens rewards the tokens axis") {
        SynergyAxes.of(PhantomGeneral).rewards shouldContain SynergyAxis.TOKENS
        SynergyAxes.of(IntangibleVirtue).rewards shouldContain SynergyAxis.TOKENS
    }

    test("an anthem for every creature you control is not a tokens payoff") {
        val gloriousAnthem = com.wingedsheep.mtg.sets.definitions.usg.cards.GloriousAnthem
        SynergyAxes.of(gloriousAnthem).rewards shouldNotContain SynergyAxis.TOKENS
    }
})
