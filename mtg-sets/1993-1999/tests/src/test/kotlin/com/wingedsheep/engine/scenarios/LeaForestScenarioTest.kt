package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.AlphaSet
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaForest294
import com.wingedsheep.mtg.sets.definitions.lea.cards.AlphaForest295
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class LeaForestScenarioTest : FunSpec({
    val variants = listOf(
        AlphaForest294,
        AlphaForest295
    )

    test("set discovery exposes both Alpha Forest arts") {
        val discovered = AlphaSet.basicLands.filter { it.name == "Forest" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("294", "295")
        discovered.size shouldBe 2
        discovered.forEach { it.setCode shouldBe "LEA" }
    }

    variants.forEach { forest ->
        test("Alpha Forest ${forest.metadata.collectorNumber} taps for green mana immediately") {
            val driver = GameTestDriver()
            driver.registerCards(TestCards.all.filter { it.name != "Forest" })
            driver.registerCard(forest)
            driver.initMirrorMatch(deck = Deck.of("Plains" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

            val player = driver.activePlayer!!
            val land = driver.putPermanentOnBattlefield(player, "Forest")
            val result = driver.submit(
                ActivateAbility(player, land, forest.activatedAbilities.single().id)
            )

            result.outcome shouldBe Outcome.Done
            driver.isTapped(land) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.green shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
        }
    }
})
