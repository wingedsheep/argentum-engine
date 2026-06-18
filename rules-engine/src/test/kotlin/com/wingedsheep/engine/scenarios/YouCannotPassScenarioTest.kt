package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * You Cannot Pass! — "Destroy target creature that blocked or was blocked by a legendary creature
 * this turn." Exercises the new `BlockedOrWasBlockedByLegendaryThisTurn` predicate (stamped at
 * block declaration) in both directions, plus rejection of a creature that never fought a legendary.
 */
class YouCannotPassScenarioTest : ScenarioTestBase() {

    init {
        test("destroys a creature that blocked a legendary creature this turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Naban, Dean of Iteration") // legendary attacker
                .withCardOnBattlefield(2, "Grizzly Bears")            // non-legendary blocker
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "You Cannot Pass!")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Naban, Dean of Iteration" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Naban, Dean of Iteration")))
            game.passPriority() // blocking player passes; active player (P1) gets priority

            val target = game.findPermanent("Grizzly Bears")!!
            game.castSpell(1, "You Cannot Pass!", target).error shouldBe null
            game.resolveStack()

            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("destroys a creature that was blocked by a legendary creature this turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Grizzly Bears")            // non-legendary attacker
                .withCardOnBattlefield(2, "Naban, Dean of Iteration") // legendary blocker
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "You Cannot Pass!")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Naban, Dean of Iteration" to listOf("Grizzly Bears")))
            game.passPriority()

            // The attacker (blocked by a legendary) is a legal target.
            game.castSpell(1, "You Cannot Pass!", game.findPermanent("Grizzly Bears")!!).error shouldBe null
            game.resolveStack()
            game.isOnBattlefield("Grizzly Bears") shouldBe false
        }

        test("cannot target a creature that never fought a legendary this turn") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Naban, Dean of Iteration") // legendary attacker
                .withCardOnBattlefield(2, "Grizzly Bears")            // blocks the legendary
                .withCardOnBattlefield(2, "Savannah Lions")           // bystander, never in combat
                .withLandsOnBattlefield(1, "Plains", 1)
                .withCardInHand(1, "You Cannot Pass!")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Naban, Dean of Iteration" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Naban, Dean of Iteration")))
            game.passPriority()

            // The bystander did not block or get blocked by anything — illegal target.
            val bystander = game.findPermanent("Savannah Lions")!!
            game.castSpell(1, "You Cannot Pass!", bystander).error shouldNotBe null
            game.isOnBattlefield("Savannah Lions") shouldBe true
        }
    }
}
