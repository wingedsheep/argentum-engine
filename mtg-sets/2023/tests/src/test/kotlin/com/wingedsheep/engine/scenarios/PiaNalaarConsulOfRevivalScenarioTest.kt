package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.state.permissions.MayPlayPermission
import com.wingedsheep.engine.state.permissions.addMayPlayPermission
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.matchers.shouldBe

/**
 * Pia Nalaar, Consul of Revival — "Thopters you control have haste. Whenever you play a land from
 * exile or cast a spell from exile, create a 1/1 colorless Thopter artifact creature token with
 * flying."
 */
class PiaNalaarConsulOfRevivalScenarioTest : ScenarioTestBase() {

    private fun TestGame.grantPlay(card: EntityId) {
        val (id, withId) = state.newEntity()
        state = withId.addMayPlayPermission(MayPlayPermission(id, setOf(card), player1Id, timestamp = withId.timestamp))
    }

    private fun TestGame.thopters() = findPermanents("Thopter Token")

    init {
        test("playing a land from exile creates a hasty flying Thopter") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pia Nalaar, Consul of Revival")
                .withCardInExile(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mountain = g.state.getExile(g.player1Id).single()
            g.grantPlay(mountain)

            g.execute(PlayLand(g.player1Id, mountain)).error shouldBe null
            g.resolveStack()

            val thopter = g.thopters().single()
            g.state.projectedState.hasKeyword(thopter, Keyword.FLYING) shouldBe true
            g.state.projectedState.hasKeyword(thopter, Keyword.HASTE) shouldBe true
        }

        test("casting a spell from exile creates a Thopter") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pia Nalaar, Consul of Revival")
                .withCardInExile(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Forest", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            g.grantPlay(g.state.getExile(g.player1Id).single())

            g.castSpellFromExile(1, "Grizzly Bears").error shouldBe null
            g.resolveStack()

            g.thopters().size shouldBe 1
            g.isOnBattlefield("Grizzly Bears") shouldBe true
        }

        test("playing a land from hand creates nothing") {
            val g = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Pia Nalaar, Consul of Revival")
                .withCardInHand(1, "Mountain")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            g.execute(PlayLand(g.player1Id, g.state.getHand(g.player1Id).single())).error shouldBe null
            g.resolveStack()

            g.thopters() shouldBe emptyList()
        }
    }
}
