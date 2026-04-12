package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Chronicle of Victory.
 *
 * Card reference:
 * - Chronicle of Victory ({6}): Legendary Artifact
 *   "As Chronicle of Victory enters, choose a creature type.
 *    Creatures you control of the chosen type get +2/+2 and have first strike and trample.
 *    Whenever you cast a spell of the chosen type, draw a card."
 */
class ChronicleOfVictoryScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = decision.options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    /**
     * Helper: cast Chronicle of Victory from hand (costs {6}) and choose a creature type.
     */
    private fun TestGame.castChronicleAndChoose(playerNumber: Int, typeName: String) {
        val castResult = castSpell(playerNumber, "Chronicle of Victory")
        withClue("Chronicle of Victory should be cast successfully: ${castResult.error}") {
            castResult.error shouldBe null
        }
        resolveStack()
        chooseCreatureType(typeName)
    }

    init {
        context("Chronicle of Victory - static ability: +2/+2, first strike, trample") {

            test("creatures you control of the chosen type get +2/+2 and keywords") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chronicle of Victory")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castChronicleAndChoose(1, "Elf")

                val projected = stateProjector.project(game.state)
                val elfWarrior = game.findPermanent("Elvish Warrior")!!

                withClue("Elvish Warrior (2/3 Elf) should be 4/5") {
                    projected.getPower(elfWarrior) shouldBe 4
                    projected.getToughness(elfWarrior) shouldBe 5
                }

                withClue("Elvish Warrior should have first strike") {
                    projected.hasKeyword(elfWarrior, Keyword.FIRST_STRIKE) shouldBe true
                }

                withClue("Elvish Warrior should have trample") {
                    projected.hasKeyword(elfWarrior, Keyword.TRAMPLE) shouldBe true
                }
            }

            test("only affects creatures you control, not opponent's") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chronicle of Victory")
                    .withCardOnBattlefield(1, "Elvish Warrior") // Elf Warrior, 2/3
                    .withCardOnBattlefield(2, "Wirewood Elf")   // Elf Druid, 1/2
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castChronicleAndChoose(1, "Elf")

                val projected = stateProjector.project(game.state)

                // Player's Elf should be buffed
                val elfWarrior = game.findPermanent("Elvish Warrior")!!
                withClue("Player's Elvish Warrior (2/3) should be 4/5") {
                    projected.getPower(elfWarrior) shouldBe 4
                    projected.getToughness(elfWarrior) shouldBe 5
                }

                // Opponent's Elf should NOT be buffed
                val wirewoodElf = game.findPermanent("Wirewood Elf")!!
                withClue("Opponent's Wirewood Elf (1/2) should remain 1/2") {
                    projected.getPower(wirewoodElf) shouldBe 1
                    projected.getToughness(wirewoodElf) shouldBe 2
                }

                withClue("Opponent's Wirewood Elf should not have first strike") {
                    projected.hasKeyword(wirewoodElf, Keyword.FIRST_STRIKE) shouldBe false
                }
            }

            test("creatures of a different type are not affected") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chronicle of Victory")
                    .withCardOnBattlefield(1, "Elvish Warrior")    // Elf, 2/3
                    .withCardOnBattlefield(1, "Goblin Sky Raider") // Goblin, 1/2
                    .withLandsOnBattlefield(1, "Plains", 6)
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castChronicleAndChoose(1, "Elf")

                val projected = stateProjector.project(game.state)

                val goblin = game.findPermanent("Goblin Sky Raider")!!
                withClue("Goblin Sky Raider (1/2) should remain 1/2") {
                    projected.getPower(goblin) shouldBe 1
                    projected.getToughness(goblin) shouldBe 2
                }

                withClue("Goblin should not have first strike") {
                    projected.hasKeyword(goblin, Keyword.FIRST_STRIKE) shouldBe false
                }
            }
        }

        context("Chronicle of Victory - triggered ability: draw on cast") {

            test("drawing a card when casting a spell of the chosen type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chronicle of Victory")
                    .withCardInHand(1, "Elvish Warrior") // Elf creature spell
                    .withLandsOnBattlefield(1, "Forest", 8) // 6 for Chronicle + 2 for Elvish Warrior
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castChronicleAndChoose(1, "Elf")

                val handSizeBefore = game.handSize(1)

                // Cast Elvish Warrior (Elf creature)
                val castResult = game.castSpell(1, "Elvish Warrior")
                withClue("Elvish Warrior should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                // Resolve the Elf creature spell — Chronicle trigger fires and draws a card
                game.resolveStack()

                // Hand: started with 1 (Elvish Warrior), cast it (-1), drew from trigger (+1) = same
                withClue("Player should have drawn a card from Chronicle of Victory trigger") {
                    game.handSize(1) shouldBe handSizeBefore
                }
            }

            test("no draw when casting a spell of a different type") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Chronicle of Victory")
                    .withCardInHand(1, "Goblin Sky Raider") // Goblin creature spell
                    .withLandsOnBattlefield(1, "Mountain", 9) // 6 for Chronicle + 3 for Goblin
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(1, "Forest")
                    .withCardInLibrary(2, "Forest")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castChronicleAndChoose(1, "Elf")

                val handSizeBefore = game.handSize(1)

                // Cast Goblin Sky Raider (Goblin, not Elf)
                val castResult = game.castSpell(1, "Goblin Sky Raider")
                withClue("Goblin Sky Raider should be cast successfully: ${castResult.error}") {
                    castResult.error shouldBe null
                }

                game.resolveStack()

                // No trigger — hand should be one less (cast the goblin, no draw)
                withClue("Player should NOT have drawn a card (wrong type)") {
                    game.handSize(1) shouldBe handSizeBefore - 1
                }
            }
        }
    }
}
