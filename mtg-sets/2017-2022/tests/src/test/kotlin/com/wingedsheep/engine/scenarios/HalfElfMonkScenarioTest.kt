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
 * Scenario tests for Half-Elf Monk (Adventures in the Forgotten Realms #19).
 *
 * Vigilance
 * Stunning Strike — {1}{W}, {T}: Tap target creature.
 *
 * Proves Stunning Strike taps both the Monk (as a cost) and the targeted opposing creature.
 */
class HalfElfMonkScenarioTest : ScenarioTestBase() {

    init {
        test("Stunning Strike taps the Monk and the target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Half-Elf Monk")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val monk = game.findPermanent("Half-Elf Monk")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val strike = cardRegistry.getCard("Half-Elf Monk")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = monk,
                    abilityId = strike,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.resolveStack()

            withClue("the Monk tapped to pay the cost") {
                game.state.getEntity(monk)?.has<TappedComponent>() shouldBe true
            }
            withClue("the targeted Bears are tapped") {
                game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
            }
        }
    }
}
