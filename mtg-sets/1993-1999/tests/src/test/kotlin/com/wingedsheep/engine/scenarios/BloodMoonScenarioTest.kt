package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.drk.cards.BloodMoon
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.CardScript
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

class BloodMoonScenarioTest : FunSpec({

    val tropicalIsland = CardDefinition(
        name = "Tropical Island",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype("Island"), Subtype("Forest")),
        ),
        script = CardScript(),
    )
    val forest = basicLand("Forest") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(BloodMoon, tropicalIsland, forest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    test("turns nonbasic lands into Mountains while leaving basic lands unchanged") {
        val (driver, player) = newGame()
        val opponent = driver.state.getOpponents(player).single()
        driver.putPermanentOnBattlefield(player, "Blood Moon")
        val tropical = driver.putLandOnBattlefield(player, "Tropical Island")
        val opponentsTropical = driver.putLandOnBattlefield(opponent, "Tropical Island")
        val basicForest = driver.putLandOnBattlefield(player, "Forest")

        driver.state.projectedState.hasSubtype(tropical, "Mountain").shouldBeTrue()
        driver.state.projectedState.hasSubtype(tropical, "Island") shouldBe false
        driver.state.projectedState.hasSubtype(tropical, "Forest") shouldBe false
        driver.state.projectedState.hasSubtype(opponentsTropical, "Mountain").shouldBeTrue()
        driver.state.projectedState.hasSubtype(opponentsTropical, "Island") shouldBe false
        driver.state.projectedState.hasSubtype(basicForest, "Forest").shouldBeTrue()
        driver.state.projectedState.hasSubtype(basicForest, "Mountain") shouldBe false

        driver.submitSuccess(
            ActivateAbility(player, tropical, AbilityId.intrinsicMana('R'))
        )
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.red shouldBe 1
    }
})
