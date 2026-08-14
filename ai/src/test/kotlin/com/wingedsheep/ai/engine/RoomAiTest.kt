package com.wingedsheep.ai.engine

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.PassPriority
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnholyAnnexRitualChamber
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe

/**
 * The AI has to actually play Rooms (CR 709.5): cast a half, and pay to unlock the other door.
 *
 * Both halves of a Room carry their rules text on [com.wingedsheep.sdk.model.CardFace.script],
 * never on the card's top-level script — so anything that reads only the top level sees a blank
 * card. `Unholy Annex // Ritual Chamber` is the card the bug was reported on: the AI never cast the
 * Annex half (a repeatable draw engine that reads as a vanilla enchantment) and never unlocked the
 * Annex door (an unlock that only turns face text on moves nothing a name-keyed valuation can see).
 */
class RoomAiTest : FunSpec({

    val allCards = TestCards.all + UnholyAnnexRitualChamber

    fun driver(): GameTestDriver = GameTestDriver().apply {
        registerCards(allCards)
        initMirrorMatch(deck = Deck.of("Swamp" to 40), skipMulligans = true, startingPlayer = 0)
        passPriorityUntil(Step.PRECOMBAT_MAIN)
    }

    fun ai(playerId: EntityId): AIPlayer =
        AIPlayer.create(CardRegistry().apply { register(allCards) }, playerId, AiProfile.PRODUCTION)

    /** Untapped Swamps for the auto-tapper to find. */
    fun swamps(driver: GameTestDriver, playerId: EntityId, count: Int) {
        repeat(count) { driver.putLandOnBattlefield(playerId, "Swamp") }
    }

    /** Pass until the stack is empty — a Room's door-unlock trigger needs its own round. */
    fun settle(driver: GameTestDriver) {
        var rounds = 0
        while (driver.state.stack.isNotEmpty()) {
            withClue("the stack never emptied, so the test's premise was never set up") {
                rounds++ shouldBeLessThan 10
            }
            driver.bothPass()
        }
    }

    test("the AI casts the Unholy Annex half rather than passing") {
        val d = driver()
        val p1 = d.player1
        val roomId = d.putCardInHand(p1, UnholyAnnexRitualChamber.name)
        // {2}{B} affords the Annex half only — the choice under test is "cast this or not".
        swamps(d, p1, 3)

        val choices = d.legalActions(p1).filter {
            (it.action as? CastSpell)?.cardId == roomId || it.action is PassPriority
        }
        choices.count { it.action is CastSpell } shouldBe 1

        val chosen = ai(p1).chooseFrom(d.state, choices).action
        (chosen as? CastSpell)?.faceIndex shouldBe 0
    }

    test("the AI unlocks Ritual Chamber once Unholy Annex is on the battlefield") {
        val d = driver()
        val p1 = d.player1
        val roomId = d.putCardInHand(p1, UnholyAnnexRitualChamber.name)
        swamps(d, p1, 8)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        settle(d)
        d.state.getEntity(roomId)?.get<RoomComponent>()?.unlocked shouldBe setOf(RoomFaceId("Unholy Annex"))

        val choices = d.legalActions(p1).filter {
            it.action is UnlockRoomDoor || it.action is PassPriority
        }
        choices.count { it.action is UnlockRoomDoor } shouldBe 1

        val chosen = ai(p1).chooseFrom(d.state, choices).action
        (chosen as? UnlockRoomDoor)?.faceId shouldBe RoomFaceId("Ritual Chamber")
    }

    test("the AI unlocks Unholy Annex once Ritual Chamber is on the battlefield") {
        val d = driver()
        val p1 = d.player1
        val roomId = d.putCardInHand(p1, UnholyAnnexRitualChamber.name)
        swamps(d, p1, 8)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        settle(d)
        d.state.getEntity(roomId)?.get<RoomComponent>()?.unlocked shouldBe setOf(RoomFaceId("Ritual Chamber"))

        val choices = d.legalActions(p1).filter {
            it.action is UnlockRoomDoor || it.action is PassPriority
        }
        choices.count { it.action is UnlockRoomDoor } shouldBe 1

        val chosen = ai(p1).chooseFrom(d.state, choices).action
        (chosen as? UnlockRoomDoor)?.faceId shouldBe RoomFaceId("Unholy Annex")
    }
})
