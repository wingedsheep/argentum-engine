package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Scenario tests for Angus Mackenzie (LEG #217).
 *
 * "{G}{W}{U}, {T}: Prevent all combat damage that would be dealt this turn. Activate only before
 *  the combat damage step."
 *
 * The window runs up to and including declare blockers; the first-strike combat damage step is
 * already a combat damage step, and the rest of the turn after it is closed too.
 */
class AngusMackenzieScenarioTest : ScenarioTestBase() {

    private val abilityId by lazy {
        cardRegistry.getCard("Angus Mackenzie")!!.script.activatedAbilities[0].id
    }

    private fun board(phase: Phase, step: Step) = scenario()
        .withPlayers("Angus", "Opponent")
        .withCardOnBattlefield(1, "Angus Mackenzie", summoningSickness = false)
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withLandsOnBattlefield(1, "Forest", 1)
        .withLandsOnBattlefield(1, "Plains", 1)
        .withLandsOnBattlefield(1, "Island", 1)
        .withActivePlayer(1)
        .inPhase(phase, step)
        .build()

    init {
        context("Angus Mackenzie") {

            test("activated before combat, it prevents the turn's combat damage") {
                val game = board(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val angus = game.findPermanent("Angus Mackenzie")!!

                game.execute(ActivateAbility(game.player1Id, angus, abilityId)).error shouldBe null
                game.resolveStack()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("the unblocked Bears' combat damage is prevented") {
                    game.getLifeTotal(2) shouldBe 20
                }
            }

            test("it can be activated in the declare blockers step") {
                val game = board(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                val angus = game.findPermanent("Angus Mackenzie")!!

                game.execute(ActivateAbility(game.player1Id, angus, abilityId)).error shouldBe null
            }

            for (step in listOf(Step.FIRST_STRIKE_COMBAT_DAMAGE, Step.COMBAT_DAMAGE, Step.END_COMBAT, Step.POSTCOMBAT_MAIN)) {
                test("it can't be activated in the ${step.displayName}") {
                    val game = board(step.phase, step)
                    val angus = game.findPermanent("Angus Mackenzie")!!

                    game.getLegalActions(1).map { it.action }.any {
                        it is ActivateAbility && it.sourceId == angus
                    } shouldBe false
                    game.execute(ActivateAbility(game.player1Id, angus, abilityId)).error shouldNotBe null
                }
            }
        }
    }
}
