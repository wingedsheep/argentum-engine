package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe

/**
 * Giant Adephage (GTC) — Trample. Whenever this creature deals combat damage to a player, create a
 * token that's a copy of this creature.
 */
class GiantAdephageScenarioTest : ScenarioTestBase() {
    init {
        test("dealing combat damage to a player creates a token copy of itself") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Giant Adephage", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Giant Adephage" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("7 trample damage hit the opponent and a copy was created") {
                game.getLifeTotal(2) shouldBe 13
                game.findPermanents("Giant Adephage") shouldHaveSize 2
            }
        }

        test("blocked and dealing no damage to a player, it makes no copy") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Giant Adephage")
                .withCardOnBattlefield(2, "Grizzly Bears", summoningSickness = false)
                .withCardInLibrary(1, "Forest")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(2)
                .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                .build()

            game.declareAttackers(mapOf("Grizzly Bears" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Giant Adephage" to listOf("Grizzly Bears"))).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
            game.resolveStack()

            withClue("damage to a creature doesn't trigger it") {
                game.findPermanents("Giant Adephage") shouldHaveSize 1
            }
        }
    }
}
