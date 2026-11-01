package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ReorderLibraryDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * House Guildmage (GRN #177) — {U}{B} 2/2 Human Wizard.
 *
 *   {1}{U}, {T}: Target creature doesn't untap during its controller's next untap step.
 *   {2}{B}, {T}: Surveil 2.
 */
class HouseGuildmageScenarioTest : ScenarioTestBase() {
    private fun TestGame.activate(index: Int, targets: List<ChosenTarget> = emptyList()) =
        execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = findPermanent("House Guildmage")!!,
                abilityId = cardRegistry.getCard("House Guildmage")!!.script.activatedAbilities[index].id,
                targets = targets
            )
        )

    init {
        test("{1}{U}, {T}: the target creature is marked to skip its controller's next untap") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "House Guildmage")
                .withCardOnBattlefield(2, "Hill Giant", tapped = true)
                .withCardOnBattlefield(2, "Grizzly Bears", tapped = true)
                .withLandsOnBattlefield(1, "Island", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            val giant = game.findPermanent("Hill Giant")!!
            val bears = game.findPermanent("Grizzly Bears")!!
            game.activate(0, listOf(ChosenTarget.Permanent(giant))).error shouldBe null
            game.resolveStack()

            game.state.projectedState.hasKeyword(giant, AbilityFlag.DOESNT_UNTAP) shouldBe true
            game.state.projectedState.hasKeyword(bears, AbilityFlag.DOESNT_UNTAP) shouldBe false
        }

        test("{2}{B}, {T}: surveil 2 can put both cards into the graveyard") {
            val game = scenario()
                .withPlayers("Player1", "Player2")
                .withCardOnBattlefield(1, "House Guildmage")
                .withLandsOnBattlefield(1, "Swamp", 3)
                .withCardInLibrary(1, "Grizzly Bears")
                .withCardInLibrary(1, "Hill Giant")
                .withCardInLibrary(1, "Savannah Lions")
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.activate(1).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<SelectCardsDecision>()
            decision.options.size shouldBe 2
            game.selectCards(decision.options).error shouldBe null
            if (game.getPendingDecision() is ReorderLibraryDecision) game.keepLibraryOrder()

            game.graveyardSize(1) shouldBe 2
            game.librarySize(1) shouldBe 1
        }
    }
}
