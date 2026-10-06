package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.core.SubmitDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class InfiltrationLensScenarioTest : ScenarioTestBase() {
    init {
        for ((accept, removeLens) in listOf(true to false, false to false, true to true)) {
            test("two blockers each offer two cards, accept=$accept, removeLens=$removeLens") {
                val game = scenario()
                    .withPlayers()
                    .withCardOnBattlefield(1, "Hill Giant")
                    .withCardOnBattlefield(1, "Infiltration Lens")
                    .withCardOnBattlefield(1, "Plains")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardOnBattlefield(1, "Mountain")
                    .withCardInHand(1, "Shatter")
                    .withCardOnBattlefield(2, "Wind Drake")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .apply { repeat(8) { withCardInLibrary(1, "Plains") } }
                    .apply { repeat(8) { withCardInLibrary(2, "Island") } }
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.execute(ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = game.findPermanent("Infiltration Lens")!!,
                    abilityId = cardRegistry.requireCard("Infiltration Lens").activatedAbilities.single().id,
                    targets = listOf(ChosenTarget.Permanent(game.findPermanent("Hill Giant")!!)),
                )).error shouldBe null
                game.resolveStack()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val before = game.handSize(1)
                game.declareBlockers(mapOf("Wind Drake" to listOf("Hill Giant"), "Grizzly Bears" to listOf("Hill Giant"))).error shouldBe null
                if (game.state.pendingDecision == null) game.passPriority()
                val order = game.state.pendingDecision
                if (order is OrderObjectsDecision) {
                    game.execute(SubmitDecision(order.playerId, OrderedResponse(order.id, order.objects))).error shouldBe null
                }
                if (removeLens) {
                    game.castSpell(1, "Shatter", game.findPermanent("Infiltration Lens")!!).error shouldBe null
                }
                repeat(2) {
                    game.resolveStack()
                    (game.state.pendingDecision is YesNoDecision) shouldBe true
                    game.answerYesNo(accept).error shouldBe null
                }
                game.resolveStack()
                game.handSize(1) shouldBe before + (if (accept) 4 else 0) - (if (removeLens) 1 else 0)
                game.state.stack.size shouldBe 0
                if (removeLens) game.findPermanent("Infiltration Lens") shouldBe null
            }
        }
    }
}
