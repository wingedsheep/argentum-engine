package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Battle-Scarred Goblin — "Whenever this creature becomes blocked, it deals 1 damage to each
 * creature blocking it." Exercises the new source-relative `IsBlockingSource` predicate: only the
 * Goblin's own blockers take damage, not blockers of other attackers.
 */
class BattleScarredGoblinScenarioTest : ScenarioTestBase() {

    init {
        test("on becoming blocked, deals 1 to each of its blockers only") {
            val game = scenario()
                .withPlayers()
                .withCardOnBattlefield(1, "Battle-Scarred Goblin")   // 2/2
                .withCardOnBattlefield(1, "Centaur Courser")          // unrelated attacker
                .withCardOnBattlefield(2, "Savannah Lions")           // 1/1 blocks Goblin
                .withCardOnBattlefield(2, "Goblin Guide")             // 2/1 blocks Goblin
                .withCardOnBattlefield(2, "Deathtouch Rat")           // 1/1 blocks the Courser
                .withCardInLibrary(1, "Mountain")
                .withCardInLibrary(2, "Forest")
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Battle-Scarred Goblin" to 2, "Centaur Courser" to 2))
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)

            game.declareBlockers(
                mapOf(
                    "Savannah Lions" to listOf("Battle-Scarred Goblin"),
                    "Goblin Guide" to listOf("Battle-Scarred Goblin"),
                    "Deathtouch Rat" to listOf("Centaur Courser")
                )
            )
            // The becomes-blocked trigger goes on the stack; resolve it before combat damage.
            game.resolveStack()

            // The Goblin's two blockers (toughness 1) took 1 damage and died...
            game.isOnBattlefield("Savannah Lions") shouldBe false
            game.isOnBattlefield("Goblin Guide") shouldBe false
            // ...but the Courser's blocker was NOT touched by the Goblin (still pre-combat-damage).
            game.isOnBattlefield("Deathtouch Rat") shouldBe true
        }
    }
}
