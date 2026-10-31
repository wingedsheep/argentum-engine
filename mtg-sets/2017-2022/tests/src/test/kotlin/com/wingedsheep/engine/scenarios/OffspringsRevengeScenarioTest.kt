package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe

/**
 * Offspring's Revenge — {2}{R}{W}{B} Enchantment
 * At the beginning of combat on your turn, exile target red, white, or black creature card from
 * your graveyard. Create a token that's a copy of that card, except it's 1/1. It gains haste
 * until your next turn.
 *
 * Covers the colour-restricted graveyard target, the 1/1 copy, and haste granted to the created
 * token lasting through the opponent's turn.
 */
class OffspringsRevengeScenarioTest : ScenarioTestBase() {

    init {
        context("Offspring's Revenge") {

            test("exiles a red creature card and makes a hasty 1/1 copy that keeps haste on the opponent's turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Offspring's Revenge")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(2, "Hill Giant")
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val myGiant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val theirGiant = game.findCardsInGraveyard(2, "Hill Giant").single()

                game.passUntilPhase(Phase.COMBAT, Step.BEGIN_COMBAT)

                val decision = game.getPendingDecision() as ChooseTargetsDecision
                val legal = decision.legalTargets.getValue(0)
                legal shouldContain myGiant
                legal shouldNotContain bears // green
                legal shouldNotContain theirGiant // not your graveyard

                game.selectTargets(listOf(myGiant))
                game.resolveStack()

                game.isInExile(1, "Hill Giant") shouldBe true
                val token = game.findAllPermanents("Hill Giant").single()
                game.state.getEntity(token)?.get<CardComponent>()?.name shouldBe "Hill Giant"
                val projected = game.state.projectedState
                projected.getPower(token) shouldBe 1
                projected.getToughness(token) shouldBe 1
                projected.hasKeyword(token, Keyword.HASTE) shouldBe true

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)
                game.state.activePlayerId shouldBe game.player2Id
                game.state.projectedState.hasKeyword(token, Keyword.HASTE) shouldBe true
            }
        }
    }
}
