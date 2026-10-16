package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Puresteel Paladin — {W}{W} 2/2.
 *
 * Metalcraft — Equipment you control have equip {0} as long as you control three or more
 * artifacts. The grant is a conditional static, so the free equip ability exists only while the
 * artifact count is met.
 */
class PuresteelPaladinScenarioTest : ScenarioTestBase() {

    private fun grantedEquipId() = cardRegistry.getCard("Puresteel Paladin")!!.script.staticAbilities
        .filterIsInstance<ConditionalStaticAbility>().single()
        .ability.let { it as GrantActivatedAbility }.ability.id

    init {
        test("with three artifacts an Equipment can be equipped for {0}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Puresteel Paladin")
                .withCardOnBattlefield(1, "Vulshok Gauntlets")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gauntlets = game.findPermanent("Vulshok Gauntlets")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, gauntlets, grantedEquipId(), targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.state.getEntity(gauntlets)?.get<AttachedToComponent>()?.targetId shouldBe bears
        }

        test("with only two artifacts the free equip ability is not available") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Puresteel Paladin")
                .withCardOnBattlefield(1, "Vulshok Gauntlets")
                .withCardOnBattlefield(1, "Ornithopter")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val gauntlets = game.findPermanent("Vulshok Gauntlets")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, gauntlets, grantedEquipId(), targets = listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
        }
    }
}
