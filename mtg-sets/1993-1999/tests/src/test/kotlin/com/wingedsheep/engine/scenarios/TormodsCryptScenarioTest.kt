package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

class TormodsCryptScenarioTest : ScenarioTestBase() {

    init {
        test("sacrifices itself and exiles every card in the targeted player's graveyard") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Tormod's Crypt")
                .withCardInGraveyard(1, "Grizzly Bears")
                .withCardInGraveyard(2, "Hill Giant")
                .withCardInGraveyard(2, "Ornithopter")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val crypt = game.findPermanent("Tormod's Crypt")!!
            val abilityId = cardRegistry.getCard("Tormod's Crypt")!!.activatedAbilities.single().id
            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = crypt,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Tormod's Crypt") shouldBe true
            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.isInExile(2, "Hill Giant") shouldBe true
            game.isInExile(2, "Ornithopter") shouldBe true
            game.state.getGraveyard(game.player2Id).isEmpty() shouldBe true
        }
    }
}
