package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.s99.Starter1999Set
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Swamp162
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Swamp163
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Swamp164
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Swamp165
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class S99SwampScenarioTest : FunSpec({
    test("Starter 1999 exposes every Swamp art with its own collector number") {
        val swamps = Starter1999Set.basicLands.filter { it.name == "Swamp" }
        swamps.map { it.metadata.collectorNumber }.toSet() shouldBe setOf("162", "163", "164", "165")
        swamps.map { it.metadata.imageUri }.toSet().size shouldBe 4
        swamps.map { it.setCode }.toSet() shouldBe setOf("S99")
    }

    listOf(
        Starter1999Swamp162,
        Starter1999Swamp163,
        Starter1999Swamp164,
        Starter1999Swamp165
    ).forEach { card ->
        test("Swamp ${card.metadata.collectorNumber} enters untapped and immediately produces black mana") {
            val driver = GameTestDriver()
            driver.registerCard(card)
            driver.initMirrorMatch(deck = Deck.of("Swamp" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = driver.activePlayer!!
            val swamp = driver.putCardInHand(player, "Swamp")

            driver.submitSuccess(PlayLand(player, swamp))
            driver.isTapped(swamp) shouldBe false

            val activation = ActivateAbility(player, swamp, card.script.activatedAbilities.single().id)
            driver.submitSuccess(activation)

            driver.isTapped(swamp) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.black shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
            driver.pendingDecision shouldBe null

            driver.submitExpectFailure(activation)
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.black shouldBe 1
        }
    }
})
