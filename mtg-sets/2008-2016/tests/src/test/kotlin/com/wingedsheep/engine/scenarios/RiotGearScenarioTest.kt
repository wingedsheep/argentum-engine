package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/** Riot Gear (GTC) — equipped creature gets +1/+2; equip {2}. */
class RiotGearScenarioTest : ScenarioTestBase() {
    init {
        test("equip {2} attaches it and the creature gets +1/+2") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Riot Gear")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gear = game.findPermanent("Riot Gear")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            val equip = cardRegistry.getCard("Riot Gear")!!.script.activatedAbilities.single { it.isEquipAbility }

            val result = game.execute(
                ActivateAbility(game.player1Id, gear, equip.id, targets = listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("equip should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.getEntity(gear)?.get<AttachedToComponent>()?.targetId shouldBe bears
            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 4
        }
    }
}
