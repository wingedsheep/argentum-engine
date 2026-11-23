package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Alms Beast — "Creatures blocking or blocked by this creature have lifelink."
 *
 * Pins the static grant over the live combat pairing in both directions: the attacker's blocker
 * gains life from its damage, the attacker Alms Beast blocks gains life from its damage, and
 * creatures outside the pairing (or any creature outside combat) don't get lifelink.
 */
class AlmsBeastScenarioTest : ScenarioTestBase() {

    private fun hasLifelink(game: TestGame, name: String): Boolean =
        game.state.projectedState.hasKeyword(game.findPermanent(name)!!, Keyword.LIFELINK)

    init {
        test("a creature blocking Alms Beast has lifelink and gains its controller life") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Alms Beast")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withCardOnBattlefield(2, "Hill Giant")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            withClue("outside combat nothing has lifelink") {
                hasLifelink(game, "Centaur Courser") shouldBe false
            }

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Alms Beast" to 2, "Grizzly Bears" to 2)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(
                mapOf("Centaur Courser" to listOf("Alms Beast"), "Hill Giant" to listOf("Grizzly Bears"))
            ).error shouldBe null

            withClue("only the creature blocking Alms Beast has lifelink") {
                hasLifelink(game, "Centaur Courser") shouldBe true
                hasLifelink(game, "Hill Giant") shouldBe false
                hasLifelink(game, "Alms Beast") shouldBe false
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the Courser's 3 damage to Alms Beast gained the opponent 3 life; Hill Giant gained none") {
                game.getLifeTotal(2) shouldBe 23
                game.getLifeTotal(1) shouldBe 20
            }
        }

        test("an attacker blocked by Alms Beast has lifelink") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Alms Beast")
                .withCardOnBattlefield(2, "Centaur Courser")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Centaur Courser" to 1)).error shouldBe null
            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
            game.declareBlockers(mapOf("Alms Beast" to listOf("Centaur Courser"))).error shouldBe null

            withClue("the attacker Alms Beast blocks has lifelink") {
                hasLifelink(game, "Centaur Courser") shouldBe true
            }

            game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

            withClue("the Courser's 3 damage to Alms Beast gained its controller 3 life") {
                game.getLifeTotal(2) shouldBe 23
            }
        }
    }
}
