package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.OrderObjectsDecision
import com.wingedsheep.engine.core.OrderedResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Grasping Giant (THB) — "Whenever this creature becomes blocked by a creature, exile that creature
 * until this creature leaves the battlefield."
 *
 * Covers the per-blocker trigger, the Giant staying blocked after its blockers vanish, the return
 * when the Giant leaves, and the ruling that nothing is exiled if the Giant leaves before the
 * trigger resolves.
 */
class GraspingGiantScenarioTest : ScenarioTestBase() {

    private fun TestGame.resolveAll() {
        var guard = 0
        resolveStack()
        while (hasPendingDecision() && guard++ < 10) {
            when (val decision = getPendingDecision()!!) {
                is OrderObjectsDecision -> submitDecision(OrderedResponse(decision.id, decision.objects))
                else -> error("unexpected decision ${decision::class.simpleName}")
            }
            resolveStack()
        }
    }

    private fun blockedGame(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Grasping Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInHand(1, "Murder")
            .withLandsOnBattlefield(1, "Swamp", 3)
            .withCardInHand(2, "Murder")
            .withLandsOnBattlefield(2, "Swamp", 3)
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()

        game.declareAttackers(mapOf("Grasping Giant" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(
            mapOf("Grizzly Bears" to listOf("Grasping Giant"), "Hill Giant" to listOf("Grasping Giant"))
        ).error shouldBe null
        return game
    }

    init {
        test("each blocker is exiled, the Giant stays blocked, and they return when it leaves") {
            val game = blockedGame()
            game.resolveAll()

            withClue("one trigger per blocking creature exiled both blockers") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isInExile(2, "Hill Giant") shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
            withClue("the Giant remained blocked, so it dealt no combat damage") {
                game.getLifeTotal(2) shouldBe 20
            }

            game.castSpell(1, "Murder", game.findPermanent("Grasping Giant")!!).error shouldBe null
            game.resolveAll()

            game.isInGraveyard(1, "Grasping Giant") shouldBe true
            withClue("the exiled blockers return as soon as the Giant leaves") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
                game.isOnBattlefield("Hill Giant") shouldBe true
                game.isInExile(2, "Grizzly Bears") shouldBe false
            }
        }

        test("nothing is exiled if the Giant leaves before the trigger resolves") {
            val game = blockedGame()
            var guard = 0
            while (game.hasPendingDecision() && guard++ < 5) {
                val decision = game.getPendingDecision() as OrderObjectsDecision
                game.submitDecision(OrderedResponse(decision.id, decision.objects))
            }

            game.castSpell(2, "Murder", game.findPermanent("Grasping Giant")!!).error shouldBe null
            game.resolveAll()

            game.isInGraveyard(1, "Grasping Giant") shouldBe true
            game.isOnBattlefield("Grizzly Bears") shouldBe true
            game.isOnBattlefield("Hill Giant") shouldBe true
            game.isInExile(2, "Grizzly Bears") shouldBe false
            game.isInExile(2, "Hill Giant") shouldBe false
        }
    }
}
