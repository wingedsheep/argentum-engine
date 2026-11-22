package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Awe for the Guilds — "Monocolored creatures can't block this turn."
 *
 * First card to put the monocolored predicate in a dynamic group restriction: a one-colour
 * creature can't block, while a multicoloured and a colorless creature still can.
 */
class AweForTheGuildsScenarioTest : ScenarioTestBase() {

    init {
        fun board(): TestGame = scenario()
            .withPlayers("Caster", "Opponent")
            .withCardInHand(1, "Awe for the Guilds")
            .withLandsOnBattlefield(1, "Mountain", 3)
            .withCardOnBattlefield(1, "Hill Giant")
            .withCardOnBattlefield(2, "Grizzly Bears")
            .withCardOnBattlefield(2, "Centaur Healer")
            .withCardOnBattlefield(2, "Memnite")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        fun TestGame.castAndAttack() {
            castSpell(1, "Awe for the Guilds").error shouldBe null
            resolveStack()
            passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            declareAttackers(mapOf("Hill Giant" to 2)).error shouldBe null
            passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        }

        test("a monocolored creature can't block") {
            val game = board()
            game.castAndAttack()
            game.declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant"))).error shouldNotBe null
        }

        test("a multicolored creature can still block") {
            val game = board()
            game.castAndAttack()
            game.declareBlockers(mapOf("Centaur Healer" to listOf("Hill Giant"))).error shouldBe null
        }

        test("a colorless creature isn't monocolored and can still block") {
            val game = board()
            game.castAndAttack()
            game.declareBlockers(mapOf("Memnite" to listOf("Hill Giant"))).error shouldBe null
        }
    }
}
