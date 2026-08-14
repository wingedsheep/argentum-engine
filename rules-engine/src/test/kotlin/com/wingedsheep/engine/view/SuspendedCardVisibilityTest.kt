package com.wingedsheep.engine.view

import com.wingedsheep.engine.core.SuspendCardFromHand
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.tsp.cards.AncestralVision
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * A suspended card (CR 702.62) sits face-up in exile carrying a
 * [com.wingedsheep.engine.state.components.battlefield.SuspendedComponent]: it counts down a time
 * counter at its owner's upkeep, then may be cast for free once the last is gone.
 * [ClientStateTransformer] surfaces that as [ClientCard.isSuspended] so the client can show it in a
 * dedicated public pile — otherwise it's indistinguishable from any other exiled card. Suspend
 * exiles the card face-up (public information), so the flag is visible to both players.
 *
 * CR 702.62b: "suspended" requires at least one time counter — the marker itself outlives that
 * (a declined free cast leaves the card exiled with the marker but no counters), so the flag must
 * track counter count, not just marker presence.
 */
class SuspendedCardVisibilityTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + listOf(AncestralVision))
        d.initMirrorMatch(deck = Deck.of("Island" to 40), startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun transformer(d: GameTestDriver): ClientStateTransformer =
        ClientStateTransformer(cardRegistry = d.cardRegistry)

    /** Advance to the owner's next upkeep and resolve the suspend countdown trigger there. */
    fun resolveNextOwnerUpkeep(d: GameTestDriver, owner: EntityId) {
        do {
            d.passPriorityUntil(Step.PRECOMBAT_MAIN)
            d.passPriorityUntil(Step.UPKEEP)
        } while (d.activePlayer != owner)
        d.bothPass()
    }

    test("a card in hand (not yet suspended) is not flagged isSuspended") {
        val d = driver()
        val player = d.activePlayer!!
        val card = d.putCardInHand(player, "Ancestral Vision")

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[card]?.isSuspended shouldBe false
    }

    test("suspending it flags isSuspended in exile for both players") {
        val d = driver()
        val player = d.activePlayer!!
        val opponent = d.getOpponent(player)

        val card = d.putCardInHand(player, "Ancestral Vision")
        d.giveMana(player, Color.BLUE, 1) // pays {U}
        d.submitSuccess(SuspendCardFromHand(player, card))

        d.getExile(player).contains(card) shouldBe true

        // Suspend exile is face-up / public, so both the owner and the opponent see the flag.
        val ownerView = transformer(d).transform(d.state, viewingPlayerId = player)
        val opponentView = transformer(d).transform(d.state, viewingPlayerId = opponent)

        ownerView.cards[card].shouldNotBeNull().isSuspended shouldBe true
        opponentView.cards[card].shouldNotBeNull().isSuspended shouldBe true
    }

    test("declining the free cast at zero time counters clears isSuspended, even though it stays in exile") {
        val d = driver()
        val player = d.activePlayer!!

        val card = d.putCardInHand(player, "Ancestral Vision")
        d.giveMana(player, Color.BLUE, 1)
        d.submitSuccess(SuspendCardFromHand(player, card))

        repeat(4) { resolveNextOwnerUpkeep(d, player) }
        d.submitYesNo(player, false) // decline the CR 702.62a free cast
        d.bothPass()

        d.getExile(player).contains(card) shouldBe true

        val view = transformer(d).transform(d.state, viewingPlayerId = player)
        view.cards[card].shouldNotBeNull().isSuspended shouldBe false
    }
})
