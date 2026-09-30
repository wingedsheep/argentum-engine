package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.mtg.sets.definitions.s99.Starter1999Set
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Island158
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Island159
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Island160
import com.wingedsheep.mtg.sets.definitions.s99.cards.Starter1999Island161
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class S99IslandScenarioTest : FunSpec({
    test("Starter 1999 exposes every Island art with its own collector number") {
        val islands = Starter1999Set.basicLands.filter { it.name == "Island" }
        islands.map { it.metadata.collectorNumber }.toSet() shouldBe setOf("158", "159", "160", "161")
        islands.map { it.metadata.imageUri }.toSet().size shouldBe 4
    }

    listOf(
        Starter1999Island158,
        Starter1999Island159,
        Starter1999Island160,
        Starter1999Island161
    ).forEach { card ->
        test("Island ${card.metadata.collectorNumber} enters untapped and immediately produces blue mana") {
            val driver = GameTestDriver()
            driver.registerCard(card)
            driver.initMirrorMatch(deck = Deck.of("Island" to 40))
            driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
            val player = driver.activePlayer!!
            val island = driver.putCardInHand(player, "Island")

            driver.submitSuccess(PlayLand(player, island))
            driver.isTapped(island) shouldBe false

            val activation = ActivateAbility(player, island, card.script.activatedAbilities.single().id)
            driver.submitSuccess(activation)

            driver.isTapped(island) shouldBe true
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.blue shouldBe 1
            driver.state.stack.isEmpty() shouldBe true
            driver.pendingDecision shouldBe null

            driver.submitExpectFailure(activation)
            driver.state.getEntity(player)?.get<ManaPoolComponent>()?.blue shouldBe 1
        }
    }
})
