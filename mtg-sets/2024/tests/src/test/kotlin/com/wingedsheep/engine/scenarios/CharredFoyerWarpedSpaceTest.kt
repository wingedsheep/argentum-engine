package com.wingedsheep.engine.scenarios

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.UnlockRoomDoor
import com.wingedsheep.engine.state.components.battlefield.MayCastWithoutPayingCostUsedThisTurnComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.CharredFoyerWarpedSpace
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import com.wingedsheep.engine.core.Outcome

/**
 * Scenario for `Charred Foyer // Warped Space` (DSK 129), a split-layout Room (CR 709.5).
 *
 * Charred Foyer {3}{R} — "At the beginning of your upkeep, exile the top card of your library. You
 *   may play it this turn." — standard impulse draw.
 * Warped Space {4}{R}{R} — "Once each turn, you may pay {0} rather than pay the mana cost for a
 *   spell you cast from exile." — the `MayCastWithoutPayingManaCost(fromZones = setOf(Zone.EXILE),
 *   oncePerTurn = true)` free-cast static.
 *
 * The deck is all Grizzly Bears so the impulse always exiles a castable creature; p1 is never given
 * mana for the impulse-exiled spell, so it can only resolve via Warped Space's {0} cost.
 */
class CharredFoyerWarpedSpaceTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(CharredFoyerWarpedSpace)
        d.initMirrorMatch(
            deck = Deck.of("Grizzly Bears" to 40),
            skipMulligans = true,
        )
        return d
    }

    test("Warped Space lets a Charred Foyer impulse-exiled spell be cast from exile for free") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Cast Charred Foyer (face 0) and unlock Warped Space too, so both doors function.
        val roomId = d.putCardInHand(p1, CharredFoyerWarpedSpace.name)
        d.giveMana(p1, Color.RED, 4)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        d.bothPass()
        d.giveMana(p1, Color.RED, 6)
        d.submitSuccess(UnlockRoomDoor(p1, roomId, RoomFaceId("Warped Space")))

        // Advance to p1's next turn; the Charred Foyer upkeep impulse fires along the way, exiling
        // the top card (a Grizzly Bears) and granting permission to play it this turn.
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // opponent's turn
        d.passPriorityUntil(Step.END)
        d.passPriorityUntil(Step.PRECOMBAT_MAIN) // p1's next turn — impulse already resolved at upkeep

        val exiledBears = d.getExile(p1).firstOrNull { id ->
            d.state.getEntity(id)?.get<CardComponent>()?.name == "Grizzly Bears"
        }
        exiledBears shouldNotBe null

        // p1 has no mana — the only way to cast the exiled spell is Warped Space's {0} cost.
        val creaturesBefore = d.getCreatures(p1).size
        d.submitSuccess(CastSpell(p1, exiledBears!!, useWithoutPayingManaCost = true))
        d.bothPass()

        d.getCreatures(p1).size shouldBe creaturesBefore + 1
        // Warped Space's once-per-turn permission is now spent.
        d.state.getEntity(roomId)?.get<MayCastWithoutPayingCostUsedThisTurnComponent>() shouldNotBe null
    }

    test("Warped Space does not make a spell cast from hand free (fromZones gate)") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Unlock Warped Space.
        val roomId = d.putCardInHand(p1, CharredFoyerWarpedSpace.name)
        d.giveMana(p1, Color.RED, 6)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass()

        // A free cast of a hand spell is refused — Warped Space only frees spells cast from exile.
        val handBears = d.putCardInHand(p1, "Grizzly Bears")
        val result = d.submit(CastSpell(p1, handBears, useWithoutPayingManaCost = true))
        result.outcome shouldNotBe Outcome.Done
    }
})
