package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.identity.LifeTotalComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.mtg.sets.definitions.ltc.cards.TheBlackGate
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.nulls.shouldNotBeNull

/**
 * The Black Gate (LTC #80) — "{1}{B}, {T}: Choose a player with the most life or tied for most
 * life. Target creature can't be blocked by creatures that player controls this turn."
 *
 * Pinned: the resolution-time choice offers only the players with the most life (a lone one is
 * chosen without a prompt); the chosen
 * player's creatures can't block the target while another player's can; and the chosen player is
 * frozen at resolution (ruling) — a later life change doesn't move the restriction, which is what
 * baking the pipeline-chosen player into the granted static guarantees.
 */
class TheBlackGateScenarioTest : ScenarioTestBase() {

    private val gateAbilityId = TheBlackGate.activatedAbilities.first { !it.isManaAbility }.id

    private fun board(yourLife: Int, opponentLife: Int): TestGame = scenario()
        .withPlayers("Gatekeeper", "Opponent")
        .withCardOnBattlefield(1, "The Black Gate")
        .withLandsOnBattlefield(1, "Swamp", 2)
        .withCardOnBattlefield(1, "Hill Giant")
        .withCardOnBattlefield(2, "Grizzly Bears")
        .withLifeTotal(1, yourLife)
        .withLifeTotal(2, opponentLife)
        .withCardInLibrary(1, "Swamp")
        .withCardInLibrary(2, "Swamp")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    /**
     * Activates the Gate on Hill Giant and passes until it resolves. Returns the player choice
     * when there is one to make; a lone player with the most life is chosen without a prompt.
     */
    private fun TestGame.activate(): ChooseTargetsDecision? {
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("The Black Gate")!!,
                abilityId = gateAbilityId,
                targets = listOf(ChosenTarget.Permanent(findPermanent("Hill Giant")!!))
            )
        )
        withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
        if (getPendingDecision() is SelectManaSourcesDecision) submitManaSourcesAutoPay()
        passPriority()
        passPriority()
        val decision = getPendingDecision() as? ChooseTargetsDecision ?: return null
        withClue("the activator makes the choice") { decision.playerId shouldBe player1Id }
        return decision
    }

    private fun TestGame.choose(decision: ChooseTargetsDecision, player: EntityId) {
        submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(player)))).error shouldBe null
        resolveStack()
    }

    private fun TestGame.bearsTryToBlockGiant() = run {
        passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        check(declareAttackers(mapOf("Hill Giant" to 2)).error == null) { "Hill Giant should attack" }
        passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
        declareBlockers(mapOf("Grizzly Bears" to listOf("Hill Giant")))
    }

    init {
        test("only the player with the most life is offered, and their creatures can't block") {
            val game = board(yourLife = 20, opponentLife = 25)
            withClue("only the opponent has the most life, so they are chosen without a prompt") {
                game.activate() shouldBe null
            }
            game.state.stack.size shouldBe 0

            withClue("the chosen player's Grizzly Bears can't block Hill Giant") {
                game.bearsTryToBlockGiant().error shouldNotBe null
            }
        }

        test("when you have the most life you must choose yourself, and the opponent may block") {
            val game = board(yourLife = 25, opponentLife = 20)
            withClue("only the activator has the most life, so they are chosen without a prompt") {
                game.activate() shouldBe null
            }
            game.state.stack.size shouldBe 0

            withClue("the opponent wasn't chosen, so their Bears may block") {
                game.bearsTryToBlockGiant().error shouldBe null
            }
        }

        test("a tie offers both; the chosen player stays chosen after life totals change") {
            val game = board(yourLife = 20, opponentLife = 20)
            val decision = game.activate()
            withClue("tied players are both offered") {
                decision.shouldNotBeNull().legalTargets[0]!!
                    .shouldContainExactlyInAnyOrder(game.player1Id, game.player2Id)
            }
            game.choose(decision!!, game.player2Id)

            // The opponent drops well below; the ruling says the choice doesn't move.
            game.state = game.state.updateEntity(game.player2Id) { it.with(LifeTotalComponent(5)) }

            withClue("the restriction still names the opponent chosen at resolution") {
                game.bearsTryToBlockGiant().error shouldNotBe null
            }
        }
    }
}
