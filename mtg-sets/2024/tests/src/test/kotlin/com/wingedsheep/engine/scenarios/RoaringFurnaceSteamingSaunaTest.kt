package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.MaximumHandSize
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.RoaringFurnaceSteamingSauna
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * End-to-end scenario for `Roaring Furnace // Steaming Sauna` (DSK 230), a UR Room.
 *
 * Roaring Furnace {1}{R}: "When you unlock this door, this Room deals damage equal to the number
 * of cards in your hand to target creature an opponent controls."
 * Steaming Sauna {3}{U}{U}: "You have no maximum hand size. At the beginning of your end step,
 * draw a card."
 */
class RoaringFurnaceSteamingSaunaTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(RoaringFurnaceSteamingSauna)
        d.initMirrorMatch(
            deck = Deck.of(
                "Mountain" to 15,
                "Island" to 15,
                "Grizzly Bears" to 10,
            ),
            skipMulligans = true,
        )
        return d
    }

    fun GameTestDriver.maxHandSize(playerId: com.wingedsheep.sdk.model.EntityId): Int? = MaximumHandSize.effective(
        state, playerId, cardRegistry, services.conditionEvaluator, services.conditionEvaluator.amounts
    )

    test("unlocking Roaring Furnace deals damage equal to cards in hand to an opponent's creature") {
        val d = driver()
        val p1 = d.activePlayer!!
        val p2 = d.getOpponent(p1)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Opponent controls a 2/2 to be the damage target.
        val bears = d.putPermanentOnBattlefield(p2, "Grizzly Bears")

        // Cast Roaring Furnace (face 0, {1}{R}); it enters unlocked, firing the unlock trigger.
        val roomId = d.putCardInHand(p1, RoaringFurnaceSteamingSauna.name)
        // Give p1 two spare cards in hand so the damage is exactly 2 (lethal to the 2/2).
        d.putCardInHand(p1, "Grizzly Bears")
        d.putCardInHand(p1, "Grizzly Bears")
        d.giveMana(p1, Color.RED, 2)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        // Resolve the Room spell; it enters unlocked and the "when you unlock this door" trigger
        // goes on the stack, prompting for its target.
        d.bothPass()

        // The unlock trigger targets a creature an opponent controls.
        d.submitTargetSelection(p1, listOf(bears))
        d.bothPass()

        val room = d.state.getEntity(roomId)?.get<RoomComponent>()
        room shouldNotBe null
        room!!.unlocked shouldBe setOf(RoomFaceId("Roaring Furnace"))
        // Steaming Sauna's door is still locked, so its "no maximum hand size" doesn't apply (CR 709.5).
        d.maxHandSize(p1) shouldBe 7

        // 2 cards in hand -> 2 damage -> the 2/2 dies (moves to its owner's graveyard).
        d.findPermanent(p2, "Grizzly Bears") shouldBe null
        (bears in d.getGraveyard(p2)) shouldBe true
    }

    test("Steaming Sauna grants no maximum hand size and an end-step draw") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Steaming Sauna (face 1, {3}{U}{U}).
        val roomId = d.putCardInHand(p1, RoaringFurnaceSteamingSauna.name)
        d.giveMana(p1, Color.BLUE, 5)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass()

        val room = d.state.getEntity(roomId)?.get<RoomComponent>()!!
        room.unlocked shouldBe setOf(RoomFaceId("Steaming Sauna"))
        d.maxHandSize(p1) shouldBe null

        // End step: the "draw a card" trigger fires.
        val handBefore = d.getHand(p1).size
        d.passPriorityUntil(Step.END)
        d.bothPass()
        d.getHand(p1).size shouldBe handBefore + 1
    }
})
