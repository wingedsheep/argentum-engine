package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Drownyard Amalgam (MID #50) — "When this creature enters, target player mills three cards.
 * {2}{U}: This creature can't be blocked this turn."
 */
class DrownyardAmalgamScenarioTest : ScenarioTestBase() {

    init {
        test("entering makes the targeted player mill three cards") {
            val builder = scenario()
                .withPlayers("Player1", "Player2")
                .withCardInHand(1, "Drownyard Amalgam")
                .withLandsOnBattlefield(1, "Island", 5)
                .withCardInLibrary(1, "Island")
            repeat(5) { builder.withCardInLibrary(2, "Forest") }
            val game = builder
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Drownyard Amalgam").error shouldBe null
            game.resolveStack()
            game.selectTargets(listOf(game.player2Id)).error shouldBe null
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.findCardsInGraveyard(2, "Forest").size shouldBe 3
            game.librarySize(1) shouldBe 1
        }

        test("{2}{U}: it can't be blocked this turn") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Drownyard Amalgam")
                .withLandsOnBattlefield(1, "Island", 3)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val amalgam = game.findPermanent("Drownyard Amalgam")!!
            val abilityId = cardRegistry.getCard("Drownyard Amalgam")!!.activatedAbilities.first().id
            game.state.projectedState.hasKeyword(amalgam, AbilityFlag.CANT_BE_BLOCKED) shouldBe false

            game.execute(ActivateAbility(playerId = game.player1Id, sourceId = amalgam, abilityId = abilityId))
                .error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(amalgam, AbilityFlag.CANT_BE_BLOCKED) shouldBe true
        }
    }
}
