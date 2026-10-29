package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.state.components.combat.BlockedComponent
import com.wingedsheep.engine.state.components.combat.BlockingComponent
import com.wingedsheep.engine.state.components.identity.TokenComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

/**
 * Brimaz, King of Oreskos — the attack token enters attacking, the block token enters blocking the
 * creature Brimaz blocked (CR 509.4) and deals its damage to it.
 */
class BrimazKingOfOreskosScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveAll() {
        repeat(10) {
            val decision = state.pendingDecision
            if (decision is OrderObjectsDecision) submitDecision(OrderedResponse(decision.id, decision.objects))
            resolveStack()
        }
    }

    private fun TestGame.catTokens(): List<EntityId> =
        state.getBattlefield().filter { state.getEntity(it)?.has<TokenComponent>() == true }

    init {
        context("Brimaz, King of Oreskos") {

            test("attacking creates a vigilant 1/1 Cat Soldier that's attacking") {
                val game = scenario()
                    .withPlayers("Attacker", "Defender")
                    .withCardOnBattlefield(1, "Brimaz, King of Oreskos", summoningSickness = false)
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Brimaz, King of Oreskos" to 2)).error shouldBe null
                game.resolveAll()

                val token = game.catTokens().single()
                withClue("the token enters attacking the defender, untapped") {
                    game.state.getEntity(token)!!.has<AttackingComponent>() shouldBe true
                    game.state.getEntity(token)!!.has<TappedComponent>() shouldBe false
                }
                withClue("Brimaz has vigilance") {
                    game.state.getEntity(game.findPermanent("Brimaz, King of Oreskos")!!)!!.has<TappedComponent>() shouldBe false
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("3 from Brimaz + 1 from the token") { game.getLifeTotal(2) shouldBe 16 }
            }

            test("blocking creates a Cat Soldier blocking that creature, and both deal it damage") {
                val game = scenario()
                    .withPlayers("Defender", "Attacker")
                    .withCardOnBattlefield(1, "Brimaz, King of Oreskos")
                    .withCardOnBattlefield(2, "Hill Giant", summoningSickness = false)
                    .withActivePlayer(2)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Hill Giant" to 1)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Brimaz, King of Oreskos" to listOf("Hill Giant"))).error shouldBe null
                game.resolveAll()

                val giant = game.findPermanent("Hill Giant")!!
                val token = game.catTokens().single()
                withClue("the token blocks Hill Giant") {
                    game.state.getEntity(token)!!.get<BlockingComponent>()!!.blockedAttackerIds shouldBe listOf(giant)
                    game.state.getEntity(giant)!!.get<BlockedComponent>()!!.blockerIds shouldContainExactlyInAnyOrder
                        listOf(game.findPermanent("Brimaz, King of Oreskos")!!, token)
                }

                game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
                withClue("3 + 1 damage kills the 3/3 Hill Giant") { game.isInGraveyard(2, "Hill Giant") shouldBe true }
                game.getLifeTotal(1) shouldBe 20
            }
        }
    }
}
