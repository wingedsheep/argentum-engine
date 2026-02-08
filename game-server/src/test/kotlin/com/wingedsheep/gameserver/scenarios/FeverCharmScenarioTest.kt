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
 * Scenario tests for Fever Charm.
 *
 * Card reference:
 * - Fever Charm ({R}): Instant
 *   Choose one —
 *   • Target creature gains haste until end of turn.
 *   • Target creature gets +2/+0 until end of turn.
 *   • Fever Charm deals 3 damage to target Wizard creature.
 */
class FeverCharmScenarioTest : ScenarioTestBase() {

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

    init {
        context("Fever Charm modal spell") {

            test("mode 1: target creature gains haste until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2, has summoning sickness
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cast Fever Charm (no targets at cast time for modal spells)
                game.castSpell(1, "Fever Charm")
                game.resolveStack()

                // Choose mode 0: "Target creature gains haste until end of turn"
                game.chooseMode(0)

                // Should now ask for target selection - auto-selects if only one creature
                // With only Grizzly Bears as a valid creature, it auto-selects

                // Verify Grizzly Bears has haste
                val projected = stateProjector.project(game.state)
                val bearsId = game.findPermanent("Grizzly Bears")!!
                withClue("Grizzly Bears should have haste") {
                    projected.hasKeyword(bearsId, Keyword.HASTE) shouldBe true
                }
            }

            test("mode 2: target creature gets +2/+0 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears") // 2/2
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!

                game.castSpell(1, "Fever Charm")
                game.resolveStack()

                // Choose mode 1: "Target creature gets +2/+0 until end of turn"
                game.chooseMode(1)

                // Auto-selects Grizzly Bears as only valid target

                // Verify stats: 2/2 + 2/0 = 4/2
                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should be 4/2 after +2/+0") {
                    projected.getPower(bearsId) shouldBe 4
                    projected.getToughness(bearsId) shouldBe 2
                }
            }

            test("mode 3: deals 3 damage to target Wizard creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(2, "Sage Aven") // 1/3 Bird Wizard
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Fever Charm")
                game.resolveStack()

                // Choose mode 2: "Fever Charm deals 3 damage to target Wizard creature"
                game.chooseMode(2)

                // Auto-selects Sage Aven as only valid Wizard target
                // 3 damage to a 1/3 creature = lethal

                withClue("Sage Aven should be destroyed by 3 damage") {
                    game.isOnBattlefield("Sage Aven") shouldBe false
                }
                withClue("Sage Aven should be in graveyard") {
                    game.isInGraveyard(2, "Sage Aven") shouldBe true
                }
            }

            test("mode 2 with multiple creatures prompts for target selection") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2
                    .withCardOnBattlefield(1, "Hill Giant")     // 3/3
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Fever Charm")
                game.resolveStack()

                // Choose mode 1: +2/+0
                game.chooseMode(1)

                // Multiple valid creatures -> target selection decision
                game.selectTargets(listOf(giantId))

                // Hill Giant should be 5/3 (3/3 + 2/0)
                val projected = stateProjector.project(game.state)
                withClue("Hill Giant should be 5/3 after +2/+0") {
                    projected.getPower(giantId) shouldBe 5
                    projected.getToughness(giantId) shouldBe 3
                }
            }

            test("mode 3 cannot target non-Wizard creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(2, "Grizzly Bears") // 2/2 Bear, not a Wizard
                    .withCardOnBattlefield(2, "Sage Aven")     // 1/3 Bird Wizard
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Fever Charm")
                game.resolveStack()

                // Choose mode 2: deal 3 to Wizard creature
                game.chooseMode(2)

                // Only Sage Aven is a Wizard, so it should auto-select
                // Sage Aven should die (3 damage to 1/3)
                withClue("Sage Aven should be destroyed") {
                    game.isOnBattlefield("Sage Aven") shouldBe false
                }
                withClue("Grizzly Bears should still be alive (not a Wizard)") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("Fever Charm goes to graveyard after resolving") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Fever Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Mountain", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Fever Charm")
                game.resolveStack()
                game.chooseMode(0) // haste mode

                withClue("Fever Charm should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Fever Charm") shouldBe true
                }
            }
        }
    }
}
