package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** Retreat to Emeria — a modal landfall trigger: a Kor Ally token, or +1/+1 to your team. */
class RetreatToEmeriaScenarioTest : ScenarioTestBase() {

    private fun board() = scenario()
        .withPlayers("Player", "Opponent")
        .withCardOnBattlefield(1, "Retreat to Emeria")
        .withCardOnBattlefield(1, "Grizzly Bears")
        .withCardOnBattlefield(2, "Hill Giant")
        .withCardInHand(1, "Plains")
        .withActivePlayer(1)
        .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
        .build()

    private fun TestGame.landfallChoosing(prefix: String) {
        execute(PlayLand(player1Id, findCardsInHand(1, "Plains").first())).error shouldBe null
        val decision = getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOfFirst { it.startsWith(prefix) }
        check(index >= 0) { "$prefix not offered; options=${decision.options}" }
        submitDecision(OptionChosenResponse(decision.id, optionIndex = index)).error shouldBe null
        resolveStack()
    }

    init {
        test("first mode creates a 1/1 white Kor Ally token") {
            val game = board()
            game.landfallChoosing("Create")
            val token = game.findPermanent("Kor Ally Token")!!
            game.state.projectedState.getPower(token) shouldBe 1
            game.state.projectedState.getToughness(token) shouldBe 1
        }

        test("second mode pumps only your creatures") {
            val game = board()
            game.landfallChoosing("Creatures you control")
            val projected = game.state.projectedState
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 3
            projected.getToughness(game.findPermanent("Grizzly Bears")!!) shouldBe 3
            projected.getPower(game.findPermanent("Hill Giant")!!) shouldBe 3
        }
    }
}
