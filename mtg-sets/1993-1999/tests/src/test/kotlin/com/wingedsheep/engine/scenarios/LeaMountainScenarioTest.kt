package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaMountain292
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaMountain293
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaMountainScenarioTest : FunSpec({
    val variants = listOf(
        AlphaMountain292,
        AlphaMountain293
    )

    test("set discovery exposes both Alpha Mountain arts") {
        val discovered = AlphaSet.basicLands.filter { it.name == "Mountain" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("292", "293")
        discovered.size shouldBe 2
        discovered.forEach { it.setCode shouldBe "LEA" }
    }

    variants.forEach { mountain ->
        test("Alpha Mountain ${mountain.metadata.collectorNumber} taps for red mana immediately") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all.filter { it.name != "Mountain" })
            driver.registerCard(mountain)
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val player = driver.activePlayer!!
            val land = driver.putPermanentOnBattlefield(player, "Mountain")
            val result = driver.submit(
                ActivateAbility(player, land, mountain.activatedAbilities.single().id)
            )

            result.outcome shouldBe Outcome.Done
            driver.isTapped(land) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.red shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
        }
    }
})
