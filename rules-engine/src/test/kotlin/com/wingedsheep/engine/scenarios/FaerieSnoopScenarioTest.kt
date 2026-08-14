package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.core.TurnFaceUp
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mkm.cards.FaerieSnoop
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Faerie Snoop (MKM) — {1}{U}{B} 1/4 Faerie Detective with flying and Disguise {1}{U/B}{U/B}.
 *
 * "When this creature is turned face up, look at the top two cards of your library. Put one into your
 *  hand and the other into your graveyard."
 *
 * Two named cards are seeded on top of an otherwise-uniform library so the split is observable: the
 * chosen card must land in hand and the *other* one — not some third card — in the graveyard.
 */
class FaerieSnoopScenarioTest : FunSpec({

    fun createDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all + listOf(FaerieSnoop))
        driver.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true)
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun nameOf(driver: GameTestDriver, id: EntityId): String? =
        driver.state.getEntity(id)?.get<CardComponent>()?.name

    /** Cast the Snoop face down for {3} and return the resulting face-down permanent. */
    fun castFaceDown(driver: GameTestDriver, player: EntityId): EntityId {
        val card = driver.putCardInHand(player, "Faerie Snoop")
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

    fun flipFaceUp(driver: GameTestDriver, player: EntityId, snoop: EntityId) {
        driver.giveColorlessMana(player, 1)
        driver.giveMana(player, Color.BLUE, 2)
        driver.submit(
            TurnFaceUp(
                playerId = player,
                sourceId = snoop,
                paymentStrategy = PaymentStrategy.FromPool
            )
        ).error shouldBe null
        repeat(2) { if (!driver.isPaused && driver.stackSize > 0) driver.bothPass() }
    }

    test("turning it face up splits the top two — one to hand, the other to the graveyard") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        // Seeded last, so "Lightning Bolt" is the top card and "Giant Growth" is second.
        driver.putCardOnTopOfLibrary(player, "Giant Growth")
        driver.putCardOnTopOfLibrary(player, "Lightning Bolt")

        val snoop = castFaceDown(driver, player)
        flipFaceUp(driver, player, snoop)

        val decision = driver.pendingDecision
        decision.shouldBeInstanceOf<SelectCardsDecision>()

        withClue("exactly the top two cards are looked at") {
            decision.options.mapNotNull { nameOf(driver, it) }.sorted() shouldBe
                listOf("Giant Growth", "Lightning Bolt")
        }

        val bolt = decision.options.single { nameOf(driver, it) == "Lightning Bolt" }
        driver.submitCardSelection(player, listOf(bolt))

        withClue("the kept card goes to hand and the remainder to the graveyard") {
            driver.findCardInHand(player, "Lightning Bolt").shouldNotBeNull()
            driver.getGraveyardCardNames(player) shouldBe listOf("Giant Growth")
        }
        withClue("both cards left the library") {
            driver.state.getLibrary(player).map { nameOf(driver, it) }
                .none { it == "Lightning Bolt" || it == "Giant Growth" } shouldBe true
        }
    }

    test("face up it is the printed 1/4 flier; face down it is a vanilla 2/2") {
        val driver = createDriver()
        val player = driver.activePlayer!!

        val snoop = castFaceDown(driver, player)

        withClue("CR 708.2: a face-down permanent has no name, types, keywords or abilities") {
            driver.state.projectedState.getPower(snoop) shouldBe 2
            driver.state.projectedState.getToughness(snoop) shouldBe 2
            driver.state.projectedState.hasKeyword(snoop, Keyword.FLYING) shouldBe false
        }

        flipFaceUp(driver, player, snoop)

        withClue("flipped, it is the real card") {
            driver.state.projectedState.getPower(snoop) shouldBe 1
            driver.state.projectedState.getToughness(snoop) shouldBe 4
            driver.state.projectedState.hasKeyword(snoop, Keyword.FLYING) shouldBe true
        }
    }
})
