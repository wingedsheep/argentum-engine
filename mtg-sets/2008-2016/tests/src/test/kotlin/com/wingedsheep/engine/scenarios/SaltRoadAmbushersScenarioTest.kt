package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.MorphDataComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dtk.cards.AerieBowmasters
import com.wingedsheep.mtg.sets.definitions.dtk.cards.SaltRoadAmbushers
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.KeywordAbility
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Salt Road Ambushers (DTK #198) — "Whenever another permanent you control is turned face up, if
 * it's a creature, put two +1/+1 counters on it. Megamorph {3}{G}{G}"
 *
 * Pins the two halves a script reading can't prove: the counters land on the flipped permanent
 * (on top of its own megamorph counter), and "another" keeps the Ambushers' own flip from
 * triggering it.
 */
class SaltRoadAmbushersScenarioTest : FunSpec({

    val allCards = TestCards.all + listOf(SaltRoadAmbushers, AerieBowmasters)

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
                .with(MorphDataComponent(morphAbility.morphCost, cardDef.name, morphAbility.faceUpEffect))
        })
        removeSummoningSickness(creatureId)
        return creatureId
    }

    fun GameTestDriver.plusOneCounters(id: EntityId): Int =
        state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    fun GameTestDriver.settle() {
        repeat(4) { if (state.priorityPlayerId != null && stackSize > 0) bothPass() }
    }

    test("another creature you control turned face up gets two +1/+1 counters") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val ambushers = driver.putCreatureOnBattlefield(player, "Salt Road Ambushers")
        val bowmasters = driver.putFaceDownCreature(player, "Aerie Bowmasters")

        // Megamorph {5}{G}.
        driver.giveColorlessMana(player, 5)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            TurnFaceUp(playerId = player, sourceId = bowmasters, costTargetIds = emptyList())
        ).error shouldBe null
        driver.settle()

        driver.state.getEntity(bowmasters)?.get<FaceDownComponent>() shouldBe null
        // One counter from megamorph, two from Salt Road Ambushers.
        driver.plusOneCounters(bowmasters) shouldBe 3
        driver.plusOneCounters(ambushers) shouldBe 0
    }

    test("turning Salt Road Ambushers itself face up does not trigger it — 'another'") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!

        val ambushers = driver.putFaceDownCreature(player, "Salt Road Ambushers")

        // Megamorph {3}{G}{G}.
        driver.giveColorlessMana(player, 3)
        driver.giveMana(player, Color.GREEN, 2)
        driver.submit(
            TurnFaceUp(playerId = player, sourceId = ambushers, costTargetIds = emptyList())
        ).error shouldBe null
        driver.settle()

        driver.state.getEntity(ambushers)?.get<FaceDownComponent>() shouldBe null
        driver.stackSize shouldBe 0
        // Only the megamorph counter.
        driver.plusOneCounters(ambushers) shouldBe 1
    }

    test("an opponent's creature turned face up gets nothing — 'you control'") {
        val driver = createDriver()
        driver.initMirrorMatch(deck = Deck.of("Forest" to 40))
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val player = driver.activePlayer!!
        val opponent = driver.getOpponent(player)

        driver.putCreatureOnBattlefield(opponent, "Salt Road Ambushers")
        val bowmasters = driver.putFaceDownCreature(player, "Aerie Bowmasters")

        driver.giveColorlessMana(player, 5)
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            TurnFaceUp(playerId = player, sourceId = bowmasters, costTargetIds = emptyList())
        ).error shouldBe null
        driver.settle()

        driver.plusOneCounters(bowmasters) shouldBe 1
    }
})
