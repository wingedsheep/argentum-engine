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
 * Scenario tests for Piety Charm.
 *
 * Card reference:
 * - Piety Charm ({W}): Instant
 *   Choose one —
 *   • Destroy target Aura attached to a creature.
 *   • Target Soldier creature gets +2/+2 until end of turn.
 *   • Creatures you control gain vigilance until end of turn.
 */
class PietyCharmScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.chooseMode(modeIndex: Int) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        submitDecision(OptionChosenResponse(decision.id, modeIndex))
    }

    init {
        context("Piety Charm modal spell") {

            test("mode 1: destroy target Aura attached to a creature") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Pacifism")
                    .withCardInHand(1, "Piety Charm")
                    .withCardOnBattlefield(2, "Hill Giant") // 3/3 creature to enchant
                    .withLandsOnBattlefield(1, "Plains", 3)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val giantId = game.findPermanent("Hill Giant")!!

                // Cast Pacifism on Hill Giant first
                game.castSpell(1, "Pacifism", giantId)
                game.resolveStack()

                withClue("Pacifism should be on the battlefield") {
                    game.isOnBattlefield("Pacifism") shouldBe true
                }

                // Now cast Piety Charm to destroy the Aura
                game.castSpell(1, "Piety Charm")
                game.resolveStack()

                // Choose mode 0: "Destroy target Aura attached to a creature"
                game.chooseMode(0)

                // Select the single valid Aura target
                val pacifismId = game.findPermanent("Pacifism")!!
                game.selectTargets(listOf(pacifismId))

                withClue("Pacifism should be destroyed") {
                    game.isOnBattlefield("Pacifism") shouldBe false
                }
                withClue("Pacifism should be in player's graveyard") {
                    game.isInGraveyard(1, "Pacifism") shouldBe true
                }
            }

            test("mode 2: target Soldier creature gets +2/+2 until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Piety Charm")
                    .withCardOnBattlefield(1, "Glory Seeker") // 2/2 Human Soldier
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Piety Charm")
                game.resolveStack()

                // Choose mode 1: "Target Soldier creature gets +2/+2 until end of turn"
                game.chooseMode(1)

                // Select the single valid Soldier target
                game.selectTargets(listOf(seekerId))

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 4/4 after +2/+2") {
                    projected.getPower(seekerId) shouldBe 4
                    projected.getToughness(seekerId) shouldBe 4
                }
            }

            test("mode 2 cannot target non-Soldier creatures") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Piety Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2 Bear, NOT a Soldier
                    .withCardOnBattlefield(1, "Glory Seeker")   // 2/2 Human Soldier
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val seekerId = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Piety Charm")
                game.resolveStack()

                // Choose mode 1: +2/+2 to Soldier
                game.chooseMode(1)

                // Only Glory Seeker is a Soldier - select it
                game.selectTargets(listOf(seekerId))

                val projected = stateProjector.project(game.state)
                withClue("Glory Seeker should be 4/4 after +2/+2") {
                    projected.getPower(seekerId) shouldBe 4
                    projected.getToughness(seekerId) shouldBe 4
                }
                withClue("Grizzly Bears should be unaffected at 2/2") {
                    val bearsId = game.findPermanent("Grizzly Bears")!!
                    projected.getPower(bearsId) shouldBe 2
                    projected.getToughness(bearsId) shouldBe 2
                }
            }

            test("mode 3: creatures you control gain vigilance until end of turn") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Piety Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")  // 2/2
                    .withCardOnBattlefield(1, "Glory Seeker")   // 2/1
                    .withCardOnBattlefield(2, "Hill Giant")     // 3/3 opponent's creature
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val bearsId = game.findPermanent("Grizzly Bears")!!
                val seekerId = game.findPermanent("Glory Seeker")!!
                val giantId = game.findPermanent("Hill Giant")!!

                game.castSpell(1, "Piety Charm")
                game.resolveStack()

                // Choose mode 2: "Creatures you control gain vigilance until end of turn"
                game.chooseMode(2)

                val projected = stateProjector.project(game.state)
                withClue("Grizzly Bears should have vigilance") {
                    projected.hasKeyword(bearsId, Keyword.VIGILANCE) shouldBe true
                }
                withClue("Glory Seeker should have vigilance") {
                    projected.hasKeyword(seekerId, Keyword.VIGILANCE) shouldBe true
                }
                withClue("Opponent's Hill Giant should NOT have vigilance") {
                    projected.hasKeyword(giantId, Keyword.VIGILANCE) shouldBe false
                }
            }

            test("Piety Charm goes to graveyard after resolving") {
                val game = scenario()
                    .withPlayers("Player", "Opponent")
                    .withCardInHand(1, "Piety Charm")
                    .withCardOnBattlefield(1, "Grizzly Bears")
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Piety Charm")
                game.resolveStack()
                game.chooseMode(2) // vigilance mode (no target needed)

                withClue("Piety Charm should be in graveyard after resolving") {
                    game.isInGraveyard(1, "Piety Charm") shouldBe true
                }
            }
        }
    }
}
