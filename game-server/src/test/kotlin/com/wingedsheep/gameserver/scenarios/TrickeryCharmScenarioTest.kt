package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.OptionChosenResponse
import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import io.kotest.assertions.withClue
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Trickery Charm.
 *
 * Card reference:
 * - Trickery Charm ({U}): Instant
 *   Choose one —
 *   • Target creature gains flying until end of turn.
 *   • Target creature becomes the creature type of your choice until end of turn.
 *   • Look at the top four cards of your library, then put them back in any order.
 */
class TrickeryCharmScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    /**
     * Helper to choose a mode from a modal spell by index.
     */
    private fun TestGame.chooseMode(modeIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, modeIndex))
    }

    /**
     * Helper to choose a creature type from a ChooseOptionDecision.
     */
    private fun TestGame.chooseCreatureType(type: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val index = Subtype.ALL_CREATURE_TYPES.indexOf(type)
        check(index >= 0) { "Unknown creature type: $type" }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Trickery Charm modal spell") {

            test("mode 1: target creature gains flying until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 Bear
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()

                // Choose mode 0: "Target creature gains flying until end of turn"
                game.chooseMode(0)

                // Select the single valid creature target
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bearsId))

                // Verify Grizzly Bears has flying
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should have flying") {
                    projected.hasKeyword(bearsId, Keyword.FLYING) shouldBe true
                }
            }

            test("mode 2: target creature becomes the creature type of your choice") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2 Bear
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()

                // Choose mode 1: "Target creature becomes the creature type of your choice"
                game.chooseMode(1)

                // Select the single valid creature target
                game.selectTargets(listOf(bearsId))

                // Now choose creature type: Goblin
                game.chooseCreatureType("Goblin")

                // Verify Grizzly Bears is now a Goblin, not a Bear
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should be a Goblin") {
                    projected.hasSubtype(bearsId, "Goblin") shouldBe true
                }
                withClue("Grizzly Bears should no longer be a Bear") {
                    projected.hasSubtype(bearsId, "Bear") shouldBe false
                }
                withClue("Grizzly Bears should still be a Creature") {
                    projected.hasType(bearsId, "CREATURE") shouldBe true
                }
            }

            test("mode 2: creature type change replaces all creature subtypes") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withCardOnBattlefield(2, "Sage Aven") // 1/3 Bird Wizard
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val avenId = game.findPermanent("Sage Aven")!!

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()

                // Choose mode 1: creature type change
                game.chooseMode(1)

                // Select the single valid creature target
                game.selectTargets(listOf(avenId))

                // Choose Elf as the new type
                game.chooseCreatureType("Elf")

                // Sage Aven should now be an Elf, losing both Bird and Wizard
                val projected = stateProjector.project(game.state)
                withClue("Sage Aven should be an Elf") {
                    projected.hasSubtype(avenId, "Elf") shouldBe true
                }
                withClue("Sage Aven should no longer be a Bird") {
                    projected.hasSubtype(avenId, "Bird") shouldBe false
                }
                withClue("Sage Aven should no longer be a Wizard") {
                    projected.hasSubtype(avenId, "Wizard") shouldBe false
                }
            }

            test("mode 3: look at top four cards and reorder") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()

                // Choose mode 2: look at top four and reorder
                game.chooseMode(2)

                // The reorder decision should be presented (or auto-completed if
                // library has fewer than 4 cards). Just verify the spell resolved.
                withClue("Trickery Charm should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Trickery Charm") shouldBe true
                }
            }

            test("mode 1 with multiple creatures prompts for target selection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")    // 3/3
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()

                // Choose mode 0: flying
                game.chooseMode(0)

                // Multiple valid creatures -> target selection decision
                game.selectTargets(listOf(giantId))

                // Hill Giant should have flying
                val projected = stateProjector.project(game.state)
                withClue("Hill Giant should have flying") {
                    projected.hasKeyword(giantId, Keyword.FLYING) shouldBe true
                }
            }

            test("Trickery Charm goes to graveyard after resolving") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Trickery Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Island", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Trickery Charm")
                game.resolveStack()
                game.chooseMode(0) // flying mode

                // Select the single valid creature target
                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bearsId))

                withClue("Trickery Charm should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Trickery Charm") shouldBe true
                }
            }
        }
    }
}
