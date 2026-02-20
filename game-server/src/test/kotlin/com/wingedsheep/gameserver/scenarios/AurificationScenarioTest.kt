package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.mechanics.layers.StateProjector
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.model.EntityId
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

/**
 * Scenario tests for Aurification.
 *
 * Card reference:
 * - Aurification ({2}{W}{W}): Enchantment
 *   Whenever a creature deals damage to you, put a gold counter on it.
 *   Each creature with a gold counter on it is a Wall in addition to its
 *   other creature types and has defender. (Those creatures can't attack.)
 *   When this enchantment leaves the battlefield, remove all gold counters
 *   from all creatures.
 */
class AurificationScenarioTest : ScenarioTestBase() {

    private val stateProjector = StateProjector()

    private fun TestGame.getGoldCounters(entityId: EntityId): Int {
        return state.getEntity(entityId)
            ?.get<CountersComponent>()
            ?.getCount(CounterType.GOLD) ?: 0
    }

    init {
        context("Aurification triggered ability - gold counters on combat damage") {

            test("attacker gets a gold counter after dealing combat damage to controller") {
                val game = scenario()
                    .withPlayers("Aurification Player", "Attacker")
                    .withCardOnBattlefield(1, "Aurification")
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2 creature
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                withClue("Glory Seeker should start with 0 gold counters") {
                    game.getGoldCounters(glorySeeker) shouldBe 0
                }

                // Advance to combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)

                // Attack player 1 (Aurification controller)
                game.declareAttackers(mapOf("Glory Seeker" to 1))

                // Advance to blockers and declare no blockers
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()

                // Advance through combat damage - trigger fires and resolves
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Glory Seeker should now have a gold counter
                withClue("Glory Seeker should have 1 gold counter after dealing damage") {
                    game.getGoldCounters(glorySeeker) shouldBe 1
                }

                // Player 1 should have taken 2 damage
                withClue("Aurification player should have taken 2 combat damage") {
                    game.getLifeTotal(1) shouldBe 18
                }
            }

            test("multiple attackers each get gold counters") {
                val game = scenario()
                    .withPlayers("Aurification Player", "Attacker")
                    .withCardOnBattlefield(1, "Aurification")
                    .withCardOnBattlefield(2, "Glory Seeker")   // 2/2
                    .withCardOnBattlefield(2, "Elvish Warrior") // 2/3
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val elvishWarrior = game.findPermanent("Elvish Warrior")!!

                // Combat
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1, "Elvish Warrior" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                withClue("Glory Seeker should have 1 gold counter") {
                    game.getGoldCounters(glorySeeker) shouldBe 1
                }
                withClue("Elvish Warrior should have 1 gold counter") {
                    game.getGoldCounters(elvishWarrior) shouldBe 1
                }
            }
        }

        context("Aurification static ability - Wall type and defender") {

            test("creature with gold counter has defender and is a Wall") {
                val game = scenario()
                    .withPlayers("Aurification Player", "Attacker")
                    .withCardOnBattlefield(1, "Aurification")
                    .withCardOnBattlefield(2, "Glory Seeker") // Human Soldier 2/2
                    .withActivePlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!

                // Deal combat damage to get a gold counter
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_ATTACKERS)
                game.declareAttackers(mapOf("Glory Seeker" to 1))
                game.passUntilPhase(Phase.COMBAT, Step.DECLARE_BLOCKERS)
                game.declareNoBlockers()
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)

                // Verify gold counter was placed
                game.getGoldCounters(glorySeeker) shouldBe 1

                // Check projected state for defender and Wall type
                val projected = stateProjector.project(game.state)

                withClue("Glory Seeker with gold counter should have DEFENDER keyword") {
                    projected.hasKeyword(glorySeeker, Keyword.DEFENDER) shouldBe true
                }

                withClue("Glory Seeker with gold counter should be a Wall") {
                    projected.hasSubtype(glorySeeker, "Wall") shouldBe true
                }

                withClue("Glory Seeker should still be a Soldier (in addition to its other types)") {
                    projected.hasSubtype(glorySeeker, "Soldier") shouldBe true
                }
            }
        }

        context("Aurification leaves-the-battlefield trigger") {

            test("gold counters are removed when Aurification leaves the battlefield") {
                val game = scenario()
                    .withPlayers("Aurification Player", "Opponent")
                    .withCardOnBattlefield(1, "Aurification")
                    .withCardInHand(1, "Demystify") // Destroy target enchantment
                    .withLandsOnBattlefield(1, "Plains", 1)
                    .withCardOnBattlefield(2, "Glory Seeker") // 2/2
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val glorySeeker = game.findPermanent("Glory Seeker")!!
                val aurification = game.findPermanent("Aurification")!!

                // Manually add a gold counter to Glory Seeker (simulating combat damage trigger)
                val counters = CountersComponent().withAdded(CounterType.GOLD, 1)
                game.state = game.state.updateEntity(glorySeeker) { c -> c.with(counters) }

                // Verify gold counter exists
                withClue("Glory Seeker should have 1 gold counter") {
                    game.getGoldCounters(glorySeeker) shouldBe 1
                }

                // Verify creature has defender via static ability
                val projectedBefore = stateProjector.project(game.state)
                withClue("Glory Seeker should have defender before Aurification is removed") {
                    projectedBefore.hasKeyword(glorySeeker, Keyword.DEFENDER) shouldBe true
                }

                // Cast Demystify targeting Aurification
                game.castSpell(1, "Demystify", aurification)
                game.resolveStack() // Resolve Demystify -> destroys Aurification -> LTB trigger fires and resolves

                // Gold counters should be removed
                withClue("Glory Seeker should have 0 gold counters after Aurification leaves") {
                    game.getGoldCounters(glorySeeker) shouldBe 0
                }

                // Creature should no longer have defender or be a Wall
                val projectedAfter = stateProjector.project(game.state)
                withClue("Glory Seeker should no longer have defender") {
                    projectedAfter.hasKeyword(glorySeeker, Keyword.DEFENDER) shouldBe false
                }
                withClue("Glory Seeker should no longer be a Wall") {
                    projectedAfter.hasSubtype(glorySeeker, "Wall") shouldBe false
                }
            }
        }
    }
}
