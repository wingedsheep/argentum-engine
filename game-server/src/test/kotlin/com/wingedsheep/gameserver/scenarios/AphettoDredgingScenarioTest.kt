package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.state.components.stack.SpellOnStackComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test: Aphetto Dredging creature type choice happens during casting.
 *
 * "Return up to three target creature cards of the creature type of your choice
 * from your graveyard to your hand."
 *
 * Per MTG rules, the creature type choice is part of casting, not resolution.
 * The opponent should see the chosen type on the stack before deciding to respond.
 */
class AphettoDredgingScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = state.pendingDecision
            ?: error("Expected a pending decision for creature type selection")
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Aphetto Dredging — creature type choice during casting") {

            test("type selection happens before spell goes on stack") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Aphetto Dredging")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(1, "Goblin Sledder")  // Goblin
                    .withCardInGraveyard(1, "Severed Legion") // Zombie
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Aphetto Dredging — should pause for creature type choice
                val castResult = game.castSpell(1, "Aphetto Dredging")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Should have a pending decision for creature type (CASTING phase)
                val decision = game.state.pendingDecision
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                withClue("Decision should be in CASTING phase") {
                    decision.context.phase shouldBe DecisionPhase.CASTING
                }

                // Spell should NOT be on the stack yet (it's paused mid-casting)
                withClue("Stack should be empty while choosing creature type") {
                    game.state.stack.isEmpty() shouldBe true
                }

                // Choose "Zombie"
                game.chooseCreatureType("Zombie")

                // Now the spell should be on the stack with the chosen type
                withClue("Spell should be on the stack after type choice") {
                    game.state.stack.size shouldBe 1
                }

                val spellId = game.state.stack.first()
                val spellComponent = game.state.getEntity(spellId)?.get<SpellOnStackComponent>()
                withClue("Spell should have chosenCreatureType set") {
                    spellComponent shouldNotBe null
                    spellComponent!!.chosenCreatureType shouldBe "Zombie"
                }
            }

            test("resolution uses pre-chosen type and proceeds to card selection") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Aphetto Dredging")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    .withCardInGraveyard(1, "Severed Legion")  // Zombie
                    .withCardInGraveyard(1, "Goblin Sledder")  // Goblin
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val zombieId = game.findCardsInGraveyard(1, "Severed Legion").first()

                // Cast and choose Zombie during casting
                game.castSpell(1, "Aphetto Dredging")
                game.chooseCreatureType("Zombie")

                // Resolve the stack (both pass priority)
                game.resolveStack()

                // Resolution should skip to card selection (not ask for type again)
                val decision = game.state.pendingDecision
                withClue("Should have a card selection decision, not another creature type choice") {
                    decision.shouldBeInstanceOf<SelectCardsDecision>()
                }

                // Select the Zombie creature to return
                game.selectCards(listOf(zombieId))

                // Severed Legion should now be in hand
                withClue("Severed Legion should be in hand") {
                    game.isInHand(1, "Severed Legion") shouldBe true
                }
            }

            test("no creature type choice when graveyard has no creatures") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Aphetto Dredging")
                    .withLandsOnBattlefield(1, "Swamp", 4)
                    // No creatures in graveyard
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Aphetto Dredging — should NOT pause for type choice (no creatures in GY)
                val castResult = game.castSpell(1, "Aphetto Dredging")
                withClue("Cast should succeed: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Spell should go directly on the stack (no type decision needed)
                withClue("Spell should be on the stack") {
                    game.state.stack.size shouldBe 1
                }
                withClue("No pending decision should exist") {
                    game.state.pendingDecision shouldBe null
                }
            }
        }
    }
}
