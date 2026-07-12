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
 * Scenario test for Nebelgast Beguiler (VOW #25) — {4}{W} Creature — Spirit, 2/5.
 *
 *   {W}, {T}: Tap target creature.
 *
 * Exercises the mana + tap-cost activated ability: it taps a target creature (an opponent's
 * attacker, in the common use case) and taps Nebelgast Beguiler itself as part of its own cost.
 */
class NebelgastBeguilerScenarioTest : ScenarioTestBase() {

    init {
        context("Nebelgast Beguiler — {W}, {T}: Tap target creature") {

            test("activating the ability taps the target creature") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Nebelgast Beguiler", summoningSickness = false)
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val beguiler = game.findPermanent("Nebelgast Beguiler")!!
                val bears = game.findPermanent("Grizzly Bears")!!
                val abilityId = cardRegistry.getCard("Nebelgast Beguiler")!!
                    .activatedAbilities.first().id

                withClue("Grizzly Bears does not start tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe false
                }

                val activation = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = beguiler,
                        abilityId = abilityId,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating the ability should succeed: ${activation.error}") {
                    activation.error shouldBe null
                }
                game.resolveStack()

                withClue("Grizzly Bears is now tapped") {
                    game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
                }
                withClue("Nebelgast Beguiler is tapped by its own {T} cost") {
                    game.state.getEntity(beguiler)?.has<TappedComponent>() shouldBe true
                }
            }
        }
    }
}
