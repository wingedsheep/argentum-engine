package com.wingedsheep.ai.engine

import com.wingedsheep.ai.engine.knowledge.GraveyardSynergy
import com.wingedsheep.ai.engine.knowledge.SynergyAxes
import com.wingedsheep.ai.engine.knowledge.SynergyAxis
import com.wingedsheep.mtg.sets.MtgSetCatalog
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Null Caller — "{3}{B}, Exile a creature card from your graveyard: Create a tapped 2/2 black Zombie
 * creature token." An activation cost that exiles cards from your own graveyard spends a stocked
 * graveyard, so the card is a graveyard payoff: the deck that holds it wants its graveyard filled.
 * The same atom aimed at every player's graveyard (Night Soil's shape) is graveyard hate instead.
 */
class GraveyardExileCostReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    test("an exile-from-your-graveyard activation cost makes it a graveyard payoff") {
        val nullCaller = card("Null Caller")
        GraveyardSynergy.isPayoff(nullCaller) shouldBe true
        SynergyAxes.of(nullCaller).rewards shouldContain SynergyAxis.GRAVEYARD
        GraveyardSynergy.isPayoff(card("Scarred Vinebreeder")) shouldBe true
    }

    test("the same cost over any player's graveyard is not a payoff") {
        val nightSoil = card("Night Soil")
        GraveyardSynergy.isPayoff(nightSoil) shouldBe false
        SynergyAxes.of(nightSoil).rewards shouldNotContain SynergyAxis.GRAVEYARD
    }
})
