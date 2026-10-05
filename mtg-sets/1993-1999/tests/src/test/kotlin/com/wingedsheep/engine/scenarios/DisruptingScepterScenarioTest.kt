package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.handlers.continuations.entityIdToChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class DisruptingScepterScenarioTest : ScenarioTestBase() {
    private fun activate(game: TestGame) = game.execute(
        ActivateAbility(
            playerId = game.player1Id,
            sourceId = game.findPermanent("Disrupting Scepter")!!,
            abilityId = cardRegistry.getCard("Disrupting Scepter")!!.script.activatedAbilities[0].id,
            targets = listOf(entityIdToChosenTarget(game.state, game.player2Id)),
        )
    )

    init {
        for ((phase, step) in listOf(
            Phase.PRECOMBAT_MAIN to Step.PRECOMBAT_MAIN,
            Phase.COMBAT to Step.BEGIN_COMBAT,
        )) {
            test("activates during your $step and lets the target choose their discard") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Disrupting Scepter")
                    .withLandsOnBattlefield(1, "Mountain", 3)
                    .withCardInHand(2, "Grizzly Bears")
                    .withCardInHand(2, "Hill Giant")
                    .withActivePlayer(1)
                    .inPhase(phase, step)
                    .build()
                activate(game).error shouldBe null
                game.resolveStack()
                val decision = game.getPendingDecision() as SelectCardsDecision
                decision.playerId shouldBe game.player2Id
                game.selectCards(game.findCardsInHand(2, "Hill Giant")).error shouldBe null
                game.resolveStack()
                game.handSize(2) shouldBe 1
                game.graveyardSize(2) shouldBe 1
                game.findCardsInHand(2, "Grizzly Bears").size shouldBe 1
            }
        }

        test("cannot activate during the opponent's turn even with priority and mana") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Disrupting Scepter")
                .withLandsOnBattlefield(1, "Mountain", 3)
                .withCardInHand(2, "Grizzly Bears")
                .withActivePlayer(2)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            game.passPriority().error shouldBe null
            game.state.priorityPlayerId shouldBe game.player1Id
            activate(game).error shouldNotBe null
            game.handSize(2) shouldBe 1
        }
    }
}
