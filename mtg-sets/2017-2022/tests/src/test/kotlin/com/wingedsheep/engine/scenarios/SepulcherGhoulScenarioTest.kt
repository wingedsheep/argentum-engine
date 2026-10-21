package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.AdditionalCostPayment
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Sepulcher Ghoul (AFR #118): "Sacrifice another creature: This creature gets +2/+2 until end of
 * turn. Activate only once each turn."
 */
class SepulcherGhoulScenarioTest : ScenarioTestBase() {

    init {
        test("sacrificing another creature pumps it +2/+2, and only once per turn") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Sepulcher Ghoul")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Centaur Courser")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val ghoul = game.findPermanent("Sepulcher Ghoul")!!
            val abilityId = cardRegistry.getCard("Sepulcher Ghoul")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = ghoul,
                    abilityId = abilityId,
                    costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Grizzly Bears")!!)),
                )
            ).error shouldBe null
            game.resolveStack()

            game.isInGraveyard(1, "Grizzly Bears") shouldBe true
            game.state.projectedState.getPower(ghoul) shouldBe 4
            game.state.projectedState.getToughness(ghoul) shouldBe 3

            withClue("a second activation this turn is refused") {
                game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = ghoul,
                        abilityId = abilityId,
                        costPayment = AdditionalCostPayment(sacrificedPermanents = listOf(game.findPermanent("Centaur Courser")!!)),
                    )
                ).error shouldNotBe null
                game.isOnBattlefield("Centaur Courser") shouldBe true
            }
        }
    }
}
