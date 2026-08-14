package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CardsSelectedResponse
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.FaceDownModeComponent
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.engine.state.components.identity.RoomComponent
import com.wingedsheep.engine.state.components.identity.RoomFaceId
import com.wingedsheep.engine.support.GameTestDriver
import com.wingedsheep.engine.support.TestCards
import com.wingedsheep.mtg.sets.definitions.dsk.cards.UnderwaterTunnelSlimyAquarium
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.Deck
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario for `Underwater Tunnel // Slimy Aquarium` (DSK 79), a split-layout Room (CR 709.5).
 *
 * Underwater Tunnel {U}  — "When you unlock this door, surveil 2." Casting the half enters it
 *                          unlocked, firing the trigger.
 * Slimy Aquarium {3}{U}  — "When you unlock this door, manifest dread, then put a +1/+1 counter
 *                          on that creature."
 */
class UnderwaterTunnelSlimyAquariumScenarioTest : FunSpec({

    fun driver(): GameTestDriver {
        val d = GameTestDriver()
        d.registerCards(TestCards.all)
        d.registerCard(UnderwaterTunnelSlimyAquarium)
        d.initMirrorMatch(
            deck = Deck.of("Island" to 20, "Grizzly Bears" to 20),
            skipMulligans = true,
        )
        return d
    }

    test("casting Underwater Tunnel unlocks its door and surveils 2") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Stack the top two cards so we know what surveil looks at.
        val bottom = d.putCardOnTopOfLibrary(p1, "Island")
        val top = d.putCardOnTopOfLibrary(p1, "Centaur Courser") // now the top card

        // Cast Underwater Tunnel ({U}, face 0); the cast face enters unlocked, firing the trigger.
        val roomId = d.putCardInHand(p1, UnderwaterTunnelSlimyAquarium.name)
        d.giveMana(p1, Color.BLUE, 1)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 0))
        d.bothPass()

        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe
            setOf(RoomFaceId("Underwater Tunnel"))

        // The surveil 2 trigger resolves: choose to put the top creature into the graveyard.
        d.bothPass()
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(top)))

        // If a reorder of the remaining card is offered, keep it on top.
        (d.pendingDecision as? ReorderLibraryDecision)?.let {
            d.submitDecision(p1, OrderedResponse(it.id, it.cards))
        }

        d.getGraveyard(p1) shouldContainCardId top
        d.getGraveyard(p1) shouldNotContainCardId bottom
    }

    test("casting Slimy Aquarium unlocks its door, manifests dread, and adds a +1/+1 counter") {
        val d = driver()
        val p1 = d.activePlayer!!
        d.passPriorityUntil(Step.PRECOMBAT_MAIN)

        // Top two: a creature (to manifest) and a land beneath it.
        d.putCardOnTopOfLibrary(p1, "Island")
        val creature = d.putCardOnTopOfLibrary(p1, "Centaur Courser") // now the top card

        // Cast Slimy Aquarium ({3}{U}, face 1); the cast face enters unlocked, firing the trigger.
        val roomId = d.putCardInHand(p1, UnderwaterTunnelSlimyAquarium.name)
        d.giveMana(p1, Color.BLUE, 4)
        d.submitSuccess(CastSpell(p1, roomId, faceIndex = 1))
        d.bothPass()

        d.state.getEntity(roomId)!!.get<RoomComponent>()!!.unlocked shouldBe
            setOf(RoomFaceId("Slimy Aquarium"))

        // The trigger resolves: manifest dread pauses to choose which looked-at card to manifest.
        d.bothPass()
        val pick = d.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
        d.submitDecision(p1, CardsSelectedResponse(decisionId = pick.id, selectedCards = listOf(creature)))
        while (!d.isPaused && d.state.stack.isNotEmpty()) d.bothPass()

        // The creature is a manifested 2/2 with a +1/+1 counter from the second clause = 3/3.
        d.state.getEntity(creature)?.get<FaceDownModeComponent>()?.mode shouldBe FaceDownMode.MANIFEST
        val counters = d.state.getEntity(creature)?.get<CountersComponent>()
        counters shouldNotBe null
        counters!!.getCount(CounterType.PLUS_ONE_PLUS_ONE) shouldBe 1
        d.state.projectedState.getPower(creature) shouldBe 3
        d.state.projectedState.getToughness(creature) shouldBe 3
    }
})

private infix fun List<com.wingedsheep.sdk.model.EntityId>.shouldContainCardId(
    id: com.wingedsheep.sdk.model.EntityId
) {
    (id in this) shouldBe true
}

private infix fun List<com.wingedsheep.sdk.model.EntityId>.shouldNotContainCardId(
    id: com.wingedsheep.sdk.model.EntityId
) {
    (id in this) shouldBe false
}
