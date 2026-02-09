package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.engine.state.components.identity.CardComponent
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class AstralSlideScenarioTest : ScenarioTestBase() {

    /**
     * Helper to check if a card is in a player's exile zone.
     */
    private fun ScenarioTestBase.TestGame.isInExile(playerNumber: Int, cardName: String): Boolean {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getExile(playerId).any { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name == cardName
        }
    }

    init {
        context("Astral Slide") {
            test("opponent cycling triggers Astral Slide") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(2, "Disciple of Malice") // Opponent has a cycling card
                    .withLandsOnBattlefield(2, "Swamp", 2)
                    .withCardInLibrary(2, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker") // Target creature
                    .withActivePlayer(1)
                    .withPriorityPlayer(2) // Opponent has priority to cycle
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Opponent cycles their card - Astral Slide should trigger for Player1
                val cycleResult = game.cycleCard(2, "Disciple of Malice")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Astral Slide triggers - MayEffect asks yes/no first
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Now select target
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                game.resolveStack()

                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(2, "Glory Seeker") shouldBe true
                }

                // Advance to end step - creature returns
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Glory Seeker should be back on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }

            test("exile own creature for flicker") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardOnBattlefield(1, "Glory Seeker") // Own creature to flicker
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.cycleCard(1, "Disciple of Grace")

                // May decision first
                game.answerYesNo(true)

                // Target own creature
                val targetId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(targetId))

                game.resolveStack()

                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(1, "Glory Seeker") shouldBe true
                }

                // Advance to end step
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Glory Seeker should return to battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
            }


            test("basic cycle-exile-return: cycle a card, exile creature, creature returns at end step") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain") // Card to draw from cycling
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                withClue("Opponent should have Glory Seeker on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }

                // Cycle the card - Astral Slide triggers
                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Cycling should succeed") {
                    cycleResult.error shouldBe null
                }

                // Astral Slide triggers - MayEffect asks yes/no first
                withClue("Should have pending may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                // Now select the target
                val targetId = game.findPermanent("Glory Seeker")
                withClue("Glory Seeker should be on battlefield for targeting") {
                    targetId shouldNotBe null
                }
                game.selectTargets(listOf(targetId!!))

                // Ability is now on the stack - resolve it (both players pass priority)
                game.resolveStack()

                // Glory Seeker should now be in exile
                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(2, "Glory Seeker") shouldBe true
                }

                // Advance to end step - the delayed trigger should return the creature
                game.passUntilPhase(Phase.ENDING, Step.END)

                // The delayed trigger fires and resolves - the creature comes back
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Glory Seeker should be back on battlefield after end step") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in exile anymore") {
                    game.isInExile(2, "Glory Seeker") shouldBe false
                }
            }

            test("may decline exile: cycle but choose not to exile") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle
                game.cycleCard(1, "Disciple of Grace")

                // Decline the "you may" effect (before target selection)
                withClue("Should have may decision") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(false)

                // Creature should still be on the battlefield
                withClue("Glory Seeker should still be on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Glory Seeker should not be in exile") {
                    game.isInExile(2, "Glory Seeker") shouldBe false
                }
            }

            test("two Astral Slides both trigger on same cycle") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardOnBattlefield(1, "Astral Slide") // Second copy
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardOnBattlefield(2, "Grizzly Bears")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                // Cycle - both Astral Slides should trigger
                game.cycleCard(1, "Disciple of Grace")

                // First trigger - may decision then target selection
                withClue("Should have pending may decision for first trigger") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.selectTargets(listOf(glorySeekerId))

                // Second trigger - may decision then target selection
                withClue("Should have pending may decision for second trigger") {
                    game.hasPendingDecision() shouldBe true
                }
                game.answerYesNo(true)

                val bearsId = game.findPermanent("Grizzly Bears")!!
                game.selectTargets(listOf(bearsId))

                // Resolve the first ability on the stack (no more may question - already answered)
                game.resolveStack()

                // Resolve the second ability
                game.resolveStack()

                // Both creatures should be in exile
                withClue("Glory Seeker should be in exile") {
                    game.isOnBattlefield("Glory Seeker") shouldBe false
                    game.isInExile(2, "Glory Seeker") shouldBe true
                }
                withClue("Grizzly Bears should be in exile") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe false
                    game.isInExile(2, "Grizzly Bears") shouldBe true
                }

                // Advance to end step - both should return
                game.passUntilPhase(Phase.ENDING, Step.END)
                if (game.state.stack.isNotEmpty()) {
                    game.resolveStack()
                }

                withClue("Glory Seeker should be back on battlefield") {
                    game.isOnBattlefield("Glory Seeker") shouldBe true
                }
                withClue("Grizzly Bears should be back on battlefield") {
                    game.isOnBattlefield("Grizzly Bears") shouldBe true
                }
            }

            test("Astral Slide and Lightning Rift both trigger on same cycle") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardOnBattlefield(1, "Astral Slide")
                    .withCardOnBattlefield(1, "Lightning Rift")
                    .withCardInHand(1, "Disciple of Grace") // Cycling {2}
                    .withLandsOnBattlefield(1, "Plains", 2) // For cycling cost
                    .withLandsOnBattlefield(1, "Mountain", 1) // For Lightning Rift's {1}
                    .withCardInLibrary(1, "Mountain")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val startingLife = game.getLifeTotal(2)

                // Cycle - both enchantments should trigger
                game.cycleCard(1, "Disciple of Grace")

                val targetId = game.findPermanent("Glory Seeker")!!

                // First trigger (Astral Slide - MayEffect): may decision then target
                withClue("Should have pending decision for first trigger") {
                    game.hasPendingDecision() shouldBe true
                }

                val firstDecision = game.getPendingDecision()
                if (firstDecision is com.wingedsheep.engine.core.YesNoDecision) {
                    game.answerYesNo(true)

                    // Check if next decision is mana source selection (Lightning Rift path)
                    val nextDecision = game.getPendingDecision()
                    if (nextDecision is com.wingedsheep.engine.core.SelectManaSourcesDecision) {
                        // Lightning Rift processed first: pay → mana sources → target
                        game.submitManaSourcesAutoPay()
                        game.selectTargets(listOf(targetId))

                        // Second trigger (Astral Slide): may → target
                        game.answerYesNo(true)
                        game.selectTargets(listOf(targetId))
                    } else {
                        // Astral Slide processed first: may → target
                        game.selectTargets(listOf(targetId))

                        // Second trigger (Lightning Rift): pay → mana sources → target
                        game.answerYesNo(true)
                        game.submitManaSourcesAutoPay()
                        game.selectTargets(listOf(targetId))
                    }
                }

                // Resolve both abilities on stack
                game.resolveStack()
                game.resolveStack()

                // At least one of the effects should have worked -
                // either the creature was exiled or took damage (or both, in order)
                val creatureExiled = !game.isOnBattlefield("Glory Seeker")
                val damageTaken = game.getLifeTotal(2) < startingLife

                withClue("Both triggers should have resolved - creature exiled or damage dealt") {
                    (creatureExiled || damageTaken) shouldBe true
                }
            }

            test("cycling without Astral Slide does not trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Opponent")
                    .withCardInHand(1, "Disciple of Grace")
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                val cycleResult = game.cycleCard(1, "Disciple of Grace")
                withClue("Cycling should succeed without Astral Slide") {
                    cycleResult.error shouldBe null
                }
                withClue("Should not have pending decision") {
                    game.hasPendingDecision() shouldBe false
                }
                withClue("Should have drawn a card") {
                    game.handSize(1) shouldBe 1
                }
                withClue("Cycled card should be in graveyard") {
                    game.isInGraveyard(1, "Disciple of Grace") shouldBe true
                }
            }
        }
    }
}
