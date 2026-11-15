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
 * A self-zone cast permission's price change (`MayCastSelfFromZones.costModification`) reads the
 * graveyard only to *tax* the cast — Risen Executioner pays {1} more for each other creature card
 * there — so it must not make the card a graveyard payoff. The permission's own additional cost
 * still does when it spends the graveyard (Squee, Dubious Monarch exiles four other cards).
 */
class ZonePermissionTaxReadingAiTest : FunSpec({

    fun card(name: String) = MtgSetCatalog.all.flatMap { it.cards }.first { it.name == name }

    test("Risen Executioner's graveyard tax is not a graveyard payoff") {
        val definition = card("Risen Executioner")
        GraveyardSynergy.isPayoff(definition) shouldBe false
        SynergyAxes.of(definition).rewards shouldNotContain SynergyAxis.GRAVEYARD
    }

    test("a permission whose additional cost exiles graveyard cards is still a payoff") {
        val definition = card("Squee, Dubious Monarch")
        GraveyardSynergy.isPayoff(definition) shouldBe true
        SynergyAxes.of(definition).rewards shouldContain SynergyAxis.GRAVEYARD
    }
})
