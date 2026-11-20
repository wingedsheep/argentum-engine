package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Haazda Snare Squad (DGM #2) — {2}{W} 1/4 Creature — Human Soldier.
 *
 *   Whenever this creature attacks, you may pay {W}. If you do, tap target creature an opponent
 *   controls.
 *
 * The target is locked in as the trigger goes on the stack; the optional {W} is offered on resolution.
 */
class HaazdaSnareSquadScenarioTest : ScenarioTestBase() {

    private fun isTapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun attackAndTarget(): Pair<TestGame, EntityId> {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Haazda Snare Squad", summoningSickness = false)
            .withLandsOnBattlefield(1, "Plains", 1)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withActivePlayer(1)
            .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            .build()
        val giant = game.findPermanent("Hill Giant")!!

        game.declareAttackers(mapOf("Haazda Snare Squad" to 2)).error shouldBe null
        withClue("only creatures an opponent controls are legal targets") {
            val decision = game.state.pendingDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
            decision.legalTargets[0] shouldBe listOf(giant)
        }
        game.selectTargets(listOf(giant)).error shouldBe null
        game.resolveStack()
        game.state.pendingDecision.shouldBeInstanceOf<YesNoDecision>()
        return game to giant
    }

    init {
        test("paying {W} taps the targeted creature") {
            val (game, giant) = attackAndTarget()
            game.answerYesNo(true).error shouldBe null
            if (game.state.pendingDecision is SelectManaSourcesDecision) {
                game.submitManaSourcesAutoPay().error shouldBe null
            }
            game.resolveStack()

            isTapped(game, giant) shouldBe true
            isTapped(game, game.findPermanent("Plains")!!) shouldBe true
        }

        test("declining to pay leaves the creature untapped") {
            val (game, giant) = attackAndTarget()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            isTapped(game, giant) shouldBe false
        }
    }
}
