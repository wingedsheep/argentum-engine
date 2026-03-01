package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario test: Elvish Soultiller dies trigger.
 *
 * "When Elvish Soultiller dies, choose a creature type. Shuffle all creature
 * cards of that type from your graveyard into your library."
 */
class ElvishSoultillerScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = state.pendingDecision
            ?: error("Expected a pending decision for creature type selection")
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = decision.options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Elvish Soultiller — dies trigger shuffles graveyard creatures into library") {

            test("shuffles creatures of chosen type from graveyard into library") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Soultiller")
                    .withCardInHand(2, "Death Pulse")
                    .withLandsOnBattlefield(2, "Swamp", 4)
                    .withCardInGraveyard(1, "Goblin Sledder")   // Goblin
                    .withCardInGraveyard(1, "Severed Legion")   // Zombie
                    .withCardInGraveyard(1, "Festering Goblin") // Zombie Goblin
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Kill Elvish Soultiller with Death Pulse (-4/-4 kills a 5/4)
                val soultillerId = game.findPermanent("Elvish Soultiller")!!
                game.castSpell(2, "Death Pulse", soultillerId)
                game.resolveStack()

                // Elvish Soultiller dies trigger fires — choose "Goblin"
                game.chooseCreatureType("Goblin")

                // Goblin Sledder and Festering Goblin should be shuffled into library
                // Severed Legion (Zombie, not Goblin) should stay in graveyard
                withClue("Severed Legion should still be in graveyard") {
                    game.isInGraveyard(1, "Severed Legion") shouldBe true
                }
                withClue("Goblin Sledder should NOT be in graveyard") {
                    game.isInGraveyard(1, "Goblin Sledder") shouldBe false
                }
                withClue("Festering Goblin should NOT be in graveyard") {
                    game.isInGraveyard(1, "Festering Goblin") shouldBe false
                }

                // Graveyard: Severed Legion + Elvish Soultiller = 2
                // (Death Pulse goes to Player 2's graveyard)
                withClue("Graveyard should have Severed Legion and Elvish Soultiller") {
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("no creatures of chosen type does nothing") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Soultiller")
                    .withCardInHand(2, "Death Pulse")
                    .withLandsOnBattlefield(2, "Swamp", 4)
                    .withCardInGraveyard(1, "Severed Legion")   // Zombie
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Kill Elvish Soultiller
                val soultillerId = game.findPermanent("Elvish Soultiller")!!
                game.castSpell(2, "Death Pulse", soultillerId)
                game.resolveStack()

                // Choose "Goblin" — no goblins in graveyard
                game.chooseCreatureType("Goblin")

                // Severed Legion should still be in graveyard (it's a Zombie, not Goblin)
                withClue("Severed Legion should still be in graveyard") {
                    game.isInGraveyard(1, "Severed Legion") shouldBe true
                }
                // Graveyard: Severed Legion + Elvish Soultiller = 2
                withClue("Graveyard should have 2 cards") {
                    game.graveyardSize(1) shouldBe 2
                }
            }

            test("Elvish Soultiller itself can be shuffled if matching type is chosen") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Elvish Soultiller")
                    .withCardInHand(2, "Death Pulse")
                    .withLandsOnBattlefield(2, "Swamp", 4)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Kill Elvish Soultiller
                val soultillerId = game.findPermanent("Elvish Soultiller")!!
                game.castSpell(2, "Death Pulse", soultillerId)
                game.resolveStack()

                // Choose "Elf" — Elvish Soultiller is an Elf Mutant, so it matches
                game.chooseCreatureType("Elf")

                // Elvish Soultiller should be shuffled into library (it's in GY when trigger resolves)
                withClue("Elvish Soultiller should NOT be in graveyard") {
                    game.isInGraveyard(1, "Elvish Soultiller") shouldBe false
                }
                // Graveyard should be empty (Soultiller was shuffled, Death Pulse is P2's)
                withClue("Graveyard should be empty") {
                    game.graveyardSize(1) shouldBe 0
                }
            }
        }
    }
}
