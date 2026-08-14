package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.EscapeTunnel
import com.wingedsheep.mtg.sets.definitions.mkm.cards.LushPortico
import com.wingedsheep.mtg.sets.definitions.mkm.cards.NervousGardener
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Nervous Gardener (MKM) — {1}{G} 2/2 Dryad, Disguise {G}.
 *
 * "When this creature is turned face up, search your library for a land card with a basic land type,
 *  reveal it, put it into your hand, then shuffle."
 *
 * The library here is deliberately mixed: a deck of **Escape Tunnel** (`Land`, no subtypes) with a
 * **Forest** and a **Lush Portico** (`Land — Forest Plains`) seeded on top. That makes the filter
 * falsifiable — a plain "land card" filter would offer the whole library, and a "basic land card"
 * filter would offer only the Forest. The card wants exactly the two carrying a basic land *type*.
 */
class NervousGardenerScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(NervousGardener, EscapeTunnel, LushPortico))
        driver.initMirrorMatch(deck = Deck.of("Escape Tunnel" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    /** Cast the Gardener face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Nervous Gardener")
        driver.giveColorlessMana(player, 3)
        driver.submit(
            CastSpell(
                playerId = player,
                cardId = card,
                castFaceDown = true,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        driver.bothPass()
        return driver.getPermanents(player).single {
            driver.state.getEntity(it)?.has<FaceDownComponent>() == true
        }
    }

    /** Pay {G} to flip it, then let the turned-face-up trigger reach its decision. */
    fun flipFaceUp(driver: GameTestDriver, player: EntityId, gardener: EntityId) {
        driver.giveMana(player, Color.GREEN, 1)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = gardener,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
    }

    test("turning it face up searches — and only lands with a basic land type are offered") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardOnTopOfLibrary(player, "Forest")
        driver.putCardOnTopOfLibrary(player, "Lush Portico")

        val gardener = castFaceDown(driver, player)
        flipFaceUp(driver, player, gardener)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        withClue("Escape Tunnel is a land with no basic land type and must not be searchable") {
            decision.options.mapNotNull { nameOf(driver, it) }.sorted() shouldBe
                listOf("Forest", "Lush Portico")
        }

        // Lush Portico is a nonbasic land that *has* basic land types — the case the filter exists for.
        val portico = decision.options.single { nameOf(driver, it) == "Lush Portico" }
        driver.submitCardSelection(player, listOf(portico))

        withClue("the found card goes to hand, not the battlefield") {
            driver.findCardInHand(player, "Lush Portico").shouldNotBeNull()
            driver.findPermanent(player, "Lush Portico") shouldBe null
        }
    }

    test("failing to find is legal — the search may take nothing") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardOnTopOfLibrary(player, "Forest")

        val gardener = castFaceDown(driver, player)
        val librarySizeBefore = driver.state.getLibrary(player).size

        flipFaceUp(driver, player, gardener)

        driver.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        driver.submitCardSelection(player, emptyList())

        withClue("nothing was taken, so the Forest stays in a library of unchanged size") {
            driver.findCardInHand(player, "Forest") shouldBe null
            driver.state.getLibrary(player).size shouldBe librarySizeBefore
        }
    }

    test("hard-casting it never searches — turning face up is not entering the battlefield") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        driver.putCardOnTopOfLibrary(player, "Forest")

        val card = driver.putCardInHand(player, "Nervous Gardener")
        driver.giveMana(player, Color.GREEN, 1)
        driver.giveColorlessMana(player, 1)
        driver.castSpell(player, card).error shouldBe null
        driver.bothPass()

        withClue("the 2/2 is on the battlefield") {
            driver.findPermanent(player, "Nervous Gardener").shouldNotBeNull()
        }
        withClue("CR 701.34c: no trigger fired, so no search and the Forest is still in the library") {
            driver.pendingDecision shouldBe null
            driver.findCardInHand(player, "Forest") shouldBe null
        }
    }
})
