package com.wingedsheep.engine.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.core.PlayLand
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.engine.support.ScenarioTestBase
import com.wingedsheep.mtg.sets.definitions.mrd.cards.EmpyrialPlate
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Empyrial Plate (MRD #168, {2}, Artifact — Equipment).
 *
 *   Equipped creature gets +1/+1 for each card in your hand.
 *   Equip {2}
 *
 * The point of interest is that the bonus is a *continuously recomputed* Layer 7c amount, not a
 * value snapshotted when the Plate is attached — so the second test shrinks the hand mid-turn and
 * asserts the equipped creature shrinks with it.
 */
class EmpyrialPlateScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    init {
        context("Empyrial Plate") {

            test("equipping grants +1/+1 for each card in the controller's hand") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Empyrial Plate")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardsInHand(1, "Forest", 3)
                    .withLandsOnBattlefield(1, "Forest", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val plate = game.findPermanent("Empyrial Plate")!!
                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("Grizzly Bears is a plain 2/2 before the Plate is attached") {
                    val before = stateProjector.project(game.state)
                    before.getPower(bears) shouldBe 2
                    before.getToughness(bears) shouldBe 2
                }

                val equip = EmpyrialPlate.activatedAbilities.single { it.isEquipAbility }
                val result = game.execute(
                    ActivateAbility(
                        playerId = game.player1Id,
                        sourceId = plate,
                        abilityId = equip.id,
                        targets = listOf(ChosenTarget.Permanent(bears))
                    )
                )
                withClue("Activating equip should succeed: ${result.error}") { result.error shouldBe null }
                if (game.hasPendingDecision()) game.submitManaSourcesAutoPay()
                game.resolveStack()

                withClue("The Plate should be attached to Grizzly Bears") {
                    game.state.getEntity(plate)?.get<AttachedToComponent>()?.targetId shouldBe bears
                }
                withClue("Three cards in hand turns the 2/2 into a 5/5") {
                    val after = stateProjector.project(game.state)
                    after.getPower(bears) shouldBe 5
                    after.getToughness(bears) shouldBe 5
                }
            }

            test("the bonus tracks hand size as it changes, and is zero on an empty hand") {
                val game = scenario()
                    .withPlayers("Alice", "Bob")
                    .withCardOnBattlefield(1, "Grizzly Bears", summoningSickness = false)
                    .withCardAttachedTo(1, "Empyrial Plate", "Grizzly Bears")
                    .withCardInHand(1, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bears = game.findPermanent("Grizzly Bears")!!

                withClue("One card in hand is +1/+1") {
                    val before = stateProjector.project(game.state)
                    before.getPower(bears) shouldBe 3
                    before.getToughness(bears) shouldBe 3
                }

                val forest = game.findCardsInHand(1, "Forest").single()
                val played = game.execute(PlayLand(game.player1Id, forest))
                withClue("Playing the land should succeed: ${played.error}") { played.error shouldBe null }

                withClue("Emptying the hand drops the bonus back to nothing") {
                    val after = stateProjector.project(game.state)
                    after.getPower(bears) shouldBe 2
                    after.getToughness(bears) shouldBe 2
                }
            }
        }
    }
}
