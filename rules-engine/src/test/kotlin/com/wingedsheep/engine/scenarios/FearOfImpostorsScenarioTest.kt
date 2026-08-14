package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.FearOfImpostors
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Fear of Impostors (DSK #57) — {1}{U}{U} Enchantment Creature — Nightmare 3/2, Flash.
 *
 *   "When this creature enters, counter target spell. Its controller manifests dread."
 *
 * Verifies: the targeted spell is countered (goes to its controller's graveyard), and the
 * countered spell's controller — not Fear of Impostors' controller — manifests dread (looks at the
 * top two of *their* library, manifests one as a face-down 2/2 under their control, bins the other).
 */
class FearOfImpostorsScenarioTest : FunSpec({

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(TestCards.all + FearOfImpostors)
        initMirrorMatch(deck = Deck.of("Island" to 60), startingLife = 20)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    test("counters the spell and makes that spell's controller manifest dread") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)

        // Stack the opponent's top two cards: a creature on top, a land beneath.
        val oppLand = d.putCardOnTopOfLibrary(opp, "Island")
        val oppCreature = d.putCardOnTopOfLibrary(opp, "Grizzly Bears") // now the top card

        // Active player (me) passes priority so the opponent can cast a spell.
        d.passPriority(me)

        // Opponent casts a spell, then passes priority back so I can respond.
        val oppSpell = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.castSpell(opp, oppSpell, targets = listOf(me)).error shouldBe null
        d.passPriority(opp)

        // I flash in Fear of Impostors. Its counter is an ETB triggered ability (not a spell
        // target), so it picks its target after the creature resolves.
        val fear = d.putCardInHand(me, "Fear of Impostors")
        d.giveMana(me, Color.BLUE, 3)
        d.castSpell(me, fear).error shouldBe null

        // Resolve Fear of Impostors; its ETB pauses to choose the spell to counter.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()
        (d.pendingDecision as com.wingedsheep.engine.core.ChooseTargetsDecision)
        d.submitTargetSelection(me, listOf(oppSpell))

        // Resolve the ETB (counter + manifest dread) until the opponent's manifest pick pauses.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The countered spell is in the opponent's graveyard.
        d.getGraveyard(opp) shouldContain oppSpell

        // The OPPONENT (the countered spell's controller) is the one choosing which card to manifest.
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
