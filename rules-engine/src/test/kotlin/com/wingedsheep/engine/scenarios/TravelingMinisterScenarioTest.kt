package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Traveling Minister (VOW #39) — {W} Creature — Human Cleric, 1/1.
 *
 *   {T}: Target creature gets +1/+0 until end of turn. You gain 1 life. Activate only as a sorcery.
 *
 * Exercises the tap-cost, sorcery-speed activated ability: it pumps a target creature +1/+0 and
 * gains its controller 1 life. The Minister itself is left with no summoning sickness so its {T}
 * cost is payable (CR 302.6).
 */
class TravelingMinisterScenarioTest : ScenarioTestBase() {

    init {
        context("Traveling Minister tap ability") {

            test("the {T} ability gives a target creature +1/+0 and gains 1 life") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Traveling Minister", summoningSickness = false)
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 target
                    .withLifeTotal(1, 20)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val minister = game.findPermanent("Traveling Minister")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Traveling Minister")!!
                    .activatedAbilities.first().id

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = minister,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating the {T} ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears gets +1/+0 (becomes 3/2)") {
                    game.state.projectedState.getPower(bears) shouldBe 3
                    game.state.projectedState.getToughness(bears) shouldBe 2
                }
                withClue("Player 1 gains 1 life (20 -> 21)") {
                    game.getLifeTotal(1) shouldBe 21
                }
                withClue("Traveling Minister is tapped by its own cost") {
                    game.state.getEntity(minister)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
