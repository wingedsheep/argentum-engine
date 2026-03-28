package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.DruidOfTheSpade
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Druid of the Spade:
 * As long as you control a token, this creature gets +2/+0 and has trample.
 */
class DruidOfTheSpadeTest : FunSpec({

    val projector = StateProjector()

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + DruidOfTheSpade)
        return driver
    }

    fun GameTestDriver.createTokenOnBattlefield(playerId: EntityId): EntityId {
        val tokenId = EntityId.generate()
        val tokenComponent = CardComponent(
            cardDefinitionId = "token:Mouse",
            name = "Mouse Token",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - Mouse"),
            baseStats = CreatureStats(1, 1),
            colors = setOf(Color.WHITE),
            ownerId = playerId
        )
        val tokenContainer = ComponentContainer.of(
            tokenComponent,
            TokenComponent,
            ControllerComponent(playerId),
            SummoningSicknessComponent
        )
        replaceState(
            state
                .withEntity(tokenId, tokenContainer)
                .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), tokenId)
        )
        return tokenId
    }

    test("Druid of the Spade is 2/3 without a token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Druid of the Spade")

        val druidId = driver.findPermanent(activePlayer, "Druid of the Spade")!!
        projector.getProjectedPower(driver.state, druidId) shouldBe 2
        projector.getProjectedToughness(driver.state, druidId) shouldBe 3
        projector.getProjectedKeywords(driver.state, druidId).contains(Keyword.TRAMPLE) shouldBe false
    }

    test("Druid of the Spade gets +2/+0 and trample when you control a token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Druid of the Spade")
        driver.createTokenOnBattlefield(activePlayer)

        val druidId = driver.findPermanent(activePlayer, "Druid of the Spade")!!
        projector.getProjectedPower(driver.state, druidId) shouldBe 4
        projector.getProjectedToughness(driver.state, druidId) shouldBe 3
        projector.getProjectedKeywords(driver.state, druidId).contains(Keyword.TRAMPLE) shouldBe true
    }

    test("Druid of the Spade does not benefit from opponent's token") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        driver.putCreatureOnBattlefield(activePlayer, "Druid of the Spade")
        driver.createTokenOnBattlefield(opponent)

        val druidId = driver.findPermanent(activePlayer, "Druid of the Spade")!!
        projector.getProjectedPower(driver.state, druidId) shouldBe 2
        projector.getProjectedToughness(driver.state, druidId) shouldBe 3
        projector.getProjectedKeywords(driver.state, druidId).contains(Keyword.TRAMPLE) shouldBe false
    }
})
