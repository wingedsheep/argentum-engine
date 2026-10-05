package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaSwamp290
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaSwamp291
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaSwampScenarioTest : FunSpec({
    val variants = listOf(
        AlphaSwamp290,
        AlphaSwamp291
    )

    test("set discovery exposes both Alpha Swamp arts") {
        val discovered = AlphaSet.basicLands.filter { it.name == "Swamp" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("290", "291")
        discovered.size shouldBe 2
        discovered.forEach { it.setCode shouldBe "LEA" }
    }

    variants.forEach { swamp ->
        test("Alpha Swamp ${swamp.metadata.collectorNumber} taps for black mana immediately") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all.filter { it.name != "Swamp" })
            driver.registerCard(swamp)
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val player = driver.activePlayer!!
            val land = driver.putPermanentOnBattlefield(player, "Swamp")
            val result = driver.submit(
                ActivateAbility(player, land, swamp.activatedAbilities.single().id)
            )

            result.outcome shouldBe Outcome.Done
            driver.isTapped(land) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.black shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
        }
    }
})
