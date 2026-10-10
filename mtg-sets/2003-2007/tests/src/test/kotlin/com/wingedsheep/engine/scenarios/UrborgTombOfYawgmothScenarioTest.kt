package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.plc.cards.UrborgTombOfYawgmoth
import com.wingedsheep.sdk.dsl.basicLand
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.AbilityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.shouldBe

/**
 * Urborg, Tomb of Yawgmoth — "Each land is a Swamp in addition to its other land types."
 *
 * First card to *add* a basic land type to a group of lands; the test proves the added Swamp type
 * reaches the projection for every player's lands (Urborg included) and that the intrinsic
 * "{T}: Add {B}" follows from it.
 */
class UrborgTombOfYawgmothScenarioTest : FunSpec({

    val forest = basicLand("Forest") {}

    fun newGame(): Pair<GameTestDriver, EntityId> {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(UrborgTombOfYawgmoth, forest))
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40), startingLife = 20)
        return driver to driver.activePlayer!!
    }

    fun blackIn(driver: GameTestDriver, player: EntityId): Int =
        (driver.state.getEntity(player)?.get<ManaPoolComponent>() ?: ManaPoolComponent()).black

    test("every land on the battlefield is a Swamp in addition to its other types and taps for B") {
        val (driver, player) = newGame()
        val opponent = driver.state.getOpponents(player).single()
        val urborg = driver.putLandOnBattlefield(player, "Urborg, Tomb of Yawgmoth")
        val myForest = driver.putLandOnBattlefield(player, "Forest")
        val theirForest = driver.putLandOnBattlefield(opponent, "Forest")

        val projected = driver.state.projectedState
        projected.hasSubtype(urborg, "Swamp").shouldBeTrue()
        projected.hasSubtype(myForest, "Swamp").shouldBeTrue()
        projected.hasSubtype(myForest, "Forest").shouldBeTrue()
        projected.hasSubtype(theirForest, "Swamp").shouldBeTrue()
        projected.hasSubtype(theirForest, "Forest").shouldBeTrue()

        driver.submitSuccess(ActivateAbility(player, urborg, AbilityId.intrinsicMana('B')))
        blackIn(driver, player) shouldBe 1
        driver.submitSuccess(ActivateAbility(player, myForest, AbilityId.intrinsicMana('B')))
        blackIn(driver, player) shouldBe 2
    }
})
