package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Tests that DynamicAmount.AggregateBattlefield respects temporary type changes
 * from effects like Mistform Wall's "become the creature type of your choice".
 *
 * Currently, DynamicAmountEvaluator uses base state for counting, so temporary
 * type changes are not reflected in counts used by Information Dealer and
 * Doubtless One.
 */
class MistformWallTypeChangeInteractionTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    /**
     * Activate Mistform Wall's "{1}: become the creature type of your choice" ability,
     * then choose the given creature type.
     */
    private fun TestGame.activateMistformWallAndChooseType(typeName: String) {
        val wallId = findPermanent("Mistform Wall")!!
        val cardDef = cardRegistry.getCard("Mistform Wall")!!
        val ability = cardDef.script.activatedAbilities.first()

        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = wallId,
                abilityId = ability.id
            )
        )
        withClue("Mistform Wall ability should activate: ${result.error}") {
            result.error shouldBe null
        }

        resolveStack()

        // Choose creature type
        val decision = state.pendingDecision
        withClue("Should have a pending decision for creature type selection") {
            (decision is ChooseOptionDecision) shouldBe true
        }
        decision as ChooseOptionDecision
        val index = decision.options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    private fun TestGame.activateInformationDealer() {
        val dealerId = findPermanent("Information Dealer")!!
        val cardDef = cardRegistry.getCard("Information Dealer")!!
        val ability = cardDef.script.activatedAbilities.first()
        val result = execute(
            ActivateAbility(
                playerId = player1Id,
                sourceId = dealerId,
                abilityId = ability.id
            )
        )
        withClue("Information Dealer ability should activate: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Mistform Wall + Information Dealer interaction") {

            test("Information Dealer should count Mistform Wall as a Wizard after type change") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Information Dealer")
                    .withCardOnBattlefield(1, "Mistform Wall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Turn Mistform Wall into a Wizard
                game.activateMistformWallAndChooseType("Wizard")

                // Now activate Information Dealer — should count 2 Wizards (itself + Mistform Wall)
                game.activateInformationDealer()
                game.resolveStack()

                game.hasPendingDecision() shouldBe true
                val decision = game.getPendingDecision()
                decision.shouldBeInstanceOf<ReorderLibraryDecision>()
                decision as ReorderLibraryDecision
                withClue("Should see 2 cards (2 Wizards: Information Dealer + Mistform Wall as Wizard)") {
                    decision.cards.size shouldBe 2
                }

                game.submitDecision(OrderedResponse(decision.id, decision.cards))
            }
        }

        context("Mistform Wall + Doubtless One interaction") {

            test("Doubtless One should count Mistform Wall as a Cleric after type change") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Doubtless One")
                    .withCardOnBattlefield(1, "Mistform Wall")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val doubtlessOne = game.findPermanent("Doubtless One")!!

                // Before type change: only 1 Cleric (Doubtless One itself)
                val projectedBefore = stateProjector.project(game.state)
                withClue("Doubtless One should be 1/1 with only itself as Cleric") {
                    projectedBefore.getPower(doubtlessOne) shouldBe 1
                    projectedBefore.getToughness(doubtlessOne) shouldBe 1
                }

                // Turn Mistform Wall into a Cleric
                game.activateMistformWallAndChooseType("Cleric")

                // After type change: 2 Clerics (Doubtless One + Mistform Wall)
                val projectedAfter = stateProjector.project(game.state)
                withClue("Doubtless One should be 2/2 with Mistform Wall now counting as a Cleric") {
                    projectedAfter.getPower(doubtlessOne) shouldBe 2
                    projectedAfter.getToughness(doubtlessOne) shouldBe 2
                }
            }
        }
    }
}
