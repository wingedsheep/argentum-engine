package com.wingedsheep.gameserver.scenarios

import com.wingedsheep.engine.core.ActivateAbility
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.gameserver.ScenarioTestBase
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Phase
import com.wingedsheep.sdk.core.Step
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe

class TeferiHeroOfDominariaScenarioTest : ScenarioTestBase() {

    private fun TestGame.getLibraryCardNames(playerNumber: Int): List<String> {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        return state.getLibrary(playerId).map { entityId ->
            state.getEntity(entityId)?.get<CardComponent>()?.name ?: "Unknown"
        }
    }

    private fun TestGame.addLoyalty(cardName: String, loyalty: Int) {
        val id = findPermanent(cardName)!!
        state = state.updateEntity(id) { c ->
            val counters = c.get<CountersComponent>() ?: CountersComponent()
            c.with(counters.withAdded(CounterType.LOYALTY, loyalty))
        }
    }

    private fun TestGame.activateLoyaltyAbility(
        playerNumber: Int,
        cardName: String,
        abilityIndex: Int,
        targetIds: List<EntityId> = emptyList()
    ) {
        val playerId = if (playerNumber == 1) player1Id else player2Id
        val permanentId = findPermanent(cardName)!!
        val cardDef = cardRegistry.getCard(cardName)!!
        val ability = cardDef.script.activatedAbilities[abilityIndex]

        val targets = targetIds.map { ChosenTarget.Permanent(it) }

        val result = execute(
            ActivateAbility(
                playerId = playerId,
                sourceId = permanentId,
                abilityId = ability.id,
                targets = targets
            )
        )
        withClue("Loyalty ability activation should succeed: ${result.error}") {
            result.error shouldBe null
        }
    }

    init {
        context("Teferi -3: Put target nonland permanent into library third from top") {

            test("puts target nonland permanent third from top of owner's library") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Teferi, Hero of Dominaria")
                    .withCardOnBattlefield(2, "Glory Seeker") // nonland permanent to tuck
                    .withCardInLibrary(2, "Mountain") // top
                    .withCardInLibrary(2, "Forest")   // second
                    .withCardInLibrary(2, "Swamp")    // third
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Hero of Dominaria", 4)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!

                // -3 is the second loyalty ability (index 1)
                game.activateLoyaltyAbility(1, "Teferi, Hero of Dominaria", 1,
                    listOf(glorySeekerId))
                game.resolveStack()

                // Glory Seeker should no longer be on the battlefield
                game.isOnBattlefield("Glory Seeker") shouldBe false

                // Glory Seeker should be third from top in Player 2's library
                val library = game.getLibraryCardNames(2)
                library.size shouldBe 4 // 3 original + 1 tucked
                library[0] shouldBe "Mountain"     // top
                library[1] shouldBe "Forest"       // second
                library[2] shouldBe "Glory Seeker" // third (tucked)
                library[3] shouldBe "Swamp"        // fourth (was third)
            }

            test("puts card at correct position when library has fewer than 2 cards") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Teferi, Hero of Dominaria")
                    .withCardOnBattlefield(2, "Glory Seeker")
                    .withCardInLibrary(2, "Mountain") // only 1 card in library
                    .withCardInLibrary(1, "Island")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Hero of Dominaria", 4)

                val glorySeekerId = game.findPermanent("Glory Seeker")!!
                game.activateLoyaltyAbility(1, "Teferi, Hero of Dominaria", 1,
                    listOf(glorySeekerId))
                game.resolveStack()

                game.isOnBattlefield("Glory Seeker") shouldBe false

                val library = game.getLibraryCardNames(2)
                library.size shouldBe 2
                library[0] shouldBe "Mountain"
                library[1] shouldBe "Glory Seeker" // at position 1 (coerced from 2)
            }
        }

        context("Teferi +1: Draw a card and delayed untap") {

            test("draws a card and creates delayed trigger") {
                val game = scenario()
                    .withPlayers("Player1", "Player2")
                    .withCardOnBattlefield(1, "Teferi, Hero of Dominaria")
                    .withLandsOnBattlefield(1, "Island", 3)
                    .withLandsOnBattlefield(1, "Plains", 2)
                    .withCardInLibrary(1, "Mountain") // card to draw
                    .withCardInLibrary(1, "Forest")   // safety
                    .withCardInLibrary(2, "Swamp")
                    .withActivePlayer(1)
                    .inPhase(Phase.PRECOMBAT_MAIN, Step.PRECOMBAT_MAIN)
                    .build()

                game.addLoyalty("Teferi, Hero of Dominaria", 3)

                val handSizeBefore = game.handSize(1)

                // +1 is the first loyalty ability (index 0)
                game.activateLoyaltyAbility(1, "Teferi, Hero of Dominaria", 0)
                game.resolveStack()

                // Should have drawn a card
                game.handSize(1) shouldBe handSizeBefore + 1

                // Should have a delayed trigger pending for end step
                game.state.delayedTriggers.size shouldBe 1
            }
        }
    }
}
