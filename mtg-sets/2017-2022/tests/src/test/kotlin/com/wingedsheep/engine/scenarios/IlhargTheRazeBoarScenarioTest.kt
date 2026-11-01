package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.combat.AttackingComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.engine.support.ScenarioTestBase.TestGame
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Ilharg, the Raze-Boar (WAR #133): "Whenever Ilharg attacks, you may put a creature card from your
 * hand onto the battlefield tapped and attacking. Return that creature to your hand at the
 * beginning of the next end step."
 *
 * Covers the composition the snapshot can't see: the chosen card enters tapped and attacking, the
 * per-card delayed trigger sends it back at the end step, and a creature that died in combat first
 * stays in the graveyard (2019-05-03 ruling).
 */
class IlhargTheRazeBoarScenarioTest : ScenarioTestBase() {

    init {
        context("Ilharg, the Raze-Boar") {

            fun attackWithIlhargPuttingBears(blocker: Boolean): TestGame {
                val builder = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Ilharg, the Raze-Boar", summoningSickness = false)
                    .withCardInHand(1, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                val game = (if (blocker) builder.withCardOnBattlefield(2, "Hill Giant") else builder).build()

                game.advanceToPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Ilharg, the Raze-Boar" to 2)).error shouldBe null
                game.resolveStack()
                withClue("the attack trigger offers the hand creature") {
                    game.hasPendingDecision() shouldBe true
                }
                val bears = game.findCardsInHand(1, "Grizzly Bears").single()
                game.selectCards(listOf(bears)).error shouldBe null
                game.resolveStack()
                return game
            }

            test("the creature enters tapped and attacking and returns to hand at the end step") {
                val game = attackWithIlhargPuttingBears(blocker = false)

                val bears = game.findPermanent("Grizzly Bears") ?: error("Grizzly Bears should be on the battlefield")
                withClue("entered tapped and attacking") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                    game.state.getEntity(bears)?.has<AttackingComponent>() shouldBe true
                }

                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("returned to its owner's hand at the beginning of the end step") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInHand(1, "Grizzly Bears") shouldBe true
                }
                game.isOnBattlefield("Ilharg, the Raze-Boar") shouldBe true
            }

            test("ruling: a creature that died in combat stays in the graveyard") {
                val game = attackWithIlhargPuttingBears(blocker = true)

                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Hill Giant" to listOf("Grizzly Bears"))).error shouldBe null
                game.passUntilPhase(Phase.ENDING, Step.END)
                game.resolveStack()

                withClue("the delayed trigger doesn't fetch it out of the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                    game.isInHand(1, "Grizzly Bears") shouldBe false
                }
            }
        }
    }
}
