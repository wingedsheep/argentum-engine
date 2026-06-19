package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownTurnUpComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.AerieBowmasters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Tests for Aerie Bowmasters.
 *
 * Aerie Bowmasters: {2}{G}{G}
 * Creature — Dog Archer
 * 3/4
 * Reach
 * Megamorph {5}{G}
 *
 * Megamorph is modeled as the Morph keyword with a face-up effect that puts a
 * +1/+1 counter on the creature when it is turned face up.
 */
class AerieBowmastersTest : FunSpec({

    val allCards = TestCards.all + listOf(AerieBowmasters)

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(allCards)
        return driver
    }

    fun GameTestDriver.putFaceDownCreature(playerId: EntityId, cardName: String): EntityId {
        val creatureId = putCreatureOnBattlefield(playerId, cardName)
        val cardDef = allCards.first { it.name == cardName }
        val morphAbility = cardDef.keywordAbilities
            .filterIsInstance<KeywordAbility.Morph>()
            .first()
        replaceState(state.updateEntity(creatureId) { container ->
            container
                .with(FaceDownComponent)
                .with(FaceDownTurnUpComponent(morphAbility.morphCost, cardDef.name, morphAbility.faceUpEffect))
        })
        return creatureId
    }

    test("megamorph: turning face up puts a +1/+1 counter on the creature") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            startingLife = 20
        )

        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val activePlayer = driver.activePlayer!!

        // Put Aerie Bowmasters face-down (a 2/2 morph) on the battlefield.
        val bowmasters = driver.putFaceDownCreature(activePlayer, "Aerie Bowmasters")
        driver.removeSummoningSickness(bowmasters)

        // Pay the megamorph cost {5}{G}.
        driver.giveColorlessMana(activePlayer, 5)
        driver.giveMana(activePlayer, Color.GREEN, 1)

        val result = driver.submit(
            TurnFaceUp(
                playerId = activePlayer,
                sourceId = bowmasters,
                costTargetIds = emptyList()
            )
        )
        (result.error == null) shouldBe true

        // It should now be face up as Aerie Bowmasters.
        driver.state.getEntity(bowmasters)?.get<FaceDownComponent>() shouldBe null
        driver.state.getEntity(bowmasters)?.get<CardComponent>()?.name shouldBe "Aerie Bowmasters"

        // Megamorph face-up effect: exactly one +1/+1 counter.
        val counters = driver.state.getEntity(bowmasters)?.get<CountersComponent>()
        (counters?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0) shouldBe 1
    }
})
