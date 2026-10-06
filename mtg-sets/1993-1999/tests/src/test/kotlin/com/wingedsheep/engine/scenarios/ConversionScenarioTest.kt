package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.lea.cards.Conversion
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

/**
 * Conversion's filter ("Mountains") stops matching the moment its own type change applies. The
 * type change and the ability loss are one multi-layer ability, so the set of affected lands must
 * be locked when it starts applying (CR 613.6): every Mountain becomes only a Plains and taps for {W}.
 */
class ConversionScenarioTest : FunSpec({

    val taiga = CardDefinition(
        name = "Taiga",
        manaCost = ManaCost.ZERO,
        typeLine = TypeLine(
            cardTypes = setOf(CardType.LAND),
            subtypes = setOf(Subtype("Mountain"), Subtype("Forest")),
        ),
        script = CardScript(),
    )
    val mountain = basicLand("Mountain") {}
    val forest = basicLand("Forest") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(Conversion, taiga, mountain, forest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    test("every Mountain becomes only a Plains and taps for white; other lands are untouched") {
        val (driver, player) = newGame()
        val opponent = driver.state.getOpponents(player).single()
        driver.putPermanentOnBattlefield(player, "Conversion")
        val basicMountain = driver.putLandOnBattlefield(player, "Mountain")
        val opponentsTaiga = driver.putLandOnBattlefield(opponent, "Taiga")
        val basicForest = driver.putLandOnBattlefield(player, "Forest")

        val projected = driver.state.projectedState
        projected.hasSubtype(basicMountain, "Plains").shouldBeTrue()
        projected.hasSubtype(basicMountain, "Mountain") shouldBe false
        projected.hasSubtype(opponentsTaiga, "Plains").shouldBeTrue()
        projected.hasSubtype(opponentsTaiga, "Mountain") shouldBe false
        projected.hasSubtype(opponentsTaiga, "Forest") shouldBe false
        projected.hasSubtype(basicForest, "Forest").shouldBeTrue()
        projected.hasSubtype(basicForest, "Plains") shouldBe false

        driver.submitSuccess(
            ActivateAbility(player, basicMountain, AbilityId.intrinsicMana('W'))
        )
        val pool = driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()
        pool.white shouldBe 1
        pool.red shouldBe 0
    }
})
