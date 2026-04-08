package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.YgraEaterOfAll
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class YgraEaterOfAllTest : FunSpec({

    val Bear = CardDefinition.creature(
        name = "Plain Bear",
        manaCost = ManaCost.parse("{1}{G}"),
        subtypes = setOf(Subtype("Bear")),
        power = 2,
        toughness = 2,
        oracleText = ""
    )

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(YgraEaterOfAll, Bear))
        return driver
    }

    test("opponent's creatures become Food artifacts too") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        val opponent = driver.getOpponent(active)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")
        val opponentBear = driver.putCreatureOnBattlefield(opponent, "Plain Bear")

        val projected = StateProjector().project(driver.state)
        projected.getSubtypes(opponentBear).contains("Food") shouldBe true
        projected.getTypes(opponentBear).contains("ARTIFACT") shouldBe true
    }

    test("Ygra itself is not a Food artifact") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 20, "Swamp" to 20))
        val active = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ygra = driver.putCreatureOnBattlefield(active, "Ygra, Eater of All")

        val projected = StateProjector().project(driver.state)
        projected.getSubtypes(ygra).contains("Food") shouldBe false
        projected.getTypes(ygra).contains("ARTIFACT") shouldBe false
    }
})
