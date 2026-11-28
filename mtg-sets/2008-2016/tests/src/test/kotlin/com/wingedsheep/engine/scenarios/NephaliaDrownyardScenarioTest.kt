package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Nephalia Drownyard (ISD #245) — Land.
 *
 *   {T}: Add {C}.
 *   {1}{U}{B}, {T}: Target player mills three cards.
 */
class NephaliaDrownyardScenarioTest : ScenarioTestBase() {

    init {
        val millAbilityId = cardRegistry.getCard("Nephalia Drownyard")!!.script.activatedAbilities[1].id

        test("{1}{U}{B}, {T}: the target player mills three cards") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Nephalia Drownyard")
                .withLandsOnBattlefield(1, "Island", 2)
                .withLandsOnBattlefield(1, "Swamp", 1)
                .also { b -> repeat(5) { b.withCardInLibrary(2, "Forest") } }
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val drownyard = game.findPermanent("Nephalia Drownyard")!!
            val result = game.execute(
                ActivateAbility(
                    game.player1Id,
                    drownyard,
                    millAbilityId,
                    targets = listOf(ChosenTarget.Player(game.player2Id)),
                )
            )
            withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.librarySize(2) shouldBe 2
            game.graveyardSize(2) shouldBe 3
        }
    }
}
