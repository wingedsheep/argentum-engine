package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mindmelter — {1}{U}{B} 2/2 Eldrazi Drone. "Devoid. This creature can't be blocked.
 * {3}{C}: Target opponent exiles a card from their hand. Activate only as a sorcery."
 */
class MindmelterScenarioTest : ScenarioTestBase() {
    init {
        val abilityId = cardRegistry.getCard("Mindmelter")!!.script.activatedAbilities.single().id

        fun board(phase: Phase, step: Step) = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Mindmelter")
            .withLandsOnBattlefield(1, "Wastes", 4)
            .withCardInHand(2, "Grizzly Bears")
            .withCardInHand(2, "Hill Giant")
            .withActivePlayer(1)
            .withPriorityPlayer(1)
            .inPhase(phase, step)
            .build()

        test("devoid and can't be blocked") {
            val game = board(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val melter = game.findPermanent("Mindmelter")!!
            game.state.projectedState.getColors(melter) shouldBe emptySet()
            game.state.projectedState.hasKeyword(melter, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }

        test("{3}{C}: the target opponent chooses and exiles a card from their hand") {
            val game = board(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            val melter = game.findPermanent("Mindmelter")!!
            val giant = game.findCardsInHand(2, "Hill Giant").single()

            val result = game.execute(
                ActivateAbility(game.player1Id, melter, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            val decision = game.state.pendingDecision.shouldBeInstanceOf<SelectCardsDecision>()
            withClue("the opponent makes the choice") { decision.playerId shouldBe game.player2Id }
            game.selectCards(listOf(giant)).error shouldBe null
            game.resolveStack()

            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInHand(2, "Grizzly Bears") shouldBe true
            game.handSize(2) shouldBe 1
        }

        test("activate only as a sorcery") {
            val game = board(Phase.BEGINNING, Step.UPKEEP)
            val melter = game.findPermanent("Mindmelter")!!
            game.execute(
                ActivateAbility(game.player1Id, melter, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldNotBe null
            game.handSize(2) shouldBe 2
        }
    }
}
