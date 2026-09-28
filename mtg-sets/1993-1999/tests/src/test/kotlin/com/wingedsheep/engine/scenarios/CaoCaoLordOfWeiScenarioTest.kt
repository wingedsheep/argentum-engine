package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class CaoCaoLordOfWeiScenarioTest : ScenarioTestBase() {
    init {
        context("Cao Cao, Lord of Wei") {
            test("taps to make the opponent discard two cards during your main phase") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cao Cao, Lord of Wei", summoningSickness = false)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                val src = game.findPermanent("Cao Cao, Lord of Wei")!!
                val abilityId = cardRegistry.getCard("Cao Cao, Lord of Wei")!!.script.activatedAbilities[0].id
                game.execute(
                    ActivateAbility(game.player1Id, src, abilityId,
                        targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)))
                ).error shouldBe null
                game.resolveStack()
                var guard = 0
                while (game.hasPendingDecision() && guard++ < 5) {
                    val d = game.getPendingDecision() as com.wingedsheep.engine.core.SelectCardsDecision
                    game.selectCards(d.options.take(d.minSelections))
                }
                game.handSize(2) shouldBe 0
                game.graveyardSize(2) shouldBe 2
            }

            test("cannot be activated after attackers are declared") {
                val game = scenario()
                    .withPlayers("P1", "P2")
                    .withCardOnBattlefield(1, "Cao Cao, Lord of Wei", summoningSickness = false)
                    .withCardInHand(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()
                val src = game.findPermanent("Cao Cao, Lord of Wei")!!
                val abilityId = cardRegistry.getCard("Cao Cao, Lord of Wei")!!.script.activatedAbilities[0].id
                withClue("window has closed") {
                    game.execute(
                        ActivateAbility(game.player1Id, src, abilityId,
                            targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)))
                    ).error shouldNotBe null
                }
                game.handSize(2) shouldBe 1
            }
        }
    }
}
