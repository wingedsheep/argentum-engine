package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.*
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/**
 * Scenario tests for Standardize.
 *
 * Card reference:
 * - Standardize ({U}{U}): Instant
 *   "Choose a creature type other than Wall. Each creature becomes that type until end of turn."
 */
class StandardizeScenarioTest : ScenarioTestBase() {

    private fun TestGame.chooseCreatureType(typeName: String) {
        val decision = getPendingDecision()
        decision.shouldNotBeNull()
        decision.shouldBeInstanceOf<ChooseOptionDecision>()
        val options = (decision as ChooseOptionDecision).options
        val index = options.indexOf(typeName)
        withClue("Creature type '$typeName' should be in options $options") {
            (index >= 0) shouldBe true
        }
        submitDecision(OptionChosenResponse(decision.id, index))
    }

    init {
        context("Standardize") {

            test("all creatures become the chosen type") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardOnBattlefield(2, "Goblin Sledder")   // 1/1 Goblin
                    .withCardInHand(1, "Standardize")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!
                val goblinSledderId = game.findPermanent("Goblin Sledder")!!

                // Cast Standardize (no targets)
                val result = game.castSpell(1, "Standardize")
                withClue("Cast should succeed: ${result.error}") {
                    result.error shouldBe null
                }

                // Resolve the spell
                game.resolveStack()

                // Choose "Zombie" as the creature type
                game.chooseCreatureType("Zombie")

                // All creatures should now be Zombies
                val clientState = game.getClientState(1)

                val elvishWarrior = clientState.cards[elvishWarriorId]
                withClue("Elvish Warrior should be a Zombie (lost Elf Warrior types)") {
                    elvishWarrior.shouldNotBeNull()
                    elvishWarrior.subtypes shouldBe setOf("Zombie")
                }

                val glorySeeker = clientState.cards[glorySeekerID]
                withClue("Glory Seeker should be a Zombie (lost Human Soldier types)") {
                    glorySeeker.shouldNotBeNull()
                    glorySeeker.subtypes shouldBe setOf("Zombie")
                }

                // Opponent's creature also becomes the chosen type
                val goblinSledder = clientState.cards[goblinSledderId]
                withClue("Goblin Sledder should be a Zombie (lost Goblin type)") {
                    goblinSledder.shouldNotBeNull()
                    goblinSledder.subtypes shouldBe setOf("Zombie")
                }
            }

            test("Wall cannot be chosen") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Glory Seeker")
                    .withCardInHand(1, "Standardize")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.castSpell(1, "Standardize")
                game.resolveStack()

                // Verify Wall is not in the options
                val decision = game.getPendingDecision()
                decision.shouldNotBeNull()
                decision.shouldBeInstanceOf<ChooseOptionDecision>()
                (decision as ChooseOptionDecision).options shouldNotContain "Wall"
            }

            test("effect expires at end of turn") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Elvish Warrior")   // 2/3 Elf Warrior
                    .withCardOnBattlefield(1, "Glory Seeker")     // 2/2 Human Soldier
                    .withCardInHand(1, "Standardize")
                    .withLandsOnBattlefield(1, "Island", 2)
                    .withCardInLibrary(1, "Island")
                    .withCardInLibrary(2, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val elvishWarriorId = game.findPermanent("Elvish Warrior")!!
                val glorySeekerID = game.findPermanent("Glory Seeker")!!

                game.castSpell(1, "Standardize")
                game.resolveStack()
                game.chooseCreatureType("Zombie")

                // Verify effect is active during P1's turn
                val duringTurn = game.getClientState(1)
                withClue("Elvish Warrior should be Zombie during P1's turn") {
                    duringTurn.cards[elvishWarriorId]!!.subtypes shouldBe setOf("Zombie")
                }
                withClue("Glory Seeker should be Zombie during P1's turn") {
                    duringTurn.cards[glorySeekerID]!!.subtypes shouldBe setOf("Zombie")
                }

                // Advance to P2's turn
                game.passUntilPhase(Phase.POSTCOMBAT_MAIN, Step.POSTCOMBAT_MAIN)
                game.passUntilPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)

                withClue("Active player should now be P2") {
                    game.state.activePlayerId shouldBe game.player2Id
                }

                // Effect should have expired — subtypes restored
                val afterTurn = game.getClientState(1)
                withClue("Elvish Warrior should be Elf Warrior again after end of turn") {
                    afterTurn.cards[elvishWarriorId]!!.subtypes shouldBe setOf("Elf", "Warrior")
                }
                withClue("Glory Seeker should be Human Soldier again after end of turn") {
                    afterTurn.cards[glorySeekerID]!!.subtypes shouldBe setOf("Human", "Soldier")
                }
            }
        }
    }
}
