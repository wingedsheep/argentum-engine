package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Isolation Zone — {2}{W}{W} Enchantment.
 * "When this enchantment enters, exile target creature or enchantment an opponent controls until
 * this enchantment leaves the battlefield."
 */
class IsolationZoneScenarioTest : ScenarioTestBase() {

    /** Resolve the stack, aiming any trigger at [target]. */
    private fun TestGame.drive(target: EntityId) {
        var guard = 0
        while (guard++ < 30) {
            when (val decision = getPendingDecision()) {
                is ChooseTargetsDecision -> selectTargets(listOf(target))
                null -> if (state.stack.isNotEmpty()) resolveStack() else return
                else -> error("Unexpected decision: $decision")
            }
        }
    }

    init {
        test("exiles an opponent's creature until the Zone leaves, then it returns under its owner's control") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Isolation Zone")
                .withCardInHand(1, "Disenchant")
                .withLandsOnBattlefield(1, "Plains", 6)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.castSpell(1, "Isolation Zone").error shouldBe null
            game.drive(bears)

            withClue("the Bears are exiled while the Zone stays") {
                game.isInExile(2, "Grizzly Bears") shouldBe true
                game.isOnBattlefield("Grizzly Bears") shouldBe false
            }

            val zone = game.findPermanent("Isolation Zone")!!
            game.castSpell(1, "Disenchant", zone).error shouldBe null
            game.drive(bears)

            game.isOnBattlefield("Isolation Zone") shouldBe false
            val returned = game.findPermanent("Grizzly Bears")
            withClue("the Bears come back once the Zone leaves") { returned shouldNotBe null }
            game.state.projectedState.getController(returned!!) shouldBe game.player2Id
        }

        test("your own creature isn't a legal target") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Isolation Zone")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val mine = game.findPermanent("Grizzly Bears")!!
            val theirs = game.findPermanent("Hill Giant")!!

            game.castSpell(1, "Isolation Zone").error shouldBe null
            var guard = 0
            while (game.getPendingDecision() !is ChooseTargetsDecision && guard++ < 10) game.resolveStack()
            val decision = game.getPendingDecision() as ChooseTargetsDecision
            decision.legalTargets[0].orEmpty().contains(mine) shouldBe false
            decision.legalTargets[0].orEmpty().contains(theirs) shouldBe true
        }

        test("an opponent's enchantment is a legal target too") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Isolation Zone")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardOnBattlefield(2, "Isolation Zone")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val theirs = game.findPermanent("Isolation Zone")!!

            game.castSpell(1, "Isolation Zone").error shouldBe null
            game.drive(theirs)

            game.isInExile(2, "Isolation Zone") shouldBe true
        }
    }
}
