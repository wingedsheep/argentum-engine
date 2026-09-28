package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.CardsDrawnEvent
import com.wingedsheep.engine.core.ForetellCard
import com.wingedsheep.engine.core.GameConfig
import com.wingedsheep.engine.core.GameInitializer
import com.wingedsheep.engine.core.PlayerConfig
import com.wingedsheep.engine.core.TakeMulligan
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain

/**
 * The client stream never refers to a card the viewer can't identify.
 *
 * An entity ID is a name for one physical card: a client that sees it can follow that card from
 * zone to zone, and IDs are minted card by card, so an ID can say which card it is. Hidden zones
 * are therefore counted rather than listed, and events about hidden cards carry no ID.
 */
class HiddenCardReferenceTest : FunSpec({

    val deck = Deck.of("Island" to 20, "Lightning Bolt" to 20)

    fun createDriver(skipMulligans: Boolean = true): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(deck = deck, skipMulligans = skipMulligans)
        return driver
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry, predicateEvaluator = PredicateEvaluator(cardRegistry = null))

    fun GameTestDriver.nameOf(id: EntityId): String = state.getEntity(id)!!.get<CardComponent>()!!.name

    test("a library is counted, not listed, for its owner and for an opponent") {
        val driver = createDriver()
        val owner = driver.player1
        val librarySize = driver.state.getLibrary(owner).size

        for (viewer in listOf(owner, driver.player2)) {
            val library = transformer(driver).transform(driver.state, viewer).zones
                .single { it.zoneId == ZoneKey(owner, Zone.LIBRARY) }
            library.cardIds.shouldBeEmpty()
            library.positions shouldBe emptyList()
            library.size shouldBe librarySize
        }
    }

    test("reordering a hidden library changes no player's view") {
        val driver = createDriver()
        val owner = driver.player1
        val libraryKey = ZoneKey(owner, Zone.LIBRARY)
        val reordered = driver.state.reorderZone(libraryKey, driver.state.getLibrary(owner).reversed())
        reordered.getLibrary(owner) shouldNotBe driver.state.getLibrary(owner)

        for (viewer in listOf(owner, driver.player2)) {
            transformer(driver).transform(reordered, viewer) shouldBe transformer(driver).transform(driver.state, viewer)
        }
    }

    test("an opponent's draw names neither the card nor its ID") {
        val driver = createDriver()
        val owner = driver.player1
        val opponent = driver.player2
        val (first, second) = driver.state.getLibrary(owner).let { it[0] to it[1] }
        fun drew(id: EntityId, viewer: EntityId) = ClientEventTransformer.transform(
            listOf(CardsDrawnEvent(owner, 1, listOf(id), listOf(driver.nameOf(id)))), viewer
        ).single() as ClientEvent.CardDrawn

        drew(first, opponent) shouldBe drew(second, opponent)
        drew(first, opponent).cardId shouldBe null
        drew(first, owner).cardId shouldBe first
    }

    test("a real draw step tells the other player that a card was drawn, not which") {
        val driver = createDriver()
        // Turn 1's player skips its draw; the next draw step is the other player's.
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val other = driver.activePlayer!!
        val drawer = driver.getOpponent(other)
        val before = driver.events.size
        val handBefore = driver.state.getHand(drawer).toSet()
        driver.passPriorityUntil(Step.DRAW)
        driver.state.activePlayerId shouldBe drawer
        val drawn = (driver.state.getHand(drawer).toSet() - handBefore).single()
        val events = driver.events.drop(before)

        val seen = ClientEventTransformer.transform(events, other)
        seen.filterIsInstance<ClientEvent.CardDrawn>().shouldNotBeEmpty()
        Regex("\\b${drawn.value}\\b").containsMatchIn(seen.toString()) shouldBe false
        seen.toString() shouldNotContain driver.nameOf(drawn)
    }

    test("foretelling a card tells the opponent that a card was exiled face down, not which") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val bolt = driver.putCardInHand(owner, "Demon Bolt")
        driver.giveMana(owner, Color.RED, 2)
        val before = driver.events.size

        driver.submitSuccess(ForetellCard(owner, bolt))
        val events = driver.events.drop(before)

        val opponentMove = ClientEventTransformer.transform(events, opponent, driver.state)
            .filterIsInstance<ClientEvent.PermanentLeft>().single()
        opponentMove.destination shouldBe "exile"
        opponentMove.cardName shouldNotBe "Demon Bolt"
        ClientEventTransformer.transform(events, owner, driver.state)
            .filterIsInstance<ClientEvent.PermanentLeft>().single().cardName shouldBe "Demon Bolt"
    }

    test("manifesting a card shows the opponent a face-down permanent, not the card's name") {
        val driver = createDriver()
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val top = driver.putCardOnTopOfLibrary(owner, "Grizzly Bears")

        val result = driver.services.effectExecutorRegistry.execute(
            driver.state, Patterns.Library.manifest(), EffectContext(sourceId = owner, controllerId = owner),
        )
        result.state.getBattlefield() shouldContain top
        val events = ClientEventTransformer.transform(result.events, opponent, result.state)
        val view = transformer(driver).transform(result.state, opponent)

        (events.toString() + view.toString()) shouldNotContain "Grizzly Bears"
        ClientEventTransformer.transform(result.events, owner, result.state).toString() shouldContain "Grizzly Bears"
    }

    test("a spectator's view names no library card and ignores library order") {
        val driver = createDriver()
        val owner = driver.player1
        val reordered = driver.state.reorderZone(ZoneKey(owner, Zone.LIBRARY), driver.state.getLibrary(owner).reversed())
        fun spectate(state: GameState) = transformer(driver).transform(state, owner, isSpectator = true)

        spectate(driver.state).zones.single { it.zoneId == ZoneKey(owner, Zone.LIBRARY) }.cardIds.shouldBeEmpty()
        spectate(reordered) shouldBe spectate(driver.state)
    }

    test("a mulligan tells the opponent that cards moved, not which cards") {
        val driver = createDriver(skipMulligans = false)
        val owner = driver.activePlayer!!
        val opponent = driver.getOpponent(owner)
        val hand = driver.state.getHand(owner).toSet()

        val events = driver.submit(TakeMulligan(owner)).events
        val newHand = driver.state.getHand(owner).toSet()

        val opponentMoves = ClientEventTransformer.transform(events, opponent).filterIsInstance<ClientEvent.PermanentLeft>()
        opponentMoves.shouldNotBeEmpty()
        opponentMoves.forEach { move ->
            move.cardName shouldBe "card"
            move.cardId shouldBe null
        }

        val ownerMoves = ClientEventTransformer.transform(events, owner).filterIsInstance<ClientEvent.PermanentLeft>()
        ownerMoves.map { it.cardId }.toSet() shouldBe hand + newHand
    }

    test("a card's ID doesn't say which card it is") {
        val registry = createDriver().cardRegistry
        fun cardNamesById(seed: Long): Map<EntityId, String> {
            val game = GameInitializer(registry).initializeGame(
                GameConfig(players = listOf(PlayerConfig("A", deck), PlayerConfig("B", deck)), seed = seed)
            )
            return game.state.entities.mapNotNull { (id, container) ->
                container.get<CardComponent>()?.let { id to it.name }
            }.toMap()
        }

        cardNamesById(1L) shouldBe cardNamesById(1L)
        cardNamesById(1L) shouldNotBe cardNamesById(2L)
    }
})
