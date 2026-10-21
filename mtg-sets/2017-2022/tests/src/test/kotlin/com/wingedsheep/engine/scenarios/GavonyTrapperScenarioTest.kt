package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe

/**
 * Gavony Trapper (MID #22) — {2}, {T}: Tap target creature.
 */
class GavonyTrapperScenarioTest : ScenarioTestBase() {

    init {
        test("pays {2} and taps itself to tap target creature") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Gavony Trapper")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(2, "Grizzly Bears")
                .withCardInLibrary(1, "Plains")
                .withCardInLibrary(2, "Forest")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val trapper = game.findPermanent("Gavony Trapper")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val abilityId = cardRegistry.getCard("Gavony Trapper")!!.script.activatedAbilities[0].id

            game.execute(
                ActivateAbility(
                    playerId = game.player1Id,
                    sourceId = trapper,
                    abilityId = abilityId,
                    targets = listOf(ChosenTarget.Permanent(bears))
                )
            ).error shouldBe null
            game.state.getEntity(trapper)?.has<TappedComponent>() shouldBe true
            game.resolveStack()

            game.state.getEntity(bears)?.has<TappedComponent>() shouldBe true
        }
    }
}
