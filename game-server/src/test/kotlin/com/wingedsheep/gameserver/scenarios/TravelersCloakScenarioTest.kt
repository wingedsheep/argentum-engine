package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.AttachedToComponent
import com.wingedsheep.engine.state.components.identity.ChosenLandTypeComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Traveler's Cloak.
 *
 * Card reference:
 * - Traveler's Cloak ({2}{U}): Enchantment — Aura
 *   "Enchant creature. As this Aura enters, choose a land type. When this Aura enters, draw a card.
 *    Enchanted creature has landwalk of the chosen type."
 *
 * Exercises [com.wingedsheep.sdk.scripting.GrantLandwalkOfChosenType] — the chosen-value counterpart
 * to `GrantKeyword`. The [com.wingedsheep.sdk.scripting.ChoiceType.BASIC_LAND_TYPE] entry choice
 * records a [ChosenLandTypeComponent]; at projection time
 * ([com.wingedsheep.engine.mechanics.layers.Modification.GrantLandwalkFromChosen], Layer 6) the
 * enchanted creature gains the matching landwalk keyword (Plains→Plainswalk, Island→Islandwalk, …).
 */
class TravelersCloakScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    /** Answer the pending [ChooseOptionDecision] by picking [landType], then resolve any ETB triggers. */
    private fun ScenarioTestBase.TestGame.chooseLandType(landType: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = decision.options.indexOf(landType)
        withClue("'$landType' should be among options ${decision.options}") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
        resolveStack()
    }

    init {
        context("Traveler's Cloak grants landwalk of the chosen type to the enchanted creature") {

            test("choosing Island grants islandwalk and draws a card") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Traveler's Cloak")
                    .withCardOnBattlefield(1, "Gorilla Warrior")  // the creature to enchant
                    .withCardInLibrary(1, "Forest")               // something to draw off the ETB
                    .withLandsOnBattlefield(1, "Island", 3)       // mana to pay {2}{U}
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Gorilla Warrior")!!
                val handBefore = game.handSize(1)

                val castResult = game.castSpell(1, "Traveler's Cloak", creature)
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Aura resolves — the EntersWithChoice replacement pauses for the land-type choice.
                game.resolveStack()
                withClue("Should pause for the basic land type choice") {
                    game.hasPendingDecision() shouldBe true
                }
                game.chooseLandType("Island")

                // Chosen type recorded on the aura, which is attached to the creature.
                val auraId = game.findPermanent("Traveler's Cloak")!!
                game.state.getEntity(auraId)!!.get<ChosenLandTypeComponent>()
                    .shouldNotBeNull().landType shouldBe "Island"
                game.state.getEntity(auraId)!!.get<AttachedToComponent>()!!.targetId shouldBe creature

                val projected = stateProjector.project(game.state)
                withClue("Enchanted creature should have islandwalk") {
                    projected.hasKeyword(creature, Keyword.ISLANDWALK) shouldBe true
                }

                withClue("\"When this Aura enters, draw a card\" should have drawn one card") {
                    game.handSize(1) shouldBe handBefore - 1 + 1  // -1 cast Cloak, +1 drawn
                }
            }

            test("the granted landwalk is not hardcoded — choosing Forest grants forestwalk, not islandwalk") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Traveler's Cloak")
                    .withCardOnBattlefield(1, "Gorilla Warrior")
                    .withCardInLibrary(1, "Forest")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val creature = game.findPermanent("Gorilla Warrior")!!
                game.castSpell(1, "Traveler's Cloak", creature).error shouldBe null
                game.resolveStack()
                game.chooseLandType("Forest")

                val projected = stateProjector.project(game.state)
                withClue("Enchanted creature should have forestwalk") {
                    projected.hasKeyword(creature, Keyword.FORESTWALK) shouldBe true
                }
                withClue("Enchanted creature should NOT have islandwalk (chosen type drives the grant)") {
                    projected.hasKeyword(creature, Keyword.ISLANDWALK) shouldBe false
                }
            }
        }
    }
}
