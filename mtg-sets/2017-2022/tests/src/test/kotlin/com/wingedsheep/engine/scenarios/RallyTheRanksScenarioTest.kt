package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Rally the Ranks (KHM #20) — as it enters, choose a creature type; creatures you control of the
 * chosen type get +1/+1.
 */
class RallyTheRanksScenarioTest : ScenarioTestBase() {

    init {
        test("only your creatures of the chosen type get +1/+1") {
            val game = scenario()
                .withPlayers("Player", "Opponent")
                .withCardInHand(1, "Rally the Ranks")
                .withCardOnBattlefield(1, "Llanowar Elves")
                .withCardOnBattlefield(1, "Elvish Warrior")
                .withCardOnBattlefield(1, "Grizzly Bears")
                .withCardOnBattlefield(2, "Llanowar Elves")
                .withLandsOnBattlefield(1, "Plains", 2)
                .withActivePlayer(1)
                .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                .build()

            game.castSpell(1, "Rally the Ranks").error shouldBe null
            game.resolveStack()
            val decision = game.getPendingDecision()
            decision.shouldBeInstanceOf<ChooseOptionDecision>()
            val elf = decision.options.indexOf("Elf")
            (elf >= 0) shouldBe true
            game.submitDecision(OptionChosenResponse(decision.id, elf)).error shouldBe null
            game.resolveStack()

            val projected = game.state.projectedState
            val myElves = game.state.getBattlefield().filter { game.state.getEntity(it)?.get<ControllerComponent>()?.playerId == game.player1Id }
            val elves = game.findAllPermanents("Llanowar Elves")
            val mine = elves.single { it in myElves }
            val theirs = elves.single { it !in myElves }

            projected.getPower(mine) shouldBe 2
            projected.getToughness(mine) shouldBe 2
            projected.getPower(game.findPermanent("Elvish Warrior")!!) shouldBe 3
            projected.getToughness(game.findPermanent("Elvish Warrior")!!) shouldBe 4
            projected.getPower(game.findPermanent("Grizzly Bears")!!) shouldBe 2
            projected.getPower(theirs) shouldBe 1
        }
    }
}
