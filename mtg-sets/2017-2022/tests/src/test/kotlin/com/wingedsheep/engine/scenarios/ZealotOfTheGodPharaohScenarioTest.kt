package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.hou.cards.ZealotOfTheGodPharaoh
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

/**
 * Zealot of the God-Pharaoh (HOU) — {4}{R}: 2 damage to target opponent or planeswalker.
 */
class ZealotOfTheGodPharaohScenarioTest : ScenarioTestBase() {
    private val abilityId = ZealotOfTheGodPharaoh.activatedAbilities.single().id

    init {
        test("{4}{R}: deals 2 damage to target opponent") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Zealot of the God-Pharaoh")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val zealot = game.findPermanent("Zealot of the God-Pharaoh")!!
            game.execute(
                ActivateAbility(game.player1Id, zealot, abilityId, listOf(ChosenTarget.Player(game.player2Id)))
            ).error shouldBe null
            game.resolveStack()
            game.getLifeTotal(2) shouldBe 18
        }

        test("can't target its own controller") {
            val game = scenario().withPlayers()
                .withCardOnBattlefield(1, "Zealot of the God-Pharaoh")
                .withLandsOnBattlefield(1, "Mountain", 5)
                .withActivePlayer(1).inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN).build()
            val zealot = game.findPermanent("Zealot of the God-Pharaoh")!!
            game.execute(
                ActivateAbility(game.player1Id, zealot, abilityId, listOf(ChosenTarget.Player(game.player1Id)))
            ).error shouldNotBe null
        }
    }
}
