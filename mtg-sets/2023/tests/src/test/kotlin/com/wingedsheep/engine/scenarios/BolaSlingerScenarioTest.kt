package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Bola Slinger (MOM #8) — {3}{W} 2/2. Backup 1; "Whenever this creature attacks, tap target artifact
 * or creature an opponent controls."
 *
 * Backup always puts the counter; only *another* creature also gains the attack trigger until end of turn.
 */
class BolaSlingerScenarioTest : ScenarioTestBase() {

    private fun plusOnes(game: TestGame, id: EntityId): Int =
        game.state.getEntity(id)?.get<CountersComponent>()?.getCount(CounterType.PLUS_ONE_PLUS_ONE) ?: 0

    private fun tapped(game: TestGame, id: EntityId): Boolean =
        game.state.getEntity(id)?.has<TappedComponent>() == true

    private fun castSlinger(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Bola Slinger")
            .withLandsOnBattlefield(1, "Plains", 4)
            .withCardOnBattlefield(1, "Grizzly Bears")
            .withCardOnBattlefield(2, "Hill Giant")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Plains")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(1, "Bola Slinger").error shouldBe null
        game.resolveStack()
        withClue("the backup trigger asks for its target") { game.hasPendingDecision() shouldBe true }
        return game
    }

    init {
        context("Bola Slinger") {

            test("backup on another creature: counter, and that creature taps a blocker when it attacks") {
                val game = castSlinger()
                val bears = game.findPermanent("Grizzly Bears")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(bears)).error shouldBe null
                game.resolveStack()
                plusOnes(game, bears) shouldBe 1

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                if (game.hasPendingDecision()) game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()

                withClue("the granted attack trigger taps the opponent's creature") {
                    tapped(game, giant) shouldBe true
                }
            }

            test("backup on itself: only the counter, and the other creature gains nothing") {
                val game = castSlinger()
                val slinger = game.findPermanent("Bola Slinger")!!
                val giant = game.findPermanent("Hill Giant")!!
                game.selectTargets(listOf(slinger)).error shouldBe null
                game.resolveStack()
                plusOnes(game, slinger) shouldBe 1
                game.state.projectedState.getPower(slinger) shouldBe 3

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
                withClue("Grizzly Bears has no attack trigger") {
                    game.hasPendingDecision() shouldBe false
                    game.state.stack.isEmpty() shouldBe true
                }
                tapped(game, giant) shouldBe false
            }

            test("its own attack trigger taps target artifact or creature an opponent controls") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Bola Slinger")
                    .withCardOnBattlefield(2, "Hill Giant")
                    .withCardInLibrary(1, "Plains")
                    .withCardInLibrary(2, "Plains")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val giant = game.findPermanent("Hill Giant")!!
                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Bola Slinger" to 2)).error shouldBe null
                if (game.hasPendingDecision()) game.selectTargets(listOf(giant)).error shouldBe null
                game.resolveStack()
                tapped(game, giant) shouldBe true
            }
        }
    }
}
