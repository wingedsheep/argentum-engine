package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe

/**
 * "Exile X / one or more creature cards from your graveyard" as an activation cost
 * (`AbilityCost.ExileXFromGraveyard`) spends a stocked graveyard as fuel — Corpseweft's token is
 * twice the number exiled, Winter, Cursed Rider's sweep and Necropolis Fiend's shrink are X. So the
 * card is a graveyard payoff, the variable-count twin of a `CostAtom.ExileFrom(GRAVEYARD)` cost.
 */
class ExileXGraveyardCostReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    listOf("Corpseweft", "Winter, Cursed Rider", "Necropolis Fiend").forEach { name ->
        test("$name's exile-X graveyard cost makes it a graveyard payoff") {
            val definition = card(name)
            GraveyardSynergy.isPayoff(definition) shouldBe true
            SynergyAxes.of(definition).rewards shouldContain SynergyAxis.GRAVEYARD
        }
    }
})
