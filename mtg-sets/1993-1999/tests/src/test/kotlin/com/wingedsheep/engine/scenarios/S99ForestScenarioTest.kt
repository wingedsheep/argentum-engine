package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.Outcome
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.s99.Starter1999Set
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Forest170
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Forest171
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Forest172
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Forest173
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class S99ForestScenarioTest : FunSpec({
    val variants = listOf(
        Starter1999Forest170,
        Starter1999Forest171,
        Starter1999Forest172,
        Starter1999Forest173
    )

    test("set discovery exposes all four Starter 1999 Forest arts") {
        val discovered = Starter1999Set.basicLands.filter { it.name == "Forest" }
        discovered.map { it.metadata.collectorNumber }.toSet() shouldBe
            setOf("170", "171", "172", "173")
        discovered.size shouldBe 4
        discovered.forEach { it.setCode shouldBe "S99" }
    }

    variants.forEach { forest ->
        test("Starter 1999 Forest ${forest.metadata.collectorNumber} taps for green mana immediately") {
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
