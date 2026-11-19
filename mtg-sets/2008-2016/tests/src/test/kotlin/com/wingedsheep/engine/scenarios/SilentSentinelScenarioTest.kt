package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Silent Sentinel (BNG #26) — "Whenever this creature attacks, you may return target enchantment
 * card from your graveyard to the battlefield."
 */
class SilentSentinelScenarioTest : ScenarioTestBase() {

    private fun attack(): TestGame {
        val game = scenario()
            .withPlayers("P1", "P2")
            .withCardOnBattlefield(1, "Silent Sentinel", summoningSickness = false)
            .withCardInGraveyard(1, "Glorious Anthem")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Courser of Kruphix")
            .withCardInLibrary(1, "Plains")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
        game.declareAttackers(mapOf("Silent Sentinel" to 2)).error shouldBe null
        game.resolveStack()
        val anthem = game.findCardsInGraveyard(1, "Glorious Anthem").single()
        val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        withClue("only the enchantment card in your own graveyard is a legal target") {
            decision.legalTargets[0]!! shouldContainExactly listOf(anthem)
        }
        game.selectTargets(listOf(anthem)).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        test("attacking returns an enchantment card from your graveyard to the battlefield") {
            val game = attack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            withClue("Glorious Anthem is on the battlefield") { game.isOnBattlefield("Glorious Anthem") shouldBe true }
        }

        test("declining leaves the card in the graveyard") {
            val game = attack()
            game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            withClue("Glorious Anthem stays put") { game.isInGraveyard(1, "Glorious Anthem") shouldBe true }
        }
    }
}
