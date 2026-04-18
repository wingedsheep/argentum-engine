package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.ComponentContainer
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.battlefield.SummoningSicknessComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.bloomburrow.cards.RunAwayTogether
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.TypeLine
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.model.CreatureStats
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tokens that would be returned to hand cease to exist instead (MTG 704.5d).
 *
 * Run Away Together returns two target creatures (one yours, one an opponent's)
 * to their owners' hands. If either of those creatures is a token, the token
 * never actually makes it to the hand — the state-based action
 * `TokensInWrongZonesCheck` removes it after it leaves the battlefield.
 */
class RunAwayTogetherTokenTest : FunSpec({

    fun GameTestDriver.createMouseTokenOnBattlefield(playerId: EntityId): EntityId {
        val tokenId = EntityId.generate()
        val tokenCard = CardComponent(
            cardDefinitionId = "token:Mouse",
            name = "Mouse Token",
            manaCost = ManaCost.ZERO,
            typeLine = TypeLine.parse("Creature - Mouse"),
            baseStats = CreatureStats(1, 1),
            colors = setOf(Color.WHITE),
            ownerId = playerId,
        )
        val container = ComponentContainer.of(
            tokenCard,
            TokenComponent,
            ControllerComponent(playerId),
            SummoningSicknessComponent,
        )
        replaceState(
            state
                .withEntity(tokenId, container)
                .addToZone(ZoneKey(playerId, Zone.BATTLEFIELD), tokenId),
        )
        return tokenId
    }

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + RunAwayTogether)
        return driver
    }

    test("opponent's token ceases to exist when Run Away Together returns it to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ownCreature = driver.putCreatureOnBattlefield(activePlayer, "Grizzly Bears")
        val opponentToken = driver.createMouseTokenOnBattlefield(opponent)

        val spell = driver.putCardInHand(activePlayer, "Run Away Together")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val result = driver.castSpell(activePlayer, spell, listOf(ownCreature, opponentToken))
        result.isSuccess shouldBe true
        driver.bothPass()

        // Non-token bounces normally: leaves battlefield, ends up in owner's hand.
        driver.findPermanent(activePlayer, "Grizzly Bears") shouldBe null
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true

        // Token leaves battlefield and is removed from the game (704.5d).
        driver.findPermanent(opponent, "Mouse Token") shouldBe null
        driver.getHand(opponent).contains(opponentToken) shouldBe false
        driver.getHand(opponent).any { driver.getCardName(it) == "Mouse Token" } shouldBe false
        driver.state.getEntity(opponentToken) shouldBe null
    }

    test("your own token ceases to exist when Run Away Together returns it to hand") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Island" to 40))

        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val ownToken = driver.createMouseTokenOnBattlefield(activePlayer)
        val opponentCreature = driver.putCreatureOnBattlefield(opponent, "Grizzly Bears")

        val spell = driver.putCardInHand(activePlayer, "Run Away Together")
        driver.giveMana(activePlayer, Color.BLUE, 2)

        val result = driver.castSpell(activePlayer, spell, listOf(ownToken, opponentCreature))
        result.isSuccess shouldBe true
        driver.bothPass()

        driver.findPermanent(activePlayer, "Mouse Token") shouldBe null
        driver.getHand(activePlayer).contains(ownToken) shouldBe false
        driver.getHand(activePlayer).any { driver.getCardName(it) == "Mouse Token" } shouldBe false
        driver.state.getEntity(ownToken) shouldBe null

        driver.findPermanent(opponent, "Grizzly Bears") shouldBe null
        driver.getHand(opponent).any { driver.getCardName(it) == "Grizzly Bears" } shouldBe true
    }
})
