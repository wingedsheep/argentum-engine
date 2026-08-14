package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardRevealedFromDrawEvent
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.LinkedExileComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * CR 614.11b — "If an effect would have a player both draw a card and perform an additional
 * action on that card, and the draw is replaced, the additional action is not performed on any
 * cards that are drawn as a result of that replacement effect."
 *
 * Primitive Etchings ("Reveal the first card you draw each turn. Whenever you reveal a creature
 * card this way, draw a card.") is the rider; Parallel Thoughts ("If you would draw a card, you
 * may instead put the top card of the pile you exiled into your hand") is the replacement. A card
 * that arrives via Parallel Thoughts was **put into** the hand, not drawn — so Etchings must not
 * reveal it, and its creature-reveal trigger must not fire off it.
 *
 * The rider is emitted by `DrawCardPrimitive`, which a replaced draw never reaches, so the
 * behaviour falls out of the draw pipeline's shape rather than from a dedicated check. That is
 * exactly why it is worth pinning: nothing in the suite covered it, and the draw domain is the
 * template the remaining replacement domains are being migrated onto.
 */
class ReplacedDrawRiderTest : FunSpec({

    fun newDriver(): GameTestDriver {
        val driver = GameTestDriver()
        driver.registerCards(TestCards.all)
        driver.initMirrorMatch(
            deck = Deck.of("Forest" to 40),
            skipMulligans = true,
            startingPlayer = 0
        )
        driver.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return driver
    }

    fun GameTestDriver.drainStack(maxIterations: Int = 30) {
        var guard = 0
        while (guard++ < maxIterations && state.stack.isNotEmpty() && !isPaused) {
            bothPass()
        }
    }

    fun GameTestDriver.revealedFromDraw(): List<CardRevealedFromDrawEvent> =
        events.filterIsInstance<CardRevealedFromDrawEvent>()

    fun GameTestDriver.isInHand(playerId: EntityId, id: EntityId): Boolean =
        getHand(playerId).contains(id)

    test("control: an unreplaced draw reveals the card and fires the creature trigger") {
        // The baseline the replaced case has to differ from. With no replacement in play the
        // first card drawn this turn is revealed, and because it is a creature Primitive
        // Etchings' trigger draws an extra card.
        val driver = newDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Primitive Etchings")
        val bears = driver.putCardOnTopOfLibrary(me, "Grizzly Bears")
        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        withClue("The first card drawn this turn is revealed (Primitive Etchings)") {
            driver.revealedFromDraw().map { it.cardEntityId } shouldBe listOf(bears)
        }
        withClue(
            "Revealing a creature this way draws an extra card, so Counsel's 2 draws plus the " +
                "trigger's 1 take 3 cards off the library"
        ) {
            driver.state.getLibrary(me).size shouldBe libBefore - 3
        }
    }

    test("CR 614.11b: a card put into hand by a replaced draw is not revealed, and fires nothing") {
        val driver = newDriver()
        val me = driver.player1

        driver.putPermanentOnBattlefield(me, "Primitive Etchings")
        val pt = driver.putPermanentOnBattlefield(me, "Parallel Thoughts")

        // A *creature* in the exiled pile: if the engine wrongly treated it as drawn-and-revealed,
        // Primitive Etchings' trigger would fire and draw an extra card, which the library count
        // below would catch.
        val exiledBears = driver.putCardInExile(me, "Grizzly Bears")
        driver.replaceState(
            driver.state.updateEntity(pt) { c -> c.with(LinkedExileComponent(listOf(exiledBears))) }
        )

        val libBefore = driver.state.getLibrary(me).size

        driver.giveMana(me, Color.BLUE, 3)
        val spell = driver.putCardInHand(me, "Counsel of the Soratami")
        driver.castSpell(me, spell)
        driver.drainStack()

        // First draw: accept the replacement — the pile's creature is put into hand, not drawn.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, true)
        driver.drainStack()

        withClue("Sanity: the replacement did happen and the creature card is in hand") {
            driver.isInHand(me, exiledBears) shouldBe true
        }
        withClue(
            "CR 614.11b — the card was put into hand, not drawn, so the draw-attached reveal " +
                "rider must not be applied to it"
        ) {
            driver.revealedFromDraw().none { it.cardEntityId == exiledBears } shouldBe true
        }

        // Second draw: decline, so a real draw happens. The pile is empty now, but the prompt is
        // still offered (a player may accept with an empty pile), so answer it explicitly.
        driver.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        driver.submitYesNo(me, false)
        driver.drainStack()

        withClue(
            "Exactly one card left the library: the declined draw. A second would mean the " +
                "replaced draw fired Primitive Etchings' creature-reveal trigger."
        ) {
            driver.state.getLibrary(me).size shouldBe libBefore - 1
        }
        withClue(
            "The declined draw is a real draw, and the first one this turn, so it *is* revealed " +
                "— a Forest, which is not a creature and fires nothing."
        ) {
            val revealed = driver.revealedFromDraw()
            revealed.size shouldBe 1
            driver.state.getEntity(revealed.single().cardEntityId)
                ?.get<CardComponent>()?.name shouldBe "Forest"
        }
    }
})
