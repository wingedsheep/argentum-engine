package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.TicketBoothTunnelOfHate
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Ticket Booth // Tunnel of Hate (DSK 158) — split-layout Room (CR 709.5).
 *
 * Ticket Booth {2}{R}   — "When you unlock this door, manifest dread." Casting the half enters it
 *                          unlocked, firing the trigger (the shared manifest-dread recipe).
 * Tunnel of Hate {4}{R}{R} — "Whenever you attack, target attacking creature gains double strike
 *                          until end of turn."
 */
class TicketBoothTunnelOfHateTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(TicketBoothTunnelOfHate)
        d.initMirrorMatch(
            deck = Deck.of("Mountain" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )
        return d
    }

    test("casting Ticket Booth unlocks its door and manifests dread") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top two of library for manifest dread: a creature to manifest, a land to bin.
        d.putCardOnTopOfLibrary(p1, "Mountain")
        val creature = d.putCardOnTopOfLibrary(p1, "Grizzly Bears")

        val roomId = d.putCardInHand(p1, TicketBoothTunnelOfHate.name)
        d.giveMana(p1, Color.RED, 1)
        d.giveColorlessMana(p1, 2)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))

        // Resolve the Room onto the battlefield, then the unlock trigger, until manifest pauses.
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe setOf(RoomFaceId("Ticket Booth"))

        // Manifest dread pauses to choose which of the looked-at two to manifest.
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The chosen card is now a face-down 2/2 manifested creature.
        d.state.getEntity(creature)?.get<FaceDownComponent>() shouldBe FaceDownComponent
        d.state.getEntity(creature)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
    }

    test("Tunnel of Hate gives a target attacking creature double strike when you attack") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Tunnel of Hate ({4}{R}{R}, face 1).
        val roomId = d.putCardInHand(p1, TicketBoothTunnelOfHate.name)
        d.giveMana(p1, Color.RED, 2)
        d.giveColorlessMana(p1, 4)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass()
        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe setOf(RoomFaceId("Tunnel of Hate"))

        // An attacker is ready.
        val attacker = d.putCreatureOnBattlefield(p1, "Grizzly Bears")
        d.removeSummoningSickness(attacker)
        d.state.projectedState.hasKeyword(attacker, Keyword.DOUBLE_STRIKE) shouldBe false

        // Declaring attackers fires "Whenever you attack"; the trigger targets the attacker.
        d.passPriorityUntil(Step.DECLARE_ATTACKERS)
        d.declareAttackers(p1, listOf(attacker), p2)
        d.submitTargetSelection(p1, listOf(attacker))
        d.bothPass()

        // The attacker now has double strike for the turn.
        d.state.projectedState.hasKeyword(attacker, Keyword.DOUBLE_STRIKE) shouldBe true
    }
})
