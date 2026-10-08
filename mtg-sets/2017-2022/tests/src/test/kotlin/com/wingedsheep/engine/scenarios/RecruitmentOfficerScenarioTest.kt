package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Recruitment Officer (BRO #23) — {3}{W}: Look at the top four cards of your library. You may
 * reveal a creature card with mana value 3 or less from among them and put it into your hand.
 * Put the rest on the bottom of your library in a random order.
 */
class RecruitmentOfficerScenarioTest : ScenarioTestBase() {

    init {
        test("takes a cheap creature from the top four and bottoms the rest") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Recruitment Officer")
                .withLandsOnBattlefield(1, "Plains", 4)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val officer = game.findPermanent("Recruitment Officer")!!
            val bears = game.findCardsInLibrary(1, "Grizzly Bears").single()
            val abilityId = cardRegistry.getCard("Recruitment Officer")!!.script.activatedAbilities[0].id

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = officer, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()

            game.hasPendingDecision() shouldBe true
            game.selectCards(listOf(bears)).error shouldBe null
            game.resolveStack()

            game.isInHand(1, "Grizzly Bears") shouldBe true
            game.isInHand(1, "Hill Giant") shouldBe false
            game.librarySize(1) shouldBe 3
        }
    }
}
