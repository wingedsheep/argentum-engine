package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mh3.cards.HarbingerOfTheSeas
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

class HarbingerOfTheSeasScenarioTest : FunSpec({

    val volcanicIsland = CardDefinition(
        name = "Test Volcanic Island",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype("Mountain"), Subtype("Island")),
        ),
        script = CardScript(),
    )
    val badlands = CardDefinition(
        name = "Test Badlands",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype("Swamp"), Subtype("Mountain")),
        ),
        script = CardScript(),
    )
    val forest = basicLand("Forest") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(HarbingerOfTheSeas, volcanicIsland, badlands, forest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    test("turns every nonbasic land into an Island that taps for blue, leaving basics alone") {
        val (driver, player) = newGame()
        val opponent = driver.state.getOpponents(player).single()
        driver.putCreatureOnBattlefield(player, "Harbinger of the Seas")
        val badlandsId = driver.putLandOnBattlefield(player, "Test Badlands")
        val opponentsVolcanic = driver.putLandOnBattlefield(opponent, "Test Volcanic Island")
        val basicForest = driver.putLandOnBattlefield(player, "Forest")

        val projected = driver.state.projectedState
        projected.hasSubtype(opponentsVolcanic, "Island").shouldBeTrue()
        projected.hasSubtype(opponentsVolcanic, "Mountain") shouldBe false
        projected.hasSubtype(badlandsId, "Island").shouldBeTrue()
        projected.hasSubtype(badlandsId, "Swamp") shouldBe false
        projected.hasSubtype(badlandsId, "Mountain") shouldBe false
        projected.hasSubtype(basicForest, "Forest").shouldBeTrue()
        projected.hasSubtype(basicForest, "Island") shouldBe false

        driver.submitSuccess(ActivateAbility(player, badlandsId, AbilityId.intrinsicMana('U')))
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.blue shouldBe 1
    }

    test("lands get their own types back once the Harbinger leaves") {
        val (driver, player) = newGame()
        val harbinger = driver.putCreatureOnBattlefield(player, "Harbinger of the Seas")
        val badlandsId = driver.putLandOnBattlefield(player, "Test Badlands")
        driver.state.projectedState.hasSubtype(badlandsId, "Island").shouldBeTrue()

        driver.moveToGraveyard(harbinger)

        driver.state.projectedState.hasSubtype(badlandsId, "Island") shouldBe false
        driver.state.projectedState.hasSubtype(badlandsId, "Swamp").shouldBeTrue()
    }
})
