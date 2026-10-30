package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Thwart the Enemy (IKO #173) — {2}{G} Instant.
 *
 *   "Prevent all damage that would be dealt this turn by creatures your opponents control."
 *
 * A source-side group shield: damage dealt by the opponent's creatures is prevented, while damage
 * dealt by the caster's own creatures goes through untouched.
 */
class ThwartTheEnemyScenarioTest : ScenarioTestBase() {

    private fun combat(castThwart: Boolean): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Grizzly Bears")   // 2/2 attacker, blocked by the Giant
            .withCardOnBattlefield(1, "Glory Seeker")    // 2/2 attacker, unblocked
            .withCardOnBattlefield(2, "Hill Giant")      // 3/3 blocker
            .withLandsOnBattlefield(1, "Forest", 3)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (castThwart) builder.withCardInHand(1, "Thwart the Enemy")
        val game = builder.build()

        if (castThwart) {
            game.castSpell(1, "Thwart the Enemy").error shouldBe null
            game.resolveStack()
        }

        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Grizzly Bears" to 2, "Glory Seeker" to 2)).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
        game.passUntilPhase(Phase.COMBAT, Step.END_COMBAT)
        return game
    }

    init {
        test("the opponent's blocker deals no damage; your creatures' damage is dealt") {
            val game = combat(castThwart = true)

            withClue("Hill Giant's 3 damage to Grizzly Bears was prevented") {
                game.isOnBattlefield("Grizzly Bears") shouldBe true
            }
            withClue("Your unblocked Glory Seeker still deals its 2 damage") {
                game.getLifeTotal(2) shouldBe 18
            }
        }

        test("without Thwart the Enemy, the blocker kills the attacker") {
            val game = combat(castThwart = false)

            game.isOnBattlefield("Grizzly Bears") shouldBe false
            game.getLifeTotal(2) shouldBe 18
        }
    }
}
