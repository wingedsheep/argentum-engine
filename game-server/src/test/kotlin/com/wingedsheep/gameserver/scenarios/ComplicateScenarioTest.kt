package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.player.ManaPoolComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class ComplicateScenarioTest : ScenarioTestBase() {

    init {
        context("Complicate - cast normally") {
            test("counters spell when opponent cannot pay") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Complicate")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Shock targeting Player1
                game.castSpellTargetingPlayer(2, "Shock", 1)

                // Pass priority so Player1 can respond
                game.passPriority()

                // Player1 responds with Complicate targeting Shock on the stack
                game.castSpellTargetingStackSpell(1, "Complicate", "Shock")

                // Resolve Complicate (both pass priority)
                game.resolveStack()

                // Opponent has no mana left (1 Mountain tapped for Shock) - auto-counter
                withClue("Shock should be countered (in graveyard)") {
                    game.isInGraveyard(2, "Shock") shouldBe true
                }
                withClue("Stack should be empty") {
                    game.state.stack.isEmpty() shouldBe true
                }
            }

            test("spell resolves when opponent pays the cost") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Complicate")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 4) // Enough to pay {3}
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Opponent casts Shock targeting Player1
                game.castSpellTargetingPlayer(2, "Shock", 1)

                // Pass priority so Player1 can respond
                game.passPriority()

                // Player1 responds with Complicate targeting Shock
                game.castSpellTargetingStackSpell(1, "Complicate", "Shock")

                // Resolve Complicate
                game.resolveStack()

                // Opponent should be asked to pay {3} (3 untapped Mountains remaining)
                withClue("Should have pending pay decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Give opponent floating mana so the continuation handler can deduct it
                game.state = game.state.updateEntity(game.player2Id) { c ->
                    c.with(ManaPoolComponent(red = 3))
                }

                // Opponent chooses to pay
                game.answerYesNo(true)

                // Shock should still be on the stack (not countered)
                withClue("Shock should still be on the stack") {
                    game.state.stack.any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                    } shouldBe true
                }

                // Resolve Shock
                game.resolveStack()

                withClue("Player1 should have taken 2 damage from Shock") {
                    game.getLifeTotal(1) shouldBe startingLife - 2
                }
            }
        }

        context("Complicate - cycling trigger") {
            test("cycling triggers may counter target spell unless pays 1") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Complicate")
                    .withLandsOnBattlefield(1, "Island", 3) // Enough to pay cycling cost {2}{U}
                    .withCardInLibrary(1, "Island") // Card to draw from cycling
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent casts Shock targeting Player1
                game.castSpellTargetingPlayer(2, "Shock", 1)

                // Pass priority so Player1 can respond
                game.passPriority()

                // Player1 cycles Complicate in response
                game.cycleCard(1, "Complicate")

                // OnCycle trigger fires - MayEffect asks yes/no
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Select Shock as the target spell
                val shockOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                }!!
                game.selectTargets(listOf(shockOnStack))

                // Resolve the triggered ability
                game.resolveStack()

                // Opponent has no mana to pay {1} (Mountain tapped for Shock) - auto-counter
                withClue("Shock should be countered (in graveyard)") {
                    game.isInGraveyard(2, "Shock") shouldBe true
                }
                withClue("Complicate should be in graveyard (was cycled)") {
                    game.isInGraveyard(1, "Complicate") shouldBe true
                }
            }

            test("cycling trigger can be declined") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Complicate")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 1)
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Opponent casts Shock targeting Player1
                game.castSpellTargetingPlayer(2, "Shock", 1)

                // Pass priority so Player1 can respond
                game.passPriority()

                // Player1 cycles Complicate
                game.cycleCard(1, "Complicate")

                // Decline the may trigger
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                // Shock should still be on the stack
                withClue("Shock should still be on the stack") {
                    game.state.stack.any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                    } shouldBe true
                }

                // Resolve Shock
                game.resolveStack()

                withClue("Player1 should have taken 2 damage") {
                    game.getLifeTotal(1) shouldBe startingLife - 2
                }
            }

            test("cycling trigger - opponent pays 1 to prevent counter") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Complicate")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withCardInLibrary(1, "Island")
                    .withCardInHand(2, "Shock")
                    .withLandsOnBattlefield(2, "Mountain", 2) // 1 tapped for Shock + 1 untapped to pay
                    .withCardInLibrary(2, "Mountain")
                    .withActivePlayer(2)
                    .withPriorityPlayer(2)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(1)

                // Opponent casts Shock targeting Player1
                game.castSpellTargetingPlayer(2, "Shock", 1)

                // Pass priority so Player1 can respond
                game.passPriority()

                // Player1 cycles Complicate
                game.cycleCard(1, "Complicate")

                // Accept the may trigger
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Select Shock as target
                val shockOnStack = game.state.stack.find { entityId ->
                    game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                }!!
                game.selectTargets(listOf(shockOnStack))

                // Resolve the triggered ability
                game.resolveStack()

                // Opponent should be asked to pay {1} (they have 1 untapped Mountain)
                withClue("Should have pending pay decision") {
                    game.hasPendingDecision() shouldBe true
                }

                // Give opponent floating mana so the continuation handler can deduct it
                game.state = game.state.updateEntity(game.player2Id) { c ->
                    c.with(ManaPoolComponent(red = 1))
                }
                game.answerYesNo(true)

                // Shock should still be on the stack
                withClue("Shock should still be on stack after paying") {
                    game.state.stack.any { entityId ->
                        game.state.getEntity(entityId)?.get<CardComponent>()?.name == "Shock"
                    } shouldBe true
                }

                // Resolve Shock
                game.resolveStack()

                withClue("Player1 should have taken 2 damage from Shock") {
                    game.getLifeTotal(1) shouldBe startingLife - 2
                }
            }
        }
    }
}
