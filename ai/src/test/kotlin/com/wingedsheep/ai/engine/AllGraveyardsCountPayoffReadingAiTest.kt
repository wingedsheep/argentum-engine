package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * Nighthowler — "This creature and enchanted creature each get +X/+X, where X is the number of
 * creature cards in all graveyards." Lhurgoyf reads the same count as its power.
 *
 * The count spans every graveyard, ours included, so each creature card we bin grows the body: a
 * graveyard payoff, which makes self-mill and discard outlets worth something to the deck holding it.
 */
class AllGraveyardsCountPayoffReadingAiTest : FunSpec({

    val byName by lazy { MtgSetCatalog.all.flatMap { it.cards }.associateBy { it.name } }

    test("a count of creature cards in all graveyards is a graveyard payoff") {
        for (name in listOf("Nighthowler", "Lhurgoyf")) {
            val card = byName.getValue(name)
            GraveyardSynergy.isPayoff(card) shouldBe true
            SynergyAxes.of(card).rewards shouldContain SynergyAxis.GRAVEYARD
        }
    }

    test("a creature with no graveyard count is no graveyard payoff") {
        GraveyardSynergy.isPayoff(byName.getValue("Grizzly Bears")) shouldBe false
    }
})
