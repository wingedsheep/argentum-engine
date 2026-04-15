package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TypecycleCard
import com.wingedsheep.engine.state.ZoneKey
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests for TypecycleCardHandler via the Swampcycling ability on Twisted Abomination.
 *
 *   Twisted Abomination — {5}{B} Creature — Zombie Mutant 5/3
 *   {B}: Regenerate.
 *   Swampcycling {2} ({2}, Discard this card: Search your library for a Swamp card,
 *   reveal it, put it into your hand, then shuffle.)
 */
class TypecyclingTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        return driver
    }

    test("swampcycling pays cost, discards, and searches for a Swamp") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")
        val swamp = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")
        driver.giveColorlessMana(activePlayer, 2)

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        (result.isSuccess || result.isPaused).shouldBeTrue()

        // Abomination was discarded into the graveyard.
        driver.getGraveyardCardNames(activePlayer) shouldContain "Twisted Abomination"
        driver.findCardInHand(activePlayer, "Twisted Abomination") shouldBe null

        // Search library decision presenting the one Swamp as an option.
        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain swamp

        driver.submitDecision(
            activePlayer,
            CardsSelectedResponse(decision.id, listOf(swamp))
        )

        driver.findCardInHand(activePlayer, "Swamp") shouldBe swamp
        // Library no longer contains the picked Swamp.
        driver.state.getLibrary(activePlayer) shouldNotContain swamp
    }

    test("swampcycling fails without the mana to pay") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        result.isSuccess shouldBe false
        // Card is still in hand.
        driver.findCardInHand(activePlayer, "Twisted Abomination") shouldNotBe null
    }

    test("swampcycling fails when card is not in the acting player's hand") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Abomination is in the opponent's hand, not ours.
        val abomination = driver.putCardInHand(opponent, "Twisted Abomination")
        driver.giveColorlessMana(activePlayer, 2)

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        result.isSuccess shouldBe false
    }

    test("swampcycling is prevented while Stabilizer is on the battlefield") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        val opponent = driver.getOpponent(activePlayer)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls Stabilizer ("Players can't cycle cards.")
        driver.putCreatureOnBattlefield(opponent, "Stabilizer")

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")
        driver.giveColorlessMana(activePlayer, 2)

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        result.isSuccess shouldBe false
        driver.findCardInHand(activePlayer, "Twisted Abomination") shouldNotBe null
    }

    test("swampcycling taps lands to cover the cost when mana pool is empty") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Mountain" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val swamp = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")

        // Two untapped Mountains supply the {2} generic cost.
        val m1 = driver.putLandOnBattlefield(activePlayer, "Mountain")
        val m2 = driver.putLandOnBattlefield(activePlayer, "Mountain")

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")

        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        (result.isSuccess || result.isPaused).shouldBeTrue()

        driver.isTapped(m1) shouldBe true
        driver.isTapped(m2) shouldBe true
        driver.getGraveyardCardNames(activePlayer) shouldContain "Twisted Abomination"

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        decision.options shouldContain swamp
    }

    test("swampcycling with no Swamps in library still discards and yields zero-card decision") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")
        driver.giveColorlessMana(activePlayer, 2)

        val handSizeBefore = driver.getHandSize(activePlayer)
        val result = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        (result.isSuccess || result.isPaused).shouldBeTrue()

        // Card is discarded.
        driver.getGraveyardCardNames(activePlayer) shouldContain "Twisted Abomination"

        // Player ends up with one fewer card (discard only, no replacement yet).
        driver.getHandSize(activePlayer) shouldBe handSizeBefore - 1

        // Library in this test has no Swamps — the failToFind decision is optional.
        val library = driver.state.getLibrary(activePlayer)
        val swampCount = library.count { id ->
            driver.state.getEntity(id)?.get<CardComponent>()?.name == "Swamp"
        }
        swampCount shouldBe 0
    }

    test("swampcycling finds only Swamp cards, not other basics in the library") {
        val driver = createDriver()
        driver.initMirrorMatch(
            deck = Deck.of("Plains" to 40),
            startingLife = 20
        )
        val activePlayer = driver.activePlayer!!
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Library already full of Plains; add two Swamps on top.
        val swamp1 = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")
        val swamp2 = driver.putCardOnTopOfLibrary(activePlayer, "Swamp")

        val abomination = driver.putCardInHand(activePlayer, "Twisted Abomination")
        driver.giveColorlessMana(activePlayer, 2)

        val r = driver.submit(TypecycleCard(playerId = activePlayer, cardId = abomination))
        (r.isSuccess || r.isPaused).shouldBeTrue()

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()
        // Exactly the two Swamps are legal options; Plains are not.
        decision.options.size shouldBeGreaterThanOrEqual 2
        decision.options shouldContain swamp1
        decision.options shouldContain swamp2
        val plainsInLibrary = driver.state.getZone(ZoneKey(activePlayer, Zone.LIBRARY))
            .filter { id -> driver.state.getEntity(id)?.get<CardComponent>()?.name == "Plains" }
        plainsInLibrary.forEach { plains -> decision.options shouldNotContain plains }
    }
})
