package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * The Kami War // O-Kagachi Made Manifest (NEO #227).
 *
 * Pins the back face's attack trigger: the *defending player* chooses which nonland card in the
 * attacker's graveyard returns to hand, and O-Kagachi gets +X/+0 for that card's mana value.
 */
class TheKamiWarScenarioTest : ScenarioTestBase() {

    init {
        context("O-Kagachi Made Manifest") {

            test("defending player chooses the nonland card returned, and O-Kagachi gets +X/+0 for its mana value") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "O-Kagachi Made Manifest")
                    .withCardInGraveyard(1, "Hill Giant")
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val okagachi = game.findPermanent("O-Kagachi Made Manifest")!!
                val giant = game.findCardsInGraveyard(1, "Hill Giant").single()
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("O-Kagachi Made Manifest" to 2)).error shouldBe null
                game.resolveStack()

                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<SelectCardsDecision>()
                withClue("the defending player makes the choice") {
                    decision.playerId shouldBe game.player2Id
                }
                withClue("only nonland cards are offered") {
                    decision.options shouldContainExactlyInAnyOrder listOf(giant, bears)
                }
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()

                game.isInHand(1, "Grizzly Bears") shouldBe true
                game.isInGraveyard(1, "Hill Giant") shouldBe true
                withClue("+2/+0 for Grizzly Bears' mana value") {
                    game.state.projectedState.getPower(okagachi) shouldBe 8
                    game.state.projectedState.getToughness(okagachi) shouldBe 6
                }
            }

            test("with no nonland card in your graveyard nothing returns and O-Kagachi stays 6/6") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "O-Kagachi Made Manifest")
                    .withCardInGraveyard(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val okagachi = game.findPermanent("O-Kagachi Made Manifest")!!

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("O-Kagachi Made Manifest" to 2)).error shouldBe null
                game.resolveStack()

                game.isInGraveyard(1, "Forest") shouldBe true
                game.state.projectedState.getPower(okagachi) shouldBe 6
            }
        }
    }
}
