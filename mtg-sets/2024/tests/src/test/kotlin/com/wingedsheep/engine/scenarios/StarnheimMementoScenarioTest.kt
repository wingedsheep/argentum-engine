package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Starnheim Memento (J25 #29) — "{T}: Add {W}. {1}{W}, {T}: Target creature gets +1/+1 and gains
 * flying until end of turn. Activate only as a sorcery."
 */
class StarnheimMementoScenarioTest : ScenarioTestBase() {

    private fun abilityId(index: Int) =
        cardRegistry.requireCard("Starnheim Memento").activatedAbilities[index].id

    init {
        test("{T} adds {W}") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Starnheim Memento")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val memento = game.findPermanent("Starnheim Memento")!!

            game.execute(ActivateAbility(game.player1Id, memento, abilityId(0))).error shouldBe null

            game.state.getEntity(game.player1Id)!!.get<ManaPoolComponent>()!!.white shouldBe 1
        }

        test("the sorcery-speed ability gives target creature +1/+1 and flying") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Starnheim Memento")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val memento = game.findPermanent("Starnheim Memento")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, memento, abilityId(1), listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            projected.getPower(bears) shouldBe 3
            projected.getToughness(bears) shouldBe 3
            projected.hasKeyword(bears, Keyword.FLYING) shouldBe true
        }

        test("the pump can't be activated outside a main phase") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "Starnheim Memento")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.COMBAT, Step.BEGIN_COMBAT)
                .build()
            val memento = game.findPermanent("Starnheim Memento")!!
            val bears = game.findPermanent("Grizzly Bears")!!

            game.execute(
                ActivateAbility(game.player1Id, memento, abilityId(1), listOf(ChosenTarget.Permanent(bears)))
            ).error shouldNotBe null
        }
    }
}
