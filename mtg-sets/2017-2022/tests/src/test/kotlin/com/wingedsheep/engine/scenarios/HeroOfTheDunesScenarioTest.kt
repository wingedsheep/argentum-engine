package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.TargetsResponse
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Hero of the Dunes (BRO #213) — when it enters, return target artifact or creature card with
 * mana value 3 or less from your graveyard to the battlefield. Creatures you control with mana
 * value 3 or less get +1/+0.
 */
class HeroOfTheDunesScenarioTest : ScenarioTestBase() {

    init {
        test("returns a cheap creature card, which then gets +1/+0; the Hero itself does not") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Hero of the Dunes")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(1, "Hill Giant")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Plains")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val bearsCard = game.findCardsInGraveyard(1, "Grizzly Bears").single()
            val giantCard = game.findCardsInGraveyard(1, "Hill Giant").single()

            game.castSpell(1, "Hero of the Dunes").error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision() as? ChooseTargetsDecision
                ?: error("expected the ETB target choice; got ${game.getPendingDecision()}")
            val legal = decision.legalTargets[0] ?: emptyList()
            legal shouldContain bearsCard
            legal shouldNotContain giantCard
            game.submitDecision(TargetsResponse(decision.id, mapOf(0 to listOf(bearsCard))))
            game.resolveStack()

            val bears = game.findPermanent("Grizzly Bears")!!
            val hero = game.findPermanent("Hero of the Dunes")!!
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 2
            game.state.projectedState.getPower(hero) shouldBe 3
            game.isInGraveyard(1, "Hill Giant") shouldBe true
        }
    }
}
