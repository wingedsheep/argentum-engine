package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Goblin Javelineer — "Whenever this creature becomes blocked, it deals 1 damage to target creature
 * blocking it." The trigger's target uses the source-relative `blockingSource()` filter: only the
 * Javelineer's own blockers are legal, not a creature blocking a different attacker.
 */
class GoblinJavelineerScenarioTest : ScenarioTestBase() {

    init {
        test("becomes-blocked trigger may target only its own blockers and deals 1 damage") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Goblin Javelineer")       // 1/1 haste
                .withCardOnBattlefield(1, "Centaur Courser")         // unrelated attacker
                .withCardOnBattlefield(2, "Savannah Lions")          // blocks Javelineer
                .withCardOnBattlefield(2, "Goblin Guide")            // blocks Javelineer
                .withCardOnBattlefield(2, "Deathtouch Rat")          // blocks the Courser
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            val lions = game.findPermanent("Savannah Lions")!!
            val guide = game.findPermanent("Goblin Guide")!!

            game.declareAttackers(mapOf("Goblin Javelineer" to 2, "Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Goblin Javelineer"),
                    "Goblin Guide" to listOf("Goblin Javelineer"),
                    "Deathtouch Rat" to listOf("Centaur Courser")
                )
            )

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0].orEmpty() shouldContainExactlyInAnyOrder listOf(lions, guide)

            game.selectTargets(listOf(lions))
            game.resolveStack()

            // The targeted blocker (toughness 1) took 1 damage and died before combat damage...
            game.isOnBattlefield("Savannah Lions") shouldBe false
            // ...while the other blocker and the Courser's blocker are untouched.
            game.isOnBattlefield("Goblin Guide") shouldBe true
            game.isOnBattlefield("Deathtouch Rat") shouldBe true
        }
    }
}
