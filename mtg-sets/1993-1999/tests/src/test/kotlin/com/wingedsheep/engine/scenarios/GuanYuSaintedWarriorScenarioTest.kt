package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.CastSpell
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.state.components.player.SkipCombatPhasesComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class GuanYuSaintedWarriorScenarioTest : ScenarioTestBase() {
    private fun killGuanYu(): TestGame {
        val game = scenario()
            .withPlayers("A", "B")
            .withCardOnBattlefield(1, "Guan Yu, Sainted Warrior")
            .withCardInHand(2, "Terror")
            .withLandsOnBattlefield(2, "Swamp", 2)
            .withCardInLibrary(1, "Plains")
            .withActivePlayer(2)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        game.castSpell(2, "Terror", game.findPermanent("Guan Yu, Sainted Warrior")!!).error shouldBe null
        game.resolveStack()
        return game
    }

    init {
        context("Guan Yu, Sainted Warrior") {
            test("accepting shuffles it into its owner's library") {
                val game = killGuanYu()
                game.hasPendingDecision() shouldBe true
                game.answerYesNo(true).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Guan Yu, Sainted Warrior") shouldBe false
                game.findCardsInLibrary(1, "Guan Yu, Sainted Warrior").size shouldBe 1
            }

            test("declining leaves it in the graveyard") {
                val game = killGuanYu()
                game.answerYesNo(false).error shouldBe null
                game.resolveStack()
                game.isInGraveyard(1, "Guan Yu, Sainted Warrior") shouldBe true
            }

            test("cannot be blocked except by horsemanship") {
                val game = scenario()
                    .withPlayers("A", "B")
                    .withCardOnBattlefield(1, "Guan Yu, Sainted Warrior")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Guan Yu, Sainted Warrior" to 2)).error shouldBe null
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareBlockers(mapOf("Grizzly Bears" to listOf("Guan Yu, Sainted Warrior"))).error shouldNotBe null
            }
        }
    }
}
