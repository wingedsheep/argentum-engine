package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnwantedRemake
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Unwanted Remake (DSK #39) — {W} Instant.
 *
 *   "Destroy target creature. Its controller manifests dread."
 *
 * Verifies the destroyed creature's controller — not Unwanted Remake's controller — is the one who
 * manifests dread (looks at the top two of *their* library, manifests one face-down 2/2 under their
 * control, bins the other). Modeled by running the shared manifest-dread recipe under
 * `ForEachPlayer(Player.ControllerOf("target creature"))`.
 */
class UnwantedRemakeScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + UnwantedRemake)
        initMirrorMatch(deck = Deck.of("Plains" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("destroys the target and makes the target's controller manifest dread") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // The opponent controls a creature; I'll destroy it.
        val victim = d.putCreatureOnBattlefield(opp, "Grizzly Bears")

        // Stack the opponent's top two cards: a creature on top, a land beneath.
        val oppLand = d.putCardOnTopOfLibrary(opp, "Plains")
        val oppCreature = d.putCardOnTopOfLibrary(opp, "Hill Giant") // now the top card

        // I cast Unwanted Remake targeting the opponent's creature.
        val spell = d.putCardInHand(me, "Unwanted Remake")
        d.giveMana(me, Color.WHITE, 1)
        d.castSpell(me, spell, targets = listOf(victim)).error shouldBe null

        // Resolve until the opponent's manifest-dread pick pauses.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The targeted creature was destroyed (in its owner's graveyard).
        d.getGraveyard(opp) shouldContain victim

        // The OPPONENT (the destroyed creature's controller) chooses which card to manifest.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        pick.options.toSet() shouldBe setOf(oppCreature, oppLand)
        pick.playerId shouldBe opp
        d.submitDecision(opp, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(oppCreature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The opponent's chosen card is a face-down 2/2 under the opponent's control; the other binned.
        d.getPermanents(opp) shouldContain oppCreature
        val entity = d.state.getEntity(oppCreature)
        entity?.get<FaceDownComponent>() shouldBe FaceDownComponent
        entity?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        d.state.projectedState.getPower(oppCreature) shouldBe 2
        d.getGraveyard(opp) shouldContain oppLand
    }
})
