package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.InquisitiveGlimmer
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnholyAnnexRitualChamber
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

/**
 * Inquisitive Glimmer (DSK 217): "Enchantment spells you cast cost {1} less to cast. Unlock costs
 * you pay cost {1} less." Proves the new [com.wingedsheep.sdk.scripting.ModifySpecialActionCost] reduces
 * the door-unlock special action through `SpecialActionCostReducer`, and confirms the enchantment-spell
 * half rides the existing `ModifySpellCost` machinery.
 */
class InquisitiveGlimmerScenarioTest : FunSpec({

    // A vanilla {2} enchantment used only to observe the enchantment-spell discount.
    val testEnchantment = card("Glimmer Test Sigil") {
        manaCost = "{2}"
        typeLine = "Enchantment"
    }

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(InquisitiveGlimmer)
        d.registerCard(UnholyAnnexRitualChamber)
        d.registerCard(testEnchantment)
        d.initMirrorMatch(
            deck = Deck.of("Swamp" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )
        return d
    }

    test("with Inquisitive Glimmer out, the {3}{B}{B} unlock costs only {2}{B}{B} — four mana is enough") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, InquisitiveGlimmer.name)

        // Cast Unholy Annex (face 0).
        val roomId = d.putCardInHand(p1, UnholyAnnexRitualChamber.name)
        d.giveMana(p1, Color.BLACK, 3)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        d.bothPass()

        // Ritual Chamber's printed unlock is {3}{B}{B} (5 mana). With Glimmer it is {2}{B}{B}
        // (4 mana). Give exactly four black — only enough if the reduction applied.
        d.giveMana(p1, Color.BLACK, 4)
        d.submitSuccess(UnlockRoomDoor(p1, roomId, RoomFaceId("Ritual Chamber")))
        d.bothPass()

        d.state.getEntity(roomId)?.get<RoomComponent>()!!.isFullyUnlocked shouldBe true
    }

    test("without Inquisitive Glimmer, four mana cannot pay the {3}{B}{B} unlock") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        val roomId = d.putCardInHand(p1, UnholyAnnexRitualChamber.name)
        d.giveMana(p1, Color.BLACK, 3)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        d.bothPass()

        // No Glimmer in play: the unlock still costs the full {3}{B}{B}, so four mana is short.
        d.giveMana(p1, Color.BLACK, 4)
        d.submitExpectFailure(UnlockRoomDoor(p1, roomId, RoomFaceId("Ritual Chamber")))
        d.state.getEntity(roomId)?.get<RoomComponent>()!!.isFullyUnlocked shouldBe false
    }

    test("with Inquisitive Glimmer out, a {2} enchantment spell costs only {1}") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        d.putCreatureOnBattlefield(p1, InquisitiveGlimmer.name)

        val sigil = d.putCardInHand(p1, testEnchantment.name)
        d.giveMana(p1, Color.BLACK, 1)
        d.submitSuccess(CastSpell(p1, sigil))
        d.bothPass()

        (sigil in d.state.getBattlefield()) shouldBe true
    }
})
