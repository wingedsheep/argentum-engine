package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.core.DistributeDecision
import com.wingedsheep.engine.core.DistributionResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Invoke Justice (NEO) — "Return target permanent card from your graveyard to the battlefield, then
 * distribute four +1/+1 counters among any number of creatures and/or Vehicles target player controls."
 *
 * Two targets of different kinds (a graveyard card and a player) feed one filter: the counter
 * recipients are resolved after the return, among the creatures and Vehicles the *targeted player*
 * controls — so the returned creature is eligible, an uncrewed Vehicle is eligible, and the other
 * player's creatures are not.
 */
class InvokeJusticeScenarioTest : ScenarioTestBase() {

    init {
        test("returns the card, then distributes four +1/+1 counters among the target player's creatures and Vehicles") {
            val game = scenario()
                .withPlayers("P1", "P2")
                .withCardInHand(1, "Invoke Justice")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 5)
                .withCardOnBattlefield(1, "Raiders' Karve")
                .withCardOnBattlefield(2, "Glory Seeker")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val spell = game.findCardsInHand(1, "Invoke Justice").single()
            val karve = game.findPermanent("Raiders' Karve")!!
            val opposing = game.findPermanent("Glory Seeker")!!

            val result = game.execute(
                CastSpell(
                    playerId = game.player1Id,
                    cardId = spell,
                    targets = listOf(
                        ChosenTarget.Card(bearsCard, game.player1Id, Zone.GRAVEYARD),
                        ChosenTarget.Player(game.player1Id),
                    ),
                )
            )
            withClue("casting Invoke Justice should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")
            withClue("Grizzly Bears returned to the battlefield") { (bears != null) shouldBe true }

            val decision = game.getPendingDecision()
            withClue("resolution pauses to distribute the four counters") { (decision is DistributeDecision) shouldBe true }
            decision as DistributeDecision
            withClue("only the targeted player's creatures and Vehicles are eligible — returned Bears included") {
                decision.targets shouldContainExactlyInAnyOrder listOf(bears!!, karve)
                decision.totalAmount shouldBe 4
            }

            game.submitDecision(DistributionResponse(decision.id, mapOf(bears!! to 3, karve to 1)))

            fun counters(id: EntityId) =
                game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0
            counters(bears) shouldBe 3
            counters(karve) shouldBe 1
            counters(opposing) shouldBe 0
        }
    }
}
