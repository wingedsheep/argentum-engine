package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.RevealedToComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.mrd.cards.SharedFate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Shared Fate (MRD #49) — "If a player would draw a card, that player exiles the top card of one of
 * their opponents' libraries face down instead. Each player may look at cards they exiled with this
 * enchantment, and they may play lands and cast spells from among those cards."
 *
 * The card is modelled as a single replacement effect, so what these tests pin is that the
 * replacement really does run *as the drawing player*: it takes from that player's opponent and
 * hands that player the permission, for **every** player at the table including Shared Fate's own
 * controller. The third test pins the window the permission lives in — it belongs to the
 * enchantment, not to the exile zone, which is the 2008-08-01 ruling.
 */
class SharedFateScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all + SharedFate)
        d.initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    /** The players holding a "may play" permission over [cardId]. */
    fun GameTestDriver.mayPlayHolders(cardId: EntityId): List<EntityId> =
        state.mayPlayPermissions.filter { cardId in it.cardIds }.map { it.controllerId }

    test("an opponent's draw exiles the top of the enchantment controller's library, face down") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Shared Fate")
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")
        val handBefore = d.getHandSize(d.player2)

        // Player 2's draw step on the following turn.
        d.passPriorityUntil(Step.DRAW)

        withClue("the draw was replaced, so player 2 drew nothing") {
            d.getHandSize(d.player2) shouldBe handBefore
        }
        withClue("and the card came off player 1's library into exile — exile is keyed by owner, " +
            "so it stays player 1's card") {
            d.getExileCardNames(d.player1) shouldBe listOf("Lightning Bolt")
        }
        withClue("face down, per the printed line") {
            d.state.getEntity(bolt)?.has<FaceDownComponent>() shouldBe true
        }
        withClue("but the player who exiled it may look at it — a card you may play is a card you " +
            "may see") {
            d.state.getEntity(bolt)?.get<RevealedToComponent>()
                ?.isRevealedTo(d.player2) shouldBe true
        }
        withClue("and the permission belongs to the exiler, not to the card's owner") {
            d.mayPlayHolders(bolt) shouldBe listOf(d.player2)
        }

        // "…they may play lands and cast spells from among those cards" — costs are still paid.
        d.giveMana(d.player2, Color.RED, 1)
        d.castSpell(d.player2, bolt, listOf(d.player1)).error shouldBe null
        d.bothPass()
        withClue("player 2 cast player 1's card out of exile") {
            d.getLifeTotal(d.player1) shouldBe 17
        }
    }

    test("the enchantment's own controller has their draws replaced too") {
        val d = driver()
        d.putPermanentOnBattlefield(d.player1, "Shared Fate")
        val bolt = d.putCardOnTopOfLibrary(d.player2, "Lightning Bolt")
        val handBefore = d.getHandSize(d.player1)

        // Player 2's draw step, then player 1's on the turn after.
        d.passPriorityUntil(Step.DRAW)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        d.passPriorityUntil(Step.DRAW)

        withClue("\"if a player would draw\" is every player, not just opponents") {
            d.getHandSize(d.player1) shouldBe handBefore
            d.getExileCardNames(d.player2).contains("Lightning Bolt") shouldBe true
        }
        withClue("player 1 exiled it, so player 1 may play it") {
            d.mayPlayHolders(bolt) shouldBe listOf(d.player1)
        }
    }

    test("the permission dies with the enchantment, and the card stays exiled") {
        val d = driver()
        val sharedFate = d.putPermanentOnBattlefield(d.player1, "Shared Fate")
        val bolt = d.putCardOnTopOfLibrary(d.player1, "Lightning Bolt")

        d.passPriorityUntil(Step.DRAW)
        d.mayPlayHolders(bolt) shouldBe listOf(d.player2)

        d.moveToGraveyard(sharedFate)
        // State-based actions are checked when a player would receive priority; the driver's
        // direct board edit isn't an action, so drive one to get there.
        val filler = d.putCardInHand(d.player2, "Lightning Bolt")
        d.giveMana(d.player2, Color.RED, 1)
        d.castSpell(d.player2, filler, listOf(d.player1)).error shouldBe null
        d.bothPass()

        withClue("the window belongs to the enchantment, not to the exile zone (2008-08-01 ruling)") {
            d.mayPlayHolders(bolt) shouldBe emptyList()
        }
        withClue("the card itself is unaffected — it remains exiled, just unplayable") {
            d.getExileCardNames(d.player1) shouldBe listOf("Lightning Bolt")
        }
    }
})
