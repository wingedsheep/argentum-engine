package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Sanctum Lurker (Reality Fracture #64) — {2}{B} Creature — Horror 3/2:
 *   When this creature enters, empower Jace 1.
 *   Planeswalkers you control aren't put into their owners' graveyards for having 0 loyalty.
 *   Planeswalkers you control have "[+2]: This planeswalker deals 1 damage to each opponent and
 *   you gain 1 life."
 *
 * Pins the `SURVIVES_ZERO_LOYALTY` exemption from CR 704.5i — including that the planeswalker
 * goes to the graveyard at the next state-based-action check once the Lurker leaves — and the
 * granted +2.
 */
class SanctumLurkerScenarioTest : ScenarioTestBase() {
    init {
        test("a planeswalker at 0 loyalty stays while the Lurker is around, and dies once it leaves") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Sanctum Lurker")
                .withCardInHand(1, "Lightning Bolt")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withLandsOnBattlefield(1, "Mountain", 1)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Sanctum Lurker").error shouldBe null
            game.resolveStack()

            val jace = game.findPermanents("Jace").single()
            fun loyalty() = game.state.getEntity(jace)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) ?: 0
            loyalty() shouldBe 1

            val surveil = game.getLegalActions(1)
                .first { (it.action as? ActivateAbility)?.sourceId == jace && it.description.contains("Surveil") }
            game.execute(surveil.action).error shouldBe null
            loyalty() shouldBe 0
            game.resolveStack()
            // Surveil 1: keep the card (one card on top needs no ordering).
            game.skipSelection()

            game.findPermanents("Jace") shouldBe listOf(jace)

            val lurker = game.findPermanent("Sanctum Lurker")!!
            game.castSpell(1, "Lightning Bolt", lurker).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Sanctum Lurker") shouldBe false
            game.findPermanents("Jace") shouldBe emptyList()
        }

        test("planeswalkers gain the +2 that pings each opponent and gains a life") {
            val game = scenario().withPlayers()
                .withCardInHand(1, "Sanctum Lurker")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Swamp")
                .withCardInLibrary(2, "Swamp")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()

            game.castSpell(1, "Sanctum Lurker").error shouldBe null
            game.resolveStack()

            val jace = game.findPermanents("Jace").single()
            val plusTwo = game.getLegalActions(1)
                .first { (it.action as? ActivateAbility)?.sourceId == jace && it.description.contains("each opponent") }
            game.execute(plusTwo.action).error shouldBe null
            game.state.getEntity(jace)?.get<CountersComponent>()?.getCount(CounterType.LOYALTY) shouldBe 3
            game.resolveStack()

            game.getLifeTotal(2) shouldBe 19
            game.getLifeTotal(1) shouldBe 21
        }
    }
}
