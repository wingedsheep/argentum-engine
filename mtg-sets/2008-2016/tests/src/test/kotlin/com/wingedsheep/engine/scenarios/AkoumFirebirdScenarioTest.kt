package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Akoum Firebird — its landfall ability works only from the graveyard: paying {4}{R}{R} returns it
 * to the battlefield, declining leaves it there.
 */
class AkoumFirebirdScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardInGraveyard(1, "Akoum Firebird")
        .withCardInHand(1, "Mountain")
        .withLandsOnBattlefield(1, "Mountain", 6)
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.playMountain() {
        val mountain = findCardsInHand(1, "Mountain").first()
        execute(PlayLand(player1Id, mountain)).error shouldBe null
    }

    init {
        test("landfall from the graveyard: paying {4}{R}{R} returns it to the battlefield") {
            val game = board()
            game.playMountain()
            game.resolveStack()
            withClue("the graveyard trigger offers the payment") {
                game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            }
            game.answerYesNo(true)
            game.getPendingDecision().shouldBeInstanceOf<SelectManaSourcesDecision>()
            game.submitManaSourcesAutoPay()
            game.resolveStack()

            game.isOnBattlefield("Akoum Firebird") shouldBe true
            game.isInGraveyard(1, "Akoum Firebird") shouldBe false
        }

        test("declining the payment leaves it in the graveyard") {
            val game = board()
            game.playMountain()
            game.resolveStack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false)
            game.resolveStack()

            game.isInGraveyard(1, "Akoum Firebird") shouldBe true
        }
    }
}
