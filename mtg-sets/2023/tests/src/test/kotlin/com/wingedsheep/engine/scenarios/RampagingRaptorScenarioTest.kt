package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.ProtectorComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Rampaging Raptor (MOM #160) — "Whenever this creature deals combat damage to an opponent, it
 * deals that much damage to target planeswalker that player controls or battle that player
 * protects."
 *
 * The board has one Invasion of Innistrad on each side. A Siege is protected by an opponent of its
 * controller (CR 310.12a), so the Raptor controller's own Siege is the one the *damaged opponent*
 * protects — the legal battle target — while the opponent's Siege is protected by the Raptor's
 * controller and must not be targetable. Likewise only the opponent's planeswalker qualifies.
 */
class RampagingRaptorScenarioTest : ScenarioTestBase() {

    private fun TestGame.controlledBy(name: String, player: EntityId): EntityId =
        findPermanents(name).single { state.getEntity(it)?.get<ControllerComponent>()?.playerId == player }

    private fun TestGame.counters(entity: EntityId, type: CounterType): Int =
        state.getEntity(entity)?.get<CountersComponent>()?.getCount(type) ?: 0

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Rampaging Raptor")
        .withCardOnBattlefield(1, "Invasion of Innistrad")  // protected by the opponent
        .withCardOnBattlefield(1, "Garruk Wildspeaker")     // your planeswalker — never a target
        .withCardOnBattlefield(2, "Invasion of Innistrad")  // protected by you — never a target
        .withCardOnBattlefield(2, "Jace Beleren")           // the opponent's planeswalker
        .withCardInLibrary(1, "Mountain")
        .withCardInLibrary(2, "Island")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.attackAndAwaitTrigger(): ChooseTargetsDecision {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Rampaging Raptor" to 2)).error shouldBe null
        passUntilPhase(Phase.COMBAT, Step.COMBAT_DAMAGE)
        var guard = 0
        while (state.pendingDecision !is ChooseTargetsDecision && guard++ < 20) resolveStack()
        return state.pendingDecision as? ChooseTargetsDecision
            ?: error("expected the Raptor's trigger to ask for a target; got ${state.pendingDecision}")
    }

    init {
        test("redirects the combat damage to a battle the damaged opponent protects") {
            val game = board()
            game.checkStateBasedActions()

            val yourSiege = game.controlledBy("Invasion of Innistrad", game.player1Id)
            val theirSiege = game.controlledBy("Invasion of Innistrad", game.player2Id)
            val jace = game.findPermanent("Jace Beleren")!!

            withClue("each Siege is protected by its controller's opponent") {
                game.state.getEntity(yourSiege)?.get<ProtectorComponent>()?.playerId shouldBe game.player2Id
                game.state.getEntity(theirSiege)?.get<ProtectorComponent>()?.playerId shouldBe game.player1Id
            }

            val td = game.attackAndAwaitTrigger()
            withClue("legal: the battle the opponent protects and the planeswalker they control only") {
                td.legalTargets[0]!!.shouldContainExactlyInAnyOrder(yourSiege, jace)
            }
            withClue("the Raptor dealt 4 combat damage to the opponent") {
                game.getLifeTotal(2) shouldBe 16
            }

            game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(yourSiege)))).error shouldBe null
            game.resolveStack()

            withClue("that much damage (4) removes 4 of the Siege's 5 defense counters") {
                game.counters(yourSiege, CounterType.DEFENSE) shouldBe 1
            }
            withClue("the battle protected by you is untouched") {
                game.counters(theirSiege, CounterType.DEFENSE) shouldBe 5
            }
        }

        test("can instead hit a planeswalker the damaged opponent controls") {
            val game = board()
            game.checkStateBasedActions()
            val jace = game.findPermanent("Jace Beleren")!!

            val td = game.attackAndAwaitTrigger()
            game.submitDecision(TargetsResponse(td.id, mapOf(0 to listOf(jace)))).error shouldBe null
            game.resolveStack()

            withClue("4 damage to a 3-loyalty Jace puts it in the graveyard") {
                game.isOnBattlefield("Jace Beleren") shouldBe false
                game.isInGraveyard(2, "Jace Beleren") shouldBe true
            }
            withClue("your own planeswalker was never in play as a target") {
                game.isOnBattlefield("Garruk Wildspeaker") shouldBe true
            }
        }
    }
}
