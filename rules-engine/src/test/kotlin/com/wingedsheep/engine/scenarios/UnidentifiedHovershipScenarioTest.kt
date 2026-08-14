package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnidentifiedHovership
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unidentified Hovership (DSK #37) — {1}{W}{W} Artifact — Vehicle, 2/2, Flying, Crew 1.
 *
 *   "When this Vehicle enters, exile up to one target creature with toughness 5 or less.
 *    When this Vehicle leaves the battlefield, the exiled card's owner manifests dread."
 *
 * The interesting, newly-built behavior is the leaves-the-battlefield trigger acting on the
 * *exiled card's owner* (often an opponent), not the Vehicle's controller — driven by the
 * [com.wingedsheep.sdk.scripting.references.Player.OwnersOfLinkedExile] reference reading the
 * source's linked-exile pile. These tests pin:
 *  - the owner of the exiled creature (the opponent) manifests dread — not the Hovership's controller;
 *  - the exiled card stays exiled (this card never returns it, unlike Fear of Abduction);
 *  - with nothing exiled, the leaves trigger does nothing — and crucially does NOT fall back to
 *    making every player manifest (the "won't do anything" ruling).
 */
class UnidentifiedHovershipScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + UnidentifiedHovership)
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("the exiled creature's owner — the opponent — manifests dread when the Vehicle leaves") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // The opponent controls a creature with toughness 5 or less (3/3) for the ETB to exile.
        val courser = d.putPermanentOnBattlefield(opp, "Centaur Courser")

        // Stack the opponent's top two cards so their manifest-dread pick is deterministic.
        val oppLand = d.putCardOnTopOfLibrary(opp, "Plains")
        val oppCreature = d.putCardOnTopOfLibrary(opp, "Grizzly Bears") // now the top card

        // I cast the Hovership; its ETB pauses to choose the (up to one) creature to exile.
        val hovInHand = d.putCardInHand(me, "Unidentified Hovership")
        d.giveMana(me, Color.WHITE, 3)
        d.castSpell(me, hovInHand).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(me, listOf(courser))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The opponent's creature is now exiled (owned by the opponent), Hovership is in play.
        d.getExile(opp) shouldContain courser
        val hov = d.findPermanent(me, "Unidentified Hovership")!!

        // Destroy the Hovership with artifact removal to fire its leaves-the-battlefield trigger.
        val disenchant = d.putCardInHand(me, "Disenchant")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpellWithTargets(me, disenchant, listOf(ChosenTarget.Permanent(hov))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The leaves trigger makes the EXILED CARD'S OWNER (the opponent) manifest dread —
        // the pick is presented to the opponent, over the opponent's own library cards.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.playerId shouldBe opp
        pick.options.toSet() shouldBe setOf(oppCreature, oppLand)
        d.submitDecision(opp, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(oppCreature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The opponent's chosen card is a face-down 2/2 under the opponent's control; the other binned.
        d.getPermanents(opp) shouldContain oppCreature
        val manifested = d.state.getEntity(oppCreature)
        manifested?.get<FaceDownComponent>() shouldBe FaceDownComponent
        manifested?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        d.state.projectedState.getPower(oppCreature) shouldBe 2
        d.getGraveyard(opp) shouldContain oppLand

        // The exiled creature is NOT returned — it stays exiled (unlike Fear of Abduction).
        d.getExile(opp) shouldContain courser
        d.getPermanents(opp) shouldNotContain courser
    }

    test("with nothing exiled, the leaves trigger does nothing — no player manifests dread") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // The opponent has an eligible creature, but we DECLINE the "up to one" exile — so
        // nothing is exiled and the linked-exile pile stays empty.
        val courser = d.putPermanentOnBattlefield(opp, "Centaur Courser")
        d.putCardOnTopOfLibrary(opp, "Grizzly Bears")

        val hovInHand = d.putCardInHand(me, "Unidentified Hovership")
        d.giveMana(me, Color.WHITE, 3)
        d.castSpell(me, hovInHand).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()
        // Decline the optional exile target (choose zero creatures).
        d.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
        d.submitTargetSelection(me, emptyList())
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        val hov = d.findPermanent(me, "Unidentified Hovership")!!
        d.getExile(me).isEmpty() shouldBe true
        d.getExile(opp).isEmpty() shouldBe true
        // The declined creature is untouched on the battlefield.
        d.getPermanents(opp) shouldContain courser

        // Destroy the Hovership; its leaves trigger resolves with an empty linked-exile pile.
        val disenchant = d.putCardInHand(me, "Disenchant")
        d.giveMana(me, Color.WHITE, 2)
        d.castSpellWithTargets(me, disenchant, listOf(ChosenTarget.Permanent(hov))).error shouldBe null
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // Nobody manifests dread: no decision is pending, and no manifested permanent exists.
        (d.pendingDecision is SelectCardsDecision) shouldBe false
        (d.getPermanents(me) + d.getPermanents(opp)).none {
            d.state.getEntity(it)?.get<FaceDownModeComponent>() != null
        } shouldBe true
    }
})
