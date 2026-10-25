package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.dungeon.DungeonService
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Delver's Torch (AFR #10): equipped creature gets +1/+1, and ventures into the dungeon whenever it
 * attacks. An unequipped attacker ventures nowhere.
 */
class DelversTorchScenarioTest : ScenarioTestBase() {

    private val equipAbilityId by lazy {
        cardRegistry.requireCard("Delver's Torch").activatedAbilities[0].id
    }

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
        .withCardOnBattlefield(1, "Delver's Torch")
        .withLandsOnBattlefield(1, "Plains", 3)
        .withCardInLibrary(1, "Plains")
        .withCardInLibrary(2, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    init {
        test("equipped creature gets +1/+1 and ventures into the dungeon when it attacks") {
            val game = board()
            val bears = game.findPermanent("Grizzly Bears")!!
            val torch = game.findPermanent("Delver's Torch")!!

            game.execute(
                ActivateAbility(game.player1Id, torch, equipAbilityId, listOf(ChosenTarget.Permanent(bears)))
            ).error shouldBe null
            game.resolveStack()

            game.state.projectedState.getPower(bears) shouldBe 3
            game.state.projectedState.getToughness(bears) shouldBe 3
            DungeonService.marker(game.state, game.player1Id) shouldBe null

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            val decision = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(
                OptionChosenResponse(decision.id, decision.options.indexOf("Lost Mine of Phandelver"))
            ).error shouldBe null
            game.resolveStack()

            DungeonService.marker(game.state, game.player1Id)!!.dungeon shouldBe "Lost Mine of Phandelver"
        }

        test("an unequipped attacker does not venture") {
            val game = board()

            game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
            game.declareAttackers(mapOf("Grizzly Bears" to 2)).error shouldBe null
            game.resolveStack()

            game.getPendingDecision() shouldBe null
            DungeonService.marker(game.state, game.player1Id) shouldBe null
        }
    }
}
