package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsc.cards.SpikedCorridorTorturePit
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Spiked Corridor // Torture Pit (DSC 28), a split-layout Room (CR 709.5).
 *
 * Spiked Corridor {3}{R} — "When you unlock this door, create three 1/1 red Devil creature tokens
 *                           with 'When this token dies, it deals 1 damage to any target.'"
 * Torture Pit {3}{R}     — "If a source you control would deal noncombat damage to an opponent, it
 *                           deals that much damage plus 2 instead."
 */
class SpikedCorridorTorturePitScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(SpikedCorridorTorturePit)
        d.initMirrorMatch(deck = Deck.of("Mountain" to 40), skipMulligans = true, startingLife = 20)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)
        return d
    }

    fun GameTestDriver.castDoor(me: EntityId, faceIndex: Int): EntityId {
        val roomId = putCardInHand(me, SpikedCorridorTorturePit.name)
        giveMana(me, Color.RED, 4)
        submitSuccess(CastSpell(me, roomId, faceIndex = faceIndex))
        var guard = 0
        while ((state.stack.isNotEmpty() || pendingDecision != null) && guard++ < 10) bothPass()
        return roomId
    }

    fun GameTestDriver.devils(me: EntityId): Int = getCreatures(me).count { getCardName(it) == "Devil Token" || getCardName(it) == "Devil" }

    fun GameTestDriver.bolt(me: EntityId, target: EntityId) {
        val bolt = putCardInHand(me, "Lightning Bolt")
        giveMana(me, Color.RED, 1)
        castSpell(me, bolt, listOf(target))
        bothPass()
    }

    test("casting Spiked Corridor creates three Devils; Torture Pit stays locked") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.castDoor(me, faceIndex = 0)

        d.devils(me) shouldBe 3
        d.bolt(me, opp)
        d.getLifeTotal(opp) shouldBe 17
    }

    test("Torture Pit adds 2 to noncombat damage your sources deal to an opponent, not to you") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.castDoor(me, faceIndex = 1)
        d.devils(me) shouldBe 0

        d.bolt(me, opp)
        d.getLifeTotal(opp) shouldBe 15
        d.bolt(me, me)
        d.getLifeTotal(me) shouldBe 17
    }

    test("an opponent's source isn't boosted against you") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        d.castDoor(me, faceIndex = 1)

        // The opponent's own Bolt aimed at me: Torture Pit only boosts sources *I* control.
        val bolt = d.putCardInHand(opp, "Lightning Bolt")
        d.giveMana(opp, Color.RED, 1)
        d.passPriority(me)
        d.castSpell(opp, bolt, listOf(me))
        d.bothPass()
        d.getLifeTotal(me) shouldBe 17
    }

    test("unlocking Torture Pit after Spiked Corridor turns the damage bonus on") {
        val d = driver()
        val me = d.activePlayer!!
        val opp = d.getOpponent(me)
        val roomId = d.castDoor(me, faceIndex = 0)

        d.giveMana(me, Color.RED, 4)
        d.submitSuccess(UnlockRoomDoor(me, roomId, RoomFaceId("Torture Pit")))
        d.bolt(me, opp)
        d.getLifeTotal(opp) shouldBe 15
    }
})
