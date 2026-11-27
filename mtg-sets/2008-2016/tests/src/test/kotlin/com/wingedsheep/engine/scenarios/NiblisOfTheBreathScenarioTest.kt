package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.dka.cards.NiblisOfTheBreath
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class NiblisOfTheBreathScenarioTest : ScenarioTestBase() {
    private val abilityId = NiblisOfTheBreath.activatedAbilities.single().id

    /** Activates on a Grizzly Bears that starts [bearsTapped]; returns whether it ends tapped. */
    private fun activate(bearsTapped: Boolean, accept: Boolean, mode: Int): Boolean {
        val game = scenario()
            .withPlayers("Player", "Opponent")
            .withCardOnBattlefield(1, "Niblis of the Breath")
            .withLandsOnBattlefield(1, "Island", 1)
            .withCardOnBattlefield(2, "Grizzly Bears", tapped = bearsTapped)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val niblis = game.findPermanent("Niblis of the Breath")!!
        val bears = game.findPermanent("Grizzly Bears")!!
        game.execute(
            ActivateAbility(game.player1Id, niblis, abilityId, targets = listOf(ChosenTarget.Permanent(bears)))
        ).error shouldBe null
        game.resolveStack()
        game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        game.answerYesNo(accept)
        if (accept) {
            val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
            game.submitDecision(OptionChosenResponse(choice.id, optionIndex = mode))
        }
        game.resolveStack()
        return game.state.getEntity(bears)!!.has<TappedComponent>()
    }

    init {
        test("taps an untapped creature") {
            activate(bearsTapped = false, accept = true, mode = 0) shouldBe true
        }

        test("untaps a tapped creature") {
            activate(bearsTapped = true, accept = true, mode = 1) shouldBe false
        }

        test("declining leaves the creature as it was") {
            activate(bearsTapped = true, accept = false, mode = 1) shouldBe true
        }
    }
}
