package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.SelectManaSourcesDecision
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mh3.cards.ShiftingWoodland
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario test for Shifting Woodland (MH3 #228).
 *
 * "Delirium — {2}{G}{G}: This land becomes a copy of target permanent card in your graveyard until
 *  end of turn. Activate only if there are four or more card types among cards in your graveyard."
 *
 * Pins the delirium activation gate, the graveyard-sourced self-copy, and the end-of-turn revert.
 */
class ShiftingWoodlandScenarioTest : ScenarioTestBase() {

    init {
        val copyAbilityId = ShiftingWoodland.activatedAbilities[1].id

        context("Shifting Woodland") {

            fun board(withDelirium: Boolean): TestGame {
                val b = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardOnBattlefield(1, "Shifting Woodland")
                    .withLandsOnBattlefield(1, "Forest", 4)
                    .withCardInGraveyard(1, "Grizzly Bears")
                    .withCardInGraveyard(1, "Forest")
                    .withCardInGraveyard(1, "Lightning Bolt")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                if (withDelirium) b.withCardInGraveyard(1, "Lava Axe")
                return b.build()
            }

            fun TestGame.canActivateCopy(): Boolean {
                val woodland = findPermanent("Shifting Woodland")!!
                return getLegalActions(1).any { info ->
                    val a = info.action
                    a is ActivateAbility && a.sourceId == woodland && a.abilityId == copyAbilityId
                }
            }

            test("the copy ability is not offered with only three card types in the graveyard") {
                val game = board(withDelirium = false)
                game.canActivateCopy() shouldBe false
            }

            test("with delirium it becomes a copy of the graveyard permanent card until end of turn") {
                val game = board(withDelirium = true)
                withClue("ability is offered with four card types") { game.canActivateCopy() shouldBe true }

                val woodland = game.findPermanent("Shifting Woodland")!!
                val bears = game.findCardsInGraveyard(1, "Grizzly Bears").single()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = woodland,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Card(cardId = bears, ownerId = game.player1Id, zone = Zone.GRAVEYARD)),
                    )
                )
                withClue("activation should succeed: ${result.error}") { result.error shouldBe null }
                if (game.getPendingDecision() is SelectManaSourcesDecision) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("it is now a Grizzly Bears creature") {
                    game.state.getEntity(woodland)!!.get<CardComponent>()!!.name shouldBe "Grizzly Bears"
                    game.state.projectedState.isCreature(woodland) shouldBe true
                    game.state.projectedState.getPower(woodland) shouldBe 2
                    game.state.projectedState.getToughness(woodland) shouldBe 2
                }
                withClue("the copied card stays in the graveyard") {
                    game.isInGraveyard(1, "Grizzly Bears") shouldBe true
                }

                game.passUntilPhase(Phase.BEGINNING, Step.UPKEEP)

                withClue("the copy ends at end of turn") {
                    game.state.getEntity(woodland)!!.get<CardComponent>()!!.name shouldBe "Shifting Woodland"
                    game.state.projectedState.isCreature(woodland) shouldBe false
                }
            }

            test("a nonpermanent card in the graveyard is not a legal target") {
                val game = board(withDelirium = true)
                val woodland = game.findPermanent("Shifting Woodland")!!
                val bolt = game.findCardsInGraveyard(1, "Lightning Bolt").single()
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = woodland,
                        abilityId = copyAbilityId,
                        targets = listOf(ChosenTarget.Card(cardId = bolt, ownerId = game.player1Id, zone = Zone.GRAVEYARD)),
                    )
                )
                withClue("instant card must be rejected") { (result.error != null) shouldBe true }
            }
        }
    }
}
