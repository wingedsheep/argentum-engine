package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseTargetsDecision
import com.wingedsheep.engine.core.YesNoDecision
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class MeandersGuideScenarioTest : ScenarioTestBase() {

    init {
        context("Meanders Guide attack trigger") {

            test("declining the may does not tap any merfolk") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Meanders Guide")
                    .withCardOnBattlefield(1, "Merfolk of the Pearl Trident")
                    .withCardInGraveyard(1, "Merfolk of the Pearl Trident")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Meanders Guide" to 2))

                // Resolve until the first decision (the optional "may tap" yes/no).
                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }

                val firstDecision = game.getPendingDecision()
                withClue("Meanders Guide should ask the controller whether to tap a Merfolk before any merfolk is committed as a target") {
                    firstDecision.shouldBeInstanceOf<YesNoDecision>()
                }

                game.answerYesNo(false)

                // Finish resolving the trigger (it should fizzle gracefully on No).
                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }

                val merfolk = game.findAllPermanents("Merfolk of the Pearl Trident").single()
                withClue("Other Merfolk must remain untapped when the controller declines the may") {
                    game.state.getEntity(merfolk)?.has<TappedComponent>() shouldBe false
                }

                withClue("Graveyard creature must not return when the controller declines the may") {
                    game.findAllPermanents("Merfolk of the Pearl Trident").size shouldBe 1
                }
            }

            test("accepting the may taps the chosen merfolk and returns a graveyard creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Meanders Guide")
                    .withCardOnBattlefield(1, "Merfolk of the Pearl Trident")
                    .withCardInGraveyard(1, "Merfolk of the Pearl Trident")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                    .build()

                game.declareAttackers(mapOf("Meanders Guide" to 2))

                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }

                val firstDecision = game.getPendingDecision()
                firstDecision.shouldBeInstanceOf<YesNoDecision>()
                game.answerYesNo(true)

                // Now the player picks which Merfolk to tap.
                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }
                val merfolkSelectDecision = game.getPendingDecision()
                merfolkSelectDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val merfolkBattlefield = game.findAllPermanents("Merfolk of the Pearl Trident").single()
                game.selectTargets(listOf(merfolkBattlefield))

                // Reflexive trigger: pick a creature card from graveyard with mana value <= 3.
                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }
                val graveyardDecision = game.getPendingDecision()
                graveyardDecision.shouldBeInstanceOf<ChooseTargetsDecision>()
                val merfolkInGy = game.findCardsInGraveyard(1, "Merfolk of the Pearl Trident").single()
                game.selectTargets(listOf(merfolkInGy))

                while (game.state.stack.isNotEmpty() && !game.hasPendingDecision()) {
                    game.passPriority()
                }

                withClue("The chosen Merfolk should be tapped") {
                    game.state.getEntity(merfolkBattlefield)?.has<TappedComponent>() shouldBe true
                }
                withClue("The graveyard Merfolk should be returned to the battlefield") {
                    game.findAllPermanents("Merfolk of the Pearl Trident").size shouldBe 2
                }
                withClue("The graveyard should no longer contain that card") {
                    game.isInGraveyard(1, "Merfolk of the Pearl Trident") shouldBe false
                }
            }
        }
    }
}
