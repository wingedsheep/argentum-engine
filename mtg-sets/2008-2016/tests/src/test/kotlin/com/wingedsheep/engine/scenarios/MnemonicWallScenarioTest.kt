package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Mnemonic Wall (ROE #78) — {4}{U} Creature — Wall 0/4.
 *
 *   Defender
 *   When this creature enters, you may return target instant or sorcery card from your graveyard
 *   to your hand.
 */
class MnemonicWallScenarioTest : ScenarioTestBase() {

    private fun castWall(): TestGame {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardInHand(1, "Mnemonic Wall")
            .withLandsOnBattlefield(1, "Island", 5)
            .withCardInGraveyard(1, "Lightning Bolt")
            .withCardInGraveyard(1, "Grizzly Bears")
            .withCardInGraveyard(2, "Shock")
            .withCardInLibrary(1, "Island")
            .withCardInLibrary(2, "Island")
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()

        game.castSpell(1, "Mnemonic Wall").error shouldBe null
        game.resolveStack()

        val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseTargetsDecision>()
        val legal = decision.legalTargets[0].orEmpty()
        legal shouldContain game.findCardsInGraveyard(1, "Lightning Bolt").single()
        legal shouldNotContain game.findCardsInGraveyard(1, "Grizzly Bears").single()
        legal shouldNotContain game.findCardsInGraveyard(2, "Shock").single()

        game.selectTargets(game.findCardsInGraveyard(1, "Lightning Bolt")).error shouldBe null
        game.resolveStack()
        game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        return game
    }

    init {
        test("has defender; accepting returns an instant from your graveyard to your hand") {
            val game = castWall()
            game.answerYesNo(true).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Lightning Bolt") shouldBe true
            game.isInGraveyard(1, "Lightning Bolt") shouldBe false
            val wall = game.findPermanent("Mnemonic Wall")!!
            game.state.projectedState.hasKeyword(wall, Keyword.DEFENDER) shouldBe true
        }

        test("declining leaves the card in the graveyard") {
            val game = castWall()
            game.answerYesNo(false).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Lightning Bolt") shouldBe true
            game.isInHand(1, "Lightning Bolt") shouldBe false
        }
    }
}
