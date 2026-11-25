package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PaymentStrategy
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.view.LegalActionInfo
import com.wingedsheep.mtg.sets.definitions.blc.cards.BrightcapBadger
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A spell cast from exile or the graveyard is paid for like any other (CR 601.2g–h): the player
 * chooses which mana abilities to activate. The client can only offer that choice when the legal
 * action carries the player's mana sources, and the enricher publishes those for an action that has
 * an auto-tap preview — so every paid cast-from-zone offer must carry one.
 *
 * Engine-level: covers the from-zone offers that used to ship without a preview — a card on an
 * adventure, a may-play grant (either face), a card's own cast-from-exile permission
 * (Eternal Scourge), and a graveyard-permanent permission (Muldrotha, the Gravetide).
 */
class ZoneCastManualManaPaymentTest : ScenarioTestBase() {

    private fun TestGame.untappedForests(): List<EntityId> =
        findPermanents("Forest").filter { state.getEntity(it)?.get<TappedComponent>() == null }

    private fun TestGame.castOffer(cardId: EntityId, faceIndex: Int? = null): LegalActionInfo =
        getLegalActions(1).single {
            val cast = it.action as? CastSpell
            cast != null && cast.cardId == cardId && cast.faceIndex == faceIndex && !cast.useAlternativeCost
        }

    private fun TestGame.cardIn(zone: List<EntityId>, name: String): EntityId =
        zone.single { state.getEntity(it)?.get<CardComponent>()?.name == name }

    /** The offer names every untapped land as a payable source and previews a payment of [cost] lands. */
    private fun TestGame.shouldOfferManualPayment(offer: LegalActionInfo, cost: Int) {
        offer.availableManaSources.shouldNotBeNull().map { it.entityId } shouldContainExactlyInAnyOrder untappedForests()
        offer.autoTapPreview.shouldNotBeNull() shouldHaveSize cost
    }

    init {
        cardRegistry.register(BrightcapBadger)

        test("the creature half of a card on an adventure can be paid for with hand-picked lands") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Brightcap Badger")
                .withLandsOnBattlefield(1, "Forest", 8)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.findCardsInHand(1, "Brightcap Badger").single()

            // Fungus Frolic ({2}{G}) resolves and sends the card on an adventure.
            game.execute(CastSpell(game.player1Id, card, faceIndex = 0)).error shouldBe null
            game.resolveStack()
            game.state.getExile(game.player1Id) shouldBe listOf(card)

            val untapped = game.untappedForests()
            untapped shouldHaveSize 5
            game.shouldOfferManualPayment(game.castOffer(card), cost = 4)

            // Pay with the four lands the player picked, leaving the one they wanted untapped.
            val kept = untapped.first()
            val picked = untapped - kept
            game.execute(
                CastSpell(game.player1Id, card, paymentStrategy = PaymentStrategy.Explicit(picked))
            ).error shouldBe null
            withClue("exactly the picked lands were tapped") {
                game.untappedForests() shouldBe listOf(kept)
            }
        }

        test("a may-play grant offers manual payment for both the creature and its Adventure") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInExile(1, "Brightcap Badger")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.cardIn(game.state.getExile(game.player1Id), "Brightcap Badger")
            val (permissionId, withId) = game.state.newEntity()
            game.state = withId.addMayPlayPermission(
                MayPlayPermission(permissionId, setOf(card), game.player1Id, timestamp = withId.timestamp)
            )

            game.shouldOfferManualPayment(game.castOffer(card), cost = 4)
            game.shouldOfferManualPayment(game.castOffer(card, faceIndex = 0), cost = 3)
        }

        test("a card's own cast-from-exile permission offers manual payment") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInExile(1, "Eternal Scourge")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.cardIn(game.state.getExile(game.player1Id), "Eternal Scourge")

            game.shouldOfferManualPayment(game.castOffer(card), cost = 3)
        }

        test("a graveyard-permanent permission offers manual payment") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Muldrotha, the Gravetide")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 5)
                .withCardInLibrary(1, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val card = game.cardIn(game.state.getGraveyard(game.player1Id), "Grizzly Bears")

            game.shouldOfferManualPayment(game.castOffer(card), cost = 2)
        }
    }
}
