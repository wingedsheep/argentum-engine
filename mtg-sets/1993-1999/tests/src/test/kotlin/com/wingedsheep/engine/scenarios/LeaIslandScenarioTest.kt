package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaIsland288
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaIsland289
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaIslandScenarioTest : FunSpec({
    val variants = listOf(
        AlphaIsland288,
        AlphaIsland289
    )

    test("set discovery exposes both Alpha Island arts") {
        val discovered = AlphaSet.basicLands.filter { it.name == "Island" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("288", "289")
        discovered.size shouldBe 2
        discovered.forEach { it.setCode shouldBe "LEA" }
    }

    variants.forEach { island ->
        test("Alpha Island ${island.metadata.collectorNumber} taps for blue mana immediately") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all.filter { it.name != "Island" })
            driver.registerCard(island)
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val player = driver.activePlayer!!
            val land = driver.putPermanentOnBattlefield(player, "Island")
            val result = driver.submit(
                ActivateAbility(player, land, island.activatedAbilities.single().id)
            )

            result.outcome shouldBe Outcome.Done
            driver.isTapped(land) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.blue shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
        }
    }
})
