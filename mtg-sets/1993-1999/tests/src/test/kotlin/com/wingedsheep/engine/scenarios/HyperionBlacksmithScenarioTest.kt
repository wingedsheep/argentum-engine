package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.leg.cards.HyperionBlacksmith
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Hyperion Blacksmith (LEG): "{T}: You may tap or untap target artifact an opponent controls."
 */
class HyperionBlacksmithScenarioTest : ScenarioTestBase() {
    private val abilityId = HyperionBlacksmith.activatedAbilities.single().id

    private fun activate(artifactTapped: Boolean, mode: Int): Boolean {
        val game = scenario()
            .withPlayers("Player1", "Player2")
            .withCardOnBattlefield(1, "Hyperion Blacksmith")
            .withCardOnBattlefield(2, "Jayemdae Tome", tapped = artifactTapped)
            .withActivePlayer(1)
            .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
            .build()
        val smith = game.findPermanent("Hyperion Blacksmith")!!
        val tome = game.findPermanent("Jayemdae Tome")!!
        game.execute(
            ActivateAbility(game.player1Id, smith, abilityId, targets = listOf(ChosenTarget.Permanent(tome)))
        ).error shouldBe null
        game.resolveStack()
        game.getPendingDecision().shouldBeInstanceOf<YesNoDecision>()
        game.answerYesNo(true)
        val choice = game.getPendingDecision().shouldBeInstanceOf<ChooseOptionDecision>()
        game.submitDecision(OptionChosenResponse(choice.id, optionIndex = mode))
        game.resolveStack()
        return game.state.getEntity(tome)!!.has<TappedComponent>()
    }

    init {
        test("taps an untapped opposing artifact") {
            activate(artifactTapped = false, mode = 0) shouldBe true
        }

        test("untaps a tapped opposing artifact") {
            activate(artifactTapped = true, mode = 1) shouldBe false
        }
    }
}
