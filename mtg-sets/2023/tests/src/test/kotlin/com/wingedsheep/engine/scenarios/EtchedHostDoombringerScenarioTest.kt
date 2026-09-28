package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Etched Host Doombringer (MOM #102) — "When this creature enters, choose one — • Target opponent
 * loses 2 life and you gain 2 life. • Choose target battle. If an opponent protects it, remove
 * three defense counters from it. Otherwise, put three defense counters on it."
 */
class EtchedHostDoombringerScenarioTest : ScenarioTestBase() {

    private fun defenseOf(game: TestGame, battle: EntityId): Int =
        game.state.getEntity(battle)?.get<CountersComponent>()?.getCount(CounterType.DEFENSE) ?: 0

    private fun castAndChooseMode(game: TestGame, mode: Int) {
        game.castSpell(1, "Etched Host Doombringer").error shouldBe null
        game.resolveStack()
        val modeDecision = game.state.pendingDecision as? ChooseOptionDecision
            ?: error("expected a ChooseOptionDecision; got ${game.state.pendingDecision}")
        game.submitDecision(OptionChosenResponse(modeDecision.id, optionIndex = mode))
    }

    private fun answerTargetIfAsked(game: TestGame, target: EntityId) {
        val decision = game.state.pendingDecision as? ChooseTargetsDecision
        if (decision != null) {
            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(target))))
        }
    }

    private fun board(withOwnSiege: Boolean = false, withTheirSiege: Boolean = false): TestGame {
        val builder = scenario()
            .withPlayers("Player", "Opponent")
            .withCardInHand(1, "Etched Host Doombringer")
            .withLandsOnBattlefield(1, "Swamp", 5)
            .withLifeTotal(1, 20)
            .withLifeTotal(2, 20)
            .withCardInLibrary(1, "Swamp")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        if (withOwnSiege) builder.withCardOnBattlefield(1, "Invasion of Innistrad")
        if (withTheirSiege) builder.withCardOnBattlefield(2, "Invasion of Innistrad")
        val game = builder.build()
        game.checkStateBasedActions()
        return game
    }

    init {
        test("drain mode: target opponent loses 2 life and you gain 2") {
            val game = board()
            castAndChooseMode(game, 0)
            answerTargetIfAsked(game, game.player2Id)
            game.resolveStack()

            withClue("opponent loses 2") { game.getLifeTotal(2) shouldBe 18 }
            withClue("you gain 2") { game.getLifeTotal(1) shouldBe 22 }
        }

        test("battle mode: removes three defense counters from a battle an opponent protects") {
            val game = board(withOwnSiege = true)
            val battle = game.findPermanent("Invasion of Innistrad")!!
            defenseOf(game, battle) shouldBe 5

            castAndChooseMode(game, 1)
            answerTargetIfAsked(game, battle)
            game.resolveStack()

            defenseOf(game, battle) shouldBe 2
        }

        test("battle mode: puts three defense counters on a battle you protect") {
            val game = board(withTheirSiege = true)
            val battle = game.findPermanent("Invasion of Innistrad")!!
            defenseOf(game, battle) shouldBe 5

            castAndChooseMode(game, 1)
            answerTargetIfAsked(game, battle)
            game.resolveStack()

            defenseOf(game, battle) shouldBe 8
        }
    }
}
