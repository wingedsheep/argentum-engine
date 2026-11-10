package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Chitinous Cloak — {3} Equipment. "Equipped creature gets +2/+2 and has menace. Equip {3}"
 */
class ChitinousCloakScenarioTest : ScenarioTestBase() {
    init {
        val equipId = cardRegistry.getCard("Chitinous Cloak")!!.script.activatedAbilities.single().id

        test("equipped creature gets +2/+2 and has menace") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardAttachedTo(1, "Chitinous Cloak", "Grizzly Bears")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
        }

        test("equip {3} attaches it and the bonus follows") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(1, "Chitinous Cloak")
                .withLandsOnBattlefield(1, "Plains", 3)
                .withActivePlayer(1)
                .withPriorityPlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()
            val bears = game.findPermanent("Grizzly Bears")!!
            val cloak = game.findPermanent("Chitinous Cloak")!!

            withClue("unattached, the Cloak gives nothing") {
                game.state.projectedState.getPower(bears) shouldBe 2
                game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe false
            }

            val result = game.execute(
                ActivateAbility(game.player1Id, cloak, equipId, listOf(ChosenTarget.Permanent(bears)))
            )
            withClue("equip should succeed: ${result.error}") { result.error shouldBe null }
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 4
            game.state.projectedState.getToughness(bears) shouldBe 4
            game.state.projectedState.hasKeyword(bears, Keyword.MENACE) shouldBe true
        }
    }
}
