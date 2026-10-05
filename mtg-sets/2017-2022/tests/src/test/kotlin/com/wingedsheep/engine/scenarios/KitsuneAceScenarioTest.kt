package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.CrewVehicle
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Kitsune Ace (NEO #22) — {1}{W} Creature — Fox Pilot 2/2.
 *
 *   Whenever a Vehicle you control attacks, choose one —
 *   • That Vehicle gains first strike until end of turn.
 *   • Untap this creature.
 *
 * In each trigger test Kitsune Ace crews Sleek Schooner (Crew 1), so the Ace is tapped when the
 * Vehicle attacks — which is what makes the untap mode observable.
 */
class KitsuneAceScenarioTest : ScenarioTestBase() {

    private val firstStrikeMode = "That Vehicle gains first strike until end of turn"
    private val untapMode = "Untap this creature"

    private fun TestGame.crewAndAttack(): ChooseOptionDecision {
        val schooner = findPermanent("Sleek Schooner")!!
        val ace = findPermanent("Kitsune Ace")!!
        execute(CrewVehicle(player1Id, schooner, listOf(ace))).error shouldBe null
        resolveStack()
        state.projectedState.isCreature(schooner) shouldBe true
        withClue("crewing tapped Kitsune Ace") {
            state.getEntity(ace)?.has<TappedComponent>() shouldBe true
        }

        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        declareAttackers(mapOf("Sleek Schooner" to 2)).error shouldBe null
        if (getPendingDecision() == null) resolveStack()
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        return decision as ChooseOptionDecision
    }

    private fun TestGame.chooseMode(decision: ChooseOptionDecision, description: String) {
        val index = decision.options.indexOf(description)
        check(index >= 0) { "Mode '$description' not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun board() = scenario()
        .withPlayers("Player1", "Player2")
        .withCardOnBattlefield(1, "Kitsune Ace")
        .withCardOnBattlefield(1, "Sleek Schooner")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        context("Kitsune Ace") {

            test("untap mode: the Ace that crewed the attacking Vehicle untaps") {
                val game = board()
                val ace = game.findPermanent("Kitsune Ace")!!
                val schooner = game.findPermanent("Sleek Schooner")!!

                val choice = game.crewAndAttack()
                withClue("both modes are offered") { choice.options.size shouldBe 2 }
                game.chooseMode(choice, untapMode)
                game.resolveStack()

                withClue("Kitsune Ace is untapped") {
                    game.state.getEntity(ace)?.has<TappedComponent>() shouldBe false
                }
                withClue("the Vehicle did not gain first strike") {
                    game.state.projectedState.hasKeyword(schooner, Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("first strike mode: the attacking Vehicle gains first strike and the Ace stays tapped") {
                val game = board()
                val ace = game.findPermanent("Kitsune Ace")!!
                val schooner = game.findPermanent("Sleek Schooner")!!

                val choice = game.crewAndAttack()
                game.chooseMode(choice, firstStrikeMode)
                game.resolveStack()

                withClue("Sleek Schooner has first strike") {
                    game.state.projectedState.hasKeyword(schooner, Keyword.FIRST_STRIKE) shouldBe true
                }
                withClue("Kitsune Ace stays tapped") {
                    game.state.getEntity(ace)?.has<TappedComponent>() shouldBe true
                }
            }

            test("a non-Vehicle creature attacking does not trigger it") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Kitsune Ace")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2, "Kitsune Ace" to 2)).error shouldBe null

                withClue("no trigger: neither attacker is a Vehicle") {
                    game.state.stack.isEmpty() shouldBe true
                    game.getPendingDecision() shouldBe null
                }
            }
        }
    }
}
